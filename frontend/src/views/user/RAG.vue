<template>
  <div class="rg-page">
    <div class="rg-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="rg-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="rg-crumb-link">首页</router-link>
        <span class="rg-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/ai" class="rg-crumb-link">AI 助手</router-link>
        <span class="rg-crumb-sep" aria-hidden="true">›</span>
        <span class="rg-crumb-here">照护知识库</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="rg-head">
        <div class="rg-head-copy">
          <div class="rg-eyebrow" aria-hidden="true">
            <span class="rg-eyebrow-line"></span>
            <span>知识库</span>
          </div>
          <h1 class="rg-title">照护知识库</h1>
          <p class="rg-sub">服务标准、退改规则与应激处置手册的原文都在这里。答案基于制度文档生成，可点开原文核对。</p>

          <p v-if="canViewDocs" class="rg-facts">
            <span class="rg-fact"><span class="rg-fact-num tabular">{{ docCount }}</span><span class="rg-fact-label">收录文档</span></span>
          </p>
        </div>
        <div class="rg-actions">
          <button v-if="authStore.isAdmin" type="button" class="cta cta-outline" @click="toggleDocForm">
            {{ showDocForm ? '收起表单' : '新增文档' }}
          </button>
          <router-link to="/ai/chat" class="cta cta-primary">改为对话提问</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 提问 ═══ -->
      <section class="rg-section" aria-label="提问">
        <header class="rg-sec-head">
          <div class="rg-head-copy">
            <p class="rg-eyebrow rg-sec-eyebrow">
              <span class="rg-idx">01</span>
              <span class="rg-line" aria-hidden="true"></span>
              <span>问答</span>
            </p>
            <h2 class="rg-sec-title">提问</h2>
            <p class="rg-sec-desc">回答会附上引用的制度文档，可逐条核对。</p>
          </div>
        </header>

        <div class="rg-ask">
          <form class="rg-ask-bar" @submit.prevent="ask">
            <div class="rg-ask-field">
              <svg class="rg-ask-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
              </svg>
              <input
                v-model.trim="question"
                type="text"
                maxlength="200"
                placeholder="例如：提前一天取消能退多少钱？"
                class="rg-ask-input"
                @keyup.enter="ask"
              >
            </div>
            <button type="submit" class="cta cta-primary" :disabled="asking || !question">
              {{ asking ? '查一下…' : '查一下' }}
            </button>
          </form>

          <div class="rg-chips">
            <button
              v-for="q in suggestions"
              :key="q"
              type="button"
              class="rg-chip"
              :disabled="asking"
              @click="useSuggestion(q)"
            >
              {{ q }}
            </button>
          </div>

          <div v-if="answer" class="rg-answer">
            <p class="rg-answer-label">回答</p>
            <p class="rg-answer-text">{{ answerText }}</p>

            <div v-if="answerNeedHuman" class="rg-handoff">
              <p class="rg-handoff-text">这个问题需要人工确认，建议转人工客服处理。</p>
              <router-link to="/chat" class="cta cta-outline cta-sm">转人工客服</router-link>
            </div>

            <div v-if="answerSources.length" class="rg-sources">
              <p class="rg-sources-label">引用文档</p>
              <ul class="rg-sources-list">
                <li v-for="(src, index) in answerSources" :key="index" class="rg-source">
                  <span class="rg-source-index tabular">{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="rg-source-title">{{ src }}</span>
                </li>
              </ul>
              <router-link to="/ai/chat" class="rg-sources-link">去对话里继续追问 →</router-link>
            </div>
          </div>
        </div>
      </section>

      <!-- ═══ 01b · 新增文档（管理员） ═══ -->
      <section v-if="showDocForm" class="rg-section" aria-label="新增文档">
        <header class="rg-sec-head">
          <div class="rg-head-copy">
            <p class="rg-eyebrow rg-sec-eyebrow">
              <span class="rg-idx">＋</span>
              <span class="rg-line" aria-hidden="true"></span>
              <span>添加</span>
            </p>
            <h2 class="rg-sec-title">新增文档</h2>
            <p class="rg-sec-desc">录入一条制度原文，提交后会进入知识库检索范围。</p>
          </div>
        </header>

        <form class="rg-form" @submit.prevent="createDocument">
          <label class="rg-field">
            <span class="rg-label">标题<span class="rg-req">*</span></span>
            <input v-model.trim="docForm.title_wsh" type="text" maxlength="120" placeholder="例如：寄养取消与退款规则" class="rg-input" required>
          </label>
          <label class="rg-field">
            <span class="rg-label">内容<span class="rg-req">*</span></span>
            <textarea v-model.trim="docForm.content_wsh" rows="6" maxlength="8000" placeholder="制度原文内容" class="rg-textarea" required></textarea>
          </label>
          <div class="rg-form-actions">
            <button type="button" class="cta cta-outline" @click="toggleDocForm">取消</button>
            <button type="submit" class="cta cta-primary" :disabled="docSaving">
              {{ docSaving ? '提交中…' : '提交' }}
            </button>
          </div>
        </form>
      </section>

      <!-- ═══ 02 · 收录文档 ═══ -->
      <section class="rg-section" aria-label="收录文档">
        <header class="rg-sec-head">
          <div class="rg-head-copy">
            <p class="rg-eyebrow rg-sec-eyebrow">
              <span class="rg-idx">02</span>
              <span class="rg-line" aria-hidden="true"></span>
              <span>文档</span>
            </p>
            <h2 class="rg-sec-title">收录文档</h2>
            <p class="rg-sec-desc">平台制度与作业手册原文，可直接查阅。</p>
          </div>
        </header>

        <div v-if="!canViewDocs" class="rg-docs-note">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          <div class="rg-docs-note-copy">
            <p class="rg-docs-note-title">知识库原文对门店与平台人员开放</p>
            <p class="rg-docs-note-desc">制度文档全文面向门店与平台人员；普通用户可以在上方「提问」，随时获取制度相关摘要与结论。</p>
          </div>
        </div>

        <template v-else>
          <div v-if="docsLoading" class="rg-skeletons">
            <div v-for="i in 4" :key="i" class="rg-skeleton"></div>
          </div>

          <div v-else-if="docsError && !documents.length" class="rg-docs-state">
            <p class="rg-docs-state-title">知识库加载失败</p>
            <p class="rg-docs-state-desc">{{ docsError }}</p>
            <button type="button" class="cta cta-outline" @click="loadDocuments">重新加载</button>
          </div>

          <div v-else-if="!documents.length" class="rg-docs-state">
            <p class="rg-docs-state-title">知识库暂时为空</p>
            <p class="rg-docs-state-desc">制度文档正在整理，稍后再来查阅。</p>
            <router-link to="/ai/chat" class="cta cta-outline">改为对话提问</router-link>
          </div>

          <ul v-else class="rg-docs">
            <li v-for="doc in documents" :key="doc.id_wsh" class="rg-doc">
              <div class="rg-doc-top">
                <h3 class="rg-doc-title">{{ doc.title_wsh || `文档 #${doc.id_wsh}` }}</h3>
                <span class="rg-doc-meta">
                  <span v-if="doc.category_wsh" class="badge badge-info">{{ doc.category_wsh }}</span>
                  <span class="rg-doc-date tabular">{{ formatDate(doc.created_at_wsh) }}</span>
                </span>
              </div>
              <p class="rg-doc-content">{{ doc.content_wsh }}</p>
            </li>
          </ul>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { askRag, getRagDocuments, createRagDocument } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const authStore = useAuthStore()
const appStore = useAppStore()

const suggestions = [
  '提前一天取消能退多少钱？',
  '寄养的标准流程有哪几步？',
  '宠物应激了门店会怎么处理？',
]

const question = ref('')
const asking = ref(false)
const answer = ref(null)
const answerText = computed(() => answer.value?.reply_wsh || '知识库没有找到对应的答案。')
const answerNeedHuman = computed(() => !!answer.value?.need_human_wsh)
const answerSources = computed(() => Array.isArray(answer.value?.sources_wsh) ? answer.value.sources_wsh : [])

const canViewDocs = computed(() => authStore.isAdmin || authStore.isMerchant || authStore.isCs)
const documents = ref([])
const docCount = ref(0)
const docsLoading = ref(false)
const docsError = ref('')

const showDocForm = ref(false)
const docSaving = ref(false)
const docForm = reactive({ title_wsh: '', content_wsh: '' })

onMounted(async () => {
  if (canViewDocs.value) await loadDocuments()
})

function toggleDocForm() {
  showDocForm.value = !showDocForm.value
  if (!showDocForm.value) Object.assign(docForm, { title_wsh: '', content_wsh: '' })
}

async function ask() {
  if (!question.value || asking.value) return
  asking.value = true
  try {
    const r = await askRag({ question_wsh: question.value })
    if (r.code === 200) {
      answer.value = r.data || null
    } else {
      appStore.addToast(r.message || '查询失败', 'error')
    }
  } catch (e) {
    appStore.addToast('查询失败，请稍后重试', 'error')
  }
  asking.value = false
}

function useSuggestion(q) {
  question.value = q
  ask()
}

async function loadDocuments() {
  docsLoading.value = true
  docsError.value = ''
  try {
    const r = await getRagDocuments({ page: 1, size: 100 })
    if (r.code === 200) {
      const data = r.data || {}
      documents.value = Array.isArray(data.list) ? data.list : []
      docCount.value = Number(data.total ?? documents.value.length)
    } else {
      docsError.value = r.message || '加载失败'
    }
  } catch (e) {
    docsError.value = '知识库加载失败，请稍后重试。'
  }
  docsLoading.value = false
}

async function createDocument() {
  if (docSaving.value || !docForm.title_wsh || !docForm.content_wsh) return
  docSaving.value = true
  try {
    const r = await createRagDocument({ ...docForm })
    if (r.code === 200) {
      appStore.addToast('文档添加成功', 'success')
      Object.assign(docForm, { title_wsh: '', content_wsh: '' })
      showDocForm.value = false
      await loadDocuments()
    } else {
      appStore.addToast(r.message || '添加失败', 'error')
    }
  } catch (e) {
    appStore.addToast('添加失败，请稍后重试', 'error')
  }
  docSaving.value = false
}

function formatDate(dt) {
  if (!dt) return ''
  return String(dt).slice(0, 10)
}
</script>

<style scoped>
.rg-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.rg-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.rg-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.rg-crumb-link { color: var(--ref-muted); text-decoration: none; }
.rg-crumb-link:hover { color: var(--ref-ink); }
.rg-crumb-sep { color: var(--ref-line); }
.rg-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.rg-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rg-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.rg-idx { font-variant-numeric: tabular-nums; }
.rg-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.rg-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.rg-head-copy { min-width: 0; }
.rg-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.rg-sub {
  margin: 14px 0 0;
  max-width: 660px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.rg-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 28px;
  margin: 22px 0 0;
}
.rg-fact {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}
.rg-fact-num {
  font-family: var(--ref-font-display);
  font-size: 26px;
  font-weight: 500;
  color: var(--ref-ink);
}
.rg-fact-label {
  font-size: 12px;
  color: var(--ref-muted);
}
.rg-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: 11px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-sm { height: 34px; padding: 0 13px; font-size: 12.5px; border-radius: 9px; }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  padding: 3px 9px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-info { background: color-mix(in srgb, var(--color-info) 12%, transparent); color: var(--color-info); border-color: color-mix(in srgb, var(--color-info) 30%, transparent); }

/* ═══ Section ═══ */
.rg-section { margin-top: 56px; }
.rg-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.rg-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.rg-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.rg-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ 01 · 提问 ═══ */
.rg-ask {
  margin-top: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
}
.rg-ask-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
}
.rg-ask-field {
  position: relative;
  flex: 1 1 320px;
  min-width: 0;
}
.rg-ask-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ref-muted);
  pointer-events: none;
}
.rg-ask-input {
  width: 100%;
  height: 44px;
  padding: 0 14px 0 40px;
  border: 1px solid var(--ref-line);
  border-radius: 11px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.rg-ask-input::placeholder { color: color-mix(in srgb, var(--ref-muted) 70%, transparent); }
.rg-ask-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.rg-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.rg-chip {
  padding: 7px 13px;
  border: 1px solid var(--ref-line);
  border-radius: 999px;
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.rg-chip:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--ref-ink) 25%, transparent);
  background: color-mix(in srgb, var(--ref-sand) 60%, transparent);
  color: var(--ref-ink);
}
.rg-chip:disabled { opacity: 0.5; cursor: not-allowed; }

.rg-answer {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
}
.rg-answer-label {
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rg-answer-text {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: color-mix(in srgb, var(--ref-ink-soft) 92%, transparent);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.rg-handoff {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 11px;
  background: color-mix(in srgb, var(--color-warning) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-warning) 28%, transparent);
}
.rg-handoff-text {
  margin: 0;
  flex: 1 1 240px;
  font-size: 13px;
  color: color-mix(in srgb, var(--ref-ink) 80%, transparent);
}
.rg-sources { margin-top: 20px; }
.rg-sources-label {
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.rg-sources-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.rg-source {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--ref-cream) 45%, transparent);
}
.rg-source-index {
  font-size: 11px;
  color: var(--ref-muted);
}
.rg-source-title {
  font-size: 13px;
  color: var(--ref-ink);
}
.rg-sources-link {
  display: inline-block;
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--ref-brand);
  text-decoration: none;
}
.rg-sources-link:hover { text-decoration: underline; }

/* ═══ 01b · 新增文档 ═══ */
.rg-form {
  margin-top: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
  display: grid;
  gap: 16px;
}
.rg-field {
  display: grid;
  gap: 8px;
}
.rg-label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.rg-req { color: var(--ref-brand); margin-left: 2px; }
.rg-input,
.rg-textarea {
  width: 100%;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.rg-input { height: 42px; }
.rg-textarea { padding-top: 11px; padding-bottom: 11px; line-height: 1.7; resize: vertical; min-height: 120px; }
.rg-input:focus,
.rg-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.rg-form-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

/* ═══ 02 · 收录文档 ═══ */
.rg-docs-note {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-top: 20px;
  padding: 20px 22px;
  border: 1px dashed var(--ref-line);
  border-radius: 16px;
  background: color-mix(in srgb, var(--ref-sand) 40%, transparent);
  color: var(--ref-ink-soft);
}
.rg-docs-note svg { flex: 0 0 auto; margin-top: 2px; color: var(--ref-muted); }
.rg-docs-note-copy { min-width: 0; }
.rg-docs-note-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}
.rg-docs-note-desc {
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}

.rg-docs {
  list-style: none;
  margin: 20px 0 0;
  padding: 0;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  overflow: hidden;
}
.rg-doc {
  padding: 20px 22px;
  border-bottom: 1px solid var(--ref-line);
}
.rg-doc:last-child { border-bottom: none; }
.rg-doc-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
}
.rg-doc-title {
  margin: 0;
  min-width: 0;
  font-size: 14.5px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.rg-doc-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.rg-doc-date {
  font-size: 11.5px;
  color: var(--ref-muted);
}
.rg-doc-content {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ref-muted);
  overflow-wrap: anywhere;
}

.rg-skeletons {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
.rg-skeleton {
  height: 84px;
  border-radius: 14px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--ref-line) 55%, transparent), color-mix(in srgb, var(--ref-line) 25%, transparent), color-mix(in srgb, var(--ref-line) 55%, transparent));
  background-size: 200% 100%;
  animation: rg-shimmer 1.4s infinite;
}
@keyframes rg-shimmer {
  to { background-position: -200% 0; }
}

.rg-docs-state {
  margin-top: 20px;
  padding: 40px 22px;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  text-align: center;
}
.rg-docs-state-title {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
.rg-docs-state-desc {
  margin: 8px auto 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.rg-docs-state .cta { margin-top: 18px; }

/* ═══ Responsive ═══ */
@media (max-width: 640px) {
  .rg-shell { padding: 0 16px; }
  .rg-head { padding: 30px 0 22px; }
  .rg-sub { font-size: 13.5px; }
  .rg-section { margin-top: 44px; }
  .rg-ask, .rg-form { padding: 18px; }
  .rg-ask-bar .cta { flex: 1; }
  .rg-doc { padding: 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .rg-skeleton { animation: none; }
}
</style>
