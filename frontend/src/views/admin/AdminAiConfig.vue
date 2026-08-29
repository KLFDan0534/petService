<template>
  <div class="ai-config-page">
    <div class="page-header">
      <h2>AI 智能客服配置</h2>
      <p class="page-desc">配置 AI 大模型的 API 地址、密钥和模型参数</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else class="config-card">
      <form @submit.prevent="handleSave">
        <div class="form-grid">
          <label class="form-field form-field--wide">
            API 地址 (Endpoint)
            <input v-model.trim="form.endpoint" required placeholder="https://open.bigmodel.cn/api/paas/v4" />
            <small class="field-help">AI 服务的基础 URL，不需要包含 /chat/completions</small>
          </label>

          <label class="form-field form-field--wide">
            API Key
            <div class="key-input-row">
              <input
                v-model.trim="form.apiKey"
                :type="showKey ? 'text' : 'password'"
                placeholder="请输入 API Key"
              />
              <button type="button" class="btn btn-outline btn-sm" @click="showKey = !showKey">
                {{ showKey ? '隐藏' : '显示' }}
              </button>
            </div>
            <small class="field-help">
              <template v-if="currentKeyMask">当前密钥: {{ currentKeyMask }}</template>
              <template v-else>尚未配置 API Key</template>
            </small>
          </label>

          <label class="form-field">
            模型 (Model)
            <input v-model.trim="form.model" required placeholder="glm-4.7-flash" />
          </label>

          <label class="form-field">
            最大 Token 数
            <input v-model.number="form.maxTokens" required min="1" max="131072" type="number" />
          </label>

          <label class="form-field">
            温度 (Temperature)
            <input v-model.number="form.temperature" required min="0" max="2" step="0.1" type="number" />
            <small class="field-help">0 = 精确，1 = 创意，2 = 最大随机</small>
          </label>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-outline" :disabled="testing" @click="handleTest">
            {{ testing ? '测试中...' : '🔗 测试连通性' }}
          </button>
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '保存中...' : '💾 保存配置' }}
          </button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getAiConfig, updateAiConfig, testAiConnection } from '@/api/ai'

const appStore = useAppStore()
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const showKey = ref(false)
const currentKeyMask = ref('')
const testResult = ref(null)

const form = reactive({
  apiKey: '',
  model: '',
  endpoint: '',
  maxTokens: 65536,
  temperature: 1.0,
})

onMounted(loadConfig)

async function loadConfig() {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200 && res.data) {
      const d = res.data
      form.apiKey = '' // 不回填明文 key
      form.model = d.model || 'glm-4.7-flash'
      form.endpoint = d.endpoint || 'https://open.bigmodel.cn/api/paas/v4'
      form.maxTokens = d.maxTokens || 65536
      form.temperature = d.temperature ?? 1.0
      currentKeyMask.value = d.apiKey || ''
    }
  } catch (e) {
    appStore.addToast('加载配置失败', 'error')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (saving.value) return
  saving.value = true
  testResult.value = null
  try {
    const payload = {}
    if (form.apiKey) payload.apiKey = form.apiKey
    payload.model = form.model
    payload.endpoint = form.endpoint
    payload.maxTokens = Number(form.maxTokens)
    payload.temperature = Number(form.temperature)

    const res = await updateAiConfig(payload)
    if (res.code === 200) {
      appStore.addToast('AI 配置已保存', 'success')
      // 重新加载以获取脱敏的 key
      await loadConfig()
    } else {
      appStore.addToast(res.message || '保存失败', 'error')
    }
  } catch (e) {
    appStore.addToast('保存失败: ' + (e.message || ''), 'error')
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (testing.value) return
  testing.value = true
  testResult.value = null
  try {
    const res = await testAiConnection()
    if (res.code === 200) {
      testResult.value = res.data
    } else {
      testResult.value = { success: false, message: res.message || '测试失败' }
    }
  } catch (e) {
    testResult.value = { success: false, message: '请求失败: ' + (e.message || '') }
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.ai-config-page {
  display: grid;
  gap: 20px;
}

.page-header h2 {
  margin: 0 0 4px;
}

.page-desc {
  color: var(--color-muted-foreground);
  font-size: 14px;
  margin: 0;
}

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

.form-field--wide {
  grid-column: 1 / -1;
}

.form-field input {
  width: 100%;
}

.key-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.key-input-row input {
  flex: 1;
}

.field-help {
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
}

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

.test-success {
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.test-error {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.test-icon {
  font-size: 20px;
  line-height: 1;
}

.test-message {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-foreground);
}

.test-detail {
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin-top: 4px;
}
</style>
