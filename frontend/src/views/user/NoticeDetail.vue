<template>
  <div>
    <PageHero :title="notice?.title_wsh || '公告详情'" subtitle="" />
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!notice" class="empty-state">
      <p>公告不存在或已删除</p>
      <button class="btn btn-primary btn-sm" style="margin-top:12px" @click="router.back()">返回</button>
    </div>
    <div v-else class="card" style="max-width:720px;margin:0 auto;padding:24px">
      <div style="font-size:12px;color:var(--color-muted-foreground);margin-bottom:16px">
        {{ new Date(notice.created_at_wsh).toLocaleString() }}
      </div>
      <div style="font-size:15px;line-height:1.8;white-space:pre-wrap">{{ notice.content_wsh }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeById } from '@/api/notice'
import PageHero from '@/components/common/PageHero.vue'

const route = useRoute()
const router = useRouter()
const notice = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const r = await getNoticeById(route.params.id)
    if (r.code === 200) notice.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})
</script>
