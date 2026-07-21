<template>
  <div class="chat-entry-page">
    <section class="entry-panel">
      <h1>Order chat</h1>
      <p>Chat is available inside each order, so both sides are verified participants before messages can be sent.</p>
      <div class="entry-actions">
        <router-link to="/orders" class="btn btn-primary">View orders</router-link>
        <router-link to="/dashboard" class="btn btn-outline">Find services</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  if (route.query.orderId) {
    router.replace({
      name: 'OrderDetail',
      params: { id: route.query.orderId },
      query: { tab: 'chat' },
    })
  }
})
</script>

<style scoped>
.chat-entry-page {
  min-height: 60vh;
  display: grid;
  place-items: center;
}
.entry-panel {
  width: min(560px, 100%);
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
}
.entry-panel h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
.entry-panel p {
  margin: 0;
  color: var(--color-muted-foreground);
  line-height: 1.6;
}
.entry-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}
</style>
