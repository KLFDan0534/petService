<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="label"
    :title="label"
    @click="appStore.toggleTheme()"
  >
    <AppIcon aria-hidden="true">
      <Moon v-if="appStore.isDarkTheme" />
      <Sunny v-else />
    </AppIcon>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const label = computed(() => appStore.isDarkTheme ? '切换到日间主题' : '切换到夜间主题')
</script>
