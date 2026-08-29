<template>
  <div class="ai-page">
    <div class="ai-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ai-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ai-crumb-link">首页</router-link>
        <span class="ai-crumb-sep" aria-hidden="true">›</span>
        <span class="ai-crumb-here">AI 助手</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ai-head">
        <div class="ai-head-copy">
          <div class="ai-eyebrow" aria-hidden="true">
            <span class="ai-eyebrow-line"></span>
            <span>AI</span>
          </div>
          <h1 class="ai-title">AI 助手</h1>
          <p class="ai-sub">AI 在这里只做三件事：回答照护问题、按指令下单、查平台制度原文。涉及用药与疾病判断，始终以宠物医院意见为准。</p>
        </div>
        <div class="ai-actions">
          <router-link to="/ai/chat" class="cta cta-primary">开始对话</router-link>
        </div>
      </header>

      <!-- ═══ 01 · 三种能力 ═══ -->
      <section class="ai-section" aria-label="三种能力">
        <header class="ai-sec-head">
          <div class="ai-head-copy">
            <p class="ai-eyebrow ai-sec-eyebrow">
              <span class="ai-idx">01</span>
              <span class="ai-line" aria-hidden="true"></span>
              <span>Capabilities</span>
            </p>
            <h2 class="ai-sec-title">三种能力</h2>
            <p class="ai-sec-desc">按你要做的事选择入口。</p>
          </div>
        </header>

        <div class="ai-tools">
          <article
            v-for="(tool, index) in tools"
            :key="tool.key"
            class="ai-tool"
            :class="{ 'is-primary': tool.primary }"
          >
            <div class="ai-tool-top">
              <span class="ai-tool-icon" :class="{ 'on-dark': tool.primary }" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path v-if="tool.key === 'chat'" d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  <path v-else-if="tool.key === 'agent'" d="M12 8V4H8" /><rect v-else-if="tool.key === 'agent'" x="2" y="2" width="20" height="8" rx="2" /><rect v-else-if="tool.key === 'agent'" x="2" y="14" width="20" height="8" rx="2" /><path v-else-if="tool.key === 'agent'" d="M6 6h.01M6 18h.01" />
                  <path v-else-if="tool.key === 'knowledge'" d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" /><path v-else-if="tool.key === 'knowledge'" d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                </svg>
              </span>
              <span class="ai-tool-en" :class="{ 'on-dark': tool.primary }">{{ tool.en }}</span>
            </div>

            <h3 class="ai-tool-title" :class="{ 'on-dark': tool.primary }">{{ tool.title }}</h3>
            <p class="ai-tool-desc" :class="{ 'on-dark': tool.primary }">{{ tool.desc }}</p>

            <ul class="ai-tool-examples">
              <li v-for="example in tool.examples" :key="example" :class="{ 'on-dark': tool.primary }">
                「{{ example }}」
              </li>
            </ul>

            <div class="ai-tool-cta">
              <router-link :to="tool.to" class="ai-tool-link" :class="{ 'on-dark': tool.primary }">
                进入{{ tool.title }}
              </router-link>
            </div>
          </article>
        </div>
      </section>

      <!-- ═══ 02 · 宠物健康报告 ═══ -->
      <section class="ai-section" aria-label="宠物健康报告">
        <header class="ai-sec-head">
          <div class="ai-head-copy">
            <p class="ai-eyebrow ai-sec-eyebrow">
              <span class="ai-idx">02</span>
              <span class="ai-line" aria-hidden="true"></span>
              <span>Health reports</span>
            </p>
            <h2 class="ai-sec-title">宠物健康报告</h2>
            <p class="ai-sec-desc">寄养期间每日生成照护小结，离店时生成一份健康总结。报告按宠物归档。</p>
          </div>
        </header>

        <div class="ai-report">
          <div class="ai-report-card">
            <div class="ai-report-card-copy">
              <p class="ai-report-card-title">
                <!-- 报告入口 → 宠物档案 -->
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="ai-report-paw" aria-hidden="true">
                  <circle cx="11" cy="4" r="2" /><circle cx="18" cy="8" r="2" /><circle cx="20" cy="16" r="2" />
                  <path d="M9 12a5 5 0 0 1 5 5v3H4v-3a5 5 0 0 1 5-5z" /><circle cx="4" cy="10" r="2" />
                </svg>
                报告在宠物档案里查看
              </p>
              <p class="ai-report-card-desc">
                每日小结记录进食、活动、排泄与情绪；离店总结会汇总体重变化、毛发与关节情况，并给出后续关注建议。
              </p>
            </div>
            <router-link to="/pets" class="cta cta-outline">打开宠物档案</router-link>
          </div>

          <div class="ai-report-form">
            <p class="ai-report-form-hint">除了在宠物档案查看归档报告，你也可以在这里为已完成寄养的宠物即时生成报告：</p>
            <div class="ai-report-bar">
            <div class="ai-report-field">
              <label for="ai-pet">选择宠物</label>
              <select id="ai-pet" v-model="selectedPetId" class="ai-select" @change="onPetChange">
                <option value="">-- 请选择 --</option>
                <option v-for="p in pets" :key="p.id_wsh" :value="p.id_wsh">{{ p.name_wsh || p.pet_name_wsh }}</option>
              </select>
            </div>
            <div class="ai-report-field">
              <label for="ai-order">已完成订单</label>
              <select id="ai-order" v-model="selectedOrderId" class="ai-select" :disabled="!selectedPetId">
                <option value="">-- 请选择 --</option>
                <option v-for="o in completedOrders" :key="o.id_wsh" :value="o.id_wsh">
                  #{{ o.id_wsh }} {{ formatDate(o.created_at_wsh) }}
                </option>
              </select>
            </div>
            <div class="ai-report-actions">
              <button
                type="button"
                class="cta cta-outline"
                :disabled="!selectedPetId || !selectedOrderId || reportLoading"
                @click="generateReport('care_suggestion')"
              >
                培养建议
              </button>
              <button
                type="button"
                class="cta cta-primary"
                :disabled="!selectedPetId || !selectedOrderId || reportLoading"
                @click="generateReport('boarding_report')"
              >
                {{ reportLoading ? '生成中...' : '寄养报告' }}
              </button>
            </div>
            </div>
          </div>

          <div v-if="reportContent" class="ai-report-result">
            <p class="ai-report-result-head">
              <span class="badge" :class="reportType === 'care_suggestion' ? 'badge-info' : 'badge-active'">
                {{ reportType === 'care_suggestion' ? '培养建议' : '寄养报告' }}
              </span>
              <span class="ai-report-result-date">生成于 {{ generatedAt }}</span>
            </p>
            <p class="ai-report-result-text">{{ reportContent }}</p>
          </div>
        </div>
      </section>

      <!-- ═══ 03 · 能力边界 ═══ -->
      <section class="ai-section" aria-label="能力边界">
        <header class="ai-sec-head">
          <div class="ai-head-copy">
            <p class="ai-eyebrow ai-sec-eyebrow">
              <span class="ai-idx">03</span>
              <span class="ai-line" aria-hidden="true"></span>
              <span>Limits</span>
            </p>
            <h2 class="ai-sec-title">能力边界</h2>
            <p class="ai-sec-desc">写清楚 AI 不做什么，比夸大它能做什么更有用。</p>
          </div>
        </header>

        <ul class="ai-limits">
          <li v-for="limit in limits" :key="limit.title" class="ai-limit">
            <p class="ai-limit-title">{{ limit.title }}</p>
            <p class="ai-limit-desc">{{ limit.desc }}</p>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPets } from '@/api/pet'
import { getOrders } from '@/api/order'
import { getCareSuggestion, getBoardingReport } from '@/api/ai'

const tools = [
  {
    key: 'chat',
    en: 'Ask',
    title: '照护助手',
    desc: '关于寄养准备、饮食、应激与日常照护的问题，随时可以问。答复会附上依据来源，没把握时会主动建议转人工。',
    to: '/ai/chat',
    primary: true,
    examples: ['柯基第一次寄养要准备什么？', '猫到新环境不吃饭怎么办？', '老年犬寄养要盯哪些指标？'],
  },
  {
    key: 'agent',
    en: 'Act',
    title: '智能下单',
    desc: '用一句话描述需求，助手会匹配门店与服务、校验档期并生成订单。是否自动支付由你决定。',
    to: '/agent',
    examples: ['帮布丁订 9 月 15 到 18 日的寄养', '下周三给芋圆约一次上门喂养'],
  },
  {
    key: 'knowledge',
    en: 'Look up',
    title: '照护知识库',
    desc: '平台的服务标准、取消规则与应激处置手册原文可查。答案来自制度文档本身，不是模型的印象。',
    to: '/rag',
    examples: ['提前一天取消能退多少？', '寄养的标准流程有哪几步？'],
  },
]

const limits = [
  { title: '不做诊断，不给用药建议', desc: '出现呕吐、持续不进食、精神萎靡等情况，会直接建议联系合作宠物医院。' },
  { title: '不会替你付钱，除非你明确允许', desc: '智能下单默认只生成订单；开启自动支付需要单独输入支付密码，密码不会被保存。' },
  { title: '答不上来就转人工', desc: '模型对问题没有把握时会标注并提供转接入口，而不是编一个看起来合理的答案。' },
]

const pets = ref([])
const orders = ref([])
const selectedPetId = ref('')
const selectedOrderId = ref('')
const reportLoading = ref(false)
const reportContent = ref('')
const reportType = ref('')
const generatedAt = ref('')

const completedOrders = computed(() => {
  if (!selectedPetId.value) return []
  return orders.value.filter(o =>
    o.status_wsh === 'completed' && String(o.pet_id_wsh) === String(selectedPetId.value)
  )
})

onMounted(async () => {
  try {
    const r = await getPets()
    if (r.code === 200) pets.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
  try {
    const r = await getOrders()
    if (r.code === 200) orders.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {}
})

function formatDate(dt) {
  if (!dt) return ''
  return String(dt).slice(0, 10)
}

function onPetChange() {
  selectedOrderId.value = ''
}

async function generateReport(type) {
  if (!selectedPetId.value || !selectedOrderId.value || reportLoading.value) return
  reportLoading.value = true
  reportType.value = type
  try {
    const fn = type === 'care_suggestion' ? getCareSuggestion : getBoardingReport
    const r = await fn({ pet_id_wsh: Number(selectedPetId.value), order_id_wsh: Number(selectedOrderId.value) })
    if (r.code === 200) {
      reportContent.value = r.data.content_wsh || '生成成功'
      generatedAt.value = new Date().toLocaleString('zh-CN')
    } else {
      reportContent.value = '生成失败：' + (r.message || '未知错误')
    }
  } catch (e) {
    reportContent.value = '生成报告失败，请稍后重试。'
  }
  reportLoading.value = false
}
</script>

<style scoped>
.ai-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ai-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ai-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ai-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ai-crumb-link:hover { color: var(--ref-ink); }
.ai-crumb-sep { color: var(--ref-line); }
.ai-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ai-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ai-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ai-idx { font-variant-numeric: tabular-nums; }
.ai-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ai-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ai-head-copy { min-width: 0; }
.ai-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ai-sub {
  margin: 14px 0 0;
  max-width: 660px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ai-actions { display: flex; flex-wrap: wrap; gap: 10px; }

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

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-active { background: color-mix(in srgb, var(--color-success) 12%, transparent); color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, transparent); }
.badge-info { background: color-mix(in srgb, var(--color-info) 12%, transparent); color: var(--color-info); border-color: color-mix(in srgb, var(--color-info) 30%, transparent); }

/* ═══ Section ═══ */
.ai-section { margin-top: 56px; }
.ai-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ai-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ai-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ai-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ 01 · 三种能力 ═══ */
.ai-tools {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 20px;
}
.ai-tool {
  display: flex;
  flex-direction: column;
  padding: 26px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.ai-tool:hover {
  transform: translateY(-3px);
  box-shadow: 0 26px 56px -44px color-mix(in srgb, var(--ref-ink) 55%, transparent);
}
.ai-tool.is-primary {
  border-color: transparent;
  background: var(--ref-ink);
  box-shadow: 0 30px 60px -45px color-mix(in srgb, var(--ref-ink) 80%, transparent);
}
.ai-tool-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.ai-tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--ref-sand);
  color: var(--ref-brand);
}
.ai-tool-icon.on-dark { background: color-mix(in srgb, var(--ref-cream) 10%, transparent); color: var(--ref-cream); }
.ai-tool-en {
  font-size: 10px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ai-tool-en.on-dark { color: color-mix(in srgb, var(--ref-cream) 40%, transparent); }
.ai-tool-title {
  margin: 22px 0 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.ai-tool-title.on-dark { color: var(--ref-cream); }
.ai-tool-desc {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ai-tool-desc.on-dark { color: color-mix(in srgb, var(--ref-cream) 70%, transparent); }
.ai-tool-examples {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.ai-tool-examples li {
  padding: 9px 12px;
  border-radius: 9px;
  background: color-mix(in srgb, var(--ref-cream) 60%, transparent);
  color: color-mix(in srgb, var(--ref-ink-soft) 80%, transparent);
  font-size: 12px;
  line-height: 1.6;
}
.ai-tool-examples li.on-dark { background: color-mix(in srgb, var(--ref-cream) 7%, transparent); color: color-mix(in srgb, var(--ref-cream) 65%, transparent); }
.ai-tool-cta { margin-top: auto; padding-top: 24px; }
.ai-tool-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 42px;
  border-radius: 10px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: border-color 0.15s, background 0.15s;
}
.ai-tool-link:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); background: color-mix(in srgb, var(--ref-sand) 60%, transparent); }
.ai-tool-link.on-dark { background: var(--ref-cream); color: var(--ref-ink); border-color: transparent; }
.ai-tool-link.on-dark:hover { background: #fff; }

/* ═══ 02 · 宠物健康报告 ═══ */
.ai-report {
  margin-top: 20px;
  border: 1px solid var(--ref-line);
  border-radius: 18px;
  background: var(--ref-surface);
  padding: 24px;
}
.ai-report-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px 20px;
}
.ai-report-card-copy { min-width: 0; max-width: 560px; }
.ai-report-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ai-report-paw { color: var(--ref-brand); flex: 0 0 auto; }
.ai-report-card-desc {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 85%, transparent);
}
.ai-report-form {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
}
.ai-report-form-hint {
  margin: 0 0 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.ai-report-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 14px;
}
.ai-report-field {
  display: grid;
  gap: 7px;
  min-width: 200px;
  flex: 1 1 200px;
}
.ai-report-field label {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.ai-select {
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 13.5px;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ai-select:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.ai-select:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-report-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 0 0 auto;
}
.ai-report-result {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--ref-line);
}
.ai-report-result-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.ai-report-result-date {
  font-size: 11px;
  color: var(--ref-muted);
}
.ai-report-result-text {
  margin: 12px 0 0;
  font-size: 13.5px;
  line-height: 1.9;
  color: color-mix(in srgb, var(--ref-ink-soft) 90%, transparent);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ═══ 03 · 能力边界 ═══ */
.ai-limits {
  list-style: none;
  margin: 20px 0 0;
  padding: 0;
  border: 1px solid var(--ref-line);
  border-radius: 16px;
  background: var(--ref-surface);
  overflow: hidden;
}
.ai-limit {
  padding: 18px 22px;
  border-bottom: 1px solid var(--ref-line);
}
.ai-limit:last-child { border-bottom: none; }
.ai-limit-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ai-limit-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}

/* ═══ Responsive ═══ */
@media (max-width: 980px) {
  .ai-tools { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .ai-shell { padding: 0 16px; }
  .ai-head { padding: 30px 0 22px; }
  .ai-sub { font-size: 13.5px; }
  .ai-section { margin-top: 44px; }
  .ai-report { padding: 18px; }
  .ai-report-field { flex-basis: 100%; }
  .ai-report-actions .cta { flex: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .ai-tool, .ai-tool-link { transition: none; }
}
</style>
