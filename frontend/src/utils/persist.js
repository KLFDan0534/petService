export function createPersistPlugin() {
  return (context) => {
    const { store, options } = context
    if (options.persist === true) {
      const key = `persist:${store.$id}`
      try {
        const saved = localStorage.getItem(key)
        if (saved) {
          const parsed = JSON.parse(saved)
          store.$patch(parsed)
        }
      } catch (e) {}
      store.$subscribe((mutation, state) => {
        localStorage.setItem(key, JSON.stringify(state))
      })
    }
  }
}
