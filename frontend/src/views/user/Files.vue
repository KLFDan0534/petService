<template>
  <div class="fl-page">
    <div class="fl-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="fl-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="fl-crumb-link">首页</router-link>
        <span class="fl-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="fl-crumb-link">个人中心</router-link>
        <span class="fl-crumb-sep" aria-hidden="true">›</span>
        <span class="fl-crumb-here">我的文件</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="fl-head">
        <div class="fl-head-copy">
          <div class="fl-eyebrow" aria-hidden="true">
            <span class="fl-eyebrow-line"></span>
            <span>Asset Library</span>
          </div>
          <h1 class="fl-title">我的文件</h1>
          <p class="fl-sub">上传过的照片与证明材料都在这里，可以随时下载或删除。</p>
          <dl class="fl-facts" aria-label="文件概览">
            <div class="fl-fact">
              <dd class="tabular">{{ loading ? '--' : files.length }}</dd>
              <dt>文件数量</dt>
            </div>
            <div class="fl-fact">
              <dd class="tabular">{{ loading ? '--' : formatBytes(totalBytes) }}</dd>
              <dt>占用空间</dt>
            </div>
          </dl>
        </div>
        <div class="fl-actions">
          <input ref="fileInput" type="file" class="fl-hidden-input" @change="onPickFile">
          <button type="button" class="cta cta-primary" :disabled="uploading" @click="triggerUpload">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
            </svg>
            {{ uploading ? '上传中…' : '上传文件' }}
          </button>
        </div>
      </header>

      <!-- ═══ 01 · Library ═══ -->
      <section class="fl-section" aria-label="全部文件">
        <header class="fl-sec-head">
          <div class="fl-head-copy">
            <p class="fl-eyebrow fl-sec-eyebrow">
              <span class="fl-idx">01</span>
              <span class="fl-line" aria-hidden="true"></span>
              <span>Library</span>
            </p>
            <h2 class="fl-sec-title">全部文件</h2>
          </div>
        </header>

        <div class="fl-toolbar">
          <div class="fl-kinds" role="tablist" aria-label="文件类型">
            <button
              type="button"
              role="tab"
              :aria-selected="kind === 'all'"
              class="fl-chip"
              :class="{ active: kind === 'all' }"
              @click="kind = 'all'"
            >全部 <span class="fl-count tabular">{{ counts.all }}</span></button>
            <button
              type="button"
              role="tab"
              :aria-selected="kind === 'image'"
              class="fl-chip"
              :class="{ active: kind === 'image' }"
              @click="kind = 'image'"
            >图片 <span class="fl-count tabular">{{ counts.image }}</span></button>
            <button
              type="button"
              role="tab"
              :aria-selected="kind === 'document'"
              class="fl-chip"
              :class="{ active: kind === 'document' }"
              @click="kind = 'document'"
            >文档 <span class="fl-count tabular">{{ counts.document }}</span></button>
            <button
              type="button"
              role="tab"
              :aria-selected="kind === 'other'"
              class="fl-chip"
              :class="{ active: kind === 'other' }"
              @click="kind = 'other'"
            >其他 <span class="fl-count tabular">{{ counts.other }}</span></button>
          </div>

          <div class="fl-views" role="group" aria-label="视图切换">
            <button
              type="button"
              :aria-pressed="view === 'grid'"
              aria-label="网格"
              class="fl-view"
              :class="{ active: view === 'grid' }"
              @click="view = 'grid'"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
              </svg>
            </button>
            <button
              type="button"
              :aria-pressed="view === 'list'"
              aria-label="列表"
              class="fl-view"
              :class="{ active: view === 'list' }"
              @click="view = 'list'"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" /><line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 加载骨架 -->
        <div v-if="loading" class="fl-grid">
          <div v-for="i in 6" :key="i" class="fl-skeleton" />
        </div>

        <!-- 空态 -->
        <div v-else-if="!visible.length" class="fl-empty">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
          </svg>
          <h3>{{ kind === 'all' ? '还没有上传任何文件' : '这个分类下暂无文件' }}</h3>
          <p>宠物照片、疫苗证明、投诉证据都可以上传到这里，方便下单和售后时随时取用。</p>
          <button type="button" class="cta cta-outline" @click="triggerUpload">上传第一个文件</button>
        </div>

        <!-- 网格视图 -->
        <ul v-else-if="view === 'grid'" class="fl-grid">
          <li v-for="(file, index) in visible" :key="file.id_wsh ?? index" class="fl-card">
            <button v-if="fileKind(file) === 'image' && file.url_wsh" type="button" class="fl-thumb" aria-label="查看图片" @click="openImage(file)">
              <img :src="file.url_wsh" :alt="file.original_name_wsh || '文件预览'" loading="lazy" @error="$event.target.style.display = 'none'">
              <span v-if="file.url_wsh" class="fl-thumb-zoom">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 3h6v6" /><path d="M10 14 21 3" /><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg>
              </span>
            </button>
            <div v-else class="fl-thumb fl-thumb-icon">
              <svg v-if="fileKind(file) === 'document'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
              </svg>
            </div>
            <div class="fl-card-body">
              <p class="fl-name" :title="file.original_name_wsh || `文件 #${file.id_wsh}`">{{ file.original_name_wsh || `文件 #${file.id_wsh}` }}</p>
              <p class="fl-meta tabular">{{ formatBytes(file.size_wsh) }} · {{ dateText(file.created_at_wsh) || '—' }}</p>
              <div class="fl-card-actions">
                <button type="button" class="cta cta-outline cta-sm" :disabled="busyId === file.id_wsh" @click="download(file)">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" />
                  </svg>
                  {{ busyId === file.id_wsh ? '下载中' : '下载' }}
                </button>
                <button type="button" class="cta cta-quiet cta-sm" @click="pendingDelete = file">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                  删除
                </button>
              </div>
            </div>
          </li>
        </ul>

        <!-- 列表视图 -->
        <ul v-else class="fl-list">
          <li v-for="(file, index) in visible" :key="file.id_wsh ?? index" class="fl-row">
            <span class="fl-row-icon" aria-hidden="true">
              <svg v-if="fileKind(file) === 'document'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" /></svg>
            </span>
            <div class="fl-row-main">
              <p class="fl-name" :title="file.original_name_wsh || `文件 #${file.id_wsh}`">{{ file.original_name_wsh || `文件 #${file.id_wsh}` }}</p>
              <p class="fl-meta tabular">{{ formatBytes(file.size_wsh) }} · {{ dateText(file.created_at_wsh) || '—' }}</p>
            </div>
            <div class="fl-row-actions">
              <button type="button" class="cta cta-outline cta-sm" :disabled="busyId === file.id_wsh" @click="download(file)">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" /></svg>
                {{ busyId === file.id_wsh ? '下载中' : '下载' }}
              </button>
              <button type="button" class="cta cta-quiet cta-sm" @click="pendingDelete = file">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></svg>
                删除
              </button>
            </div>
          </li>
        </ul>
      </section>
    </div>

    <!-- ═══ 图片预览 ═══ -->
    <div v-if="previewUrl" class="fl-overlay" @mousedown.self="previewUrl = ''">
      <div class="fl-preview" role="dialog" aria-modal="true" aria-label="图片预览">
        <button type="button" class="fl-preview-close" aria-label="关闭" @click="previewUrl = ''">✕</button>
        <img :src="previewUrl" alt="图片预览">
      </div>
    </div>

    <!-- ═══ 删除确认 ═══ -->
    <div v-if="pendingDelete" class="fl-overlay" @mousedown.self="pendingDelete = null">
      <div class="fl-confirm" role="dialog" aria-modal="true" aria-labelledby="fl-confirm-title">
        <h3 id="fl-confirm-title" class="fl-confirm-title">删除这个文件？</h3>
        <p class="fl-confirm-desc">删除后无法恢复。如果这个文件被订单或投诉引用，相关记录中将不再能打开它。</p>
        <p class="fl-confirm-detail tabular">{{ pendingDelete.original_name_wsh || `文件 #${pendingDelete.id_wsh}` }}</p>
        <div class="fl-confirm-actions">
          <button type="button" class="cta cta-outline" @click="pendingDelete = null">取消</button>
          <button type="button" class="cta cta-danger" :disabled="busyId === pendingDelete.id_wsh" @click="confirmDelete">
            {{ busyId === pendingDelete.id_wsh ? '删除中…' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getFiles, uploadFile, downloadFile, deleteFile } from '@/api/file'

const appStore = useAppStore()
const files = ref([])
const loading = ref(true)
const uploading = ref(false)
const busyId = ref(null)
const fileInput = ref(null)
const view = ref('grid')
const kind = ref('all')
const pendingDelete = ref(null)
const previewUrl = ref('')

const IMAGE_EXT = /\.(jpe?g|png|gif|webp|bmp|svg|avif)$/i
const DOC_EXT = /\.(pdf|docx?|xlsx?|pptx?|txt|md|csv)$/i

function fileKind(file) {
  const name = file.original_name_wsh || file.url_wsh || ''
  if (IMAGE_EXT.test(name)) return 'image'
  if (DOC_EXT.test(name)) return 'document'
  if (file.mime_type_wsh && String(file.mime_type_wsh).startsWith('image/')) return 'image'
  return 'other'
}

const counts = computed(() => {
  const base = { all: files.value.length, image: 0, document: 0, other: 0 }
  files.value.forEach((f) => { base[fileKind(f)] += 1 })
  return base
})

const totalBytes = computed(() => files.value.reduce((sum, f) => sum + (Number(f.size_wsh) || 0), 0))

const visible = computed(() => {
  if (kind.value === 'all') return files.value
  return files.value.filter((f) => fileKind(f) === kind.value)
})

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function dateText(value) {
  if (!value) return ''
  try {
    return new Date(value).toLocaleDateString('zh-CN')
  } catch (e) {
    return String(value)
  }
}

function openImage(file) {
  if (file.url_wsh) previewUrl.value = file.url_wsh
}
function triggerUpload() {
  fileInput.value?.click()
}

async function onPickFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || uploading.value) return
  uploading.value = true
  const fd = new FormData()
  fd.append('file', file)
  try {
    const r = await uploadFile(fd)
    if (r.code === 200) {
      files.value.push(r.data)
      appStore.addToast('上传成功', 'success')
    } else {
      appStore.addToast(r.message || '上传失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

async function download(file) {
  if (busyId.value) return
  busyId.value = file.id_wsh
  try {
    const res = await downloadFile(file.id_wsh)
    const disposition = res.headers?.['content-disposition'] || ''
    const match = disposition && disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i)
    const filename = match ? decodeURIComponent(match[1]) : (file.original_name_wsh || `file-${file.id_wsh}`)
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    appStore.addToast('下载失败：' + (e?.message || '权限不足或文件不存在'), 'error')
  } finally {
    busyId.value = null
  }
}

async function confirmDelete() {
  const target = pendingDelete.value
  if (!target || busyId.value) return
  busyId.value = target.id_wsh
  try {
    const r = await deleteFile(target.id_wsh)
    if (r.code === 200) {
      files.value = files.value.filter((f) => f.id_wsh !== target.id_wsh)
      appStore.addToast('文件已删除', 'success')
    } else {
      appStore.addToast(r.message || '删除失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '删除失败', 'error')
  } finally {
    busyId.value = null
    pendingDelete.value = null
  }
}

onMounted(async () => {
  try {
    const r = await getFiles()
    if (r.code === 200) files.value = r.data || []
  } catch (e) {
    appStore.addToast('文件加载失败', 'error')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.fl-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.fl-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
.fl-hidden-input { display: none; }

/* Breadcrumb */
.fl-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.fl-crumb-link { color: var(--ref-muted); text-decoration: none; }
.fl-crumb-link:hover { color: var(--ref-ink); }
.fl-crumb-sep { color: var(--ref-line); }
.fl-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.fl-eyebrow { display: flex; align-items: center; gap: 10px; font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.fl-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.fl-idx { font-variant-numeric: tabular-nums; }
.fl-line { width: 24px; height: 1px; background: var(--ref-line); }

/* Hero */
.fl-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 20px 24px; padding: 40px 0 28px; }
.fl-head-copy { min-width: 0; }
.fl-title { margin: 18px 0 0; font-family: var(--ref-font-display); font-size: clamp(34px, 4.4vw, 52px); line-height: 1.12; letter-spacing: -0.01em; font-weight: 500; color: var(--ref-ink); text-wrap: balance; }
.fl-sub { margin: 14px 0 0; max-width: 640px; font-size: 14px; line-height: 1.75; color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent); }
.fl-facts { display: flex; flex-wrap: wrap; gap: 0 28px; margin: 26px 0 0; }
.fl-fact { display: flex; flex-direction: column; }
.fl-fact + .fl-fact { border-left: 1px solid var(--ref-line); padding-left: 28px; }
.fl-fact dd { margin: 0; font-family: var(--ref-font-display); font-size: 28px; line-height: 1; letter-spacing: -0.01em; color: var(--ref-ink); }
.fl-fact dt { margin-top: 8px; font-size: 12px; color: var(--ref-muted); }
.fl-actions { flex-shrink: 0; margin-bottom: 6px; }

/* Section */
.fl-section { margin-top: 40px; }
.fl-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.fl-sec-title { margin: 10px 0 0; font-family: var(--ref-font-display); font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink); }

/* Toolbar */
.fl-toolbar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin-top: 22px; }
.fl-kinds { display: flex; flex-wrap: wrap; gap: 8px; }
.fl-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.fl-chip:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: var(--ref-sand); }
.fl-chip.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }
.fl-count { opacity: 0.7; font-variant-numeric: tabular-nums; }
.fl-views { display: flex; gap: 6px; }
.fl-view {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-muted);
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.fl-view:hover { color: var(--ref-ink); }
.fl-view.active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-cream); }

/* Grid */
.fl-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 20px; list-style: none; margin-block: 20px 0; padding: 0; }
.fl-card { display: flex; flex-direction: column; min-width: 0; overflow: hidden; border: 1px solid var(--ref-line); border-radius: 16px; background: var(--ref-surface); transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s; }
.fl-card:hover { border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent); transform: translateY(-3px); box-shadow: 0 28px 60px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent); }
.fl-thumb { position: relative; display: flex; align-items: center; justify-content: center; width: 100%; aspect-ratio: 4 / 3; overflow: hidden; padding: 0; background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface)); border: none; cursor: zoom-in; }
.fl-thumb-icon { background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface)); color: var(--ref-muted); }
.fl-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.fl-thumb-zoom {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}
.fl-thumb:hover .fl-thumb-zoom { opacity: 1; }
.fl-card-body { flex: 1; display: flex; flex-direction: column; padding: 14px 16px; }
.fl-name { margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13.5px; font-weight: 500; color: var(--ref-ink); }
.fl-meta { margin: 6px 0 0; font-size: 11.5px; color: var(--ref-muted); }
.fl-card-actions { display: flex; gap: 6px; margin-top: auto; padding-top: 14px; }

/* List */
.fl-list { margin-top: 20px; padding: 0; list-style: none; overflow: hidden; border: 1px solid var(--ref-line); border-radius: 16px; background: var(--ref-surface); }
.fl-row { display: flex; align-items: center; gap: 14px; padding: 14px 20px; border-bottom: 1px solid var(--ref-line); }
.fl-row:last-child { border-bottom: 0; }
.fl-row-icon { display: flex; align-items: center; justify-content: center; width: 38px; height: 38px; flex-shrink: 0; border-radius: 50%; background: var(--ref-sand); color: var(--ref-brand); }
.fl-row-main { min-width: 0; flex: 1; }
.fl-row-actions { display: flex; gap: 6px; flex-shrink: 0; }

/* Skeleton */
.fl-skeleton { height: 220px; border: 1px solid var(--ref-line); border-radius: 16px; background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%); background-size: 200% 100%; animation: fl-shimmer 1.3s linear infinite; }

/* Empty */
.fl-empty { margin-top: 20px; padding: 56px 24px; border: 1px dashed var(--ref-line); border-radius: 16px; background: var(--ref-surface); text-align: center; }
.fl-empty svg { color: var(--ref-brand); }
.fl-empty h3 { margin: 16px 0 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.fl-empty p { margin: 10px auto 0; max-width: 440px; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.fl-empty .cta { margin-top: 22px; }

/* CTA */
.cta { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 42px; padding: 0 18px; border-radius: 11px; font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent; text-decoration: none; transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s; }
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-sm { height: 36px; padding: 0 13px; font-size: 12.5px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-quiet { background: transparent; color: var(--ref-ink-soft); border-color: transparent; }
.cta-quiet:hover { background: color-mix(in srgb, var(--ref-cream) 60%, transparent); color: var(--ref-ink); }
.cta-danger { background: color-mix(in srgb, var(--color-danger, #ef4444) 90%, transparent); color: #fff; }
.cta-danger:hover:not(:disabled) { background: var(--color-danger, #ef4444); }

/* Overlay / Image preview / Confirm */
.fl-overlay { position: fixed; inset: 0; z-index: 2000; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(10, 8, 6, 0.55); backdrop-filter: blur(2px); animation: fl-fade 0.15s ease; }
.fl-preview { position: relative; max-width: min(90vw, 880px); max-height: 90vh; }
.fl-preview img { max-width: 100%; max-height: 90vh; border-radius: 16px; display: block; }
.fl-preview-close {
  position: absolute; top: -14px; right: -14px; display: flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 50%; border: none; background: var(--ref-surface); color: var(--ref-ink); box-shadow: 0 8px 24px color-mix(in srgb, var(--ref-ink) 30%, transparent); cursor: pointer; font-size: 14px;
}
.fl-confirm { width: 100%; max-width: 420px; padding: 24px; border-radius: 18px; background: var(--ref-surface); border: 1px solid var(--ref-line); box-shadow: 0 40px 80px -40px color-mix(in srgb, var(--ref-ink) 60%, transparent); }
.fl-confirm-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.fl-confirm-desc { margin: 12px 0 0; font-size: 13.5px; line-height: 1.75; color: var(--ref-muted); }
.fl-confirm-detail { margin: 14px 0 0; padding: 12px 14px; border-radius: 10px; background: color-mix(in srgb, var(--ref-sand) 45%, transparent); font-size: 13px; font-weight: 500; color: var(--ref-ink-soft); overflow-wrap: anywhere; }
.fl-confirm-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }

/* Animations */
@keyframes fl-shimmer { to { background-position: -200% 0; } }
@keyframes fl-fade { from { opacity: 0; } to { opacity: 1; } }

/* Responsive */
@media (max-width: 900px) { .fl-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) {
  .fl-shell { padding: 0 16px; }
  .fl-head { padding: 30px 0 22px; }
  .fl-sub { font-size: 13.5px; }
  .fl-section { margin-top: 32px; }
  .fl-grid { grid-template-columns: 1fr; }
  .fl-row-actions { flex-direction: column; }
}
@media (prefers-reduced-motion: reduce) { .fl-skeleton { animation: none; } }
</style>