<template>
  <div>
    <PageHero title="我的评价" subtitle="查看和管理您的评价" />
    <button class="btn btn-primary" style="margin-bottom:24px" @click="openSubmit">+ 发表评价</button>
    <div v-if="loading" class="loading">加载中...</div>
    <EmptyState v-else-if="ratings.length === 0" title="暂无评价" icon="🔍" />
    <div v-else v-for="r in ratings" :key="r.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between">
        <div style="font-weight:600">{{ r.serviceName || r.serviceType }} <span style="color:gold;margin-left:8px">{{ '⭐'.repeat(Math.floor(r.score_wsh)) }}</span></div>
        <span style="font-size:12px;color:var(--color-muted-foreground)">{{ new Date(r.created_at_wsh).toLocaleDateString() }}</span>
      </div>
      <p style="margin-top:8px;font-size:14px">{{ r.content_wsh || '暂无评价内容' }}</p>
      <div v-if="r.reply_wsh" style="margin-top:8px;padding:8px;background:var(--color-muted);border-radius:8px;font-size:13px">
        <strong>回复：</strong>{{ r.reply_wsh }}
      </div>
      <div style="margin-top:8px">
        <button v-if="!r.reply_wsh && replyFormId !== r.id_wsh && (authStore.isMerchant || authStore.hasRole('KEEPER'))" class="btn btn-sm btn-outline" @click="replyFormId = r.id_wsh; replyText = ''">回复</button>
        <div v-if="replyFormId === r.id_wsh" style="display:flex;gap:8px;margin-top:8px">
          <textarea v-model="replyText" rows="2" class="form-control" placeholder="输入回复..." style="flex:1"></textarea>
          <button class="btn btn-sm btn-primary" @click="submitReply(r)">提交</button>
          <button class="btn btn-sm btn-outline" @click="replyFormId = null">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>发表评价</h2>
        <form @submit.prevent="submitRating">
          <div class="form-group">
            <label>评分</label>
            <div style="display:flex;gap:4px;margin-top:4px">
              <span v-for="i in 5" :key="i" style="cursor:pointer;font-size:24px" @click="form.score_wsh = i">{{ i <= form.score_wsh ? '⭐' : '☆' }}</span>
            </div>
          </div>
          <div class="form-group">
            <label>评价内容</label>
            <textarea v-model="form.content_wsh" rows="3" required></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">提交</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyRatings, createRating, replyToRating } from '@/api/rating'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const ratings = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ score_wsh: 5, content_wsh: '' })
const replyFormId = ref(null)
const replyText = ref('')

function openSubmit() { form.score_wsh = 5; form.content_wsh = ''; showForm.value = true }

onMounted(async () => {
  try { const r = await getMyRatings(); if (r.code === 200) ratings.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function submitRating() {
  try {
    const r = await createRating(form)
    if (r.code === 200) {
      appStore.addToast('评价成功', 'success')
      showForm.value = false
      ratings.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}

async function submitReply(r) {
  if (!replyText.value.trim()) return appStore.addToast('请输入回复内容', 'error')
  try {
    const res = await replyToRating(r.id_wsh, replyText.value)
    if (res.code === 200) { appStore.addToast('回复成功', 'success'); r.reply_wsh = replyText.value; replyFormId.value = null; replyText.value = '' }
  } catch (e) { appStore.addToast('回复失败', 'error') }
}
</script>
