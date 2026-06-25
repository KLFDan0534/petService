<template>
  <div>
    <PageHero title="商家列表" subtitle="浏览所有宠物服务商家" />
    <div class="form-group" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;margin-bottom:20px">
      <div>
        <label>纬度</label>
        <input v-model="lat" placeholder="纬度" class="input" style="width:120px" />
      </div>
      <div>
        <label>经度</label>
        <input v-model="lng" placeholder="经度" class="input" style="width:120px" />
      </div>
      <div>
        <label>半径(km)</label>
        <input v-model="radius" placeholder="半径" class="input" style="width:100px" />
      </div>
      <button class="btn btn-primary" @click="nearbySearch">附近搜索</button>
      <button class="btn btn-outline" @click="resetSearch">重置</button>
    </div>
    <div style="margin-bottom:16px;display:flex;gap:8px;align-items:center">
      <button class="btn btn-primary btn-sm" @click="openRegister">注册商家</button>
      <button class="btn btn-outline btn-sm" @click="openMyMerchant">我的商家</button>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="pet-grid">
      <div v-for="m in merchants" :key="m.id_wsh" class="pet-card" style="cursor:pointer" @click="viewMerchant(m.id_wsh)">
        <div class="pet-avatar">🏪</div>
        <div class="pet-name">{{ m.name_wsh || m.username_wsh }}</div>
        <div class="pet-info">{{ m.description_wsh || '暂无介绍' }}</div>
        <div class="pet-info">⭐ {{ m.rating_wsh || '暂无评分' }} · {{ m.orderCount || 0 }} 订单</div>
        <div style="margin-top:12px"><button class="btn btn-sm btn-primary" @click.stop="$router.push('/dashboard')">查看服务</button></div>
      </div>
    </div>

    <div v-if="detailMerchant" class="modal-overlay" @mousedown.self="detailMerchant = null">
      <div class="modal" style="max-width:600px">
        <h2>{{ detailMerchant.name_wsh || '商家详情' }}</h2>
        <div class="card" style="padding:16px;margin-top:12px">
          <div><strong>名称：</strong>{{ detailMerchant.name_wsh || '-' }}</div>
          <div><strong>描述：</strong>{{ detailMerchant.description_wsh || '-' }}</div>
          <div><strong>评分：</strong>⭐ {{ detailMerchant.rating_wsh || '暂无' }}</div>
          <div><strong>联系电话：</strong>{{ detailMerchant.phone_wsh || '-' }}</div>
          <div><strong>地址：</strong>{{ detailMerchant.address_wsh || '-' }}</div>
          <div><strong>审核状态：</strong>
            <span :class="['badge', detailMerchant.status_wsh === 1 ? 'badge-success' : detailMerchant.status_wsh === 0 ? 'badge-warning' : 'badge-danger']">
              {{ detailMerchant.status_wsh === 1 ? '已通过' : detailMerchant.status_wsh === 0 ? '待审核' : '已拒绝' }}
            </span>
          </div>
        </div>
        <div style="margin-top:12px;display:flex;gap:8px">
          <button class="btn btn-sm btn-outline" @click="openEditMerchant(detailMerchant)">编辑</button>
          <button class="btn btn-sm btn-secondary" @click="detailMerchant = null">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="showMerchantForm" class="modal-overlay" @mousedown.self="showMerchantForm = false">
      <div class="modal" style="max-width:600px">
        <h2>{{ editingMerchantId ? '编辑商家' : '注册商家' }}</h2>
        <form @submit.prevent="saveMerchant">
          <div class="form-group">
            <label>商家名称</label>
            <input v-model="merchantForm.name_wsh" required placeholder="输入商家名称">
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="merchantForm.description_wsh" rows="3" placeholder="商家介绍"></textarea>
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input v-model="merchantForm.phone_wsh" placeholder="联系电话">
          </div>
          <div class="form-group">
            <label>地址</label>
            <input v-model="merchantForm.address_wsh" placeholder="商家地址">
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showMerchantForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const merchants = ref([])
const loading = ref(true)
const lat = ref('')
const lng = ref('')
const radius = ref('')
const detailMerchant = ref(null)
const showMerchantForm = ref(false)
const editingMerchantId = ref(null)
const merchantForm = reactive({ name_wsh: '', description_wsh: '', phone_wsh: '', address_wsh: '' })

async function loadMerchants() {
  loading.value = true
  try { const r = await authStore.apiGet('/api/merchants'); if (r.code === 200) merchants.value = r.data }
  catch (e) {}
  finally { loading.value = false }
}

async function nearbySearch() {
  if (!lat.value || !lng.value) return
  loading.value = true
  try {
    const r = await authStore.apiGet('/api/merchants/nearby', { lat: lat.value, lng: lng.value, radius: radius.value || undefined })
    if (r.code === 200) merchants.value = r.data
  } catch (e) {}
  finally { loading.value = false }
}

function resetSearch() {
  lat.value = ''
  lng.value = ''
  radius.value = ''
  loadMerchants()
}

function openRegister() {
  editingMerchantId.value = null
  merchantForm.name_wsh = ''; merchantForm.description_wsh = ''; merchantForm.phone_wsh = ''; merchantForm.address_wsh = ''
  showMerchantForm.value = true
}

async function openMyMerchant() {
  try {
    const r = await authStore.apiGet('/api/merchants/my')
    if (r.code === 200 && r.data) {
      detailMerchant.value = r.data
    } else {
      appStore.addToast('您还没有注册商家', 'info')
    }
  } catch (e) { appStore.addToast('查询失败', 'error') }
}

async function viewMerchant(id) {
  try {
    const r = await authStore.apiGet(`/api/merchants/${id}`)
    if (r.code === 200) detailMerchant.value = r.data
  } catch (e) { appStore.addToast('获取详情失败', 'error') }
}

function openEditMerchant(m) {
  editingMerchantId.value = m.id_wsh
  merchantForm.name_wsh = m.name_wsh || ''
  merchantForm.description_wsh = m.description_wsh || ''
  merchantForm.phone_wsh = m.phone_wsh || ''
  merchantForm.address_wsh = m.address_wsh || ''
  showMerchantForm.value = true
  detailMerchant.value = null
}

async function saveMerchant() {
  try {
    let r
    if (editingMerchantId.value) {
      r = await authStore.apiPut(`/api/merchants/${editingMerchantId.value}`, merchantForm)
    } else {
      r = await authStore.apiPost('/api/merchants', merchantForm)
    }
    if (r.code === 200) {
      appStore.addToast(editingMerchantId.value ? '更新成功' : '注册成功', 'success')
      showMerchantForm.value = false
      loadMerchants()
    }
  } catch (e) { appStore.addToast('保存失败', 'error') }
}

onMounted(loadMerchants)
</script>
