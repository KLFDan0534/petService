<template>
  <div class="merchant-section">
    <div class="section-label">商家服务</div>
    <div v-if="merchantStatus === null" class="merchant-entry" @click="$emit('apply')">
      <div class="merchant-icon">🏪</div>
      <div class="merchant-info">
        <div class="merchant-title">申请成为商家</div>
        <div class="merchant-desc">入驻平台，发布宠物寄养服务</div>
      </div>
      <span class="merchant-arrow">→</span>
    </div>
    <div v-else-if="merchantStatus === 0" class="merchant-entry" style="cursor:default">
      <div class="merchant-icon">⏳</div>
      <div class="merchant-info">
        <div class="merchant-title">商家审核中</div>
        <div class="merchant-desc">管理员正在审核您的申请，请耐心等待</div>
      </div>
      <span class="badge badge-warning">待审核</span>
    </div>
    <div v-else-if="merchantStatus === 1" class="merchant-entry" @click="$emit('goDashboard')">
      <div class="merchant-icon">✅</div>
      <div class="merchant-info">
        <div class="merchant-title">商家中心</div>
        <div class="merchant-desc">管理您的商家信息、订单和服务</div>
      </div>
      <span class="merchant-arrow">→</span>
    </div>
    <div v-else-if="merchantStatus === 2" class="merchant-entry" @click="$emit('apply')">
      <div class="merchant-icon">❌</div>
      <div class="merchant-info">
        <div class="merchant-title">申请被拒绝</div>
        <div class="merchant-desc">点击重新申请</div>
      </div>
      <span class="merchant-arrow">→</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  merchantStatus: { type: Number, default: null },
})
defineEmits(['apply', 'goDashboard'])
</script>

<style scoped>
.merchant-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid #dee0e3;
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 12px;
}
.merchant-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.merchant-entry:hover {
  background: #f5f6f7;
}
.merchant-icon {
  font-size: 24px;
}
.merchant-info {
  flex: 1;
}
.merchant-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}
.merchant-desc {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.merchant-arrow {
  font-size: 16px;
  color: #8f959e;
}
</style>
