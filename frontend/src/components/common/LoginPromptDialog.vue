<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal login-prompt-modal" role="dialog" aria-modal="true">
        <div class="login-prompt-icon" aria-hidden="true">
          <el-icon><Lock /></el-icon>
        </div>
        <p>登录后即可使用该功能</p>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="$emit('close')">暂不登录</button>
          <button class="btn btn-primary" @click="goLogin">去登录</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const router = useRouter()
const appStore = useAppStore()

function safeRedirect(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) return '/dashboard'
  return raw
}

function goLogin() {
  emit('close')
  const redirect = encodeURIComponent(safeRedirect(appStore.loginRedirectPath))
  router.push('/login?redirect=' + redirect)
}
</script>

<style scoped>
.login-prompt-modal {
  text-align: center;
  max-width: 400px;
}
.login-prompt-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  margin-bottom: 12px;
  margin-inline: auto;
  color: var(--color-primary);
  background: var(--color-muted);
  border-radius: 14px;
  font-size: 26px;
}
.login-prompt-modal p {
  color: var(--color-muted-foreground);
  font-size: 14px;
  line-height: 1.6;
  margin: 8px 0 0;
}
</style>
