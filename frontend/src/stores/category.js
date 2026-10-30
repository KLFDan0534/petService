import { defineStore } from 'pinia'
import { getServiceCategoryList } from '@/api/serviceCategory'

// 先按排序号排，号一样的再按 id，保证每次顺序都一样
function sortByOrder(list = []) {
  return [...list].sort((a, b) => {
    const orderDiff = (a.sort_order_wsh || 0) - (b.sort_order_wsh || 0)
    if (orderDiff !== 0) return orderDiff
    return (a.id_wsh || 0) - (b.id_wsh || 0)
  })
}

export const useCategoryStore = defineStore('category', {
  state: () => ({
    categories: [],
    categoryMap: {},
    codeMap: {},
    loaded: false
  }),
  getters: {
    // 拿 id 换分类名；没传 id 或查不到都统一给「服务」，页面上不会空着
    getCategoryName: (state) => (categoryId) => {
      if (!categoryId) return '服务'
      const cat = state.categoryMap[categoryId]
      return cat ? cat.name_wsh : '服务'
    },
    getCategoryCode: (state) => (categoryId) => {
      if (!categoryId) return ''
      const cat = state.categoryMap[categoryId]
      return cat ? cat.code_wsh : ''
    },
    getCategoryByCode: (state) => (code) => {
      return state.codeMap[code] || null
    },
    getChildren: (state) => (parentId) => {
      return sortByOrder(state.categories.filter(cat => (cat.parent_id_wsh || 0) === (parentId || 0)))
    },
    // 先把父子关系攒成一张表，再递归拼树，不用每层都翻一遍全表
    tree: (state) => {
      const childrenMap = new Map()
      state.categories.forEach(cat => {
        const parentId = cat.parent_id_wsh || 0
        if (!childrenMap.has(parentId)) childrenMap.set(parentId, [])
        childrenMap.get(parentId).push(cat)
      })

      const buildTree = (parentId = 0) =>
        sortByOrder(childrenMap.get(parentId) || []).map(cat => ({
          ...cat,
          children: buildTree(cat.id_wsh)
        }))

      return buildTree()
    }
  },
  actions: {
    // 分类表全局只拉一次，拉过就直接用缓存；要刷新就传 force
    async loadCategories(force = false) {
      if (this.loaded && !force) return
      try {
        const res = await getServiceCategoryList()
        if (res.code === 200) {
          this.categories = res.data || []
          this.categoryMap = {}
          this.codeMap = {}
          // 顺手建两份索引：按 id 查和按 code 查，后面用起来快
          this.categories.forEach(cat => {
            this.categoryMap[cat.id_wsh] = cat
            if (cat.code_wsh) this.codeMap[cat.code_wsh] = cat
          })
          this.loaded = true
        }
      } catch (e) {
        console.error('加载服务分类失败', e)
      }
    }
  }
})
