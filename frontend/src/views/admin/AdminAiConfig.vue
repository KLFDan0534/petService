<template>
  <div class="ai-config-page">
    <div class="page-header">
      <h2>AI 配置管理</h2>
      <p class="page-desc">管理多套 AI 大模型配置（智能下单/AI助手 agent · 智能客服 cs），每类用途各启用一套</p>
    </div>

    <!-- 当前生效 -->
    <div v-if="effectiveText" class="effective-banner">
      <span class="effective-label">当前生效</span>
      <span class="effective-item">agent：{{ effectiveText.agent }}</span>
      <span class="effective-item">cs：{{ effectiveText.cs }}</span>
    </div>

    <div class="config-head">
      <button type="button" class="btn btn-primary" @click="openCreate">
        {{ formVisible && !editingId ? '收起' : '+ 新增配置' }}
      </button>
    </div>

    <!-- 新增/编辑表单 -->
    <div v-if="formVisible" class="config-card">
      <form @submit.prevent="handleSave">
        <div class="form-grid">
          <label class="form-field">
            配置名称
            <input v-model.trim="form.name" required placeholder="如：b.ai glm-5.3" />
          </label>

          <label class="form-field">
            用途
            <select v-model="form.usage" :disabled="Boolean(editingId)" required>
              <option value="agent">agent（智能下单 / AI助手）</option>
              <option value="cs">cs（智能客服）</option>
            </select>
          </label>

          <label class="form-field form-field--wide">
            API 地址 (Endpoint)
            <input v-model.trim="form.endpoint" required placeholder="https://api.b.ai/v1" />
            <small class="field-help">AI 服务的基础 URL，不需要包含 /chat/completions</small>
          </label>

          <label class="form-field form-field--wide">
            API Key
            <div class="key-input-row">
              <input
                v-model.trim="form.apiKey"
                :type="showKey ? 'text' : 'password'"
                :placeholder="editingId ? '留空表示不修改' : '请输入 API Key'"
              />
              <button type="button" class="btn btn-outline btn-sm" @click="showKey = !showKey">
                {{ showKey ? '隐藏' : '显示' }}
              </button>
            </div>
          </label>

          <label class="form-field">
            模型 (Model)
            <input v-model.trim="form.model" required placeholder="glm-5.3-flash" />
          </label>

          <label class="form-field">
            最大 Token 数
            <input v-model.number="form.maxTokens" required min="1" type="number" />
          </label>

          <label class="form-field">
            温度 (Temperature)
            <input v-model.number="form.temperature" required min="0" max="2" step="0.1" type="number" />
          </label>

          <label v-if="!editingId" class="form-field form-field--checkbox">
            保存后立即启用
            <input v-model="form.enable" type="checkbox" />
            <small class="field-help">启用会停用同用途其它配置</small>
          </label>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-outline" :disabled="testing" @click="handleTestCurrent">
            {{ testing ? '测试中...' : '🔗 测试连通性' }}
          </button>
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '保存中...' : '💾 保存配置' }}
          </button>
          <button type="button" class="btn btn-outline" @click="closeForm">取消</button>
        </div>
      </form>

      <div v-if="testResult" :class="['test-result', testResult.success ? 'test-success' : 'test-error']">
        <span class="test-icon">{{ testResult.success ? '✅' : '❌' }}</span>
        <div>
          <div class="test-message">{{ testResult.message }}</div>
          <div v-if="testResult.model" class="test-detail">模型: {{ testResult.model }} · 端点: {{ testResult.endpoint }}</div>
        </div>
      </div>
    </div>

    <!-- 配置列表 -->
    <div class="config-list">
      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="!configs.length" class="config-empty">暂无配置，点击上方「新增配置」添加第一套。</div>

      <div v-else class="config-item" v-for="cfg in configs" :key="cfg.id">
        <div class="cfg-main">
          <div class="cfg-title-row">
            <span class="cfg-name">{{ cfg.name || '(未命名)' }}</span>
            <span :class="['usage-badge', cfg.usage === 'agent' ? 'usage-agent' : 'usage-cs']">
              {{ cfg.usage === 'agent' ? '智能下单/AI助手' : '智能客服' }}
            </span>
            <span v-if="cfg.enabled" class="enabled-badge">已启用</span>
          </div>
          <div class="cfg-meta">
            <span>{{ cfg.model }}</span>
            <span class="sep">·</span>
            <span>{{ cfg.endpoint }}</span>
            <span class="sep">·</span>
            <span>Key: {{ cfg.apiKey }}</span>
          </div>
        </div>
        <div class="cfg-actions">
          <label class="switch">
            <input
              type="checkbox"
              :checked="Boolean(cfg.enabled)"
              :disabled="saving"
              @change="handleToggle(cfg, $event.target.checked)"
            />
            <span class="switch-slider"></span>
          </label>
          <button type="button" class="btn btn-outline btn-sm" @click="openEdit(cfg)">编辑</button>
          <button type="button" class="btn btn-outline btn-sm" :disabled="testBusyId === cfg.id" @click="handleTestRow(cfg)">
            {{ testBusyId === cfg.id ? '测试中...' : '测试' }}
          </button>
          <button type="button" class="btn btn-outline btn-sm btn-danger" @click="handleDelete(cfg)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  listAiConfigs,
  createAiConfig,
  updateAiConfigById,
  deleteAiConfig,
  enableAiConfig,
  testAiConfigById,
  getEffectiveAiConfig,
} from '@/api/ai'

const appStore = useAppStore()
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const testBusyId = ref(null)
const showKey = ref(false)
const testResult = ref(null)
const configs = ref([])
const formVisible = ref(false)
const editingId = ref(null)
const effective = ref({ agent: null, cs: null })

const form = reactive({
  name: '',
  usage: 'agent',
  endpoint: '',
  apiKey: '',
  model: '',
  maxTokens: 8192,
  temperature: 1.0,
  enable: false,
})

const effectiveText = computed(() => ({
  agent: effective.value.agent ? formatEffective('agent', effective.value.agent) : '未配置（回退默认）',
  cs: effective.value.cs ? formatEffective('cs', effective.value.cs) : '未配置（回退默认）',
}))
function formatEffective(usage, data) {
  return data.source === 'db' ? `${data.name} (${data.model})` : `默认 ${data.model}`
}

onMounted(async () => {
  try {
    const res = await listAiConfigs()
    if (res.code === 200) configs.value = res.data || []
  } catch (e) {
    appStore.addToast('加载配置失败', 'error')
  } finally {
    loading.value = false
  }
  loadEffective()
})

async function loadEffective() {
  try {
    const [a, c] = await Promise.all([
      getEffectiveAiConfig('agent'),
      getEffectiveAiConfig('cs'),
    ])
    if (a.code === 200) effective.value.agent = a.data
    if (c.code === 200) effective.value.cs = c.data
  } catch (e) { /* 忽略 */ }
}

function openCreate() {
  formVisible.value = !formVisible.value
  editingId.value = null
  testResult.value = null
  Object.assign(form, { name: '', usage: 'agent', endpoint: '', apiKey: '', model: '', maxTokens: 8192, temperature: 1.0, enable: true })
}

function openEdit(cfg) {
  editingId.value = cfg.id
  testResult.value = null
  formVisible.value = true
  Object.assign(form, {
    name: cfg.name || '',
    usage: cfg.usage,
    endpoint: cfg.endpoint || '',
    apiKey: '',
    model: cfg.model || '',
    maxTokens: cfg.maxTokens || 8192,
    temperature: cfg.temperature ?? 1.0,
    enable: false,
  })
}

function closeForm() {
  formVisible.value = false
  editingId.value = null
  testResult.value = null
}

async function handleSave() {
  if (saving.value) return
  saving.value = true
  testResult.value = null
  try {
    const payload = {
      name: form.name,
      usage: form.usage,
      endpoint: form.endpoint,
      model: form.model,
      maxTokens: Number(form.maxTokens),
      temperature: Number(form.temperature),
    }
    if (form.apiKey) payload.apiKey = form.apiKey
    if (!editingId.value) payload.enable = form.enable

    const res = editingId.value
      ? await updateAiConfigById(editingId.value, payload)
      : await createAiConfig(payload)
    if (res.code === 200) {
      appStore.addToast(editingId.value ? '配置已更新' : '配置已保存', 'success')
      closeForm()
      await reloadList()
      loadEffective()
    } else {
      appStore.addToast(res.message || '保存失败', 'error')
    }
  } catch (e) {
    appStore.addToast('保存失败: ' + (e.message || ''), 'error')
  } finally {
    saving.value = false
  }
}

async function handleToggle(cfg, enabled) {
  try {
    const res = await enableAiConfig(cfg.id, enabled)
    if (res.code === 200) {
      await reloadList()
      loadEffective()
      appStore.addToast(enabled ? '已启用' : '已停用', 'success')
    } else {
      appStore.addToast(res.message || '操作失败', 'error')
      await reloadList()
    }
  } catch (e) {
    appStore.addToast('操作失败: ' + (e.message || ''), 'error')
    await reloadList()
  }
}

async function handleTestRow(cfg) {
  testBusyId.value = cfg.id
  try {
    const res = await testAiConfigById(cfg.id)
    const data = res.code === 200 ? res.data : { success: false, message: res.message || '测试失败' }
    appStore.addToast(data.message || (data.success ? '连接成功' : '测试失败'),
      data.success ? 'success' : 'error')
  } catch (e) {
    appStore.addToast('测试失败: ' + (e.message || ''), 'error')
  } finally {
    testBusyId.value = null
  }
}

async function handleTestCurrent() {
  if (testing.value) return
  testing.value = true
  testResult.value = null
  try {
    // 编辑已有配置时按 id 测试；新增时按表单参数临时构造调用
    let res
    if (editingId.value) {
      res = await testAiConfigById(editingId.value)
    } else {
      res = await testAiConfigByForm()
    }
    testResult.value = res.code === 200 ? res.data : { success: false, message: res.message || '测试失败' }
  } catch (e) {
    testResult.value = { success: false, message: '请求失败: ' + (e.message || '') }
  } finally {
    testing.value = false
  }
}

async function testAiConfigByForm() {
  // 新增场景：临时以表单参数走一期测试端点（由后端 current 配置测试），若不支持则提示
  const model = form.model || 'glm-5.3-flash'
  const endpoint = form.endpoint || 'https://api.b.ai/v1'
  const apiKey = form.apiKey || ''
  return { code: apiKey ? 200 : 400, data: { success: !!apiKey, message: apiKey ? '请保存后点击列表内的「测试」验证' : '请先填写 API Key', model, endpoint } }
}

async function handleDelete(cfg) {
  if (!window.confirm(`确认删除配置「${cfg.name || cfg.id}」？`)) return
  try {
    const res = await deleteAiConfig(cfg.id)
    if (res.code === 200) {
      appStore.addToast('已删除', 'success')
      await reloadList()
      loadEffective()
    } else {
      appStore.addToast(res.message || '删除失败', 'error')
    }
  } catch (e) {
    appStore.addToast('删除失败: ' + (e.message || ''), 'error')
  }
}

async function reloadList() {
  try {
    const res = await listAiConfigs()
    if (res.code === 200) configs.value = res.data || []
  } catch (e) { /* 忽略 */ }
}
</script>

<style scoped>
.ai-config-page {
  display: grid;
  gap: 20px;
}

.page-header h2 { margin: 0 0 4px; }
.page-desc {
  color: var(--color-muted-foreground);
  font-size: 14px;
  margin: 0;
}

.effective-banner {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
  font-size: 13px;
  color: var(--color-muted-foreground);
}
.effective-label { font-weight: 600; color: var(--color-foreground); }
.effective-item { color: var(--color-foreground); }

.config-head { display: flex; justify-content: flex-end; }

.config-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 24px;
}

.form-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr 1fr;
}
.form-field {
  color: var(--color-muted-foreground);
  display: grid;
  font-size: 13px;
  gap: 6px;
}
.form-field--wide { grid-column: 1 / -1; }
.form-field--checkbox { align-items: center; grid-auto-flow: column; justify-content: start; }
.form-field input, .form-field select { width: 100%; }
.key-input-row { display: flex; gap: 8px; align-items: center; }
.key-input-row input { flex: 1; }
.field-help { color: var(--color-muted-foreground); font-size: 12px; line-height: 1.5; }

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.test-result {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  border-radius: var(--radius-md);
}
.test-success { background: rgba(34, 197, 94, 0.08); border: 1px solid rgba(34, 197, 94, 0.3); }
.test-error { background: rgba(239, 68, 68, 0.08); border: 1px solid rgba(239, 68, 68, 0.3); }
.test-icon { font-size: 20px; line-height: 1; }
.test-message { font-size: 14px; font-weight: 500; color: var(--color-foreground); }
.test-detail { font-size: 12px; color: var(--color-muted-foreground); margin-top: 4px; }

/* 列表 */
.config-list { display: grid; gap: 12px; }
.config-empty {
  padding: 40px;
  text-align: center;
  color: var(--color-muted-foreground);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
}
.config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
}
.cfg-main { min-width: 0; }
.cfg-title-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.cfg-name { font-size: 14px; font-weight: 600; color: var(--color-foreground); }
.usage-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; }
.usage-agent { background: rgba(64, 158, 255, 0.12); color: #409eff; }
.usage-cs { background: rgba(103, 194, 58, 0.12); color: #67c23a; }
.enabled-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: rgba(34, 197, 94, 0.15); color: #22c55e; }
.cfg-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--color-muted-foreground);
}
.sep { opacity: 0.4; }
.cfg-actions { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }

/* switch */
.switch { position: relative; display: inline-block; width: 40px; height: 22px; }
.switch input { opacity: 0; width: 0; height: 0; }
.switch-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background: var(--color-border);
  border-radius: 999px;
  transition: background 0.2s;
}
.switch-slider:before {
  content: "";
  position: absolute;
  height: 16px;
  width: 16px;
  left: 3px;
  top: 3px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
}
.switch input:checked + .switch-slider { background: var(--color-primary, #4f8cff); }
.switch input:checked + .switch-slider:before { transform: translateX(18px); }

.btn-danger:hover { border-color: rgba(239, 68, 68, 0.5); color: #ef4444; }
</style>