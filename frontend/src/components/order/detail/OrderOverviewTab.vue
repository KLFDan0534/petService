<template>
  <div class="overview-tab">
    <section class="pet-profile card">
      <div class="pet-heading">
        <div class="pet-avatar" :style="avatarStyle(order.pet_avatar_wsh)">
          <span v-if="!order.pet_avatar_wsh">{{ petInitial }}</span>
        </div>
        <div class="pet-title">
          <span class="section-kicker">宠物档案</span>
          <h2>{{ order.pet_name_wsh || `宠物 #${order.pet_id_wsh || '-'}` }}</h2>
          <p>{{ petTypeLabel(order.pet_type_wsh) }} / {{ order.pet_breed_wsh || '未填写品种' }}</p>
        </div>
        <div class="pet-flags">
          <span :class="['badge', order.pet_vaccinated_wsh === 1 ? 'badge-success' : 'badge-disabled']">
            {{ order.pet_vaccinated_wsh === 1 ? '已免疫' : '未免疫' }}
          </span>
          <span :class="['badge', order.pet_sterilized_wsh === 1 ? 'badge-info' : 'badge-disabled']">
            {{ order.pet_sterilized_wsh === 1 ? '已绝育' : '未绝育' }}
          </span>
        </div>
      </div>

      <div class="pet-detail-grid">
        <div>
          <span>年龄</span>
          <strong>{{ formatAge(order.pet_age_wsh) }}</strong>
        </div>
        <div>
          <span>体重</span>
          <strong>{{ formatWeight(order.pet_weight_wsh) }}</strong>
        </div>
        <div>
          <span>性别</span>
          <strong>{{ genderLabel(order.pet_gender_wsh) }}</strong>
        </div>
        <div>
          <span>档案</span>
          <strong>{{ order.pet_description_wsh ? '已补充' : '待完善' }}</strong>
        </div>
      </div>

      <div class="pet-notes">
        <div>
          <span>性格与照护说明</span>
          <p>{{ order.pet_description_wsh || '暂无补充说明' }}</p>
        </div>
        <div>
          <span>过敏/禁忌</span>
          <p :class="{ danger: order.pet_allergies_wsh }">{{ order.pet_allergies_wsh || '暂无记录' }}</p>
        </div>
        <div>
          <span>生活习惯</span>
          <p>{{ order.pet_habits_wsh || '暂无记录' }}</p>
        </div>
      </div>
    </section>

    <section class="info-grid">
      <article class="card info-card">
        <div class="card-title">
          <span>订单信息</span>
          <strong>{{ statusLabel(order.status_wsh) }}</strong>
        </div>
        <dl class="fact-list">
          <div><dt>订单号</dt><dd>{{ order.order_no_wsh || '-' }}</dd></div>
          <div><dt>下单时间</dt><dd>{{ formatTime(order.created_at_wsh) }}</dd></div>
          <div><dt>服务时间</dt><dd>{{ formatDate(order.start_date_wsh) }} 至 {{ formatDate(order.end_date_wsh) }}</dd></div>
          <div><dt>服务天数</dt><dd>{{ order.days_wsh || '-' }} 天</dd></div>
          <div><dt>总金额</dt><dd>￥{{ money(order.total_amount_wsh) }}</dd></div>
          <div><dt>优惠金额</dt><dd>￥{{ money(order.discount_wsh) }}</dd></div>
        </dl>
      </article>

      <article class="card info-card">
        <div class="card-title">
          <span>服务信息</span>
          <strong>￥{{ money(order.price_per_day_wsh) }}/天</strong>
        </div>
        <dl class="fact-list">
          <div><dt>服务名称</dt><dd>{{ order.service_name_wsh || '-' }}</dd></div>
          <div><dt>商家名称</dt><dd>{{ order.merchant_name_wsh || '-' }}</dd></div>
          <div><dt>看护人</dt><dd>{{ order.keeper_name_wsh || '-' }}</dd></div>
          <div><dt>交接码</dt><dd class="handover-code">{{ order.handover_code_wsh || '-' }}</dd></div>
          <div><dt>服务说明</dt><dd>{{ order.service_description_wsh || '-' }}</dd></div>
        </dl>
      </article>

      <article class="card info-card">
        <div class="card-title">
          <span>交接安排</span>
          <strong>{{ order.merchant_name_wsh || '商家' }}</strong>
        </div>
        <dl class="fact-list">
          <div><dt>送达地址</dt><dd>{{ order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</dd></div>
          <div><dt>接回地址</dt><dd>{{ order.pickup_address_wsh || order.delivery_address_wsh || order.merchant_address_wsh || '-' }}</dd></div>
          <div><dt>送达时间</dt><dd>{{ formatTime(order.delivery_time_wsh) }}</dd></div>
          <div><dt>接回时间</dt><dd>{{ formatTime(order.pickup_time_wsh) }}</dd></div>
        </dl>
      </article>

      <article class="card info-card">
        <div class="card-title">
          <span>联系与备注</span>
          <strong>{{ order.owner_name_wsh || '主人' }}</strong>
        </div>
        <dl class="fact-list">
          <div><dt>主人姓名</dt><dd>{{ order.owner_name_wsh || '-' }}</dd></div>
          <div><dt>看护人电话</dt><dd>{{ order.keeper_phone_wsh || '-' }}</dd></div>
          <div><dt>商家电话</dt><dd>{{ order.merchant_phone_wsh || '-' }}</dd></div>
          <div><dt>备注</dt><dd>{{ order.remark_wsh || '-' }}</dd></div>
        </dl>
      </article>
    </section>

    <section v-if="order.start_photo_wsh" class="card start-photo-card">
      <div class="card-title">
        <span>开始服务照片</span>
      </div>
      <img :src="order.start_photo_wsh" alt="开始服务照片" class="start-photo">
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ order: { type: Object, required: true } })

const petInitial = computed(() => props.order.pet_name_wsh?.charAt(0) || '宠')

const statusMap = {
  pending: '待付款',
  paid: '已支付',
  confirmed: '待送达',
  delivered: '已送达',
  received: '已接收',
  in_progress: '服务中',
  completed: '已完成',
  cancelled: '已取消',
  refunding: '退款中',
  refunded: '已退款',
}

const typeMap = {
  dog: '狗',
  cat: '猫',
  rabbit: '兔子',
  bird: '鸟',
  fish: '鱼',
  hamster: '仓鼠',
  other: '其他',
}

function avatarStyle(url) {
  return url ? { backgroundImage: `url(${url})` } : {}
}

function petTypeLabel(type) {
  return typeMap[String(type || 'other').toLowerCase()] || type || '其他'
}

function statusLabel(status) {
  return statusMap[status] || status || '-'
}

function genderLabel(gender) {
  const value = Number(gender)
  if (value === 1) return '公'
  if (value === 2) return '母'
  return '未知'
}

function formatAge(age) {
  return age ? `${age} 个月` : '-'
}

function formatWeight(weight) {
  return weight ? `${weight} kg` : '-'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function money(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.overview-tab {
  display: grid;
  gap: 16px;
}
.card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}
.pet-profile {
  padding: 20px;
}
.pet-heading {
  display: flex;
  align-items: center;
  gap: 16px;
}
.pet-avatar {
  width: 78px;
  height: 78px;
  border-radius: 999px;
  background: var(--color-muted);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-weight: 800;
  font-size: 28px;
  flex: 0 0 auto;
}
.pet-title {
  min-width: 0;
  flex: 1;
}
.section-kicker {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.pet-title h2 {
  margin: 2px 0;
  font-size: 22px;
}
.pet-title p {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 14px;
}
.pet-flags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.pet-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 18px 0;
}
.pet-detail-grid div {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
}
.pet-detail-grid span,
.pet-notes span,
.card-title span,
.fact-list dt {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.pet-detail-grid strong {
  display: block;
  margin-top: 3px;
}
.pet-notes {
  display: grid;
  gap: 10px;
}
.pet-notes div {
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.pet-notes p {
  margin: 3px 0 0;
  font-size: 14px;
  white-space: pre-wrap;
}
.pet-notes .danger {
  color: var(--color-destructive);
  font-weight: 600;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.info-card,
.start-photo-card {
  padding: 18px;
}
.card-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 12px;
}
.card-title strong {
  color: var(--color-primary);
  font-size: 14px;
}
.fact-list {
  display: grid;
  gap: 10px;
}
.fact-list div {
  min-width: 0;
}
.fact-list dd {
  margin: 2px 0 0;
  font-size: 14px;
  overflow-wrap: anywhere;
}
.handover-code {
  font-family: var(--font-mono);
  font-weight: 800;
  letter-spacing: 2px;
}
.start-photo {
  width: 320px;
  max-width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-muted);
}
@media (max-width: 760px) {
  .pet-heading {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .pet-flags {
    justify-content: flex-start;
  }
  .pet-detail-grid,
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .pet-detail-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
