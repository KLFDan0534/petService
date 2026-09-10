<template>
  <button
    type="button"
    class="favorite-toggle btn"
    :class="[isFavorited ? activeClass : inactiveClass, showText ? 'has-text' : 'is-icon-only']"
    :disabled="buttonDisabled"
    :aria-label="ariaLabel"
    @click.stop="handleClick"
  >
    <AppIcon class="favorite-toggle-icon">
      <StarFilled v-if="isFavorited" />
      <Star v-else />
    </AppIcon>
    <span v-if="showText">{{ isFavorited ? favoritedLabel : unfavoritedLabel }}</span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useFavoriteState } from '@/composables/useFavoriteState'
import { isFavoriteTargetType, normalizeFavoriteTargetType } from '@/constants/favorite'

const props = defineProps({
  targetId: {
    type: [Number, String],
    required: true,
  },
  targetType: {
    type: String,
    required: true,
  },
  showText: {
    type: Boolean,
    default: true,
  },
  favoritedLabel: {
    type: String,
    default: '\u5df2\u6536\u85cf',
  },
  unfavoritedLabel: {
    type: String,
    default: '\u6536\u85cf',
  },
  activeClass: {
    type: String,
    default: 'is-favorited',
  },
  inactiveClass: {
    type: String,
    default: 'is-unfavorited',
  },
})

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const targetId = computed(() => Number(props.targetId))
const targetType = computed(() => normalizeFavoriteTargetType(props.targetType))
const targetReady = computed(() => targetId.value > 0 && isFavoriteTargetType(targetType.value))
const syncEnabled = computed(() => authStore.isLoggedIn && targetReady.value)

const {
  isFavorited,
  loading,
  toggling,
  toggle,
} = useFavoriteState(targetId, targetType, { enabled: syncEnabled })

const buttonDisabled = computed(() => loading.value || toggling.value || !targetReady.value)
const ariaLabel = computed(() => (isFavorited.value ? props.favoritedLabel : props.unfavoritedLabel))

async function handleClick() {
  if (!authStore.isLoggedIn) {
    appStore.loginRedirectPath = route.fullPath
    appStore.showLoginPrompt = true
    return
  }
  if (!targetReady.value) {
    appStore.addToast('\u4e0d\u652f\u6301\u7684\u6536\u85cf\u7c7b\u578b', 'error')
    return
  }
  try {
    await toggle()
    appStore.addToast(isFavorited.value ? '\u5df2\u6536\u85cf' : '\u5df2\u53d6\u6d88\u6536\u85cf', 'success')
  } catch {
    appStore.addToast('\u64cd\u4f5c\u5931\u8d25', 'error')
  }
}
</script>

<style scoped>
.favorite-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: var(--color-muted-foreground);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: color 0.2s, background-color 0.2s, border-color 0.2s, transform 0.2s;
}

/* 带文字：胶囊按钮，宽度随文案自适应 */
.favorite-toggle.has-text {
  height: var(--control-height);
  padding: 0 16px;
  border: 1px solid var(--ref-line);
  border-radius: 999px;
  background: var(--ref-surface);
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.favorite-toggle.has-text:hover {
  color: #f59e0b;
  border-color: color-mix(in srgb, #f59e0b 55%, transparent);
  background: color-mix(in srgb, #f59e0b 8%, var(--ref-surface));
}

/* 纯图标：40×40 圆形按钮 */
.favorite-toggle.is-icon-only {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
}

.favorite-toggle.is-icon-only:hover {
  color: #f59e0b;
  transform: scale(1.1);
}

.favorite-toggle.is-favorited {
  color: #f59e0b;
}

.favorite-toggle.has-text.is-favorited {
  border-color: color-mix(in srgb, #f59e0b 55%, transparent);
  background: color-mix(in srgb, #f59e0b 10%, var(--ref-surface));
}

.favorite-toggle.is-favorited .favorite-toggle-icon {
  color: #f59e0b;
  filter: drop-shadow(0 0 3px rgba(245, 158, 11, 0.5));
}

.favorite-toggle.is-unfavorited {
  color: var(--color-muted-foreground);
}

.favorite-toggle:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.favorite-toggle-icon {
  font-size: 20px;
  flex-shrink: 0;
}
</style>
