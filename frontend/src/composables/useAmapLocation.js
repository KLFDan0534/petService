import axios from 'axios'
import request from '@/utils/request'

const amapRest = axios.create({
  baseURL: 'https://restapi.amap.com/v3',
  timeout: 10000,
})

let configPromise
let loaderPromise

async function loadGeoConfig() {
  if (!configPromise) {
    configPromise = request.get('/api/geo/config').then(response => {
      const data = response.data?.data || {}
      const apiKey = data.apiKey || ''
      if (!apiKey) {
        throw new Error('未配置高德地图 key，请在 .env 写入 GAO_MAP_API_KEY')
      }
      return {
        apiKey,
        securityCode: data.securityCode || '',
        handoverRadiusMeters: data.handoverRadiusMeters || 500,
        attendanceRadiusMeters: data.attendanceRadiusMeters || 300,
      }
    })
  }
  return configPromise
}

async function amapGet(path, params = {}) {
  const config = await loadGeoConfig()
  const response = await amapRest.get(path, {
    params: {
      output: 'json',
      ...params,
      key: config.apiKey,
    },
  })
  const data = response.data || {}
  if (String(data.status) !== '1') {
    throw new Error(data.info || '高德地图请求失败')
  }
  return data
}

export async function loadAmap() {
  if (window.AMap) return window.AMap
  if (!loaderPromise) {
    loaderPromise = loadGeoConfig().then(config => new Promise((resolve, reject) => {
      if (config.securityCode) {
        window._AMapSecurityConfig = {
          ...(window._AMapSecurityConfig || {}),
          securityJsCode: config.securityCode,
        }
      }
      const callbackName = '__amapInitCallbackWsh'
      const cleanup = () => {
        try {
          delete window[callbackName]
        } catch (error) {
          window[callbackName] = undefined
        }
      }
      window[callbackName] = () => {
        cleanup()
        if (window.AMap) resolve(window.AMap)
        else reject(new Error('高德地图加载失败'))
      }
      const script = document.createElement('script')
      script.charset = 'utf-8'
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.apiKey)}&plugin=AMap.Geolocation,AMap.Geocoder,AMap.AutoComplete,AMap.PlaceSearch&callback=${callbackName}`
      script.async = true
      script.onerror = () => {
        cleanup()
        reject(new Error('高德地图加载失败'))
      }
      document.head.appendChild(script)
    }))
  }
  return loaderPromise
}

export async function getCurrentAddress() {
  const AMap = await loadAmap()
  await loadPlugin(AMap, ['AMap.Geolocation', 'AMap.Geocoder'])
  return new Promise((resolve, reject) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      convert: true,
      showButton: false,
      showMarker: false,
      showCircle: false,
    })
    geolocation.getCurrentPosition(async (status, result) => {
      if (status !== 'complete' || !result?.position) {
        reject(new Error(result?.message || '无法获取当前位置，请允许浏览器定位'))
        return
      }
      const point = normalizePoint(result.position)
      if (!point) {
        reject(new Error('无法识别当前位置'))
        return
      }
      try {
        const address = result.formattedAddress || await reverseGeocode(point.longitude_wsh, point.latitude_wsh)
        resolve({
          ...point,
          address_wsh: address || '当前位置',
          accuracy_wsh: numberOrNull(result.accuracy),
          location_source_wsh: 'amap_geolocation',
        })
      } catch (error) {
        resolve({
          ...point,
          address_wsh: result.formattedAddress || '当前位置',
          accuracy_wsh: numberOrNull(result.accuracy),
          location_source_wsh: 'amap_geolocation',
        })
      }
    })
  })
}

export async function searchAmapAddress(keyword, options = {}) {
  const text = String(keyword || '').trim()
  if (!text) return []

  const limit = clampNumber(options.limit, 1, 20, 10)
  const center = normalizePoint(options.center)
    || normalizeNumbers(options.latitude, options.longitude)
    || normalizeNumbers(options.centerLatitude, options.centerLongitude)

  const [geocodes, places, nearbyPlaces] = await Promise.all([
    geocodeAddress(text),
    searchPlaceRest(text),
    center ? searchPlaceAroundRest(text, center, options.nearbyRadius || 5000) : Promise.resolve([]),
  ])

  let results = mergeSearchResults([...geocodes, ...places, ...nearbyPlaces])
  if (!results.length) {
    results = await searchAmapAddressByJs(text, center, options)
  }

  const exactCenter = results[0] ? normalizeNumbers(results[0].latitude_wsh, results[0].longitude_wsh) : null
  if (options.withNearby !== false && exactCenter) {
    try {
      const detail = await reverseGeocodeDetail(exactCenter.longitude_wsh, exactCenter.latitude_wsh, {
        radius: options.nearbyRadius || 3000,
      })
      const nearbyPois = (detail.pois_wsh || []).map(poi => ({
        ...poi,
        location_source_wsh: 'amap_nearby',
      }))
      results = mergeSearchResults([...results, ...nearbyPois])
    } catch (error) {
      // Search suggestions should still work when nearby lookup is unavailable.
    }
  }

  return rankSearchResults(results, text).slice(0, limit)
}

export async function reverseGeocode(longitude, latitude) {
  const detail = await reverseGeocodeDetail(longitude, latitude)
  return detail.address_wsh
}

export async function reverseGeocodeDetail(longitude, latitude, options = {}) {
  const point = normalizeNumbers(latitude, longitude)
  if (!point) throw new Error('坐标无效，无法解析地址')

  try {
    return await reverseGeocodeDetailRest(point, options)
  } catch (error) {
    return reverseGeocodeDetailByJs(point, options)
  }
}

async function geocodeAddress(text) {
  try {
    const data = await amapGet('/geocode/geo', { address: text })
    return Array.isArray(data.geocodes)
      ? data.geocodes.map(item => normalizeGeocode(item)).filter(Boolean)
      : []
  } catch (error) {
    try {
      return await geocodeAddressByJs(text)
    } catch (fallbackError) {
      return []
    }
  }
}

async function searchPlaceRest(text) {
  try {
    const data = await amapGet('/place/text', {
      keywords: text,
      offset: 10,
      page: 1,
      extensions: 'all',
    })
    return Array.isArray(data.pois)
      ? data.pois.map(poi => normalizePoi(poi, 'amap_place')).filter(Boolean)
      : []
  } catch (error) {
    return []
  }
}

async function searchPlaceAroundRest(text, center, radius) {
  try {
    const data = await amapGet('/place/around', {
      keywords: text,
      location: `${center.longitude_wsh},${center.latitude_wsh}`,
      radius: clampNumber(radius, 100, 50000, 5000),
      offset: 10,
      page: 1,
      extensions: 'all',
    })
    return Array.isArray(data.pois)
      ? data.pois.map(poi => normalizePoi(poi, 'amap_nearby')).filter(Boolean)
      : []
  } catch (error) {
    return []
  }
}

async function reverseGeocodeDetailRest(point, options = {}) {
  const data = await amapGet('/geocode/regeo', {
    location: `${point.longitude_wsh},${point.latitude_wsh}`,
    radius: clampNumber(options.radius, 100, 10000, 1000),
    extensions: 'all',
    batch: false,
    roadlevel: 0,
  })
  const regeocode = data.regeocode
  const address = cleanText(regeocode?.formatted_address) || buildAddressFromComponent(regeocode?.addressComponent)
  if (!address) throw new Error('未识别到详细地址，请重新点选或搜索地点')

  return {
    ...point,
    address_wsh: address,
    display_name_wsh: address,
    location_source_wsh: 'amap_reverse',
    pois_wsh: Array.isArray(regeocode?.pois)
      ? regeocode.pois.map(poi => normalizePoi(poi, 'amap_nearby')).filter(Boolean)
      : [],
  }
}

async function searchAmapAddressByJs(text, center, options = {}) {
  const AMap = await loadAmap()
  await loadPlugin(AMap, ['AMap.AutoComplete', 'AMap.PlaceSearch'])
  const [tips, places, nearbyPlaces] = await Promise.all([
    searchAutocompleteByJs(AMap, text),
    searchPlaceByJs(AMap, text),
    center ? searchPlaceNearbyByJs(AMap, text, center, options.nearbyRadius || 5000) : Promise.resolve([]),
  ])
  return mergeSearchResults([...tips, ...places, ...nearbyPlaces])
}

async function geocodeAddressByJs(text) {
  const AMap = await loadAmap()
  await loadPlugin(AMap, ['AMap.Geocoder'])
  return new Promise(resolve => {
    const geocoder = new AMap.Geocoder()
    geocoder.getLocation(text, (status, result) => {
      const geocodes = result?.geocodes
      resolve(status === 'complete' && Array.isArray(geocodes)
        ? geocodes.map(item => normalizeGeocode(item)).filter(Boolean)
        : [])
    })
  })
}

function reverseGeocodeDetailByJs(point, options = {}) {
  return loadAmap().then(async AMap => {
    await loadPlugin(AMap, ['AMap.Geocoder'])
    return new Promise((resolve, reject) => {
      const geocoder = new AMap.Geocoder({
        radius: clampNumber(options.radius, 100, 10000, 1000),
        extensions: 'all',
      })
      geocoder.getAddress([point.longitude_wsh, point.latitude_wsh], (status, result) => {
        const regeocode = result?.regeocode
        const address = cleanText(regeocode?.formattedAddress) || buildAddressFromComponent(regeocode?.addressComponent)
        if (status !== 'complete' || !regeocode || !address) {
          reject(new Error('逆地址解析失败，请重新点选或搜索地点'))
          return
        }
        resolve({
          ...point,
          address_wsh: address,
          display_name_wsh: address,
          location_source_wsh: 'amap_reverse',
          pois_wsh: Array.isArray(regeocode.pois)
            ? regeocode.pois.map(poi => normalizePoi(poi, 'amap_nearby')).filter(Boolean)
            : [],
        })
      })
    })
  })
}

function searchAutocompleteByJs(AMap, text) {
  return new Promise(resolve => {
    const autocomplete = new AMap.AutoComplete({ city: '全国', citylimit: false })
    autocomplete.search(text, (status, result) => {
      resolve(status === 'complete' && Array.isArray(result?.tips)
        ? result.tips.map(tip => normalizeTip(tip)).filter(Boolean)
        : [])
    })
  })
}

function searchPlaceByJs(AMap, text) {
  return new Promise(resolve => {
    const placeSearch = new AMap.PlaceSearch({
      city: '全国',
      citylimit: false,
      extensions: 'all',
      pageSize: 10,
      pageIndex: 1,
    })
    placeSearch.search(text, (status, result) => {
      resolve(status === 'complete' ? normalizePoiList(result, 'amap_place') : [])
    })
  })
}

function searchPlaceNearbyByJs(AMap, text, center, radius) {
  return new Promise(resolve => {
    const placeSearch = new AMap.PlaceSearch({
      extensions: 'all',
      pageSize: 10,
      pageIndex: 1,
    })
    placeSearch.searchNearBy(text, [center.longitude_wsh, center.latitude_wsh], clampNumber(radius, 100, 50000, 5000), (status, result) => {
      resolve(status === 'complete' ? normalizePoiList(result, 'amap_nearby') : [])
    })
  })
}

function normalizePoiList(result, source) {
  const pois = result?.poiList?.pois || result?.pois || []
  return Array.isArray(pois)
    ? pois.map(poi => normalizePoi(poi, source)).filter(Boolean)
    : []
}

function loadPlugin(AMap, names) {
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error('高德地图插件加载超时')), 15000)
    AMap.plugin(names, () => {
      window.clearTimeout(timer)
      resolve()
    })
  })
}

function normalizeGeocode(geocode) {
  const point = normalizePoint(geocode.location)
  if (!point) return null
  const address = cleanText(geocode.formatted_address || geocode.formattedAddress)
    || buildAddressFromComponent(geocode.addressComponent)
  return {
    ...point,
    address_wsh: address,
    display_name_wsh: address,
    district_wsh: joinAddress([geocode.province, geocode.city, geocode.district]),
    distance_wsh: null,
    location_source_wsh: 'amap_geocode',
    match_score_wsh: 100,
    poi_id_wsh: geocode.adcode || '',
  }
}

function normalizeTip(tip) {
  const point = normalizePoint(tip.location)
  if (!point) return null
  const name = cleanText(tip.name)
  const district = cleanText(tip.district)
  const detail = cleanText(tip.address)
  const address = joinAddress([district, name, detail])
  return {
    ...point,
    address_wsh: address || name,
    display_name_wsh: name || address,
    district_wsh: district,
    distance_wsh: numberOrNull(tip.distance),
    location_source_wsh: 'amap_tip',
    poi_id_wsh: tip.id || '',
  }
}

function normalizePoi(poi, source) {
  const point = normalizePoint(poi.location)
  if (!point) return null
  const name = cleanText(poi.name)
  const province = cleanText(poi.pname || poi.provinceName)
  const city = cleanText(poi.cityname || poi.cityName)
  const district = cleanText(poi.adname || poi.adName || poi.district)
  const detail = cleanText(poi.address)
  const address = joinAddress([province, city, district, name, detail])
  return {
    ...point,
    address_wsh: address || name,
    display_name_wsh: name || address,
    district_wsh: joinAddress([province, city, district]),
    distance_wsh: numberOrNull(poi.distance),
    location_source_wsh: source,
    poi_id_wsh: poi.id || '',
  }
}

function rankSearchResults(items, keyword) {
  const ranked = items.map((item, index) => {
    const matchScore = item.match_score_wsh || calculateMatchScore(keyword, item)
    const distance = numberOrNull(item.distance_wsh)
    return {
      ...item,
      match_score_wsh: matchScore,
      match_label_wsh: buildDistanceLabel(distance),
      _index_wsh: index,
    }
  })

  const sorted = [...ranked].sort(compareSearchResult)
  const primary = sorted.find(item => item.location_source_wsh !== 'amap_nearby') || sorted[0]
  if (!primary) return []

  const rest = sorted.filter(item => item !== primary)
  const nearby = rest
    .filter(item => item.location_source_wsh === 'amap_nearby')
    .filter(item => item.match_score_wsh >= 45 || numberOrNull(item.distance_wsh) == null || Number(item.distance_wsh) <= 1200)
    .sort(compareNearbyResult)
  const others = rest
    .filter(item => item.location_source_wsh !== 'amap_nearby')
    .sort(compareSearchResult)

  return [primary, ...nearby, ...others].map(({ _index_wsh, ...item }) => item)
}

function compareSearchResult(a, b) {
  const scoreDiff = Number(b.match_score_wsh || 0) - Number(a.match_score_wsh || 0)
  if (scoreDiff !== 0) return scoreDiff
  const sourceDiff = sourcePriority(a.location_source_wsh) - sourcePriority(b.location_source_wsh)
  if (sourceDiff !== 0) return sourceDiff
  return compareDistance(a, b) || Number(a._index_wsh || 0) - Number(b._index_wsh || 0)
}

function compareNearbyResult(a, b) {
  const distanceDiff = compareDistance(a, b)
  if (distanceDiff !== 0) return distanceDiff
  return Number(b.match_score_wsh || 0) - Number(a.match_score_wsh || 0)
}

function compareDistance(a, b) {
  const first = numberOrNull(a.distance_wsh)
  const second = numberOrNull(b.distance_wsh)
  if (first == null && second == null) return 0
  if (first == null) return 1
  if (second == null) return -1
  return first - second
}

function mergeSearchResults(items) {
  const seen = new Set()
  const merged = []
  for (const item of items) {
    if (!item?.address_wsh || item.latitude_wsh == null || item.longitude_wsh == null) continue
    const key = item.poi_id_wsh
      || `${Math.round(Number(item.longitude_wsh) * 100000)}:${Math.round(Number(item.latitude_wsh) * 100000)}:${item.display_name_wsh || item.address_wsh}`
    if (seen.has(key)) continue
    seen.add(key)
    merged.push(item)
  }
  return merged
}

function calculateMatchScore(keyword, item) {
  const tokens = tokenizeKeyword(keyword)
  const target = normalizeSearchText(`${item.display_name_wsh || ''}${item.address_wsh || ''}`)
  if (!tokens.length || !target) return 60
  if (tokens.some(token => target.includes(token))) return item.location_source_wsh === 'amap_nearby' ? 92 : 100

  const bestRatio = Math.max(...tokens.map(token => overlapRatio(token, target)))
  const distance = numberOrNull(item.distance_wsh)
  const nearbyBonus = distance == null ? 0 : Math.max(0, 12 - Math.floor(distance / 400))
  return Math.max(35, Math.min(99, Math.round(bestRatio * 82 + nearbyBonus)))
}

function buildDistanceLabel(distance) {
  if (distance == null) return ''
  return `距离${formatDistance(distance)}`
}

function sourcePriority(source) {
  return {
    amap_geocode: 0,
    amap_tip: 1,
    amap_place: 2,
    amap_nearby: 3,
  }[source] ?? 4
}

function tokenizeKeyword(keyword) {
  return String(keyword || '')
    .split(/[\s,，、;；]+/)
    .map(normalizeSearchText)
    .filter(token => token.length >= 2)
}

function overlapRatio(token, target) {
  const chars = Array.from(new Set(token))
  if (!chars.length) return 0
  const matched = chars.filter(char => target.includes(char)).length
  return matched / chars.length
}

function normalizeSearchText(value) {
  return String(value || '').replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, '').toLowerCase()
}

function buildAddressFromComponent(component = {}) {
  return joinAddress([
    component.province,
    component.city,
    component.district,
    component.township,
    component.streetNumber?.street,
    component.streetNumber?.number,
    component.neighborhood?.name,
    component.building?.name,
  ])
}

function joinAddress(parts) {
  const values = []
  for (const part of parts) {
    const text = cleanText(part)
    if (!text || values.some(value => value === text || value.includes(text))) continue
    values.push(text)
  }
  return values.join(' ')
}

function cleanText(value) {
  if (Array.isArray(value)) return value.map(cleanText).filter(Boolean).join(' ')
  return typeof value === 'string' ? value.trim() : ''
}

function normalizePoint(value) {
  if (!value) return null
  if (typeof value === 'string') {
    const [lng, lat] = value.split(',').map(item => Number(item))
    return normalizeNumbers(lat, lng)
  }
  const lat = typeof value.getLat === 'function' ? value.getLat() : value.lat ?? value.latitude ?? value.latitude_wsh
  const lng = typeof value.getLng === 'function' ? value.getLng() : value.lng ?? value.longitude ?? value.longitude_wsh
  return normalizeNumbers(lat, lng)
}

function normalizeNumbers(latitude, longitude) {
  const lat = Number(latitude)
  const lng = Number(longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null
  return { latitude_wsh: lat, longitude_wsh: lng }
}

function numberOrNull(value) {
  if (value == null || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function clampNumber(value, min, max, fallback) {
  const num = Number(value)
  if (!Number.isFinite(num)) return fallback
  return Math.max(min, Math.min(max, num))
}

function formatDistance(value) {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return ''
  return distance >= 1000 ? `${(distance / 1000).toFixed(1)}公里` : `${Math.round(distance)}米`
}
