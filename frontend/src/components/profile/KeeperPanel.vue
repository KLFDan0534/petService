<template>
  <div class="keeper-section">
    <div class="section-label">寄养员服务</div>
    <div v-if="keeperStatus === null && !loading" class="keeper-entry" @click="$emit('apply')">
      <div class="keeper-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </div>
      <div class="keeper-info">
        <div class="keeper-title">申请成为寄养员</div>
        <div class="keeper-desc">加入商户，提供宠物寄养服务</div>
      </div>
      <span class="keeper-arrow">→</span>
    </div>
    <div v-else-if="loading" class="keeper-entry" style="cursor:default">
      <div class="keeper-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </div>
      <div class="keeper-info">
        <div class="keeper-title">加载中...</div>
        <div class="keeper-desc">请稍候</div>
      </div>
    </div>
    <div v-else-if="keeperStatus === 0" class="keeper-entry" style="cursor:default">
      <div class="keeper-icon">⏳</div>
      <div class="keeper-info">
        <div class="keeper-title">寄养员审核中</div>
        <div class="keeper-desc">商户正在审核您的申请，请耐心等待</div>
      </div>
      <span class="badge badge-warning">待审核</span>
    </div>
    <div v-else-if="keeperStatus === 1" class="keeper-entry" @click="$emit('goWorkflow')">
      <div class="keeper-icon">✅</div>
      <div class="keeper-info">
        <div class="keeper-title">寄养员工作台</div>
        <div class="keeper-desc">管理您的寄养服务、订单和排班</div>
      </div>
      <span class="badge badge-success">在线</span>
    </div>
    <div v-else-if="keeperStatus === 3" class="keeper-entry" @click="$emit('goWorkflow')">
      <div class="keeper-icon">💤</div>
      <div class="keeper-info">
        <div class="keeper-title">寄养员工作台</div>
        <div class="keeper-desc">您当前为离线状态，点击进入工作台切换</div>
      </div>
      <span class="badge badge-muted">离线</span>
    </div>
    <div v-else-if="keeperStatus === 4" class="keeper-entry" @click="$emit('goWorkflow')">
      <div class="keeper-icon">⏰</div>
      <div class="keeper-info">
        <div class="keeper-title">寄养员工作台</div>
        <div class="keeper-desc">您当前为忙碌状态，点击进入工作台切换</div>
      </div>
      <span class="badge badge-warning">忙碌</span>
    </div>
    <div v-else-if="keeperStatus === 2" class="keeper-entry" @click="$emit('apply')">
      <div class="keeper-icon">❌</div>
      <div class="keeper-info">
        <div class="keeper-title">申请被拒绝</div>
        <div class="keeper-desc">点击重新申请</div>
      </div>
      <span class="keeper-arrow">→</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  keeperStatus: { type: Number, default: null },
  loading: { type: Boolean, default: false },
})
defineEmits(['apply', 'goWorkflow'])
</script>

<style scoped>
.keeper-section {
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
.keeper-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.keeper-entry:hover {
  background: #f5f6f7;
}
.keeper-icon {
  font-size: 24px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #646a73;
}
.keeper-info {
  flex: 1;
}
.keeper-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}
.keeper-desc {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.keeper-arrow {
  font-size: 16px;
  color: #8f959e;
}
.badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 2px;
  font-weight: 500;
}
.badge-warning {
  color: #ad6800;
  background: #fffbe6;
  border: 1px solid #ffe58f;
}
.badge-success {
  color: #237804;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}
.badge-muted {
  color: #8f959e;
  background: #f5f6f7;
  border: 1px solid #dee0e3;
}
</style>
