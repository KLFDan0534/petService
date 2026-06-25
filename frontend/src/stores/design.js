import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { generateDesignSystem } from '@/design-system/engine/search'

export const useDesignStore = defineStore('design', () => {
  const activeDesign = ref(null)
  const cssVars = ref({})

  const applyDesignSystem = (query, projectName) => {
    const ds = generateDesignSystem(query, projectName)
    activeDesign.value = ds
    cssVars.value = ds.colors
    // Apply CSS custom properties
    const root = document.documentElement
    if (ds.colors.primary) root.style.setProperty('--color-primary', ds.colors.primary)
    if (ds.colors.background) root.style.setProperty('--color-background', ds.colors.background)
    if (ds.colors.foreground) root.style.setProperty('--color-foreground', ds.colors.foreground)
    if (ds.colors.accent) root.style.setProperty('--color-accent', ds.colors.accent)
    if (ds.colors.border) root.style.setProperty('--color-border', ds.colors.border)
    if (ds.colors.card) root.style.setProperty('--color-card', ds.colors.card)
    if (ds.colors.muted) root.style.setProperty('--color-muted', ds.colors.muted)
    return ds
  }

  return { activeDesign, cssVars, applyDesignSystem }
})
