import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

// 全局客服实时消息中心
// - 服务端 SSE 推送 chat-message / thread-message 时，实时触发【右上角弹窗 + 声音】
// - 同时将消息事件转发给 CsChat 组件做会话列表实时更新
// - 全局连接在 App.vue 建立，因此客服在任意页面都能收到消息提醒
export const useCsChatStore = defineStore('csChat', () => {
  const unreadCount = ref(0)           // 会话内新收到的未读消息数
  const incomingNotice = ref(null)     // 最近一条新消息，用于右上角浮窗
  const activeView = ref(null)         // 当前正在查看的会话 {type:'conv'|'thread', id}
  const listening = ref(false)

  let eventSource = null
  let audioCtx = null
  let noticeTimer = null
  let reconnectTimer = null
  let reconnectAttempts = 0
  const listeners = {}

  function playSound() {
    try {
      if (!audioCtx) {
        const Ctor = window.AudioContext || window.webkitAudioContext
        if (!Ctor) return
        audioCtx = new Ctor()
      }
      const ctx = audioCtx
      if (ctx.state === 'suspended') ctx.resume().catch(() => {})
      const oscillator = ctx.createOscillator()
      const gain = ctx.createGain()
      oscillator.type = 'sine'
      oscillator.frequency.setValueAtTime(880, ctx.currentTime)
      oscillator.frequency.exponentialRampToValueAtTime(1320, ctx.currentTime + 0.15)
      gain.gain.setValueAtTime(0.0001, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.35)
      oscillator.connect(gain)
      gain.connect(ctx.destination)
      oscillator.start(ctx.currentTime)
      oscillator.stop(ctx.currentTime + 0.35)
    } catch (e) { /* 忽略声音失败 */ }
  }

  function emit(event, data) {
    listeners[event]?.forEach(cb => {
      try { cb(data) } catch (e) { /* 忽略订阅者错误 */ }
    })
  }

  function on(event, callback) {
    if (!listeners[event]) listeners[event] = []
    listeners[event].push(callback)
    return () => off(event, callback)
  }

  function off(event, callback) {
    if (!listeners[event]) return
    listeners[event] = listeners[event].filter(cb => cb !== callback)
  }

  // 是否提醒：非自己发送，且不是当前正在查看的会话
  function shouldNotify(fromUserId, isSelf, threadKey) {
    if (isSelf) return false
    const v = activeView.value
    if (!v) return true
    if (v.type === 'conv') return v.id !== fromUserId
    if (v.type === 'thread') return !threadKey || v.id !== threadKey
    return true
  }

  function triggerNotice(payload) {
    incomingNotice.value = { ...payload, time: Date.now() }
    unreadCount.value += 1
    playSound()
    clearTimeout(noticeTimer)
    noticeTimer = setTimeout(() => { incomingNotice.value = null }, 6000)
  }

  // 统一判定并触发弹窗：仅提醒“他人”发来的新消息
  function maybeNotify(fromUserId, preview, type, threadKey, fromUserName) {
    if (fromUserId == null) return
    const selfId = useAuthStore().user?.id_wsh
    if (fromUserId === selfId) return
    if (!shouldNotify(fromUserId, false, threadKey)) return
    triggerNotice({
      type,
      threadKey,
      userId: fromUserId,
      name: fromUserName || (fromUserId != null ? `用户#${fromUserId}` : '用户'),
      preview: preview || '[图片]',
    })
  }

  // SSE：chat-message 事件 → 转发给 CsChat 列表 + 实时弹窗提醒
  function handleChatMessage(msg) {
    emit('chat-message', msg)
    if (!msg) return
    maybeNotify(
      msg.from_user_id_wsh,
      msg.file_url_wsh ? '[图片]' : msg.content_wsh,
      'conv',
      null,
      msg.from_user_name_wsh,
    )
  }

  // SSE：thread-message 事件 → 转发给 CsChat 列表 + 实时弹窗提醒
  function handleThreadMessage(data) {
    emit('thread-message', data)
    if (!data) return
    const tkey = `${data.type}-${data.biz_id_wsh}`
    maybeNotify(
      data.from_user_id_wsh,
      data.file_url_wsh ? '[图片]' : data.content_wsh,
      'thread',
      tkey,
      data.from_user_name_wsh,
    )
  }

  function connect(force = false) {
    if (listening.value && !force) return
    disconnect(false)
    const authStore = useAuthStore()
    const token = authStore.token
    if (!token) return
    listening.value = true
    try {
      eventSource = new EventSource(`/api/chat-events/stream?token=${encodeURIComponent(token)}`)
      eventSource.addEventListener('chat-message', (event) => {
        try { handleChatMessage(JSON.parse(event.data)) } catch (e) { /* 忽略异常事件 */ }
      })
      eventSource.addEventListener('thread-message', (event) => {
        try { handleThreadMessage(JSON.parse(event.data)) } catch (e) { /* 忽略异常事件 */ }
      })
      eventSource.onerror = () => {
        if (eventSource) { eventSource.close(); eventSource = null }
        listening.value = false
        scheduleReconnect()
      }
    } catch (e) {
      listening.value = false
    }
  }

  function scheduleReconnect() {
    if (reconnectAttempts >= 10) return
    reconnectAttempts++
    const delay = Math.min(2000 * Math.pow(1.5, reconnectAttempts - 1), 30000)
    clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      const authStore = useAuthStore()
      if (authStore.token) connect(true)
    }, delay)
  }

  function disconnect(clearListeners = true) {
    clearTimeout(noticeTimer)
    clearTimeout(reconnectTimer)
    if (eventSource) { eventSource.close(); eventSource = null }
    eventSource = null
    listening.value = false
    if (audioCtx && audioCtx.close) audioCtx.close().catch(() => {})
    audioCtx = null
    if (clearListeners) { Object.keys(listeners).forEach(k => { listeners[k] = [] }) }
  }

  function resetUnread() {
    unreadCount.value = 0
    incomingNotice.value = null
    clearTimeout(noticeTimer)
  }

  function setActive(type, id) { activeView.value = { type, id } }
  function clearActive() { activeView.value = null }
  function dismissNotice() { incomingNotice.value = null }

  return {
    unreadCount, incomingNotice, listening,
    on, off, connect, disconnect, resetUnread, dismissNotice, setActive, clearActive,
  }
})