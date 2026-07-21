<template>
  <button
    type="button"
    class="favorite-toggle btn"
    :class="[isFavorited ? activeClass : inactiveClass]"
    :disabled="buttonDisabled"
    :aria-label="ariaLabel"
    @click.stop="handleClick"
  >
    <el-icon class="favorite-toggle-icon">
      <Star />
    </el-icon>
    <span v-if="showText">{{ isFavorited ? favoritedLabel : unfavoritedLabel }}</span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Star } from '@element-plus/icons-vue'
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
    default: 'btn-primary',
  },
  inactiveClass: {
    type: String,
    default: 'btn-outline',
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
  gap: 6px;
  white-space: nowrap;
}

.favorite-toggle-icon {
  font-size: 16px;
}
</style>
