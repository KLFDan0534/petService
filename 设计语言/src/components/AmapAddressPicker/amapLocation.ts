/**
 * Amap (高德地图) location helpers.
 *
 * Ported from the product's `src/composables/useAmapLocation.js`. The only
 * behavioural change: the API key is supplied by the caller (component prop)
 * instead of being fetched from `/api/geo/config`, and REST calls use `fetch`
 * so the design system stays dependency-free. Result shapes (`*_wsh` fields),
 * merge/rank logic and error messages are kept 1:1 with the source.
 */

export interface AmapConfig {
  apiKey: string;
  securityCode?: string;
}

export interface AmapPoint {
  latitude_wsh: number;
  longitude_wsh: number;
}

export interface AmapPlace extends AmapPoint {
  address_wsh: string;
  display_name_wsh?: string;
  district_wsh?: string;
  distance_wsh?: number | null;
  location_source_wsh?: string;
  poi_id_wsh?: string;
  match_score_wsh?: number;
  match_label_wsh?: string;
  accuracy_wsh?: number | null;
  pois_wsh?: AmapPlace[];
}

export interface AmapSearchOptions {
  config?: AmapConfig;
  center?: AmapPoint | null;
  latitude?: number | null;
  longitude?: number | null;
  withNearby?: boolean;
  nearbyRadius?: number;
  limit?: number;
}

export interface AmapReverseOptions {
  config?: AmapConfig;
  radius?: number;
}

/* ── Minimal typings for the Amap JS SDK surface this component uses ── */

export interface AmapLngLat {
  lng?: number;
  lat?: number;
  getLng?: () => number;
  getLat?: () => number;
}

export interface AmapMapLike {
  on: (event: string, handler: (payload: {lnglat?: AmapLngLat;}) => void) => void;
  add: (overlay: AmapMarkerLike) => void;
  setCenter: (position: [number, number]) => void;
  setZoom: (zoom: number) => void;
  setZoomAndCenter?: (zoom: number, position: [number, number]) => void;
  getCenter?: () => AmapLngLat;
  destroy: () => void;
}

export interface AmapMarkerLike {
  on: (event: string, handler: (payload: {lnglat?: AmapLngLat;}) => void) => void;
  setPosition: (position: [number, number]) => void;
  getPosition: () => AmapLngLat;
}

interface AmapGeolocationResult {
  position?: AmapLngLat;
  formattedAddress?: string;
  accuracy?: number | string;
  message?: string;
}

export interface AmapNamespace {
  Map: new (container: HTMLElement, options: Record<string, unknown>) => AmapMapLike;
  Marker: new (options: Record<string, unknown>) => AmapMarkerLike;
  plugin: (names: string[], callback: () => void) => void;
  Geolocation: new (options: Record<string, unknown>) => {
    getCurrentPosition: (callback: (status: string, result: AmapGeolocationResult | null) => void) => void;
  };
  Geocoder: new (options?: Record<string, unknown>) => {
    getAddress: (position: [number, number], callback: (status: string, result: RawGeocodeResult | null) => void) => void;
    getLocation: (text: string, callback: (status: string, result: RawGeocodeResult | null) => void) => void;
  };
  AutoComplete: new (options: Record<string, unknown>) => {
    search: (text: string, callback: (status: string, result: {tips?: RawTip[];} | null) => void) => void;
  };
  PlaceSearch: new (options: Record<string, unknown>) => {
    search: (text: string, callback: (status: string, result: RawPlaceResult | null) => void) => void;
    searchNearBy: (
    text: string,
    center: [number, number],
    radius: number,
    callback: (status: string, result: RawPlaceResult | null) => void)
    => void;
  };
}

/* ── Raw Amap payload shapes ── */

type RawText = string | string[] | undefined | null;
type RawLocation = string | AmapLngLat | undefined | null;

interface RawAddressComponent {
  province?: RawText;
  city?: RawText;
  district?: RawText;
  township?: RawText;
  streetNumber?: {street?: RawText;number?: RawText;};
  neighborhood?: {name?: RawText;};
  building?: {name?: RawText;};
}

interface RawPoi {
  id?: string;
  name?: RawText;
  address?: RawText;
  location?: RawLocation;
  pname?: RawText;
  provinceName?: RawText;
  cityname?: RawText;
  cityName?: RawText;
  adname?: RawText;
  adName?: RawText;
  district?: RawText;
  distance?: number | string;
}

interface RawGeocode {
  location?: RawLocation;
  formatted_address?: RawText;
  formattedAddress?: RawText;
  addressComponent?: RawAddressComponent;
  province?: RawText;
  city?: RawText;
  district?: RawText;
  adcode?: string;
}

interface RawRegeocode {
  formatted_address?: RawText;
  formattedAddress?: RawText;
  addressComponent?: RawAddressComponent;
  pois?: RawPoi[];
}

interface RawGeocodeResult {
  regeocode?: RawRegeocode;
  geocodes?: RawGeocode[];
}

interface RawPlaceResult {
  poiList?: {pois?: RawPoi[];};
  pois?: RawPoi[];
}

interface RawTip {
  id?: string;
  name?: RawText;
  district?: RawText;
  address?: RawText;
  location?: RawLocation;
  distance?: number | string;
}

interface AmapRestResponse {
  status?: string | number;
  info?: string;
  geocodes?: RawGeocode[];
  pois?: RawPoi[];
  regeocode?: RawRegeocode;
}

declare global {
  interface Window {
    AMap?: AmapNamespace;
    _AMapSecurityConfig?: {securityJsCode?: string;};
    __amapInitCallbackWsh?: () => void;
  }
}

const MISSING_KEY_MESSAGE = '未配置高德地图 key，请在 .env 写入 GAO_MAP_API_KEY';

let loaderPromise: Promise<AmapNamespace> | null = null;

function resolveConfig(config?: AmapConfig): AmapConfig {
  if (!config?.apiKey) throw new Error(MISSING_KEY_MESSAGE);
  return config;
}

async function amapGet(
path: string,
params: Record<string, string | number | boolean | undefined>,
config?: AmapConfig)
: Promise<AmapRestResponse> {
  const resolved = resolveConfig(config);
  const url = new URL(`https://restapi.amap.com/v3${path}`);
  url.searchParams.set('output', 'json');
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) url.searchParams.set(key, String(value));
  });
  url.searchParams.set('key', resolved.apiKey);

  const response = await fetch(url.toString());
  const data = (await response.json()) as AmapRestResponse;
  if (String(data.status) !== '1') {
    throw new Error(data.info || '高德地图请求失败');
  }
  return data;
}

export async function loadAmap(config?: AmapConfig): Promise<AmapNamespace> {
  if (window.AMap) return window.AMap;
  const resolved = resolveConfig(config);
  if (!loaderPromise) {
    loaderPromise = new Promise<AmapNamespace>((resolve, reject) => {
      if (resolved.securityCode) {
        window._AMapSecurityConfig = {
          ...(window._AMapSecurityConfig || {}),
          securityJsCode: resolved.securityCode
        };
      }
      const cleanup = () => {
        window.__amapInitCallbackWsh = undefined;
      };
      window.__amapInitCallbackWsh = () => {
        cleanup();
        if (window.AMap) resolve(window.AMap);else
        reject(new Error('高德地图加载失败'));
      };
      const script = document.createElement('script');
      script.charset = 'utf-8';
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(
        resolved.apiKey
      )}&plugin=AMap.Geolocation,AMap.Geocoder,AMap.AutoComplete,AMap.PlaceSearch&callback=__amapInitCallbackWsh`;
      script.async = true;
      script.onerror = () => {
        cleanup();
        loaderPromise = null;
        reject(new Error('高德地图加载失败'));
      };
      document.head.appendChild(script);
    });
  }
  return loaderPromise;
}

export async function getCurrentAddress(config?: AmapConfig): Promise<AmapPlace> {
  const AMap = await loadAmap(config);
  await loadPlugin(AMap, ['AMap.Geolocation', 'AMap.Geocoder']);
  return new Promise<AmapPlace>((resolve, reject) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      convert: true,
      showButton: false,
      showMarker: false,
      showCircle: false
    });
    geolocation.getCurrentPosition(async (status, result) => {
      if (status !== 'complete' || !result?.position) {
        reject(new Error(result?.message || '无法获取当前位置，请允许浏览器定位'));
        return;
      }
      const point = normalizeLocation(result.position);
      if (!point) {
        reject(new Error('无法识别当前位置'));
        return;
      }
      const fallbackAddress = cleanText(result.formattedAddress) || '当前位置';
      try {
        const detail = await reverseGeocodeDetail(point.longitude_wsh, point.latitude_wsh, { config });
        resolve({
          ...point,
          address_wsh: cleanText(result.formattedAddress) || detail.address_wsh || '当前位置',
          accuracy_wsh: numberOrNull(result.accuracy),
          location_source_wsh: 'amap_geolocation'
        });
      } catch (error) {
        resolve({
          ...point,
          address_wsh: fallbackAddress,
          accuracy_wsh: numberOrNull(result.accuracy),
          location_source_wsh: 'amap_geolocation'
        });
      }
    });
  });
}

export async function searchAmapAddress(keyword: string, options: AmapSearchOptions = {}): Promise<AmapPlace[]> {
  const text = String(keyword || '').trim();
  if (!text) return [];

  const limit = clampNumber(options.limit, 1, 20, 10);
  const center =
  normalizePoint(options.center) || normalizeNumbers(options.latitude, options.longitude) || null;

  const [geocodes, places, nearbyPlaces] = await Promise.all([
  geocodeAddress(text, options.config),
  searchPlaceRest(text, options.config),
  center ? searchPlaceAroundRest(text, center, options.nearbyRadius || 5000, options.config) : Promise.resolve([])]
  );

  let results = mergeSearchResults([...geocodes, ...places, ...nearbyPlaces]);
  if (!results.length) {
    results = await searchAmapAddressByJs(text, center, options);
  }

  const exactCenter = results[0] ? normalizeNumbers(results[0].latitude_wsh, results[0].longitude_wsh) : null;
  if (options.withNearby !== false && exactCenter) {
    try {
      const detail = await reverseGeocodeDetail(exactCenter.longitude_wsh, exactCenter.latitude_wsh, {
        config: options.config,
        radius: options.nearbyRadius || 3000
      });
      const nearbyPois = (detail.pois_wsh || []).map((poi) => ({ ...poi, location_source_wsh: 'amap_nearby' }));
      results = mergeSearchResults([...results, ...nearbyPois]);
    } catch (error) {

      // Search suggestions should still work when nearby lookup is unavailable.
    }}

  return rankSearchResults(results, text).slice(0, limit);
}

export async function reverseGeocodeDetail(
longitude: number | string,
latitude: number | string,
options: AmapReverseOptions = {})
: Promise<AmapPlace> {
  const point = normalizeNumbers(latitude, longitude);
  if (!point) throw new Error('坐标无效，无法解析地址');

  try {
    return await reverseGeocodeDetailRest(point, options);
  } catch (error) {
    return reverseGeocodeDetailByJs(point, options);
  }
}

/* ── REST lookups ── */

async function geocodeAddress(text: string, config?: AmapConfig): Promise<AmapPlace[]> {
  try {
    const data = await amapGet('/geocode/geo', { address: text }, config);
    return Array.isArray(data.geocodes) ?
    data.geocodes.map((item) => normalizeGeocode(item)).filter(isPlace) :
    [];
  } catch (error) {
    try {
      return await geocodeAddressByJs(text, config);
    } catch (fallbackError) {
      return [];
    }
  }
}

async function searchPlaceRest(text: string, config?: AmapConfig): Promise<AmapPlace[]> {
  try {
    const data = await amapGet('/place/text', { keywords: text, offset: 10, page: 1, extensions: 'all' }, config);
    return Array.isArray(data.pois) ? data.pois.map((poi) => normalizePoi(poi, 'amap_place')).filter(isPlace) : [];
  } catch (error) {
    return [];
  }
}

async function searchPlaceAroundRest(
text: string,
center: AmapPoint,
radius: number,
config?: AmapConfig)
: Promise<AmapPlace[]> {
  try {
    const data = await amapGet(
      '/place/around',
      {
        keywords: text,
        location: `${center.longitude_wsh},${center.latitude_wsh}`,
        radius: clampNumber(radius, 100, 50000, 5000),
        offset: 10,
        page: 1,
        extensions: 'all'
      },
      config
    );
    return Array.isArray(data.pois) ? data.pois.map((poi) => normalizePoi(poi, 'amap_nearby')).filter(isPlace) : [];
  } catch (error) {
    return [];
  }
}

async function reverseGeocodeDetailRest(point: AmapPoint, options: AmapReverseOptions): Promise<AmapPlace> {
  const data = await amapGet(
    '/geocode/regeo',
    {
      location: `${point.longitude_wsh},${point.latitude_wsh}`,
      radius: clampNumber(options.radius, 100, 10000, 1000),
      extensions: 'all',
      batch: false,
      roadlevel: 0
    },
    options.config
  );
  const regeocode = data.regeocode;
  const address = cleanText(regeocode?.formatted_address) || buildAddressFromComponent(regeocode?.addressComponent);
  if (!address) throw new Error('未识别到详细地址，请重新点选或搜索地点');

  return {
    ...point,
    address_wsh: address,
    display_name_wsh: address,
    location_source_wsh: 'amap_reverse',
    pois_wsh: Array.isArray(regeocode?.pois) ?
    regeocode.pois.map((poi) => normalizePoi(poi, 'amap_nearby')).filter(isPlace) :
    []
  };
}

/* ── JS SDK fallbacks ── */

async function searchAmapAddressByJs(
text: string,
center: AmapPoint | null,
options: AmapSearchOptions)
: Promise<AmapPlace[]> {
  const AMap = await loadAmap(options.config);
  await loadPlugin(AMap, ['AMap.AutoComplete', 'AMap.PlaceSearch']);
  const [tips, places, nearbyPlaces] = await Promise.all([
  searchAutocompleteByJs(AMap, text),
  searchPlaceByJs(AMap, text),
  center ? searchPlaceNearbyByJs(AMap, text, center, options.nearbyRadius || 5000) : Promise.resolve([])]
  );
  return mergeSearchResults([...tips, ...places, ...nearbyPlaces]);
}

async function geocodeAddressByJs(text: string, config?: AmapConfig): Promise<AmapPlace[]> {
  const AMap = await loadAmap(config);
  await loadPlugin(AMap, ['AMap.Geocoder']);
  return new Promise<AmapPlace[]>((resolve) => {
    const geocoder = new AMap.Geocoder();
    geocoder.getLocation(text, (status, result) => {
      const geocodes = result?.geocodes;
      resolve(
        status === 'complete' && Array.isArray(geocodes) ?
        geocodes.map((item) => normalizeGeocode(item)).filter(isPlace) :
        []
      );
    });
  });
}

async function reverseGeocodeDetailByJs(point: AmapPoint, options: AmapReverseOptions): Promise<AmapPlace> {
  const AMap = await loadAmap(options.config);
  await loadPlugin(AMap, ['AMap.Geocoder']);
  return new Promise<AmapPlace>((resolve, reject) => {
    const geocoder = new AMap.Geocoder({
      radius: clampNumber(options.radius, 100, 10000, 1000),
      extensions: 'all'
    });
    geocoder.getAddress([point.longitude_wsh, point.latitude_wsh], (status, result) => {
      const regeocode = result?.regeocode;
      const address = cleanText(regeocode?.formattedAddress) || buildAddressFromComponent(regeocode?.addressComponent);
      if (status !== 'complete' || !regeocode || !address) {
        reject(new Error('逆地址解析失败，请重新点选或搜索地点'));
        return;
      }
      resolve({
        ...point,
        address_wsh: address,
        display_name_wsh: address,
        location_source_wsh: 'amap_reverse',
        pois_wsh: Array.isArray(regeocode.pois) ?
        regeocode.pois.map((poi) => normalizePoi(poi, 'amap_nearby')).filter(isPlace) :
        []
      });
    });
  });
}

function searchAutocompleteByJs(AMap: AmapNamespace, text: string): Promise<AmapPlace[]> {
  return new Promise<AmapPlace[]>((resolve) => {
    const autocomplete = new AMap.AutoComplete({ city: '全国', citylimit: false });
    autocomplete.search(text, (status, result) => {
      resolve(
        status === 'complete' && Array.isArray(result?.tips) ?
        (result?.tips || []).map((tip) => normalizeTip(tip)).filter(isPlace) :
        []
      );
    });
  });
}

function searchPlaceByJs(AMap: AmapNamespace, text: string): Promise<AmapPlace[]> {
  return new Promise<AmapPlace[]>((resolve) => {
    const placeSearch = new AMap.PlaceSearch({
      city: '全国',
      citylimit: false,
      extensions: 'all',
      pageSize: 10,
      pageIndex: 1
    });
    placeSearch.search(text, (status, result) => {
      resolve(status === 'complete' ? normalizePoiList(result, 'amap_place') : []);
    });
  });
}

function searchPlaceNearbyByJs(
AMap: AmapNamespace,
text: string,
center: AmapPoint,
radius: number)
: Promise<AmapPlace[]> {
  return new Promise<AmapPlace[]>((resolve) => {
    const placeSearch = new AMap.PlaceSearch({ extensions: 'all', pageSize: 10, pageIndex: 1 });
    placeSearch.searchNearBy(
      text,
      [center.longitude_wsh, center.latitude_wsh],
      clampNumber(radius, 100, 50000, 5000),
      (status, result) => {
        resolve(status === 'complete' ? normalizePoiList(result, 'amap_nearby') : []);
      }
    );
  });
}

function normalizePoiList(result: RawPlaceResult | null, source: string): AmapPlace[] {
  const pois = result?.poiList?.pois || result?.pois || [];
  return Array.isArray(pois) ? pois.map((poi) => normalizePoi(poi, source)).filter(isPlace) : [];
}

function loadPlugin(AMap: AmapNamespace, names: string[]): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error('高德地图插件加载超时')), 15000);
    AMap.plugin(names, () => {
      window.clearTimeout(timer);
      resolve();
    });
  });
}

/* ── Normalisation, merging and ranking ── */

function normalizeGeocode(geocode: RawGeocode): AmapPlace | null {
  const point = normalizeLocation(geocode.location);
  if (!point) return null;
  const address =
  cleanText(geocode.formatted_address || geocode.formattedAddress) ||
  buildAddressFromComponent(geocode.addressComponent);
  return {
    ...point,
    address_wsh: address,
    display_name_wsh: address,
    district_wsh: joinAddress([geocode.province, geocode.city, geocode.district]),
    distance_wsh: null,
    location_source_wsh: 'amap_geocode',
    match_score_wsh: 100,
    poi_id_wsh: geocode.adcode || ''
  };
}

function normalizeTip(tip: RawTip): AmapPlace | null {
  const point = normalizeLocation(tip.location);
  if (!point) return null;
  const name = cleanText(tip.name);
  const district = cleanText(tip.district);
  const detail = cleanText(tip.address);
  const address = joinAddress([district, name, detail]);
  return {
    ...point,
    address_wsh: address || name,
    display_name_wsh: name || address,
    district_wsh: district,
    distance_wsh: numberOrNull(tip.distance),
    location_source_wsh: 'amap_tip',
    poi_id_wsh: tip.id || ''
  };
}

function normalizePoi(poi: RawPoi, source: string): AmapPlace | null {
  const point = normalizeLocation(poi.location);
  if (!point) return null;
  const name = cleanText(poi.name);
  const province = cleanText(poi.pname || poi.provinceName);
  const city = cleanText(poi.cityname || poi.cityName);
  const district = cleanText(poi.adname || poi.adName || poi.district);
  const detail = cleanText(poi.address);
  const address = joinAddress([province, city, district, name, detail]);
  return {
    ...point,
    address_wsh: address || name,
    display_name_wsh: name || address,
    district_wsh: joinAddress([province, city, district]),
    distance_wsh: numberOrNull(poi.distance),
    location_source_wsh: source,
    poi_id_wsh: poi.id || ''
  };
}

interface RankedPlace extends AmapPlace {
  _index_wsh: number;
}

function rankSearchResults(items: AmapPlace[], keyword: string): AmapPlace[] {
  const ranked: RankedPlace[] = items.map((item, index) => {
    const matchScore = item.match_score_wsh || calculateMatchScore(keyword, item);
    const distance = numberOrNull(item.distance_wsh);
    return {
      ...item,
      match_score_wsh: matchScore,
      match_label_wsh: buildDistanceLabel(distance),
      _index_wsh: index
    };
  });

  const sorted = [...ranked].sort(compareSearchResult);
  const primary = sorted.find((item) => item.location_source_wsh !== 'amap_nearby') || sorted[0];
  if (!primary) return [];

  const rest = sorted.filter((item) => item !== primary);
  const nearby = rest.
  filter((item) => item.location_source_wsh === 'amap_nearby').
  filter(
    (item) =>
    Number(item.match_score_wsh) >= 45 ||
    numberOrNull(item.distance_wsh) == null ||
    Number(item.distance_wsh) <= 1200
  ).
  sort(compareNearbyResult);
  const others = rest.filter((item) => item.location_source_wsh !== 'amap_nearby').sort(compareSearchResult);

  return [primary, ...nearby, ...others].map(({ _index_wsh, ...item }) => item);
}

function compareSearchResult(a: RankedPlace, b: RankedPlace): number {
  const scoreDiff = Number(b.match_score_wsh || 0) - Number(a.match_score_wsh || 0);
  if (scoreDiff !== 0) return scoreDiff;
  const sourceDiff = sourcePriority(a.location_source_wsh) - sourcePriority(b.location_source_wsh);
  if (sourceDiff !== 0) return sourceDiff;
  return compareDistance(a, b) || Number(a._index_wsh || 0) - Number(b._index_wsh || 0);
}

function compareNearbyResult(a: RankedPlace, b: RankedPlace): number {
  const distanceDiff = compareDistance(a, b);
  if (distanceDiff !== 0) return distanceDiff;
  return Number(b.match_score_wsh || 0) - Number(a.match_score_wsh || 0);
}

function compareDistance(a: AmapPlace, b: AmapPlace): number {
  const first = numberOrNull(a.distance_wsh);
  const second = numberOrNull(b.distance_wsh);
  if (first == null && second == null) return 0;
  if (first == null) return 1;
  if (second == null) return -1;
  return first - second;
}

function mergeSearchResults(items: AmapPlace[]): AmapPlace[] {
  const seen = new Set<string>();
  const merged: AmapPlace[] = [];
  for (const item of items) {
    if (!item?.address_wsh || item.latitude_wsh == null || item.longitude_wsh == null) continue;
    const key =
    item.poi_id_wsh ||
    `${Math.round(Number(item.longitude_wsh) * 100000)}:${Math.round(Number(item.latitude_wsh) * 100000)}:${
    item.display_name_wsh || item.address_wsh}`;

    if (seen.has(key)) continue;
    seen.add(key);
    merged.push(item);
  }
  return merged;
}

function calculateMatchScore(keyword: string, item: AmapPlace): number {
  const tokens = tokenizeKeyword(keyword);
  const target = normalizeSearchText(`${item.display_name_wsh || ''}${item.address_wsh || ''}`);
  if (!tokens.length || !target) return 60;
  if (tokens.some((token) => target.includes(token))) return item.location_source_wsh === 'amap_nearby' ? 92 : 100;

  const bestRatio = Math.max(...tokens.map((token) => overlapRatio(token, target)));
  const distance = numberOrNull(item.distance_wsh);
  const nearbyBonus = distance == null ? 0 : Math.max(0, 12 - Math.floor(distance / 400));
  return Math.max(35, Math.min(99, Math.round(bestRatio * 82 + nearbyBonus)));
}

function buildDistanceLabel(distance: number | null): string {
  if (distance == null) return '';
  return `距离${formatDistance(distance)}`;
}

function sourcePriority(source?: string): number {
  const priorities: Record<string, number> = {
    amap_geocode: 0,
    amap_tip: 1,
    amap_place: 2,
    amap_nearby: 3
  };
  return source && source in priorities ? priorities[source] : 4;
}

function tokenizeKeyword(keyword: string): string[] {
  return String(keyword || '').
  split(/[\s,，、;；]+/).
  map(normalizeSearchText).
  filter((token) => token.length >= 2);
}

function overlapRatio(token: string, target: string): number {
  const chars = Array.from(new Set(token));
  if (!chars.length) return 0;
  const matched = chars.filter((char) => target.includes(char)).length;
  return matched / chars.length;
}

function normalizeSearchText(value: string): string {
  return String(value || '').
  replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, '').
  toLowerCase();
}

function buildAddressFromComponent(component: RawAddressComponent = {}): string {
  return joinAddress([
  component.province,
  component.city,
  component.district,
  component.township,
  component.streetNumber?.street,
  component.streetNumber?.number,
  component.neighborhood?.name,
  component.building?.name]
  );
}

function joinAddress(parts: RawText[]): string {
  const values: string[] = [];
  for (const part of parts) {
    const text = cleanText(part);
    if (!text || values.some((value) => value === text || value.includes(text))) continue;
    values.push(text);
  }
  return values.join(' ');
}

export function cleanText(value: RawText): string {
  if (Array.isArray(value)) return value.map(cleanText).filter(Boolean).join(' ');
  return typeof value === 'string' ? value.trim() : '';
}

function normalizeLocation(value: RawLocation): AmapPoint | null {
  if (!value) return null;
  if (typeof value === 'string') {
    const [lng, lat] = value.split(',').map((item) => Number(item));
    return normalizeNumbers(lat, lng);
  }
  return normalizeLngLat(value);
}

export function normalizeLngLat(value?: AmapLngLat | null): AmapPoint | null {
  if (!value) return null;
  const lat = typeof value.getLat === 'function' ? value.getLat() : value.lat;
  const lng = typeof value.getLng === 'function' ? value.getLng() : value.lng;
  return normalizeNumbers(lat, lng);
}

export function normalizePoint(value?: AmapPoint | null): AmapPoint | null {
  if (!value) return null;
  return normalizeNumbers(value.latitude_wsh, value.longitude_wsh);
}

export function normalizeNumbers(
latitude: number | string | null | undefined,
longitude: number | string | null | undefined)
: AmapPoint | null {
  const lat = Number(latitude);
  const lng = Number(longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  return { latitude_wsh: lat, longitude_wsh: lng };
}

export function toAmapPosition(point: AmapPoint): [number, number] {
  return [Number(point.longitude_wsh), Number(point.latitude_wsh)];
}

function numberOrNull(value: number | string | null | undefined): number | null {
  if (value == null || value === '') return null;
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

function clampNumber(value: number | undefined, min: number, max: number, fallback: number): number {
  const num = Number(value);
  if (!Number.isFinite(num)) return fallback;
  return Math.max(min, Math.min(max, num));
}

function formatDistance(value: number): string {
  const distance = Number(value);
  if (!Number.isFinite(distance)) return '';
  return distance >= 1000 ? `${(distance / 1000).toFixed(1)}公里` : `${Math.round(distance)}米`;
}

function isPlace(item: AmapPlace | null): item is AmapPlace {
  return Boolean(item);
}

export function resolveErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}