<template>
  <div>
    <button class="btn btn-primary" style="margin-bottom:16px" @click="addBanner()">+ 投放广告</button>
    <DataTable :columns="[{label:'ID',key:'id_wsh'},{label:'标题',key:'title_wsh'},{label:'图片',key:'image_url_wsh'},{label:'链接',key:'link_url_wsh'},{label:'排序',key:'sort_order_wsh'},{label:'状态',key:'status_label_wsh'},]" :data="banners">
      <template #default="{ row }">
        <button class="btn btn-sm btn-success" @click="toggleBanner(row)" v-if="row.status_wsh === 0">上架</button>
        <button class="btn btn-sm btn-warning" @click="toggleBanner(row)" v-if="row.status_wsh === 1">下架</button>
        <button class="btn btn-sm btn-secondary" @click="editBanner(row)">编辑</button>
        <button class="btn btn-sm btn-danger" @click="deleteBanner(row.id_wsh)">删除</button>
      </template>
    </DataTable>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>{{ editingBanner ? '编辑广告' : '投放广告' }}</h2>
        <form @submit.prevent="saveBanner">
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group">
            <label>图片</label>
            <input type="file" ref="fileInput" accept="image/*" @change="onImageSelect" style="display:none">
            <button type="button" class="btn btn-outline btn-sm" @click="fileInput?.click()" :disabled="uploading">{{ uploading ? '上传中...' : '选择图片' }}</button>
            <img v-if="imagePreview" :src="imagePreview" style="max-height:100px;margin-top:8px;display:block">
          </div>
          <div class="form-group"><label>链接URL</label><input v-model="form.link_url_wsh" placeholder="请输入链接地址"></div>
          <div class="form-group"><label>排序</label><input v-model.number="form.sort_order_wsh" type="number" min="0"></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="uploading">{{ editingBanner ? '保存' : '投放' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getNotices, createNotice, updateNotice, deleteNotice } from '@/api/notice'
import { uploadFileToDirectory } from '@/api/file'
import { BannerStatus, enrichWithStatus } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'
const appStore = useAppStore()
const banners = ref([])

function enrichBanner(b) {
  return enrichWithStatus(b, 'status_wsh', BannerStatus)
}
const showForm = ref(false)
const editingBanner = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const imagePreview = ref('')
const form = reactive({ title_wsh: '', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 0 })

onMounted(loadBanners)

async function loadBanners() {
  try {
    const r = await getNotices({ type: 'banner', _t: Date.now() })
    if (r.code === 200) banners.value = r.data.filter(n => n.type_wsh === 'banner').map(enrichBanner)
  } catch (e) {}
}

function addBanner() {
  editingBanner.value = null
  Object.assign(form, { title_wsh: '', image_url_wsh: '', link_url_wsh: '', sort_order_wsh: 0 })
  imagePreview.value = ''
  showForm.value = true
}

async function onImageSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  imagePreview.value = URL.createObjectURL(file)
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await uploadFileToDirectory('banners', formData)
    if (r.code === 200 && r.data.url_wsh) {
      form.image_url_wsh = r.data.url_wsh
    } else {
      appStore.addToast('上传失败', 'error')
    }
  } catch (err) {
    appStore.addToast('上传失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function editBanner(banner) {
  editingBanner.value = banner
  Object.assign(form, { title_wsh: banner.title_wsh, image_url_wsh: banner.image_url_wsh || '', link_url_wsh: banner.link_url_wsh || '', sort_order_wsh: banner.sort_order_wsh || 0 })
  imagePreview.value = banner.image_url_wsh || ''
  showForm.value = true
}

async function saveBanner() {
  try {
    const payload = { title_wsh: form.title_wsh, content_wsh: '', type_wsh: 'banner', image_url_wsh: form.image_url_wsh, link_url_wsh: form.link_url_wsh, sort_order_wsh: form.sort_order_wsh }
    if (editingBanner.value) {
      await updateNotice(editingBanner.value.id_wsh, payload)
      appStore.addToast('更新成功', 'success')
    } else {
      await createNotice(payload)
      appStore.addToast('投放成功', 'success')
    }
    showForm.value = false; editingBanner.value = null; loadBanners()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function toggleBanner(banner) {
  try {
    await updateNotice(banner.id_wsh, { status_wsh: banner.status_wsh === 1 ? 0 : 1 })
    appStore.addToast(banner.status_wsh === 1 ? '已下架' : '已上架', 'success')
    loadBanners()
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

async function deleteBanner(id) {
  if (!confirm('确定删除？')) return
  try { await deleteNotice(id); appStore.addToast('删除成功', 'success'); loadBanners() }
  catch (e) { appStore.addToast('删除失败', 'error') }
}
</script>
