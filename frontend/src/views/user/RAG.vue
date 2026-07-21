<template>
  <div>
    <PageHero title="知识库" subtitle="宠物护理智能检索" />
    <div style="margin-bottom:24px;display:flex;gap:12px">
      <input v-model="query" placeholder="搜索宠物护理知识..." style="flex:1" @keyup.enter="search">
      <button class="btn btn-primary" @click="search">搜索</button>
      <button v-if="authStore.isAdmin" class="btn btn-outline" @click="showDocForm = !showDocForm">新增文档</button>
    </div>

    <div v-if="showDocForm" class="card" style="margin-bottom:16px;padding:20px">
      <h3 style="margin-bottom:16px">新增文档</h3>
      <form @submit.prevent="createDocument">
        <div class="form-group"><label>标题</label><input v-model="docForm.title_wsh" required></div>
        <div class="form-group"><label>内容</label><textarea v-model="docForm.content_wsh" rows="5" required></textarea></div>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary btn-sm" @click="showDocForm = false">取消</button>
          <button type="submit" class="btn btn-primary btn-sm">提交</button>
        </div>
      </form>
    </div>
    <div v-if="loading" class="loading">搜索中...</div>
    <div v-else-if="query && results.length === 0" class="empty-state">
      <div class="icon">🔍</div>
      <h3>未找到结果</h3>
    </div>
    <div v-else v-for="r in results" :key="r.id_wsh" class="card" style="margin-bottom:12px">
      <h3 style="font-size:16px;margin-bottom:8px">{{ r.title_wsh }}</h3>
      <p style="font-size:14px;color:var(--color-muted-foreground)">{{ r.content_wsh }}</p>
      <div v-if="r.score_wsh !== undefined" style="font-size:12px;color:var(--color-muted-foreground);margin-top:8px">
        相关性: {{ (r.score_wsh * 100).toFixed(0) }}%
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { searchRag, createRagDocument } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const query = ref('')
const results = ref([])
const loading = ref(false)
const showDocForm = ref(false)
const docForm = reactive({ title_wsh: '', content_wsh: '' })

async function search() {
  if (!query.value.trim()) return
  loading.value = true
  try {
    const r = await searchRag({ query: query.value })
    if (r.code === 200) results.value = r.data
  } catch (e) {}
  loading.value = false
}

async function createDocument() {
  try {
    const r = await createRagDocument({ ...docForm })
    if (r.code === 200) {
      appStore.addToast('文档添加成功', 'success')
      showDocForm.value = false
      Object.assign(docForm, { title_wsh: '', content_wsh: '' })
    }
  } catch (e) { appStore.addToast('添加失败', 'error') }
}
</script>
