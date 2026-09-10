<template>
  <div class="ad-page">
    <div class="ad-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ad-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ad-crumb-link">首页</router-link>
        <span class="ad-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="ad-crumb-link">个人中心</router-link>
        <span class="ad-crumb-sep" aria-hidden="true">›</span>
        <span class="ad-crumb-here">接送地址</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ad-head">
        <div class="ad-head-copy">
          <div class="ad-eyebrow" aria-hidden="true">
            <span class="ad-eyebrow-line"></span>
            <span>地址</span>
          </div>
          <h1 class="ad-title">接送地址</h1>
          <p class="ad-sub">上门接送、送回与美容到家都会用到这里的地址。设为默认后，下单时会自动带入。</p>
        </div>
        <div class="ad-actions">
          <button type="button" class="cta cta-primary" @click="openAdd">添加地址</button>
        </div>
      </header>

      <!-- ═══ 01 · 已保存地址 ═══ -->
      <section class="ad-section" aria-label="已保存地址">
        <header class="ad-sec-head">
          <div class="ad-head-copy">
            <p class="ad-eyebrow ad-sec-eyebrow">
              <span class="ad-idx">01</span>
              <span class="ad-line" aria-hidden="true"></span>
              <span>已保存</span>
            </p>
            <h2 class="ad-sec-title">已保存地址</h2>
            <p class="ad-sec-desc">默认地址排在最前，接送司机按坐标导航。</p>
          </div>
        </header>

        <template v-if="loading">
          <div class="ad-grid"><div v-for="i in 2" :key="i" class="ad-skeleton" /></div>
        </template>

        <div v-else-if="!addresses.length" class="ad-empty">
          <p class="ad-empty-title">还没有接送地址</p>
          <p class="ad-empty-desc">添加一个常用地址，预约上门接送时就不用每次重新填写联系人与门牌号。</p>
          <button type="button" class="cta cta-primary" @click="openAdd">添加地址</button>
        </div>

        <div v-else class="ad-grid">
          <article v-for="a in addresses" :key="a.id_wsh" class="ad-card" :class="{ 'is-default': a.is_default_wsh }">
            <div class="ad-card-head">
              <div class="ad-id">
                <h3 class="ad-name">{{ a.name_wsh || '未填写联系人' }}</h3>
                <p class="ad-phone">{{ a.phone_wsh || '未填写电话' }}</p>
              </div>
              <span v-if="a.is_default_wsh" class="badge badge-action">默认地址</span>
            </div>
            <p class="ad-address">{{ a.address_wsh }}</p>
            <p class="ad-detail">{{ a.detail_wsh }}</p>
            <p v-if="a.latitude_wsh != null && a.longitude_wsh != null" class="ad-coord">
              {{ Number(a.latitude_wsh).toFixed(4) }}, {{ Number(a.longitude_wsh).toFixed(4) }}
            </p>
            <div class="ad-foot">
              <button v-if="!a.is_default_wsh" type="button" class="ad-quiet" @click="setDefault(a.id_wsh)">设为默认</button>
              <span v-else class="ad-default-note">下单时默认使用</span>
              <div class="ad-ops">
                <button type="button" class="ad-quiet" @click="openEdit(a)">编辑</button>
                <button type="button" class="ad-quiet ad-danger" @click="removeAddress(a.id_wsh)">删除</button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>

    <!-- ═══ 添加 / 编辑地址 dialog ═══ -->
    <div v-if="showForm" class="dlg-overlay" @click.self="showForm = false">
      <div class="dlg-panel dlg-panel-wide" role="dialog" aria-modal="true" :aria-label="editingId ? '编辑地址' : '添加地址'">
        <div class="dlg-head">
          <h3 class="dlg-title">{{ editingId ? '编辑地址' : '添加地址' }}</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="showForm = false">✕</button>
        </div>
        <form @submit.prevent="saveAddress">
          <div class="dlg-body">
            <div class="dlg-row">
              <div class="dlg-field">
                <label class="dlg-label" for="ad-name">联系人</label>
                <input id="ad-name" v-model="form.name_wsh" class="dlg-input" required>
              </div>
              <div class="dlg-field">
                <label class="dlg-label" for="ad-phone">电话</label>
                <input id="ad-phone" v-model="form.phone_wsh" class="dlg-input" required>
              </div>
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="ad-address">所在地址</label>
              <AmapAddressPicker
                v-model="form.address_wsh"
                v-model:latitude="form.latitude_wsh"
                v-model:longitude="form.longitude_wsh"
                placeholder="搜索地址或点击定位"
              />
            </div>
            <div class="dlg-field">
              <label class="dlg-label" for="ad-detail">详细地址</label>
              <input id="ad-detail" v-model="form.detail_wsh" class="dlg-input" required>
            </div>
            <p class="dlg-note">地址与坐标用于接送司机导航，保存后即可在下单时选用。</p>
          </div>
          <div class="dlg-foot">
            <button type="button" class="dlg-cancel" @click="showForm = false">取消</button>
            <button type="submit" class="cta cta-primary">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { getAddresses, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'

const appStore = useAppStore()
const addresses = ref([])
const loading = ref(true)
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ name_wsh: '', phone_wsh: '', address_wsh: '', detail_wsh: '', latitude_wsh: null, longitude_wsh: null })

function resetForm() { form.name_wsh = ''; form.phone_wsh = ''; form.address_wsh = ''; form.detail_wsh = ''; form.latitude_wsh = null; form.longitude_wsh = null; editingId.value = null }

function openAdd() { resetForm(); showForm.value = true }

function openEdit(a) {
  editingId.value = a.id_wsh
  form.name_wsh = a.name_wsh || ''
  form.phone_wsh = a.phone_wsh || ''
  form.address_wsh = a.address_wsh || ''
  form.detail_wsh = a.detail_wsh || ''
  form.latitude_wsh = a.latitude_wsh ?? null
  form.longitude_wsh = a.longitude_wsh ?? null
  showForm.value = true
}

onMounted(async () => {
  try { const r = await getAddresses(); if (r.code === 200) addresses.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function saveAddress() {
  try {
    if (!form.address_wsh || form.latitude_wsh == null || form.longitude_wsh == null) {
      appStore.addToast('请选择或定位地址', 'warning')
      return
    }
    const payload = {
      name_wsh: form.name_wsh,
      phone_wsh: form.phone_wsh,
      address_wsh: form.address_wsh,
      detail_wsh: form.detail_wsh,
      latitude_wsh: form.latitude_wsh,
      longitude_wsh: form.longitude_wsh,
    }
    let r
    if (editingId.value) {
      r = await updateAddress(editingId.value, payload)
    } else {
      r = await createAddress(payload)
    }
    if (r.code === 200) {
      appStore.addToast(editingId.value ? '更新成功' : '添加成功', 'success')
      showForm.value = false
      const idx = addresses.value.findIndex(a => a.id_wsh === editingId.value)
      if (editingId.value && idx !== -1) {
        addresses.value[idx] = r.data
      } else {
        addresses.value.push(r.data)
      }
    }
  } catch (e) { appStore.addToast('保存失败', 'error') }
}

async function removeAddress(id) {
  if (!confirm('确定删除该地址？')) return
  try {
    const r = await deleteAddress(id)
    if (r.code === 200) {
      appStore.addToast('删除成功', 'success')
      addresses.value = addresses.value.filter(a => a.id_wsh !== id)
    }
  } catch (e) { appStore.addToast('删除失败', 'error') }
}

async function setDefault(id) {
  try {
    const r = await setDefaultAddress(id)
    if (r.code === 200) {
      appStore.addToast('已设为默认', 'success')
      addresses.value.forEach(a => { a.is_default_wsh = a.id_wsh === id })
    }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>

<style scoped>
.ad-page {
  width: 100%;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ad-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ad-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ad-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ad-crumb-link:hover { color: var(--ref-ink); }
.ad-crumb-sep { color: var(--ref-line); }
.ad-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ad-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ad-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ad-idx { font-variant-numeric: tabular-nums; }
.ad-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ad-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 24px;
  padding: 40px 0 28px;
}
.ad-head-copy { min-width: 0; }
.ad-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ad-sub {
  margin: 14px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}
.ad-actions { display: flex; flex-wrap: wrap; gap: 10px; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-control);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: var(--radius-inline);
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-action { background: var(--ref-brand); color: #fff; }

/* ═══ Section ═══ */
.ad-section { margin-top: 56px; }
.ad-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.ad-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ad-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}
.ad-sec-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}

/* ═══ Grid ═══ */
.ad-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

/* ═══ Skeleton ═══ */
.ad-skeleton {
  height: 208px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ad-shimmer 1.3s linear infinite;
}

/* ═══ Empty ═══ */
.ad-empty {
  margin-top: 20px;
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  text-align: center;
}
.ad-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ad-empty-desc { margin: 10px 0 0; font-size: 13px; color: var(--ref-muted); }
.ad-empty .cta { margin-top: 22px; }

/* ═══ Address card ═══ */
.ad-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 20px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
  transition: border-color 0.15s, transform 0.15s, box-shadow 0.15s;
}
.ad-card:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent);
  transform: translateY(-3px);
  box-shadow: var(--shadow-lift);
}
.ad-card.is-default {
  border-color: color-mix(in srgb, var(--ref-brand) 30%, var(--ref-line));
  background: color-mix(in srgb, var(--ref-cream) 38%, var(--ref-surface));
}
.ad-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.ad-id { min-width: 0; }
.ad-name {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ad-phone {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.ad-address {
  margin: 16px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  overflow-wrap: anywhere;
}
.ad-detail {
  margin: 4px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink);
  overflow-wrap: anywhere;
}
.ad-coord {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.ad-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
}
.ad-default-note { font-size: 11px; color: var(--ref-muted); }
.ad-ops { display: flex; align-items: center; gap: 16px; }
.ad-quiet {
  padding: 0;
  background: none;
  border: none;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ref-brand);
  cursor: pointer;
  transition: color 0.15s;
}
.ad-quiet:hover { color: var(--ref-brand-deep); }
.ad-danger { color: var(--color-danger); }
.ad-danger:hover { color: var(--color-danger); opacity: 0.75; }

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: ad-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: 20px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: var(--shadow-pop);
  animation: ad-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-panel-wide { max-width: 520px; }
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.dlg-title { margin: 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 20px 24px 8px; display: grid; gap: 16px; }
.dlg-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.dlg-field { display: grid; gap: 8px; }
.dlg-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.dlg-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-input::placeholder { color: var(--ref-muted); }
.dlg-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.dlg-note { margin: 0; font-size: 11.5px; line-height: 1.7; color: var(--ref-muted); }
.dlg-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 24px;
}
.dlg-cancel {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══ AmapAddressPicker warm override ═══ */
.dlg-panel :deep(.amap-address-picker .form-control) {
  height: 42px;
  border: 1px solid var(--ref-line);
  border-radius: 10px;
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  padding: 0 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.dlg-panel :deep(.amap-address-picker .form-control:focus) {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.dlg-panel :deep(.amap-address-picker .btn) {
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 12.5px;
  font-weight: 500;
}
.dlg-panel :deep(.amap-address-picker .btn:hover) {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  color: var(--ref-ink);
}
.dlg-panel :deep(.amap-address-picker .tips-panel) {
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-md);
  box-shadow: 0 24px 48px -32px color-mix(in srgb, var(--ref-ink) 60%, transparent);
}
.dlg-panel :deep(.amap-address-picker .tip-item:hover) { background: color-mix(in srgb, var(--ref-cream) 55%, transparent); }
.dlg-panel :deep(.amap-address-picker .result-title) { color: var(--ref-ink); }
.dlg-panel :deep(.amap-address-picker .result-address) { color: var(--ref-muted); }
.dlg-panel :deep(.amap-address-picker .result-meta) {
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand);
}

/* ═══ Animations ═══ */
@keyframes ad-shimmer { to { background-position: -200% 0; } }
@keyframes ad-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes ad-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .ad-grid { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .ad-shell { padding: 0 16px; }
  .ad-head { padding: 30px 0 22px; }
  .ad-sub { font-size: 13.5px; }
  .ad-section { margin-top: 44px; }
  .ad-card { padding: 16px; }
  .dlg-row { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .ad-skeleton { animation: none; }
}
</style>
