<template>
  <div>
    <DataTable :columns="[{ label: 'ID', key: 'id_wsh' }, { label: '金额', key: 'amount_wsh' }, { label: '原因', key: 'reason_wsh' }, { label: '状态', key: 'status_label_wsh' }]" :data="refunds">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="askConfirm('approve', row)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="askConfirm('reject', row)">拒绝</button>
          <!-- 完成退款是一键直发，不加二次确认 -->
          <button v-if="row.status_wsh === 'approved'" class="btn btn-sm btn-primary" :disabled="acting" @click="completeNow(row)">完成退款</button>
        </div>
      </template>
    </DataTable>

    <div style="display:flex;align-items:center;justify-content:flex-end;gap:8px;flex-wrap:wrap;margin-top:12px;font-size:13px">
      <div style="display:inline-flex;align-items:center;gap:6px;margin-right:auto">
        <span>每页</span>
        <select v-model.number="size" :disabled="loading" style="width:auto;min-width:84px" @change="changeSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </div>
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || page <= 1" @click="goPage(page - 1)">上一页</button>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || pages === 0 || page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>

    <!-- 二次确认：通过 / 拒绝 会真实改变订单状态，先确认再执行；拒绝必须写原因 -->
    <AppDialog
      :visible="!!pendingAction"
      :title="confirmMeta.title"
      @close="closeConfirm"
    >
      <template v-if="pendingAction">
        <p class="rf-confirm-desc">{{ confirmMeta.desc }}</p>

        <dl class="rf-confirm-facts">
          <div class="rf-fact">
            <dt>退款单</dt>
            <dd>#{{ pendingAction.row.id_wsh }}</dd>
          </div>
          <div class="rf-fact">
            <dt>订单号</dt>
            <dd class="rf-fact-mono">{{ pendingAction.row.order_no_wsh || '—' }}</dd>
          </div>
          <div class="rf-fact">
            <dt>退款金额</dt>
            <dd class="rf-fact-money">¥{{ formatMoney(pendingAction.row.amount_wsh) }}</dd>
          </div>
          <div class="rf-fact">
            <dt>申请原因</dt>
            <dd>{{ pendingAction.row.reason_wsh || '用户未填写原因' }}</dd>
          </div>
        </dl>

        <div v-if="pendingAction.type === 'reject'" class="rf-field">
          <label class="rf-field-label" for="rf-reject-reason">驳回原因<span class="rf-req">*</span></label>
          <textarea
            id="rf-reject-reason"
            v-model="rejectReason"
            class="rf-textarea"
            rows="3"
            maxlength="200"
            placeholder="例如：门店当天满房无法接待，建议改期后重新下单"
          />
          <p class="rf-count">{{ rejectReason.length }}/200</p>
          <p class="rf-hint">原因会存档在退款单上，并随站内信一起发给用户。</p>
        </div>

        <p v-if="confirmMeta.tip" :class="['rf-confirm-tip', { 'is-warn': confirmMeta.warn }]">
          {{ confirmMeta.tip }}
        </p>
      </template>

      <template #footer>
        <button class="btn btn-sm btn-outline" type="button" :disabled="acting" @click="closeConfirm">取消</button>
        <button
          :class="['btn', 'btn-sm', confirmMeta.btnClass]"
          type="button"
          :disabled="acting || !canConfirm"
          @click="runConfirm"
        >{{ acting ? '处理中…' : confirmMeta.btnText }}</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import { getRefundsPage, approveRefund, rejectRefund, completeRefund as apiCompleteRefund } from '@/api/refund'
import { RefundStatus, enrichWithStatus } from '@/constants/statusMaps'
import { formatMoney } from '@/utils/format'


const appStore = useAppStore()
// 退款单只读展示，能做的动作只有通过、拒绝、完成这三个
const refunds = ref([])
// 分页状态：后端已提供 /refunds/all/page
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const pageSizes = [20, 50, 100]

/** 待确认的动作：{ type: 'approve' | 'reject', row } */
const pendingAction = ref(null)
const rejectReason = ref('')
const acting = ref(false)

const CONFIRM_META = {
  approve: {
    title: '通过这笔退款申请？',
    desc: '通过只代表同意退款，订单会继续停留在「退款中」；还需要再点一次「完成退款」，钱才会真正退回用户账户余额。',
    tip: '此步不产生任何资金流水。',
    btnText: '确认通过',
    btnClass: 'btn-success',
    warn: false,
  },
  reject: {
    title: '拒绝这笔退款申请？',
    desc: '拒绝后订单会恢复到申请退款前的状态，用户可以继续使用这笔服务，也可以重新申请退款。',
    tip: '拒绝不可撤销，请先核对用户填写的原因与订单实际情况。',
    btnText: '确认拒绝',
    btnClass: 'btn-danger',
    warn: true,
  },
}

const confirmMeta = computed(() => CONFIRM_META[pendingAction.value?.type] || CONFIRM_META.approve)

/** 驳回原因必填；通过则无需额外输入 */
const canConfirm = computed(() => {
  if (pendingAction.value?.type !== 'reject') return true
  return rejectReason.value.trim().length >= 2
})

function askConfirm(type, row) {
  if (acting.value) return
  rejectReason.value = ''
  pendingAction.value = { type, row }
}

function closeConfirm() {
  if (acting.value) return
  pendingAction.value = null
}

async function runConfirm() {
  const action = pendingAction.value
  if (!action || acting.value || !canConfirm.value) return
  acting.value = true
  try {
    if (action.type === 'approve') {
      await approveRefund(action.row.id_wsh)
      appStore.addToast('已通过，记得再点「完成退款」才会打款', 'success')
    } else {
      await rejectRefund(action.row.id_wsh, rejectReason.value.trim())
      appStore.addToast('已拒绝，驳回原因已随站内信发送给用户', 'success')
    }
    pendingAction.value = null
    await loadRefunds()
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

/** 完成退款：一键直发，不弹确认 */
async function completeNow(row) {
  if (acting.value) return
  acting.value = true
  try {
    await apiCompleteRefund(row.id_wsh)
    appStore.addToast('退款完成，金额已退回用户余额', 'success')
    await loadRefunds()
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '操作失败', 'error')
  } finally {
    acting.value = false
  }
}

function enrichRefund(r) {
  return enrichWithStatus(r, 'status_wsh', RefundStatus)
}

async function loadRefunds() {
  loading.value = true
  try {
    const r = await getRefundsPage({ page: page.value, size: size.value })
    if (r.code === 200) {
      const data = r.data || {}
      refunds.value = (data.list || []).map(enrichRefund)
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    }
  } catch (e) {
    // 失败保留上一次结果
  } finally {
    loading.value = false
  }
}

async function changeSize() {
  page.value = 1
  await loadRefunds()
}

async function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  await loadRefunds()
}
onMounted(loadRefunds)
</script>

<style scoped>
/* 二次确认弹窗：说明 → 关键事实 → 风险提示，避免管理员只看到一句"确认吗" */
.rf-confirm-desc {
  margin: 0; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft);
}

.rf-confirm-facts {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px;
  margin: 16px 0 0; padding: 14px 16px;
  border: 1px solid var(--ref-line); border-radius: 12px; background: var(--ref-cream);
}
.rf-fact { min-width: 0; }
.rf-fact dt { margin: 0; font-size: 11px; color: var(--ref-muted); }
.rf-fact dd {
  margin: 4px 0 0; font-size: 13px; line-height: 1.5; color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.rf-fact-mono { font-family: var(--ref-font-mono); font-size: 12px; }
.rf-fact-money {
  font-family: var(--ref-font-display); font-size: 18px; font-weight: 500;
  color: var(--ref-brand-deep); font-variant-numeric: tabular-nums;
}

.rf-confirm-tip { margin: 12px 0 0; font-size: 11.5px; line-height: 1.7; color: var(--ref-muted); }
.rf-confirm-tip.is-warn { color: #a03422; }

/* 驳回原因输入 */
.rf-field { margin: 16px 0 0; }
.rf-field-label { display: block; margin-bottom: 8px; font-size: 12.5px; font-weight: 500; color: var(--ref-ink); }
.rf-req { margin-left: 3px; color: #a03422; }
.rf-textarea {
  width: 100%; min-height: 76px; padding: 10px 12px; resize: vertical;
  border: 1px solid var(--ref-line); border-radius: 12px;
  background: var(--ref-surface); color: var(--ref-ink);
  font-family: inherit; font-size: 13px; line-height: 1.6; outline: none;
  transition: border-color 150ms ease;
}
.rf-textarea::placeholder { color: color-mix(in srgb, var(--ref-muted) 80%, transparent); }
.rf-textarea:focus { border-color: color-mix(in srgb, var(--ref-ink) 40%, transparent); }
.rf-count { margin: 4px 0 0; text-align: right; font-size: 11px; color: var(--ref-muted); font-variant-numeric: tabular-nums; }
.rf-hint { margin: 6px 0 0; font-size: 11px; line-height: 1.6; color: var(--ref-muted); }

@media (max-width: 560px) {
  .rf-confirm-facts { grid-template-columns: 1fr; }
}
</style>
