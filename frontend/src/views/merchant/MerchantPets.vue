<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>宠物管理</h2>
      <div style="display:flex;gap:8px">
        <button class="btn btn-primary btn-sm" @click="showAdoptForm = !showAdoptForm">发布领养宠物</button>
        <button class="btn btn-secondary btn-sm">发布宠物</button>
      </div>
    </div>

    <div v-if="showAdoptForm" class="card" style="margin-bottom:16px;padding:20px">
      <h3 style="margin-bottom:16px">发布领养宠物</h3>
      <form @submit.prevent="createAdoptPet">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <div class="form-group"><label>名称</label><input v-model="adoptForm.name_wsh" required></div>
          <div class="form-group"><label>种类</label><input v-model="adoptForm.type_wsh" required></div>
          <div class="form-group"><label>品种</label><input v-model="adoptForm.breed_wsh"></div>
          <div class="form-group"><label>年龄</label><input v-model="adoptForm.age_wsh" type="number"></div>
          <div class="form-group"><label>性别</label><select v-model="adoptForm.gender_wsh"><option value="">请选择</option><option value="male">公</option><option value="female">母</option></select></div>
          <div class="form-group"><label>体重(kg)</label><input v-model="adoptForm.weight_wsh" type="number" step="0.1"></div>
          <div class="form-group"><label>封面图片</label><input v-model="adoptForm.cover_image_wsh"></div>
          <div class="form-group"><label>领养费</label><input v-model="adoptForm.adoption_fee_wsh" type="number"></div>
        </div>
        <div class="form-group" style="margin-top:12px"><label>描述</label><textarea v-model="adoptForm.description_wsh" rows="3"></textarea></div>
        <div class="modal-actions" style="margin-top:12px">
          <button type="button" class="btn btn-secondary btn-sm" @click="showAdoptForm = false">取消</button>
          <button type="submit" class="btn btn-primary btn-sm">提交</button>
        </div>
      </form>
    </div>

    <DataTable :columns="[{label:'名称',key:'name_wsh'},{label:'种类',key:'type_wsh'},{label:'年龄',key:'age_wsh'},{label:'价格/天',key:'adoption_fee_wsh'},{label:'状态',key:'status_label_wsh'}]" :data="pets">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button class="btn btn-sm btn-secondary">编辑</button>
          <button class="btn btn-sm btn-danger">下架</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const authStore = useAuthStore()
const appStore = useAppStore()
const pets = ref([])
const showAdoptForm = ref(false)
const adoptForm = reactive({ name_wsh: '', type_wsh: '', breed_wsh: '', age_wsh: '', gender_wsh: '', weight_wsh: '', description_wsh: '', cover_image_wsh: '', adoption_fee_wsh: '' })

const petStatusMap = { available: { text: '可领养', cls: 'badge-success' }, adopted: { text: '已被领养', cls: 'badge-secondary' } }

function enrichPet(p) {
  const s = petStatusMap[p.status_wsh]
  p.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : p.status_wsh
  return p
}

onMounted(async () => {
  try {
    const r = await request.get('/pets/merchant?page=1&size=100')
    if (r.data.code === 200) pets.value = (r.data.data?.list || r.data.data || []).map(enrichPet)
  } catch (e) {}
})

async function createAdoptPet() {
  try {
    const r = await authStore.apiPost('/api/adoptions/pets', { ...adoptForm })
    if (r.code === 200) {
      appStore.addToast('领养宠物发布成功', 'success')
      showAdoptForm.value = false
      Object.assign(adoptForm, { name_wsh: '', type_wsh: '', breed_wsh: '', age_wsh: '', gender_wsh: '', weight_wsh: '', description_wsh: '', cover_image_wsh: '', adoption_fee_wsh: '' })
    }
  } catch (e) { appStore.addToast('发布失败', 'error') }
}
</script>
