import { ref } from 'vue'

export function useSubmitGuard() {
  const submitting = ref(false)

  async function guard(fn) {
    if (submitting.value) return
    submitting.value = true
    try {
      await fn()
    } finally {
      submitting.value = false
    }
  }

  return { submitting, guard }
}
