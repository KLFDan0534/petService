<template>
  <div v-if="currentNotice" class="popup-overlay" @mousedown.self="dismiss(currentIndex)">
    <div class="popup-card" role="dialog" aria-modal="true" aria-labelledby="popup-notice-title">
      <div class="popup-header">
        <div>
          <span class="popup-badge">公告</span>
          <h3 id="popup-notice-title">{{ currentNotice.title_wsh }}</h3>
        </div>
        <button class="popup-close" type="button" aria-label="关闭公告" @click="dismiss(currentIndex)">&times;</button>
      </div>
      <div class="popup-body">{{ currentNotice.content_wsh }}</div>
      <div class="popup-footer">
        <span class="popup-counter" v-if="popups.length > 1">{{ currentIndex + 1 }} / {{ popups.length }}</span>
        <span v-else></span>
        <button class="btn btn-primary btn-sm" type="button" @click="dismiss(currentIndex)">{{ popups.length > 1 ? '下一条' : '我知道了' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getActiveNotices, getPopupNotices, dismissPopup } from '@/api/notice'

const authStore = useAuthStore()
// 攒着这一轮要弹的公告，currentIndex 是弹到第几条
const popups = ref([])
const currentIndex = ref(0)
// 切登录状态会连着发两次请求，用这个号把先回来的旧结果丢掉
let requestId = 0

// 取当前这一条，没了就是 null，模板靠它决定弹不弹
const currentNotice = computed(() => popups.value[currentIndex.value] || null)

// 只有"类型是公告"且"投放方式带 popup"的才弹窗，其它公告不进这里
function isPopupNotice(notice) {
  return String(notice?.type_wsh || '').toLowerCase() === 'notice'
    && String(notice?.delivery_type_wsh || '').toLowerCase().includes('popup')
}

// 没登录的人后端记不了已读，就在浏览器里按公告 id 各记一个标记
function publicReadKey(id) {
  return `notice_read_public_${id}`
}

// 登录了就问后端要"我还没读过的"，没登录就拉全部再自己筛
async function loadPopupNotices() {
  const currentRequestId = ++requestId
  try {
    const loggedIn = authStore.isLoggedIn
    const r = loggedIn
      ? await getPopupNotices()
      : await getActiveNotices({ type: 'notice', _t: Date.now() })
    if (currentRequestId !== requestId) return
    const notices = Array.isArray(r.data) ? r.data : []
    if (r.code === 200 && notices.length > 0) {
      // 登录时后端已经筛过一遍，没登录还得把本地记过已读的去掉
      const items = loggedIn
        ? notices.filter(isPopupNotice)
        : notices.filter(n =>
            isPopupNotice(n) &&
            !localStorage.getItem(publicReadKey(n.id_wsh)),
          )
      popups.value = items
      currentIndex.value = 0
      return
    }
    popups.value = []
    currentIndex.value = 0
  } catch (e) {}
}

// 登录状态一变就重新拉一次，immediate 让刚进页面时也跑一遍
watch(
  () => authStore.isLoggedIn,
  () => {
    loadPopupNotices()
  },
  { immediate: true },
)

// 关掉一条：登录的把已读报给后端，没登录的写进本地，下次就不弹了
async function dismiss(idx) {
  const notice = popups.value[idx]
  if (!notice) return
  const id = notice.id_wsh
  try {
    if (authStore.isLoggedIn) {
      await dismissPopup(id)
    } else {
      localStorage.setItem(publicReadKey(id), '1')
    }
  } catch (e) {}
  if (popups.value.length === 1) {
    popups.value = []
    currentIndex.value = 0
  } else {
    popups.value.splice(idx, 1)
    if (currentIndex.value >= popups.value.length) {
      currentIndex.value = Math.max(0, popups.value.length - 1)
    }
  }
}
</script>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.popup-card {
  background: var(--color-card, #fff);
  color: var(--color-card-foreground, #111827);
  border-radius: 8px;
  max-width: 480px;
  width: 90%;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
  overflow: hidden;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px 0;
}
.popup-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: var(--radius-pill);
  background: var(--color-primary, #2563eb);
  color: var(--color-on-primary, #fff);
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
}
.popup-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.popup-close {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #999;
  padding: 0 4px;
}
.popup-body {
  padding: 14px 20px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground, #555);
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
.popup-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 18px;
}
.popup-counter {
  font-size: 12px;
  color: #aaa;
}
</style>
