<template>
  <div class="amap-address-picker">
    <div :class="['picker-row', { 'picker-row--single': !hasActionButtons }]">
      <input
        :value="modelValue"
        class="form-control"
        :placeholder="placeholder"
        autocomplete="off"
        @input="onInput"
        @focus="search"
        @blur="hideTipsSoon"
      >
      <div v-if="hasActionButtons" class="picker-actions">
        <button
          v-if="showMapButton"
          class="btn btn-outline btn-sm"
          type="button"
          :disabled="mapLoading"
          @click="openMapDialog"
        >
          <AppIcon><MapLocation /></AppIcon>
          {{ mapLoading ? '加载地图' : mapButtonText }}
        </button>
        <button
          v-if="showLocateButton"
          class="btn btn-outline btn-sm"
          type="button"
          :disabled="locating"
          @click="useCurrentLocation"
        >
          <AppIcon><Location /></AppIcon>
          {{ locating ? '定位中' : locateButtonText }}
        </button>
      </div>
    </div>

    <div v-if="showTips && tips.length" class="tips-panel">
      <button
        v-for="tip in tips"
        :key="`${tip.poi_id_wsh || tip.address_wsh}-${tip.latitude_wsh}-${tip.longitude_wsh}`"
        type="button"
        class="tip-item"
        @mousedown.prevent="selectTip(tip)"
      >
        <span class="result-title">{{ tip.display_name_wsh || tip.address_wsh }}</span>
        <span class="result-address">{{ tip.address_wsh }}</span>
        <span v-if="tip.match_label_wsh" class="result-meta">{{ tip.match_label_wsh }}</span>
      </button>
    </div>

    <p v-if="selectedText" class="selected-text">{{ selectedText }}</p>

    <teleport to="body">
      <div v-if="mapDialogVisible" class="map-dialog-overlay" @mousedown.self="closeMapDialog">
        <div class="map-dialog" role="dialog" aria-modal="true" aria-label="地图选址">
          <header class="map-dialog__header">
            <div>
              <h3>地图选址</h3>
              <p>{{ pendingAddress || '搜索地点或在地图上定点' }}</p>
            </div>
            <button class="icon-button" type="button" aria-label="关闭地图选址" @click="closeMapDialog">
              <AppIcon><Close /></AppIcon>
            </button>
          </header>

          <div class="map-search-row">
            <input
              v-model="mapKeyword"
              class="form-control"
              placeholder="搜索地点"
              autocomplete="off"
              @keydown.enter.prevent="searchInMap"
            >
            <button class="btn btn-primary btn-sm" type="button" :disabled="mapSearching" @click="searchInMap">
              <AppIcon><Search /></AppIcon>
              {{ mapSearching ? '搜索中' : '搜索' }}
            </button>
          </div>

          <div class="map-body">
            <aside class="map-results" aria-label="地点搜索结果">
              <button
                v-for="result in mapResults"
                :key="`${result.poi_id_wsh || result.address_wsh}-${result.latitude_wsh}-${result.longitude_wsh}`"
                type="button"
                class="map-result"
                @click="selectMapResult(result)"
              >
                <span class="result-title">{{ result.display_name_wsh || result.address_wsh }}</span>
                <span class="result-address">{{ result.address_wsh }}</span>
                <span v-if="result.match_label_wsh" class="result-meta">{{ result.match_label_wsh }}</span>
              </button>
              <div v-if="mapSearched && !mapSearching && !mapResults.length" class="map-empty">
                未找到匹配地点
              </div>
            </aside>
            <div ref="mapContainer" class="map-container">
              <div v-if="mapLoading" class="map-loading">地图加载中...</div>
            </div>
          </div>

          <footer class="map-dialog__footer">
            <div class="map-current">
              <span>当前选中</span>
              <strong>{{ pendingAddress || mapAddressError || '暂未选择位置' }}</strong>
            </div>
            <div class="map-actions">
              <button class="btn btn-secondary btn-sm" type="button" @click="closeMapDialog">取消</button>
              <button class="btn btn-primary btn-sm" type="button" :disabled="!canConfirmMap" @click="confirmMapSelection">
                确认选择
              </button>
            </div>
          </footer>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Close, Location, MapLocation, Search } from '@element-plus/icons-vue'
import { getCurrentAddress, loadAmap, reverseGeocodeDetail, searchAmapAddress } from '@/composables/useAmapLocation'
import { useAppStore } from '@/stores/app'

// 既没有坐标也没搜到结果时，地图先落在市中心，总比空白强
const DEFAULT_CENTER = Object.freeze({ longitude_wsh: 121.4737, latitude_wsh: 31.2304 })
const SEARCH_DEBOUNCE_MS = 500

const props = defineProps({
  modelValue: { type: String, default: '' },
  latitude: { type: [Number, String], default: null },
  longitude: { type: [Number, String], default: null },
  source: { type: String, default: '' },
  placeholder: { type: String, default: '搜索地址或打开地图选址' },
  showLocateButton: { type: Boolean, default: true },
  locateButtonText: { type: String, default: '定位' },
  showMapButton: { type: Boolean, default: true },
  mapButtonText: { type: String, default: '地图选址' },
})

const emit = defineEmits([
  'update:modelValue',
  'update:latitude',
  'update:longitude',
  'update:source',
  'selected',
])

const appStore = useAppStore()
const tips = ref([])
const showTips = ref(false)
const locating = ref(false)
const mapDialogVisible = ref(false)
const mapLoading = ref(false)
const mapSearching = ref(false)
const mapSearched = ref(false)
const mapKeyword = ref('')
const mapResults = ref([])
const mapContainer = ref(null)
const pendingPoint = ref(null)
const pendingAddress = ref('')
const pendingSource = ref('')
const mapAddressError = ref('')
const resolvingAddress = ref(false)

let searchTimer = null
let hideTipsTimer = null
let mapSearchTimer = null
let mapInstance = null
let markerInstance = null
// 每点一次地图就自增，回来的结果对不上号就丢掉，免得旧地址盖掉新地址
let reverseSequence = 0
// 反查地址会顺手把结果填进搜索框，填的这一刻做个记号，别当成用户输入又搜一遍
let suppressedMapKeywordValue = null

const hasActionButtons = computed(() => props.showLocateButton || props.showMapButton)
// 点定下来了、地址也认出来了，才允许点确认
const canConfirmMap = computed(() => Boolean(pendingPoint.value && pendingAddress.value && !resolvingAddress.value))

// 地址是怎么来的，决定下面那句小字提示怎么写
const selectedText = computed(() => {
  if (!props.modelValue || !props.source) return ''
  if (props.source === 'merchant') return '已选择商家位置'
  if (props.source === 'amap_geolocation') return '已通过定位选择位置'
  if (props.source === 'amap_map') return '已通过地图选择位置'
  if (props.source === 'amap_tip') return '已从搜索结果选择位置'
  return '已确认地址位置'
})

// 手一打字，之前选中的坐标就作废了，否则文字和坐标会对不上
function onInput(event) {
  emit('update:modelValue', event.target.value)
  emit('update:latitude', null)
  emit('update:longitude', null)
  emit('update:source', '')
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(search, SEARCH_DEBOUNCE_MS)
}

// 少于两个字不搜，一搜就是一大把无关结果
async function search() {
  const keyword = String(props.modelValue || '').trim()
  window.clearTimeout(hideTipsTimer)
  showTips.value = true
  if (keyword.length < 2) {
    tips.value = []
    return
  }
  try {
    tips.value = await searchAmapAddress(keyword, {
      center: normalizePoint(props.longitude, props.latitude),
      withNearby: true,
      limit: 8,
    })
  } catch (error) {
    tips.value = []
  }
}

// 为什么不是马上收起来？输入框一失焦就收起，鼠标还没来得及点下拉项，它就没了
function hideTipsSoon() {
  window.clearTimeout(hideTipsTimer)
  hideTipsTimer = window.setTimeout(() => {
    showTips.value = false
  }, 160)
}

async function useCurrentLocation() {
  locating.value = true
  try {
    selectTip(await getCurrentAddress())
  } catch (error) {
    appStore.addToast(error.message || '定位失败，请检查浏览器权限', 'error')
  } finally {
    locating.value = false
  }
}

// 打开前先把上一次留下的结果清干净，不然会闪一下旧内容
async function openMapDialog() {
  mapDialogVisible.value = true
  mapLoading.value = true
  showTips.value = false
  mapKeyword.value = props.modelValue || ''
  mapResults.value = []
  mapSearched.value = false
  pendingPoint.value = null
  pendingAddress.value = ''
  pendingSource.value = ''
  mapAddressError.value = ''

  try {
    await nextTick()
    const AMap = await loadAmap()
    const initial = await resolveInitialSelection()
    initializeMap(AMap, initial.point || DEFAULT_CENTER, Boolean(initial.point))
    if (initial.point) {
      setPendingSelection(initial.point, initial.address || props.modelValue, initial.source || props.source || 'amap', false)
    }
  } catch (error) {
    appStore.addToast(error.message || '地图加载失败，请稍后重试', 'error')
    closeMapDialog()
  } finally {
    mapLoading.value = false
  }
}

function closeMapDialog() {
  mapDialogVisible.value = false
  mapLoading.value = false
  mapSearching.value = false
  resolvingAddress.value = false
  reverseSequence += 1   // 号往前推一格，还在路上的反查结果就作废了
  window.clearTimeout(mapSearchTimer)
  destroyMap()
}

// 有坐标直接用坐标；只有文字地址时，先搜一条最像的，把地图挪过去
async function resolveInitialSelection() {
  const point = normalizePoint(props.longitude, props.latitude)
  if (point) {
    return { point, address: props.modelValue || '', source: props.source || 'amap' }
  }

  const keyword = String(props.modelValue || '').trim()
  if (keyword.length >= 2) {
    try {
      const [first] = await searchAmapAddress(keyword, { withNearby: false, limit: 1 })
      const firstPoint = first ? normalizePoint(first.longitude_wsh, first.latitude_wsh) : null
      if (firstPoint) {
        return {
          point: firstPoint,
          address: first.address_wsh || keyword,
          source: first.location_source_wsh || 'amap_tip',
        }
      }
    } catch (error) {
      return { point: null, address: '', source: '' }
    }
  }

  return { point: null, address: '', source: '' }
}

function initializeMap(AMap, centerPoint, shouldShowMarker) {
  destroyMap()
  if (!mapContainer.value) return

  const center = toAmapPosition(centerPoint)
  mapInstance = new AMap.Map(mapContainer.value, {
    center,
    // 已经知道在哪就放大到街道级，只知道城市就先拉远看个大概
    zoom: shouldShowMarker ? 16 : 11,
    resizeEnable: true,
    viewMode: '2D',
  })
  mapInstance.on('click', event => {
    updateSelectionFromLngLat(event.lnglat, 'amap_map')
  })

  if (shouldShowMarker) {
    createOrMoveMarker(centerPoint)
  }
}

// 标记只建一次，之后都是挪位置；反复删了重建会闪一下
function createOrMoveMarker(point) {
  if (!window.AMap || !mapInstance) return
  const position = toAmapPosition(point)
  if (!markerInstance) {
    markerInstance = new window.AMap.Marker({
      position,
      draggable: true,
      cursor: 'move',
      anchor: 'bottom-center',
    })
    markerInstance.on('dragend', event => {
      updateSelectionFromLngLat(event.lnglat || markerInstance.getPosition(), 'amap_map')
    })
    mapInstance.add(markerInstance)
  } else {
    markerInstance.setPosition(position)
  }
}

// 点地图、拖标记、搜结果最后都走这里：先把点定下来，再去问这是哪儿
async function updateSelectionFromLngLat(lnglat, source) {
  const point = normalizeLngLat(lnglat)
  if (!point) return

  createOrMoveMarker(point)
  if (mapInstance) {
    mapInstance.setCenter(toAmapPosition(point))
  }
  setPendingSelection(point, '正在识别地址...', source, true)

  const sequence = ++reverseSequence   // 这次请求的号，回来时对不上就别写了
  try {
    const detail = await reverseGeocodeDetail(point.longitude_wsh, point.latitude_wsh, { radius: 1200 })
    if (sequence === reverseSequence) {
      pendingAddress.value = detail.address_wsh
      syncMapKeywordAndSearch(detail.address_wsh)
      mapAddressError.value = ''
    }
  } catch (error) {
    if (sequence === reverseSequence) {
      pendingAddress.value = ''
      mapAddressError.value = error.message || '未识别到详细地址，请重新点选或搜索地点'
    }
  } finally {
    if (sequence === reverseSequence) {
      resolvingAddress.value = false
    }
  }
}

// 地图里搜出结果默认就选第一条，省得用户还要再点一下
async function searchInMap(options = {}) {
  const keyword = String(mapKeyword.value || '').trim()
  if (keyword.length < 2) {
    if (!options.silent) appStore.addToast('请输入至少 2 个字搜索地点', 'warning')
    return
  }

  mapSearching.value = true
  mapSearched.value = true
  try {
    mapResults.value = await searchAmapAddress(keyword, {
      center: pendingPoint.value || getMapCenterPoint(),
      withNearby: true,
      nearbyRadius: 5000,
      limit: 12,
    })
    if (mapResults.value.length && options.selectFirst !== false) {
      selectMapResult(mapResults.value[0])
    }
  } catch (error) {
    mapResults.value = []
    appStore.addToast(error.message || '地点搜索失败', 'error')
  } finally {
    mapSearching.value = false
  }
}

function selectMapResult(result) {
  const point = normalizePoint(result.longitude_wsh, result.latitude_wsh)
  if (!point) return

  createOrMoveMarker(point)
  if (mapInstance) {
    // 高德有些版本没这个合并方法，只能拆成两次调用
    if (typeof mapInstance.setZoomAndCenter === 'function') {
      mapInstance.setZoomAndCenter(16, toAmapPosition(point))
    } else {
      mapInstance.setZoom(16)
      mapInstance.setCenter(toAmapPosition(point))
    }
  }
  setPendingSelection(point, result.address_wsh, result.location_source_wsh || 'amap_tip', false)
}

function setPendingSelection(point, address, source, isResolving) {
  pendingPoint.value = point
  pendingAddress.value = address || ''
  pendingSource.value = source || 'amap'
  mapAddressError.value = ''
  resolvingAddress.value = isResolving
}

function confirmMapSelection() {
  if (!pendingPoint.value || !pendingAddress.value) return
  selectTip({
    ...pendingPoint.value,
    address_wsh: pendingAddress.value,
    location_source_wsh: pendingSource.value || 'amap_map',
  })
  closeMapDialog()
}

function selectTip(tip) {
  emit('update:modelValue', tip.address_wsh)
  emit('update:latitude', tip.latitude_wsh)
  emit('update:longitude', tip.longitude_wsh)
  emit('update:source', tip.location_source_wsh || 'amap')
  emit('selected', tip)
  tips.value = []
  showTips.value = false
}

// 空字符串、null 转数字都会变成 0，得先确认拿到的是个正常数字
function normalizePoint(longitude, latitude) {
  const lng = Number(longitude)
  const lat = Number(latitude)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return { longitude_wsh: lng, latitude_wsh: lat }
}

// 高德给的是 LngLat 对象，自己算出来的是普通对象，两种都得认
function normalizeLngLat(value) {
  if (!value) return null
  const lng = typeof value.getLng === 'function' ? value.getLng() : value.lng
  const lat = typeof value.getLat === 'function' ? value.getLat() : value.lat
  return normalizePoint(lng, lat)
}

function toAmapPosition(point) {
  return [Number(point.longitude_wsh), Number(point.latitude_wsh)]
}

function getMapCenterPoint() {
  if (!mapInstance || typeof mapInstance.getCenter !== 'function') return null
  return normalizeLngLat(mapInstance.getCenter())
}

// 反查回来的地址要写进搜索框，但这次不算用户改的，别再搜一轮
function syncMapKeywordAndSearch(keyword) {
  const nextKeyword = String(keyword || '').trim()
  if (mapKeyword.value !== nextKeyword) {
    suppressedMapKeywordValue = nextKeyword
    mapKeyword.value = nextKeyword
    nextTick(() => {
      if (suppressedMapKeywordValue === nextKeyword) {
        suppressedMapKeywordValue = null
      }
    })
  }
  queueMapSearch()
}

// 关键字变了等半秒再搜，边打字边发请求太浪费
function queueMapSearch() {
  if (!mapDialogVisible.value) return
  window.clearTimeout(mapSearchTimer)
  const keyword = String(mapKeyword.value || '').trim()
  if (keyword.length < 2) {
    mapResults.value = []
    mapSearched.value = false
    return
  }
  mapSearchTimer = window.setTimeout(() => {
    searchInMap({ silent: true, selectFirst: false })
  }, SEARCH_DEBOUNCE_MS)
}

// 地图实例得手动销毁，否则关了弹窗它还在后台跑
function destroyMap() {
  markerInstance = null
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
}

// 搜索框内容变了才去搜；上面自己填进去的那一次直接跳过
watch(mapKeyword, value => {
  if (suppressedMapKeywordValue === value) {
    suppressedMapKeywordValue = null
    return
  }
  queueMapSearch()
})

// 组件没了，定时器和地图一起清掉，不然回调会打到已经消失的组件上
onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  window.clearTimeout(hideTipsTimer)
  window.clearTimeout(mapSearchTimer)
  destroyMap()
})
</script>

<style scoped>
.amap-address-picker { position: relative; display: grid; gap: 6px; }
.picker-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; align-items: stretch; }
.picker-row--single { grid-template-columns: minmax(0, 1fr); }
.picker-actions { display: flex; align-items: stretch; gap: 8px; flex-wrap: wrap; }
.picker-actions .btn { min-height: 0; white-space: nowrap; }
.tips-panel {
  position: absolute;
  z-index: 30;
  top: 42px;
  left: 0;
  right: 0;
  display: grid;
  max-height: 220px;
  overflow: auto;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-md);
}
.tip-item {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--color-foreground);
  text-align: left;
  display: grid;
  gap: 3px;
  padding: 10px 12px;
  cursor: pointer;
  overflow-wrap: anywhere;
}
.tip-item:hover { background: var(--color-muted); }
.result-title {
  color: var(--color-foreground);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}
.result-address {
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.35;
}
.result-meta {
  width: fit-content;
  border-radius: var(--radius-pill);
  background: var(--color-muted);
  color: var(--color-primary);
  font-size: 11px;
  line-height: 1.3;
  padding: 2px 7px;
}
.selected-text {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.map-dialog-overlay {
  position: fixed;
  z-index: 1200;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.56);
}
.map-dialog {
  width: min(960px, 100%);
  max-height: min(760px, calc(100vh - 40px));
  display: grid;
  grid-template-rows: auto auto minmax(320px, 1fr) auto;
  gap: 12px;
  overflow: hidden;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-lg);
  padding: 16px;
}
.map-dialog__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}
.map-dialog__header h3 {
  margin: 0;
  color: var(--color-foreground);
  font-size: 18px;
  line-height: 1.3;
}
.map-dialog__header p {
  margin: 4px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  overflow-wrap: anywhere;
}
.icon-button {
  width: 32px;
  height: 32px;
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  color: var(--color-foreground);
  cursor: pointer;
}
.icon-button:hover { background: var(--color-muted); }
.map-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}
.map-body {
  min-height: 320px;
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(0, 1fr);
  gap: 12px;
}
.map-results {
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 6px;
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 8px;
  background: var(--color-background);
}
.map-result {
  width: 100%;
  border: 0;
  border-radius: var(--radius-inline);
  background: transparent;
  color: var(--color-foreground);
  text-align: left;
  display: grid;
  gap: 3px;
  padding: 9px 10px;
  cursor: pointer;
  overflow-wrap: anywhere;
}
.map-result:hover,
.map-result:focus-visible { background: var(--color-muted); }
.map-empty {
  padding: 10px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.map-container {
  position: relative;
  min-height: 320px;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-muted);
}
.map-loading {
  position: absolute;
  z-index: 2;
  inset: 0;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--color-card) 78%, transparent);
  color: var(--color-muted-foreground);
}
.map-dialog__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.map-current {
  min-width: 0;
  display: grid;
  gap: 2px;
}
.map-current span {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.map-current strong {
  color: var(--color-foreground);
  font-size: 14px;
  overflow-wrap: anywhere;
}
.map-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

@media (max-width: 760px) {
  .picker-row { grid-template-columns: 1fr; }
  .picker-actions { justify-content: flex-start; }
  .tips-panel { top: 82px; }
  .map-dialog-overlay { padding: 10px; }
  .map-dialog {
    max-height: calc(100vh - 20px);
    grid-template-rows: auto auto auto auto;
    padding: 12px;
  }
  .map-body { grid-template-columns: 1fr; }
  .map-results {
    max-height: 128px;
    order: 2;
  }
  .map-container { min-height: 300px; }
  .map-dialog__footer {
    align-items: stretch;
    flex-direction: column;
  }
  .map-actions { justify-content: flex-end; }
}
</style>
