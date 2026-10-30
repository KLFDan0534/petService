<template>
  <!-- 头部自己画了图标和标题，默认那个关闭按钮就不要了，两个头看着乱 -->
  <AppDialog
    :visible="visible"
    :width="400"
    :show-close="false"
    aria-label="登录提示"
    @close="$emit('close')"
  >
    <template #header>
      <div class="login-prompt-head">
        <span class="login-prompt-icon" aria-hidden="true">
          <AppIcon><Lock /></AppIcon>
        </span>
        <div class="login-prompt-title">
          <h2>登录后继续预约</h2>
        </div>
      </div>
    </template>

    <div class="login-prompt-body">
      <p>
        下单需要绑定手机号，以便门店和照护人在服务过程中联系到你。登录后会回到
        <strong class="login-prompt-strong">「{{ returnTo }}」</strong>
        ，已填写的信息不会丢失。
      </p>
      <p class="login-prompt-meta">
        新用户用手机号验证码即可完成注册，无需单独设置密码。
      </p>
    </div>

    <template #footer>
      <AppButton variant="quiet" @click="$emit('close')">稍后再说</AppButton>
      <AppButton variant="primary" arrow @click="goLogin">登录 / 注册</AppButton>
    </template>
  </AppDialog>
</template>

<script setup>
import { computed } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import { useAppStore } from '@/stores/app'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])

const router = useRouter()
const appStore = useAppStore()

// 从哪个页面被拦下来的就回哪个，没记下就含糊说一句「原来的页面」
const returnTo = computed(() => appStore.loginRedirectPath || '原来的页面')

// 为什么还要再查一遍？这个地址是从网址里带过来的，不查就能被人改成外站，登录完直接被拐走
function safeRedirect(raw) {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//')) return '/dashboard'
  return raw
}

// 先关弹窗再跳，不然登录页后面还压着一层
function goLogin() {
  emit('close')
  const redirect = encodeURIComponent(safeRedirect(appStore.loginRedirectPath))
  router.push('/login?redirect=' + redirect)
}
</script>

<style scoped>
.login-prompt-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.login-prompt-icon {
  flex: none;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px; /* rounded-tile */
  color: var(--ref-brand);
  background: var(--ref-sand);
}
.login-prompt-icon :deep(svg) {
  width: 17px;
  height: 17px;
  stroke-width: 1.75;
}
.login-prompt-title h2 {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--ref-ink);
}
.login-prompt-body p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.login-prompt-strong {
  font-weight: 500;
  color: var(--ref-ink);
}
.login-prompt-meta {
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--ref-muted);
}
</style>