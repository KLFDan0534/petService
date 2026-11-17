<template>
  <div class="merchant-services-page">
    <div class="page-head">
      <div>
        <h2>服务管理</h2>
        <p>发布、编辑和上下架商家可提供的宠物服务。</p>
      </div>
      <button class="btn btn-primary btn-sm" :disabled="!merchant || categories.length === 0" @click="openCreate">
        新增服务
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="!merchant" class="empty-state">
      当前账号尚未绑定商家资料，暂不能发布服务。
    </div>

    <div v-else-if="categories.length === 0" class="empty-state">
      暂无可用服务分类，请联系管理员先配置服务分类。
    </div>

    <div v-else>
      <div class="filter-bar">
        <button
          v-for="cat in filterCategories"
          :key="cat.id_wsh"
          :class="['btn btn-sm', selectedCategoryId === cat.id_wsh ? 'btn-primary' : 'btn-outline']"
          @click="selectedCategoryId = cat.id_wsh"
        >
          {{ cat.name_wsh }}
          <span v-if="cat.count != null" class="filter-count">{{ cat.count }}</span>
        </button>
      </div>

      <div class="service-table">
        <div class="table-head">
          <span>服务名称</span>
          <span>服务分类</span>
          <span>价格</span>
          <span>状态</span>
          <span>操作</span>
        </div>

        <div v-if="filteredServices.length === 0" class="empty-row">
          {{ selectedCategoryId ? '该分类下暂无服务' : '暂无服务，点击"新增服务"开始发布。' }}
        </div>

        <div v-for="service in filteredServices" :key="service.id_wsh" class="table-row">
          <div class="service-summary">
            <img v-if="firstImage(service.images_wsh)" :src="firstImage(service.images_wsh)" :alt="service.name_wsh">
            <div>
              <strong>{{ service.name_wsh }}</strong>
              <p>{{ service.description_wsh || '暂无描述' }}</p>
            </div>
          </div>
          <span>{{ getCategoryName(service.category_id_wsh) }}</span>
          <span>¥{{ service.price_wsh }} / {{ unitLabel(service.unit_wsh) }}</span>
          <span :class="['badge', service.status_wsh === 1 ? 'badge-success' : 'badge-secondary']">
            {{ service.status_wsh === 1 ? '已上架' : '已下架' }}
          </span>
          <div class="row-actions">
            <button class="btn btn-sm btn-outline" @click="openEdit(service)">编辑</button>
            <button class="btn btn-sm btn-outline" @click="toggleStatus(service)">
              {{ service.status_wsh === 1 ? '下架' : '上架' }}
            </button>
            <button class="btn btn-sm btn-danger" @click="removeService(service)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <AppDialog
      :visible="showForm"
      :width="620"
      :title="editingService ? '编辑服务' : '新增服务'"
      @close="closeForm"
    >
      <p v-if="detailLoading" class="detail-loading">正在加载服务详情...</p>
      <form id="service-form" @submit.prevent="saveService">
          <div class="form-group">
            <label>服务名称</label>
            <input v-model.trim="form.name_wsh" required maxlength="100" placeholder="例如：标准寄养">
          </div>

          <div class="form-group">
            <label>服务分类</label>
            <div ref="categoryPickerRef" class="category-picker">
              <button
                type="button"
                class="category-trigger"
                :class="{ 'is-placeholder': !selectedCategoryLabel }"
                @click="toggleCategoryPanel"
              >
                {{ selectedCategoryLabel || '请选择服务分类' }}
                <span class="category-caret" aria-hidden="true">▾</span>
              </button>
              <div v-if="categoryOpen" class="category-panel">
                <div class="category-level1">
                  <button
                    v-for="root in rootCategories"
                    :key="root.id_wsh"
                    type="button"
                    :class="['category-root', { active: activeRootId === root.id_wsh, selected: form.category_id_wsh === root.id_wsh }]"
                    @mouseenter="activeRootId = root.id_wsh"
                    @click="pickCategory(root)"
                  >
                    {{ root.name_wsh }}
                  </button>
                </div>
                <div class="category-level2">
                  <button
                    v-for="child in activeChildren"
                    :key="child.id_wsh"
                    type="button"
                    :class="['category-chip', { selected: form.category_id_wsh === child.id_wsh }]"
                    @click="pickCategory(child)"
                  >
                    {{ child.name_wsh }}
                  </button>
                  <p v-if="activeChildren.length === 0" class="category-empty">该一级分类下暂无二级分类，点击左侧直接选用</p>
                </div>
              </div>
            </div>
          </div>

          <div class="form-grid">
            <div class="form-group">
              <label>价格</label>
              <input v-model.number="form.price_wsh" required type="number" min="0" step="0.01" placeholder="0.00">
            </div>
            <div class="form-group">
              <label>计价方式</label>
              <select data-testid="unit-select" :value="form.unit_wsh" :disabled="Boolean(forcedUnit)" @change="onUnitChange">
                <option value="night">按夜（每夜一个价，14:00 入住/离店）</option>
                <option value="session">按次（每次一个价）</option>
                <option value="hour">按小时（每小时一个价）</option>
              </select>
              <span v-if="forcedUnit" class="field-hint">{{ forcedUnitHint }}</span>
            </div>
          </div>

          <div class="form-group">
            <label>单次服务时长（分钟）</label>
            <input
              data-testid="duration-input"
              v-model.number="form.duration_minutes_wsh"
              type="number"
              min="15"
              max="1440"
              step="15"
              :disabled="form.unit_wsh === 'night'"
            >
            <span v-if="durationError" data-testid="duration-error" class="field-error">{{ durationError }}</span>
            <span v-else class="field-hint">{{ durationHint }}</span>
          </div>

          <div class="form-group">
            <label>服务图片</label>
            <div class="image-uploader">
              <input
                ref="imageInput"
                type="file"
                accept="image/*"
                multiple
                class="hidden-input"
                :disabled="imageUploading"
                @change="uploadServiceImages"
              >
              <button type="button" class="btn btn-outline btn-sm" :disabled="imageUploading" @click="imageInput?.click()">
                {{ imageUploading ? '上传中...' : '上传图片' }}
              </button>
              <span class="upload-hint">支持 PNG/JPEG/GIF/BMP，单张不超过 10MB，最多 {{ MAX_MEDIA }} 张。</span>
            </div>
            <div v-if="mediaItems.length" class="image-preview-grid">
              <div
                v-for="(item, index) in mediaItems"
                :key="item.file_id_wsh ?? item.url_wsh"
                class="image-preview"
                :class="{ 'is-dragging': dragIndex === index, 'is-drop-target': dropIndex === index && dragIndex !== index }"
                draggable="true"
                @dragstart="onDragStart(index)"
                @dragover.prevent="onDragOver(index)"
                @drop.prevent="onDrop(index)"
                @dragend="resetDrag"
              >
                <img :src="item.url_wsh" :alt="`服务图片 ${index + 1}`">
                <span v-if="index === 0" class="cover-badge">封面</span>
                <button type="button" class="media-remove" title="删除这张图片" @click="askRemoveMedia(index)">×</button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>服务描述</label>
            <textarea v-model.trim="form.description_wsh" rows="4" placeholder="说明服务内容、适用宠物和注意事项"></textarea>
          </div>

          <div v-if="editingService" class="form-group">
            <label>状态</label>
            <select v-model.number="form.status_wsh">
              <option :value="1">上架</option>
              <option :value="0">下架</option>
            </select>
          </div>
      </form>

      <template #footer>
        <button type="button" class="btn btn-secondary btn-sm" :disabled="saving || imageUploading || detailLoading" @click="closeForm">取消</button>
        <button form="service-form" type="submit" class="btn btn-primary btn-sm" :disabled="saving || imageUploading || detailLoading">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </AppDialog>

    <AppDialog class-name="remove-confirm" :visible="pendingRemoveIndex >= 0" :width="360" title="删除图片" @close="cancelRemoveMedia">
      <p class="remove-hint">确定删除这张图片吗？删除后需要重新上传才能恢复。</p>
      <template #footer>
        <button type="button" class="btn btn-secondary btn-sm" @click="cancelRemoveMedia">取消</button>
        <button type="button" class="btn btn-danger btn-sm" @click="confirmRemoveMedia">确认删除</button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import AppDialog from '@/components/common/AppDialog.vue'
import { getMy } from '@/services/merchantService'
import {
  createService,
  deleteService,
  getMerchantServices,
  getServiceManageDetail,
  toggleServiceStatus,
  updateService,
} from '@/api/service'
import { getServiceCategoryList } from '@/api/serviceCategory'
import { uploadProductImage } from '@/api/file'
import { unitLabel } from '@/domain/BookingUnit'

// 图片张数和单张大小的上限，前端先挡一道，后端还会再校验
const MAX_MEDIA = 10
const MAX_IMAGE_BYTES = 10 * 1024 * 1024

// 表单初始时长（分钟）：按标准计价单位给出，不从别名表推导；编辑时以服务详情值为准。
const STANDARD_DURATION_MINUTES = { night: 1440, session: 60, hour: 60 }

const appStore = useAppStore()
const loading = ref(true)
const saving = ref(false)
const imageUploading = ref(false)
const detailLoading = ref(false)
const imageInput = ref(null)
const merchant = ref(null)
const services = ref([])
const categories = ref([])
const showForm = ref(false)
const editingService = ref(null)
const selectedCategoryId = ref(0)
const mediaItems = ref([])
let formSnapshot = ''

// 分类面板：一级常驻左栏，hover 一级在右栏展开二级
const categoryOpen = ref(false)
const activeRootId = ref(0)
const categoryPickerRef = ref(null)
// 图片拖拽与删除确认
const dragIndex = ref(-1)
const dropIndex = ref(-1)
const pendingRemoveIndex = ref(-1)

// 新增和编辑共用这一份表单数据，打开弹窗前先清空，编辑时再按详情填回来
const form = reactive({
  name_wsh: '',
  category_id_wsh: null,
  description_wsh: '',
  price_wsh: null,
  unit_wsh: 'night',
  duration_minutes_wsh: 1440,
  status_wsh: 1,
})

/**
 * 寄养类服务（根分类 BOARDING）的计价方式由分类决定：
 * - 过夜寄养 → 必须按夜（night），提前送达只加收费用、不顺延离店
 * - 日间寄养（BOARDING_DAYCARE）→ 必须按次（session），时长沿用商家配置
 */
const forcedUnit = computed(() => {
  const leaf = categories.value.find(c => c.id_wsh === form.category_id_wsh)
  if (!leaf) return ''
  const root = leaf.parent_id_wsh
    ? (categories.value.find(c => c.id_wsh === leaf.parent_id_wsh) || leaf)
    : leaf
  if (String(root.code_wsh || '').toUpperCase() !== 'BOARDING') return ''
  if (String(leaf.code_wsh || '').toUpperCase() === 'BOARDING_DAYCARE') return 'session'
  return 'night'
})

const forcedUnitHint = computed(() => {
  if (forcedUnit.value === 'night') return '过夜寄养固定按夜计价：一夜 24 小时，14:00 入住、离店日 14:00 标准离店'
  if (forcedUnit.value === 'session') return '日间寄养按次计价，时长沿用你配置的营业时长'
  return ''
})

function onCategoryChange() {
  if (!forcedUnit.value) return
  form.unit_wsh = forcedUnit.value
  // 日间寄养保留商家自己的时长配置
  form.duration_minutes_wsh = forcedUnit.value === 'session'
    ? (Number(form.duration_minutes_wsh) || STANDARD_DURATION_MINUTES.session)
    : STANDARD_DURATION_MINUTES[forcedUnit.value]
}

// 换计价单位时把时长重置成这个单位该有的值，不然会留着上一单位填的数字
function onUnitChange(event) {
  const unit = event.target.value
  form.unit_wsh = unit
  form.duration_minutes_wsh = STANDARD_DURATION_MINUTES[unit] || STANDARD_DURATION_MINUTES.hour
}

// 时长的规矩跟着单位走，这里统一算出一条错误文案，空串就代表没问题
const durationError = computed(() => {
  const unit = form.unit_wsh
  const minutes = Number(form.duration_minutes_wsh)
  if (!minutes || Number.isNaN(minutes)) return '请输入服务时长'
  if (minutes < 15) return '时长不能少于 15 分钟'
  if (minutes > 1440) return '时长不能超过 1440 分钟'
  if (unit === 'hour' && minutes % 60 !== 0) return '按小时服务时长必须是 60 的整数倍'
  if (unit === 'night' && minutes !== 1440) return '按夜服务的计费基数固定为 1440 分钟（一夜）'
  return ''
})

const durationHint = computed(() => {
  const unit = form.unit_wsh
  if (unit === 'night') return '一夜 24 小时为计费基数；实际住不满也按预订夜数收，晚接另计'
  if (unit === 'hour') return '必须是 60 的整数倍，范围 60..1440 分钟'
  return '范围 15..1440 分钟'
})

// 一级分类，按 sort_order 排
const rootCategories = computed(() => sortByOrder(categories.value.filter(cat => !cat.parent_id_wsh)))

// 当前 hover 的一级分类下的二级分类，右栏用
const activeChildren = computed(() => sortByOrder(categories.value.filter(cat => cat.parent_id_wsh === activeRootId.value)))

function sortByOrder(list) {
  return [...list].sort((a, b) => {
    const orderDiff = (a.sort_order_wsh || 0) - (b.sort_order_wsh || 0)
    if (orderDiff !== 0) return orderDiff
    return (a.id_wsh || 0) - (b.id_wsh || 0)
  })
}

// 触发按钮上显示的「一级 / 二级」
const selectedCategoryLabel = computed(() => {
  const leaf = categories.value.find(cat => cat.id_wsh === form.category_id_wsh)
  if (!leaf) return ''
  const parent = leaf.parent_id_wsh ? categories.value.find(cat => cat.id_wsh === leaf.parent_id_wsh) : null
  return parent ? `${parent.name_wsh} / ${leaf.name_wsh}` : leaf.name_wsh
})

// 打开面板时把高亮定位到已选分类所属的一级，省得每次重新找
function toggleCategoryPanel() {
  if (!categoryOpen.value) {
    const selected = categories.value.find(cat => cat.id_wsh === form.category_id_wsh)
    activeRootId.value = selected
      ? (selected.parent_id_wsh || selected.id_wsh)
      : (rootCategories.value[0]?.id_wsh || 0)
  }
  categoryOpen.value = !categoryOpen.value
}

function pickCategory(cat) {
  form.category_id_wsh = cat.id_wsh
  categoryOpen.value = false
  onCategoryChange()
}

// 点面板外面收起
function onDocumentMousedown(event) {
  if (categoryOpen.value && !categoryPickerRef.value?.contains(event.target)) {
    categoryOpen.value = false
  }
}

// 老数据把图片塞在一个逗号串里，这里拆成数组；新数据走 media_wsh 就用不着它了
function parseImages(value) {
  return String(value || '')
    .split(',')
    .map(url => url.trim())
    .filter(Boolean)
}

function firstImage(value) {
  return parseImages(value)[0] || ''
}

// 把表单和图片拼成一个字符串，后面靠它判断用户到底动没动过
function currentMediaSignature() {
  return JSON.stringify({
    ...form,
    media: mediaItems.value.map(item => item.file_id_wsh ?? item.url_wsh),
  })
}

function takeSnapshot() {
  formSnapshot = currentMediaSignature()
}

// 跟打开弹窗那一刻记下的快照比一比，不一样就说明还有没保存的东西
function isDirty() {
  return formSnapshot !== currentMediaSignature()
}

// 清回新增时的初始值，编辑打开时也先过一遍这里再往上填
function resetForm() {
  Object.assign(form, {
    name_wsh: '',
    category_id_wsh: null,
    description_wsh: '',
    price_wsh: null,
    unit_wsh: 'night',
    duration_minutes_wsh: 1440,
    status_wsh: 1,
  })
  mediaItems.value = []
}

// 选了父分类时，子分类下的服务也得一起露出来，所以要把子孙 id 全收进来
function getCategoryIdsIncludingChildren(id, allCats) {
  const ids = [id]
  const children = allCats.filter(c => (c.parent_id_wsh || 0) === id)
  children.forEach(child => ids.push(...getCategoryIdsIncludingChildren(child.id_wsh, allCats)))
  return ids
}

// 顶部筛选条只放一级分类，最前面补一个"全部"
const filterCategories = computed(() => {
  const topLevel = categories.value.filter(c => !c.parent_id_wsh)
  return [
    { id_wsh: 0, name_wsh: '全部' },
    ...topLevel,
  ]
})

// 选了分类就按它和它的子孙分类过滤，没选（id 为 0）就全给
const filteredServices = computed(() => {
  if (!selectedCategoryId.value) return services.value
  const ids = getCategoryIdsIncludingChildren(selectedCategoryId.value, categories.value)
  return services.value.filter(s => ids.includes(s.category_id_wsh))
})

function getCategoryName(categoryId) {
  const category = categories.value.find(item => item.id_wsh === categoryId)
  return category?.name_wsh || '未分类'
}

// 进页面先把商家资料和分类一起拉下来，拿到商家 id 之后再去拉服务列表
async function loadPage() {
  loading.value = true
  try {
    const [myMerchant, categoryRes] = await Promise.all([
      getMy().catch(() => null),
      getServiceCategoryList(),
    ])
    merchant.value = myMerchant
    categories.value = categoryRes.code === 200 ? (categoryRes.data || []) : []
    await loadServices()
  } catch (error) {
    appStore.addToast('加载服务管理数据失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadServices() {
  if (!merchant.value?.id_wsh) {
    services.value = []
    return
  }
  const res = await getMerchantServices(merchant.value.id_wsh)
  services.value = res.code === 200 ? (res.data || []) : []
}

// 新增：清空表单、记一份初始快照，再把弹窗打开
function openCreate() {
  editingService.value = null
  resetForm()
  takeSnapshot()
  showForm.value = true
}

// 编辑：先开弹窗再拉详情，因为图片顺序和谁是封面只有详情接口才给
async function openEdit(service) {
  editingService.value = service
  resetForm()
  showForm.value = true
  detailLoading.value = true
  try {
    const res = await getServiceManageDetail(service.id_wsh)
    if (res.code !== 200 || !res.data) {
      appStore.addToast(res.message || '加载服务详情失败', 'error')
      showForm.value = false
      editingService.value = null
      return
    }
    const detail = res.data
    const rawUnit = String(detail.service_wsh?.unit_wsh ?? '').trim()
    const label = unitLabel(rawUnit)
    // 历史/异常单位直接提示数据异常并关掉弹窗，绝不静默兜底成 night 回显。
    if (!label || label.includes('历史计价单位')) {
      appStore.addToast(`服务计价单位数据异常（${rawUnit || '缺失'}），请联系管理员`, 'error')
      showForm.value = false
      editingService.value = null
      return
    }
    Object.assign(form, {
      name_wsh: detail.service_wsh?.name_wsh || '',
      category_id_wsh: detail.service_wsh?.category_id_wsh ?? null,
      description_wsh: detail.service_wsh?.description_wsh || '',
      price_wsh: Number(detail.service_wsh?.price_wsh ?? 0),
      unit_wsh: rawUnit,
      duration_minutes_wsh: Number(detail.service_wsh?.duration_minutes_wsh) || STANDARD_DURATION_MINUTES[rawUnit] || 60,
      status_wsh: Number(detail.service_wsh?.status_wsh ?? 1),
    })
    const media = (detail.media_wsh || []).map(item => ({
      file_id_wsh: item.file_id_wsh ?? null,
      url_wsh: item.url_wsh || '',
      is_cover: Number(item.is_cover_wsh) === 1,
    }))
    // 老服务没有 media_wsh，就把 images_wsh 里的逗号串当图片列表，第一张算封面
    if (!media.length) {
      parseImages(detail.service_wsh?.images_wsh).forEach((url, index) => {
        media.push({ file_id_wsh: null, url_wsh: url, is_cover: index === 0 })
      })
    }
    // 封面固定是列表第一张：回显时把原封面挪到最前，只改个价格不会顺手换封面
    mediaItems.value = [
      ...media.filter(item => item.is_cover),
      ...media.filter(item => !item.is_cover),
    ].map(item => ({ file_id_wsh: item.file_id_wsh, url_wsh: item.url_wsh }))
  } catch (error) {
    appStore.addToast('加载服务详情失败', 'error')
    showForm.value = false
    editingService.value = null
    return
  } finally {
    detailLoading.value = false
    takeSnapshot()
  }
}

// 有没保存的改动就先问一句，免得手滑关掉白填一遍
function closeForm() {
  if (saving.value || imageUploading.value || detailLoading.value) return
  if (showForm.value && isDirty() && !window.confirm('有未保存的更改，确定关闭吗？')) return
  showForm.value = false
  editingService.value = null
  mediaItems.value = []
}

// 选完文件马上传到服务器，拿到 file_id 之后才放进预览列表
async function uploadServiceImages(event) {
  const files = Array.from(event.target.files || [])
  // 先把 input 清空，不然连着选同一个文件不会再触发 change
  event.target.value = ''
  if (!files.length) return

  const invalidType = files.find(file => !file.type.startsWith('image/'))
  if (invalidType) {
    appStore.addToast('只能上传图片文件', 'error')
    return
  }
  const oversize = files.find(file => file.size > MAX_IMAGE_BYTES)
  if (oversize) {
    appStore.addToast('单张图片不能超过 10MB', 'error')
    return
  }
  if (mediaItems.value.length + files.length > MAX_MEDIA) {
    appStore.addToast(`最多上传 ${MAX_MEDIA} 张图片`, 'error')
    return
  }

  imageUploading.value = true
  const uploaded = []
  let failedCount = 0
  try {
    for (const file of files) {
      const data = new FormData()
      data.append('file', file)
      try {
        const res = await uploadProductImage(merchant.value.id_wsh, data)
        if (res.code === 200 && res.data?.id_wsh && res.data?.url_wsh) {
          uploaded.push({ file_id_wsh: res.data.id_wsh, url_wsh: res.data.url_wsh })
        } else {
          failedCount++
        }
      } catch (error) {
        failedCount++
      }
    }
    if (uploaded.length) {
      const existing = new Set(mediaItems.value.map(item => item.file_id_wsh))
      const next = mediaItems.value.slice()
      uploaded.forEach(item => {
        if (!existing.has(item.file_id_wsh)) next.push(item)
      })
      mediaItems.value = next
      if (failedCount > 0) {
        appStore.addToast(`成功上传 ${uploaded.length} 张，失败 ${failedCount} 张，可重新选择重试`, 'error')
      } else {
        appStore.addToast('图片上传成功', 'success')
      }
    } else {
      appStore.addToast('图片上传失败，请重试', 'error')
    }
  } finally {
    imageUploading.value = false
  }
}

// 拖拽换位置：把拖起的那张插到松手的位置，数组顺序就是保存时的 sort_order
function onDragStart(index) {
  dragIndex.value = index
}

function onDragOver(index) {
  dropIndex.value = index
}

function onDrop(index) {
  const from = dragIndex.value
  resetDrag()
  if (from < 0 || from === index) return
  const next = mediaItems.value.slice()
  const [moved] = next.splice(from, 1)
  next.splice(index, 0, moved)
  mediaItems.value = next
}

function resetDrag() {
  dragIndex.value = -1
  dropIndex.value = -1
}

// 删除先问一句，确认了才从列表里拿掉（不删服务器文件，保存时才解除关联）
function askRemoveMedia(index) {
  pendingRemoveIndex.value = index
}

function cancelRemoveMedia() {
  pendingRemoveIndex.value = -1
}

function confirmRemoveMedia() {
  if (pendingRemoveIndex.value >= 0) {
    mediaItems.value = mediaItems.value.filter((_, i) => i !== pendingRemoveIndex.value)
  }
  pendingRemoveIndex.value = -1
}

// 保存前在这里把关：分类、价格、时长、图片都过了才拼请求体，有一项不对就弹提示返回 null
function buildPayload() {
  if (!form.category_id_wsh) {
    appStore.addToast('请选择服务分类', 'error')
    return null
  }
  if (form.price_wsh === null || form.price_wsh === '' || Number(form.price_wsh) < 0) {
    appStore.addToast('请输入有效价格', 'error')
    return null
  }
  if (durationError.value) {
    appStore.addToast(durationError.value, 'error')
    return null
  }
  if (mediaItems.value.length && mediaItems.value.some(item => item.file_id_wsh == null)) {
    appStore.addToast('存在旧版图片，请移除后重新上传', 'error')
    return null
  }

  const payload = {
    name_wsh: form.name_wsh,
    category_id_wsh: form.category_id_wsh,
    description_wsh: form.description_wsh,
    price_wsh: form.price_wsh,
    unit_wsh: form.unit_wsh,
    duration_minutes_wsh: Number(form.duration_minutes_wsh),
    media_wsh: mediaItems.value.map((item, index) => ({
      file_id_wsh: item.file_id_wsh,
      sort_order_wsh: index,
      is_cover_wsh: index === 0 ? 1 : 0,
    })),
  }

  // 状态只在编辑时提交；新增的服务后端默认就是上架，不用让用户多选一次
  if (editingService.value) {
    payload.status_wsh = form.status_wsh
  }

  return payload
}

// 同一个按钮干两件事：手里有编辑对象就改，没有就新建
async function saveService() {
  if (saving.value || imageUploading.value) return
  const payload = buildPayload()
  if (!payload) return

  saving.value = true
  try {
    const res = editingService.value
      ? await updateService(editingService.value.id_wsh, payload)
      : await createService(payload)
    if (res.code === 200) {
      appStore.addToast(editingService.value ? '服务已更新' : '服务已发布', 'success')
      showForm.value = false
      editingService.value = null
      await loadServices()
    } else {
      appStore.addToast(res.message || '保存服务失败', 'error')
    }
  } catch (error) {
    appStore.addToast('保存服务失败', 'error')
  } finally {
    saving.value = false
  }
}

// 上下架走单独的接口，切完重新拉列表，按钮文字和状态标签才会跟着变
async function toggleStatus(service) {
  try {
    const res = await toggleServiceStatus(service.id_wsh)
    if (res.code === 200) {
      appStore.addToast(service.status_wsh === 1 ? '服务已下架' : '服务已上架', 'success')
      await loadServices()
    } else {
      appStore.addToast(res.message || '更新状态失败', 'error')
    }
  } catch (error) {
    appStore.addToast('更新状态失败', 'error')
  }
}

// 删之前弹个确认框，把服务名带上，免得手快点错
async function removeService(service) {
  if (!confirm(`确定删除服务“${service.name_wsh}”吗？`)) return
  try {
    const res = await deleteService(service.id_wsh)
    if (res.code === 200) {
      appStore.addToast('服务已删除', 'success')
      await loadServices()
    } else {
      appStore.addToast(res.message || '删除服务失败', 'error')
    }
  } catch (error) {
    appStore.addToast('删除服务失败', 'error')
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentMousedown)
  loadPage()
})

onBeforeUnmount(() => document.removeEventListener('mousedown', onDocumentMousedown))
</script>

<style scoped>
.merchant-services-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 22px;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
}

.loading,
.empty-state,
.empty-row {
  padding: 28px;
  color: var(--color-muted-foreground);
  text-align: center;
}

.service-table {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-card);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) minmax(130px, .9fr) minmax(120px, .8fr) 90px minmax(220px, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
}

.table-head {
  color: var(--color-muted-foreground);
  font-size: 13px;
  background: var(--color-muted);
  border-bottom: 1px solid var(--color-border);
}

.table-row {
  border-bottom: 1px solid var(--color-border);
}

.table-row:last-child {
  border-bottom: 0;
}

.service-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.service-summary img {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-inline);
  object-fit: cover;
  border: 1px solid var(--color-border);
  background: var(--color-muted);
  flex-shrink: 0;
}

.table-row strong {
  display: block;
}

.table-row p {
  margin: 4px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.5;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-loading {
  margin: 0 0 12px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.image-uploader {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hidden-input {
  display: none;
}

.upload-hint {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.image-preview {
  position: relative;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-muted);
  aspect-ratio: 1;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-badge {
  position: absolute;
  left: 6px;
  top: 6px;
  border: 0;
  border-radius: 4px;
  padding: 2px 8px;
  background: rgba(24, 144, 255, .92);
  color: #fff;
  font-size: 12px;
  line-height: 18px;
}

.media-remove {
  position: absolute;
  right: 4px;
  top: 4px;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: var(--radius-pill);
  background: rgba(220, 38, 38, .92);
  color: #fff;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 120ms ease;
}

.image-preview:hover .media-remove,
.media-remove:focus-visible {
  opacity: 1;
}

.image-preview.is-dragging {
  opacity: .45;
}

.image-preview.is-drop-target {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 40%, transparent);
}

.image-preview img {
  cursor: grab;
}

.remove-hint {
  margin: 0;
  color: var(--color-muted-foreground);
}

.category-picker {
  position: relative;
}

.category-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-inline);
  background: var(--color-card);
  color: var(--color-foreground);
  font-size: 13.5px;
  text-align: left;
  cursor: pointer;
}

.category-trigger.is-placeholder {
  color: var(--color-muted-foreground);
}

.category-caret {
  flex: none;
  color: var(--color-muted-foreground);
  font-size: 11px;
}

.category-panel {
  position: absolute;
  z-index: 20;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-card);
  box-shadow: var(--shadow-lift);
  overflow: hidden;
}

/* 一级列表：纵向，超高时自己滚 */
.category-level1 {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  max-height: 240px;
  overflow-y: auto;
  border-right: 1px solid var(--color-border);
  background: var(--color-muted);
}

.category-root {
  padding: 7px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--color-foreground);
  font-size: 13px;
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
}

.category-root.active {
  background: var(--color-card);
}

.category-root.selected {
  color: var(--color-primary);
  font-weight: 600;
}

/* 二级：横排自动换行，超高时自己滚，避免分类多把面板拉太长 */
.category-level2 {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px;
  padding: 10px;
  max-height: 240px;
  overflow-y: auto;
}

.category-chip {
  max-width: 100%;
  padding: 4px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-card);
  color: var(--color-foreground);
  font-size: 12.5px;
  line-height: 1.4;
  text-align: left;
  white-space: normal;
  word-break: break-word;
  cursor: pointer;
}

.category-chip:hover {
  border-color: var(--color-primary);
}

.category-chip.selected {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.category-empty {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 12px;
}

textarea {
  resize: vertical;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.field-hint {
  display: block;
  min-height: 18px;
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}

.field-error {
  display: block;
  min-height: 18px;
  color: var(--color-danger, #e5484d);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  line-height: 18px;
  background: rgba(0, 0, 0, 0.08);
}

.btn-primary .filter-count {
  background: rgba(255, 255, 255, 0.22);
}

@media (max-width: 980px) {
  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .row-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
