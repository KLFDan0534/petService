<template>
  <div>
    <PageHero title="地址管理" subtitle="管理您的收货地址" />
    <button class="btn btn-primary" style="margin-bottom:24px" @click="openAdd">+ 添加地址</button>
    <div v-for="a in addresses" :key="a.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div>
          <div style="font-weight:600">{{ a.name_wsh }} <span style="font-weight:400;color:var(--color-muted-foreground)">{{ a.phone_wsh }}</span></div>
          <div style="font-size:14px;color:var(--color-muted-foreground);margin-top:4px">{{ a.address_wsh }} {{ a.detail_wsh }}</div>
        </div>
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="!a.is_default_wsh" class="btn btn-sm btn-primary" @click="setDefault(a.id_wsh)">设为默认</button>
          <span v-else class="badge badge-success" style="padding:4px 8px">默认地址</span>
          <button class="btn btn-sm btn-outline" @click="openEdit(a)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deleteAddress(a.id_wsh)">删除</button>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && addresses.length === 0" title="暂无地址" icon="📍" description="添加您的常用地址" />
    
    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ editingId ? '编辑地址' : '添加地址' }}</h2>
        <form @submit.prevent="saveAddress">
          <div class="form-row">
            <div class="form-group"><label>联系人</label><input v-model="form.name_wsh" required></div>
            <div class="form-group"><label>电话</label><input v-model="form.phone_wsh" required></div>
          </div>
          <div class="form-row">
            <div class="form-group"><label>省份</label><input v-model="form.province" required></div>
            <div class="form-group"><label>城市</label><input v-model="form.city" required></div>
            <div class="form-group"><label>区县</label><input v-model="form.district" required></div>
          </div>
          <div class="form-group"><label>详细地址</label><input v-model="form.detail_wsh" required></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
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
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const addresses = ref([])
const loading = ref(true)
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ name_wsh: '', phone_wsh: '', province: '', city: '', district: '', detail_wsh: '' })

function resetForm() { form.name_wsh = ''; form.phone_wsh = ''; form.province = ''; form.city = ''; form.district = ''; form.detail_wsh = ''; editingId.value = null }

function openAdd() { resetForm(); showForm.value = true }

function openEdit(a) {
  editingId.value = a.id_wsh
  form.name_wsh = a.name_wsh || ''
  form.phone_wsh = a.phone_wsh || ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.detail_wsh = a.detail_wsh || ''
  showForm.value = true
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/addresses'); if (r.code === 200) addresses.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function saveAddress() {
  try {
    const payload = {
      name_wsh: form.name_wsh,
      phone_wsh: form.phone_wsh,
      address_wsh: [form.province, form.city, form.district].filter(Boolean).join(''),
      detail_wsh: form.detail_wsh,
    }
    let r
    if (editingId.value) {
      r = await authStore.apiPut(`/api/addresses/${editingId.value}`, payload)
    } else {
      r = await authStore.apiPost('/api/addresses', payload)
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

async function deleteAddress(id) {
  if (!confirm('确定删除该地址？')) return
  try {
    const r = await authStore.apiDelete(`/api/addresses/${id}`)
    if (r.code === 200) {
      appStore.addToast('删除成功', 'success')
      addresses.value = addresses.value.filter(a => a.id_wsh !== id)
    }
  } catch (e) { appStore.addToast('删除失败', 'error') }
}

async function setDefault(id) {
  try {
    const r = await authStore.apiPost(`/api/addresses/${id}/default`, {})
    if (r.code === 200) {
      appStore.addToast('已设为默认', 'success')
      addresses.value.forEach(a => { a.is_default_wsh = a.id_wsh === id })
    }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
