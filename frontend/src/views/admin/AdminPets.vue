<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="addPet()">+ 添加宠物</button>
    <DataTable :columns="[{label:'编号',key:'id_wsh'},{label:'宠物名',key:'name_wsh'},{label:'类型',key:'type_wsh'},{label:'品种',key:'breed_wsh'},{label:'主人',key:'owner_name_wsh'},]" :data="pets">
      <template #default="{ row }">
        <button class="btn btn-sm btn-outline" @click="viewDetail(row)">查看</button>
        <button class="btn btn-sm btn-outline" @click="editPet(row)">编辑</button>
        <button class="btn btn-sm btn-danger" @click="deletePet(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <AppDialog
      :visible="detailPet"
      :width="500"
      title="宠物详情"
      @close="detailPet = null"
    >
      <div class="detail-grid">
        <div class="detail-row"><label>编号</label><span>{{ detailPet.id_wsh }}</span></div>
        <div class="detail-row"><label>宠物名</label><span>{{ detailPet.name_wsh }}</span></div>
        <div class="detail-row"><label>类型</label><span>{{ detailPet.type_wsh }}</span></div>
        <div class="detail-row"><label>品种</label><span>{{ detailPet.breed_wsh }}</span></div>
        <div class="detail-row"><label>体重</label><span>{{ detailPet.weight_wsh ? detailPet.weight_wsh + 'kg' : '-' }}</span></div>
        <div class="detail-row"><label>年龄</label><span>{{ detailPet.age_wsh ? detailPet.age_wsh + '岁' : '-' }}</span></div>
        <div class="detail-row"><label>性别</label><span>{{ detailPet.gender_wsh === 'male' ? '公' : detailPet.gender_wsh === 'female' ? '母' : '-' }}</span></div>
        <div class="detail-row"><label>主人</label><span>{{ detailPet.owner_name_wsh || detailPet.owner_id_wsh }}</span></div>
        <div class="detail-row" v-if="detailPet.description_wsh"><label>描述</label><span>{{ detailPet.description_wsh }}</span></div>
      </div>
      <template #footer>
        <button class="btn btn-primary btn-sm" @click="detailPet = null">关闭</button>
      </template>
    </AppDialog>

    <AppDialog
      :visible="showForm"
      :width="500"
      :title="editingPet ? '编辑宠物' : '添加宠物'"
      @close="showForm = false"
    >
      <form id="petForm" @submit.prevent="savePet">
        <div class="form-group"><label>宠物名</label><input v-model="form.name_wsh" required></div>
        <div class="form-group">
          <label>类型</label>
          <select v-model="form.type_wsh" required>
            <option value="dog">狗</option>
            <option value="cat">猫</option>
            <option value="other">其他</option>
          </select>
        </div>
        <div class="form-group"><label>品种</label><input v-model="form.breed_wsh"></div>
        <div class="form-group"><label>年龄</label><input v-model.number="form.age_wsh" type="number" min="0"></div>
        <div class="form-group"><label>体重(kg)</label><input v-model.number="form.weight_wsh" type="number" min="0" step="0.1"></div>
        <div class="form-group">
          <label>性别</label>
          <select v-model="form.gender_wsh">
            <option value="">未知</option>
            <option value="male">公</option>
            <option value="female">母</option>
          </select>
        </div>
        <div class="form-group"><label>描述</label><textarea v-model="form.description_wsh" rows="3"></textarea></div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
        <button type="submit" form="petForm" class="btn btn-primary btn-sm">{{ editingPet ? '保存' : '创建' }}</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getAdminPets, createPet, updatePet, deletePet as apiDeletePet } from '@/api/pet'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
const appStore = useAppStore()
// detailPet 有值就弹详情，editingPet 有值就说明是改而不是加
const pets = ref([])
const detailPet = ref(null)
const showForm = ref(false)
const editingPet = ref(null)
const form = reactive({
  name_wsh: '', type_wsh: 'dog', breed_wsh: '',
  age_wsh: null, weight_wsh: null, gender_wsh: '',
  description_wsh: ''
})

onMounted(loadPets)

// 进页面拉一次全量宠物，后台这个列表不分页
async function loadPets() {
  try { const r = await getAdminPets(); if (r.code === 200) pets.value = r.data }
  catch (e) { appStore.addToast('加载失败', 'error') }
}

function viewDetail(pet) { detailPet.value = pet }

// 新增前把 editingPet 置空，不然保存时会走到更新那条分支
function addPet() {
  editingPet.value = null
  Object.assign(form, {
    name_wsh: '', type_wsh: 'dog', breed_wsh: '',
    age_wsh: null, weight_wsh: null, gender_wsh: '',
    description_wsh: ''
  })
  showForm.value = true
}

// 把这一行的数据抄进表单，同时留着整行当编辑标记
function editPet(pet) {
  editingPet.value = pet
  Object.assign(form, {
    name_wsh: pet.name_wsh || '',
    type_wsh: pet.type_wsh || 'dog',
    breed_wsh: pet.breed_wsh || '',
    age_wsh: pet.age_wsh || null,
    weight_wsh: pet.weight_wsh || null,
    gender_wsh: pet.gender_wsh || '',
    description_wsh: pet.description_wsh || ''
  })
  showForm.value = true
}

// 没填的项统一送 null，后端拿 null 就当没填
async function savePet() {
  try {
    const payload = {
      name_wsh: form.name_wsh,
      type_wsh: form.type_wsh,
      breed_wsh: form.breed_wsh || null,
      age_wsh: form.age_wsh || null,
      weight_wsh: form.weight_wsh || null,
      gender_wsh: form.gender_wsh || null,
      description_wsh: form.description_wsh || null
    }
    if (editingPet.value) {
      await updatePet(editingPet.value.id_wsh, payload)
      appStore.addToast('更新成功', 'success')
    } else {
      await createPet(payload)
      appStore.addToast('创建成功', 'success')
    }
    showForm.value = false
    editingPet.value = null
    loadPets()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

// 删之前确认一下，宠物删了关联的寄养记录就断了
async function deletePet(id) {
  if (!confirm('确定删除该宠物？')) return
  try {
    await apiDeletePet(id)
    appStore.addToast('删除成功', 'success')
    loadPets()
  } catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
