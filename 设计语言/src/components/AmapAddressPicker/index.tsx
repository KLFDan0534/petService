import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { createPortal } from 'react-dom';
import { LocateFixedIcon, MapPinnedIcon, SearchIcon, XIcon } from 'lucide-react';
import {
  getCurrentAddress as amapGetCurrentAddress,
  loadAmap as amapLoad,
  normalizeLngLat,
  normalizeNumbers,
  resolveErrorMessage,
  reverseGeocodeDetail as amapReverseGeocodeDetail,
  searchAmapAddress,
  toAmapPosition } from
'./amapLocation';
import type {
  AmapConfig,
  AmapLngLat,
  AmapMapLike,
  AmapMarkerLike,
  AmapNamespace,
  AmapPlace,
  AmapPoint,
  AmapReverseOptions,
  AmapSearchOptions } from
'./amapLocation';

export type { AmapConfig, AmapPlace, AmapPoint } from './amapLocation';

const DEFAULT_CENTER: AmapPoint = { longitude_wsh: 121.4737, latitude_wsh: 31.2304 };
const SEARCH_DEBOUNCE_MS = 500;

export interface AmapAddressSelection {
  address: string;
  latitude: number | null;
  longitude: number | null;
  source: string;
}

export interface AmapAddressPickerProps {
  /** Address text. Omit to run uncontrolled. */
  value?: string;
  latitude?: number | string | null;
  longitude?: number | string | null;
  /** How the current coordinate was picked: `merchant` | `amap_geolocation` | `amap_map` | `amap_tip` … */
  source?: string;
  placeholder?: string;
  showLocateButton?: boolean;
  locateButtonText?: string;
  showMapButton?: boolean;
  mapButtonText?: string;
  disabled?: boolean;
  /** Amap web key + optional JS security code. Required for live search / map. */
  amapConfig?: AmapConfig;
  /** Override the search call (tests, mocks, a server-side proxy). */
  searchAddress?: (keyword: string, options: AmapSearchOptions) => Promise<AmapPlace[]>;
  /** Override browser geolocation + reverse geocoding. */
  getCurrentAddress?: () => Promise<AmapPlace>;
  /** Override reverse geocoding used by map clicks / marker drags. */
  reverseGeocodeDetail?: (
  longitude: number,
  latitude: number,
  options: AmapReverseOptions)
  => Promise<AmapPlace>;
  /** Override the Amap JS SDK loader. */
  loadAmap?: () => Promise<AmapNamespace>;
  onChange?: (selection: AmapAddressSelection) => void;
  onSelected?: (place: AmapPlace) => void;
  /** Toast hook — mirrors the product's `appStore.addToast`. */
  onNotify?: (message: string, level: 'error' | 'warning') => void;
  className?: string;
}

const inputClass =
'h-[42px] w-full min-w-0 rounded-control border border-line bg-surface px-[14px] text-[14px] text-ink placeholder:text-muted transition-[border-color,box-shadow] duration-fast ease-editorial focus:border-muted focus:shadow-focus focus:outline-none disabled:cursor-not-allowed disabled:bg-sand disabled:text-muted';

const outlineButtonClass =
'inline-flex h-10 items-center justify-center gap-1.5 whitespace-nowrap rounded-control border border-line bg-surface px-3 text-[12.5px] font-medium text-ink-soft transition-colors duration-fast ease-editorial hover:border-muted hover:text-ink active:translate-y-px disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:border-line disabled:hover:text-ink-soft';

const primaryButtonClass =
'inline-flex h-10 items-center justify-center gap-1.5 whitespace-nowrap rounded-control border border-transparent bg-brand px-3 text-[12.5px] font-medium text-white transition-colors duration-fast ease-editorial hover:bg-brand-deep active:translate-y-px disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:bg-brand';

const resultTitleClass = 'text-[13px] font-semibold leading-[1.35] text-ink';
const resultAddressClass = 'text-[12px] leading-[1.35] text-muted';
const resultMetaClass = 'w-fit rounded-full bg-cream px-[7px] py-[2px] text-[11px] leading-[1.3] text-brand';

export function AmapAddressPicker({
  value,
  latitude,
  longitude,
  source,
  placeholder = '搜索地址或打开地图选址',
  showLocateButton = true,
  locateButtonText = '定位',
  showMapButton = true,
  mapButtonText = '地图选址',
  disabled = false,
  amapConfig,
  searchAddress,
  getCurrentAddress,
  reverseGeocodeDetail,
  loadAmap,
  onChange,
  onSelected,
  onNotify,
  className = ''
}: AmapAddressPickerProps) {
  const isControlled = value !== undefined;
  const [internal, setInternal] = useState<AmapAddressSelection>({
    address: '',
    latitude: null,
    longitude: null,
    source: ''
  });

  const address = isControlled ? value ?? '' : internal.address;
  const currentLatitude = isControlled ? latitude ?? null : internal.latitude;
  const currentLongitude = isControlled ? longitude ?? null : internal.longitude;
  const currentSource = isControlled ? source ?? '' : internal.source;

  const [tips, setTips] = useState<AmapPlace[]>([]);
  const [showTips, setShowTips] = useState(false);
  const [locating, setLocating] = useState(false);

  const [mapDialogVisible, setMapDialogVisible] = useState(false);
  const [mapLoading, setMapLoading] = useState(false);
  const [mapSearching, setMapSearching] = useState(false);
  const [mapSearched, setMapSearched] = useState(false);
  const [mapKeyword, setMapKeyword] = useState('');
  const [mapResults, setMapResults] = useState<AmapPlace[]>([]);
  const [pendingAddress, setPendingAddress] = useState('');
  const [mapAddressError, setMapAddressError] = useState('');
  const [mapLoadError, setMapLoadError] = useState('');
  const [resolvingAddress, setResolvingAddress] = useState(false);
  const [hasPendingPoint, setHasPendingPoint] = useState(false);

  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<AmapMapLike | null>(null);
  const markerInstanceRef = useRef<AmapMarkerLike | null>(null);
  const pendingPointRef = useRef<AmapPoint | null>(null);
  const pendingSourceRef = useRef('');
  const searchTimerRef = useRef<number | undefined>(undefined);
  const hideTipsTimerRef = useRef<number | undefined>(undefined);
  const mapSearchTimerRef = useRef<number | undefined>(undefined);
  const reverseSequenceRef = useRef(0);
  const sessionRef = useRef(0);
  const suppressedKeywordRef = useRef<string | null>(null);

  const hasActionButtons = showLocateButton || showMapButton;

  const selectedText = useMemo(() => {
    if (!address || !currentSource) return '';
    if (currentSource === 'merchant') return '已选择商家位置';
    if (currentSource === 'amap_geolocation') return '已通过定位选择位置';
    if (currentSource === 'amap_map') return '已通过地图选择位置';
    if (currentSource === 'amap_tip') return '已从搜索结果选择位置';
    return '已确认地址位置';
  }, [address, currentSource]);

  const canConfirmMap = hasPendingPoint && Boolean(pendingAddress) && !resolvingAddress;

  /* ── shared calls (props override the built-in Amap helpers) ── */

  const runSearch = (keyword: string, options: AmapSearchOptions) =>
  searchAddress ?
  searchAddress(keyword, options) :
  searchAmapAddress(keyword, { ...options, config: amapConfig });

  const runGetCurrentAddress = () =>
  getCurrentAddress ? getCurrentAddress() : amapGetCurrentAddress(amapConfig);

  const runReverseGeocode = (lng: number, lat: number, options: AmapReverseOptions) =>
  reverseGeocodeDetail ?
  reverseGeocodeDetail(lng, lat, options) :
  amapReverseGeocodeDetail(lng, lat, { ...options, config: amapConfig });

  const runLoadAmap = () => loadAmap ? loadAmap() : amapLoad(amapConfig);

  function notify(message: string, level: 'error' | 'warning') {
    onNotify?.(message, level);
  }

  function commit(selection: AmapAddressSelection) {
    if (!isControlled) setInternal(selection);
    onChange?.(selection);
  }

  /* ── inline search + tips ── */

  async function search(keyword: string) {
    const text = String(keyword || '').trim();
    window.clearTimeout(hideTipsTimerRef.current);
    setShowTips(true);
    if (text.length < 2) {
      setTips([]);
      return;
    }
    try {
      setTips(
        await runSearch(text, {
          center: normalizeNumbers(currentLatitude, currentLongitude),
          withNearby: true,
          limit: 8
        })
      );
    } catch (error) {
      setTips([]);
    }
  }

  const searchRef = useRef(search);
  searchRef.current = search;

  function onInput(event: ChangeEvent<HTMLInputElement>) {
    const nextAddress = event.target.value;
    commit({ address: nextAddress, latitude: null, longitude: null, source: '' });
    window.clearTimeout(searchTimerRef.current);
    searchTimerRef.current = window.setTimeout(() => {
      void searchRef.current(nextAddress);
    }, SEARCH_DEBOUNCE_MS);
  }

  function hideTipsSoon() {
    window.clearTimeout(hideTipsTimerRef.current);
    hideTipsTimerRef.current = window.setTimeout(() => setShowTips(false), 160);
  }

  function selectTip(tip: AmapPlace) {
    commit({
      address: tip.address_wsh,
      latitude: tip.latitude_wsh,
      longitude: tip.longitude_wsh,
      source: tip.location_source_wsh || 'amap'
    });
    onSelected?.(tip);
    setTips([]);
    setShowTips(false);
  }

  async function useCurrentLocation() {
    setLocating(true);
    try {
      selectTip(await runGetCurrentAddress());
    } catch (error) {
      notify(resolveErrorMessage(error, '定位失败，请检查浏览器权限'), 'error');
    } finally {
      setLocating(false);
    }
  }

  /* ── map dialog ── */

  function openMapDialog() {
    setMapDialogVisible(true);
    setMapLoading(true);
    setShowTips(false);
    setMapKeyword(address || '');
    suppressedKeywordRef.current = address || '';
    setMapResults([]);
    setMapSearched(false);
    setPendingAddress('');
    setMapAddressError('');
    setMapLoadError('');
    setResolvingAddress(false);
    setHasPendingPoint(false);
    pendingPointRef.current = null;
    pendingSourceRef.current = '';
  }

  function closeMapDialog() {
    sessionRef.current += 1;
    reverseSequenceRef.current += 1;
    window.clearTimeout(mapSearchTimerRef.current);
    setMapDialogVisible(false);
    setMapLoading(false);
    setMapSearching(false);
    setResolvingAddress(false);
    destroyMap();
  }

  function destroyMap() {
    markerInstanceRef.current = null;
    if (mapInstanceRef.current) {
      mapInstanceRef.current.destroy();
      mapInstanceRef.current = null;
    }
  }

  function setPendingSelection(point: AmapPoint, nextAddress: string, nextSource: string, isResolving: boolean) {
    pendingPointRef.current = point;
    pendingSourceRef.current = nextSource || 'amap';
    setHasPendingPoint(true);
    setPendingAddress(nextAddress || '');
    setMapAddressError('');
    setResolvingAddress(isResolving);
  }

  async function resolveInitialSelection(): Promise<{point: AmapPoint | null;address: string;source: string;}> {
    const point = normalizeNumbers(currentLatitude, currentLongitude);
    if (point) {
      return { point, address: address || '', source: currentSource || 'amap' };
    }

    const keyword = String(address || '').trim();
    if (keyword.length >= 2) {
      try {
        const [first] = await runSearch(keyword, { withNearby: false, limit: 1 });
        const firstPoint = first ? normalizeNumbers(first.latitude_wsh, first.longitude_wsh) : null;
        if (firstPoint) {
          return {
            point: firstPoint,
            address: first.address_wsh || keyword,
            source: first.location_source_wsh || 'amap_tip'
          };
        }
      } catch (error) {
        return { point: null, address: '', source: '' };
      }
    }

    return { point: null, address: '', source: '' };
  }

  function createOrMoveMarker(point: AmapPoint) {
    const AMap = window.AMap;
    if (!AMap || !mapInstanceRef.current) return;
    const position = toAmapPosition(point);
    if (!markerInstanceRef.current) {
      const marker = new AMap.Marker({
        position,
        draggable: true,
        cursor: 'move',
        anchor: 'bottom-center'
      });
      marker.on('dragend', (event) => {
        void updateSelectionRef.current(event.lnglat || marker.getPosition(), 'amap_map');
      });
      markerInstanceRef.current = marker;
      mapInstanceRef.current.add(marker);
    } else {
      markerInstanceRef.current.setPosition(position);
    }
  }

  function initializeMap(AMap: AmapNamespace, centerPoint: AmapPoint, shouldShowMarker: boolean) {
    destroyMap();
    if (!mapContainerRef.current) return;

    const map = new AMap.Map(mapContainerRef.current, {
      center: toAmapPosition(centerPoint),
      zoom: shouldShowMarker ? 16 : 11,
      resizeEnable: true,
      viewMode: '2D'
    });
    map.on('click', (event) => {
      void updateSelectionRef.current(event.lnglat, 'amap_map');
    });
    mapInstanceRef.current = map;

    if (shouldShowMarker) {
      createOrMoveMarker(centerPoint);
    }
  }

  async function updateSelectionFromLngLat(lnglat: AmapLngLat | undefined, nextSource: string) {
    const point = normalizeLngLat(lnglat);
    if (!point) return;

    createOrMoveMarker(point);
    mapInstanceRef.current?.setCenter(toAmapPosition(point));
    setPendingSelection(point, '正在识别地址...', nextSource, true);

    const sequence = ++reverseSequenceRef.current;
    try {
      const detail = await runReverseGeocode(point.longitude_wsh, point.latitude_wsh, { radius: 1200 });
      if (sequence === reverseSequenceRef.current) {
        setPendingAddress(detail.address_wsh);
        syncMapKeywordAndSearch(detail.address_wsh);
        setMapAddressError('');
      }
    } catch (error) {
      if (sequence === reverseSequenceRef.current) {
        setPendingAddress('');
        setMapAddressError(resolveErrorMessage(error, '未识别到详细地址，请重新点选或搜索地点'));
      }
    } finally {
      if (sequence === reverseSequenceRef.current) {
        setResolvingAddress(false);
      }
    }
  }

  const updateSelectionRef = useRef(updateSelectionFromLngLat);
  updateSelectionRef.current = updateSelectionFromLngLat;

  async function searchInMap(options: {silent?: boolean;selectFirst?: boolean;} = {}) {
    const keyword = String(mapKeyword || '').trim();
    if (keyword.length < 2) {
      if (!options.silent) notify('请输入至少 2 个字搜索地点', 'warning');
      return;
    }

    setMapSearching(true);
    setMapSearched(true);
    try {
      const results = await runSearch(keyword, {
        center: pendingPointRef.current || getMapCenterPoint(),
        withNearby: true,
        nearbyRadius: 5000,
        limit: 12
      });
      setMapResults(results);
      if (results.length && options.selectFirst !== false) {
        selectMapResult(results[0]);
      }
    } catch (error) {
      setMapResults([]);
      notify(resolveErrorMessage(error, '地点搜索失败'), 'error');
    } finally {
      setMapSearching(false);
    }
  }

  const searchInMapRef = useRef(searchInMap);
  searchInMapRef.current = searchInMap;

  function selectMapResult(result: AmapPlace) {
    const point = normalizeNumbers(result.latitude_wsh, result.longitude_wsh);
    if (!point) return;

    createOrMoveMarker(point);
    const map = mapInstanceRef.current;
    if (map) {
      if (typeof map.setZoomAndCenter === 'function') {
        map.setZoomAndCenter(16, toAmapPosition(point));
      } else {
        map.setZoom(16);
        map.setCenter(toAmapPosition(point));
      }
    }
    setPendingSelection(point, result.address_wsh, result.location_source_wsh || 'amap_tip', false);
  }

  function confirmMapSelection() {
    const point = pendingPointRef.current;
    if (!point || !pendingAddress) return;
    selectTip({
      ...point,
      address_wsh: pendingAddress,
      location_source_wsh: pendingSourceRef.current || 'amap_map'
    });
    closeMapDialog();
  }

  function getMapCenterPoint(): AmapPoint | null {
    const map = mapInstanceRef.current;
    if (!map || typeof map.getCenter !== 'function') return null;
    return normalizeLngLat(map.getCenter());
  }

  function syncMapKeywordAndSearch(keyword: string) {
    const nextKeyword = String(keyword || '').trim();
    if (mapKeyword !== nextKeyword) {
      suppressedKeywordRef.current = nextKeyword;
      setMapKeyword(nextKeyword);
    }
    queueMapSearch(nextKeyword);
  }

  function queueMapSearch(keyword: string) {
    if (!mapDialogVisible) return;
    window.clearTimeout(mapSearchTimerRef.current);
    const text = String(keyword || '').trim();
    if (text.length < 2) {
      setMapResults([]);
      setMapSearched(false);
      return;
    }
    mapSearchTimerRef.current = window.setTimeout(() => {
      void searchInMapRef.current({ silent: true, selectFirst: false });
    }, SEARCH_DEBOUNCE_MS);
  }

  const queueMapSearchRef = useRef(queueMapSearch);
  queueMapSearchRef.current = queueMapSearch;

  /* ── effects ── */

  const initMapRef = useRef<() => Promise<void>>(async () => {});
  initMapRef.current = async () => {
    const session = sessionRef.current;
    try {
      const AMap = await runLoadAmap();
      const initial = await resolveInitialSelection();
      if (session !== sessionRef.current) return;
      initializeMap(AMap, initial.point || DEFAULT_CENTER, Boolean(initial.point));
      if (initial.point) {
        setPendingSelection(
          initial.point,
          initial.address || address,
          initial.source || currentSource || 'amap',
          false
        );
      }
    } catch (error) {
      const message = resolveErrorMessage(error, '地图加载失败，请稍后重试');
      notify(message, 'error');
      if (session === sessionRef.current) setMapLoadError(message);
    } finally {
      if (session === sessionRef.current) setMapLoading(false);
    }
  };

  useEffect(() => {
    if (!mapDialogVisible) return;
    void initMapRef.current();
    return () => {
      destroyMap();
    };
  }, [mapDialogVisible]);

  useEffect(() => {
    if (!mapDialogVisible) return;
    if (suppressedKeywordRef.current === mapKeyword) {
      suppressedKeywordRef.current = null;
      return;
    }
    queueMapSearchRef.current(mapKeyword);
  }, [mapKeyword, mapDialogVisible]);

  useEffect(() => {
    if (!mapDialogVisible) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeMapDialog();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [mapDialogVisible]);

  useEffect(
    () => () => {
      window.clearTimeout(searchTimerRef.current);
      window.clearTimeout(hideTipsTimerRef.current);
      window.clearTimeout(mapSearchTimerRef.current);
      destroyMap();
    },
    []
  );

  /* ── render ── */

  const mapDialog =
  <div
    className="fixed inset-0 z-[1200] grid animate-fade place-items-center p-2.5 md:p-5"
    style={{ background: 'color-mix(in srgb, var(--ref-ink) 55%, transparent)' }}
    onMouseDown={(event) => {
      if (event.target === event.currentTarget) closeMapDialog();
    }}>
    
      <div
      className="grid w-[min(960px,100%)] max-h-[calc(100vh-20px)] animate-pop grid-rows-[auto_auto_auto_auto] gap-3 overflow-hidden rounded-frame border border-line bg-surface p-3 shadow-lift md:max-h-[min(760px,calc(100vh-40px))] md:grid-rows-[auto_auto_minmax(320px,1fr)_auto] md:p-4"
      role="dialog"
      aria-modal="true"
      aria-label="地图选址">
      
        <header className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="m-0 font-display text-display-xs text-ink">地图选址</h3>
            <p className="m-0 mt-1 break-words text-meta text-muted">
              {pendingAddress || '搜索地点或在地图上定点'}
            </p>
          </div>
          <button
          type="button"
          aria-label="关闭地图选址"
          onClick={closeMapDialog}
          className="inline-grid h-8 w-8 flex-none place-items-center rounded-tile border border-line bg-surface text-ink transition-colors duration-fast ease-editorial hover:bg-sand">
          
            <XIcon className="h-4 w-4" aria-hidden="true" />
          </button>
        </header>

        <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-2">
          <input
          className={inputClass}
          value={mapKeyword}
          placeholder="搜索地点"
          autoComplete="off"
          aria-label="搜索地点"
          onChange={(event) => setMapKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              void searchInMap();
            }
          }} />
        
          <button
          type="button"
          className={primaryButtonClass}
          disabled={mapSearching}
          onClick={() => void searchInMap()}>
          
            <SearchIcon className="h-4 w-4" aria-hidden="true" />
            {mapSearching ? '搜索中' : '搜索'}
          </button>
        </div>

        <div className="grid min-h-[320px] grid-cols-1 gap-3 md:grid-cols-[minmax(180px,260px)_minmax(0,1fr)]">
          <aside
          aria-label="地点搜索结果"
          className="order-2 grid max-h-32 min-h-0 content-start gap-1.5 overflow-auto rounded-tile border border-line bg-canvas p-2 md:order-none md:max-h-none">
          
            {mapResults.map((result) =>
          <button
            key={`${result.poi_id_wsh || result.address_wsh}-${result.latitude_wsh}-${result.longitude_wsh}`}
            type="button"
            onClick={() => selectMapResult(result)}
            className="grid w-full gap-[3px] break-words rounded-tile px-2.5 py-[9px] text-left transition-colors duration-fast ease-editorial hover:bg-cream focus-visible:bg-cream">
            
                <span className={resultTitleClass}>{result.display_name_wsh || result.address_wsh}</span>
                <span className={resultAddressClass}>{result.address_wsh}</span>
                {result.match_label_wsh ?
            <span className={resultMetaClass}>{result.match_label_wsh}</span> :
            null}
              </button>
          )}
            {mapSearched && !mapSearching && !mapResults.length ?
          <div className="p-2.5 text-meta text-muted">未找到匹配地点</div> :
          null}
          </aside>
          <div
          ref={mapContainerRef}
          className="relative min-h-[300px] overflow-hidden rounded-tile border border-line bg-sand md:min-h-[320px]">
          
            {mapLoading ?
          <div
            className="absolute inset-0 z-[2] grid place-items-center text-meta text-muted"
            style={{ background: 'color-mix(in srgb, var(--ref-surface) 78%, transparent)' }}>
            
                地图加载中...
              </div> :
          null}
            {!mapLoading && mapLoadError ?
          <div className="absolute inset-0 z-[2] grid place-items-center bg-sand p-6">
                <p className="max-w-prose text-center text-meta text-ink-soft">{mapLoadError}</p>
              </div> :
          null}
          </div>
        </div>

        <footer className="flex flex-col items-stretch justify-between gap-3 md:flex-row md:items-center">
          <div className="grid min-w-0 gap-0.5">
            <span className="text-[12px] text-muted">当前选中</span>
            <strong className="break-words text-[14px] font-medium text-ink">
              {pendingAddress || mapAddressError || '暂未选择位置'}
            </strong>
          </div>
          <div className="flex flex-none justify-end gap-2">
            <button type="button" className={outlineButtonClass} onClick={closeMapDialog}>
              取消
            </button>
            <button
            type="button"
            className={primaryButtonClass}
            disabled={!canConfirmMap}
            onClick={confirmMapSelection}>
            
              确认选择
            </button>
          </div>
        </footer>
      </div>
    </div>;


  return (
    <div className={`relative grid gap-1.5 ${className}`.trim()}>
      <div className={hasActionButtons ? 'grid gap-2 md:grid-cols-[minmax(0,1fr)_auto]' : 'grid gap-2'}>
        <input
          className={inputClass}
          value={address}
          placeholder={placeholder}
          autoComplete="off"
          disabled={disabled}
          onChange={onInput}
          onFocus={() => void search(address)}
          onBlur={hideTipsSoon} />
        
        {hasActionButtons ?
        <div className="flex flex-wrap items-stretch justify-start gap-2">
            {showMapButton ?
          <button
            type="button"
            className={outlineButtonClass}
            disabled={disabled || mapLoading}
            onClick={openMapDialog}>
            
                <MapPinnedIcon className="h-4 w-4" aria-hidden="true" />
                {mapLoading ? '加载地图' : mapButtonText}
              </button> :
          null}
            {showLocateButton ?
          <button
            type="button"
            className={outlineButtonClass}
            disabled={disabled || locating}
            onClick={() => void useCurrentLocation()}>
            
                <LocateFixedIcon className="h-4 w-4" aria-hidden="true" />
                {locating ? '定位中' : locateButtonText}
              </button> :
          null}
          </div> :
        null}
      </div>

      {showTips && tips.length ?
      <div
        className={`absolute left-0 right-0 z-30 grid max-h-[220px] animate-fade overflow-auto rounded-card border border-line bg-surface shadow-lift ${
        hasActionButtons ? 'top-[92px] md:top-[46px]' : 'top-[46px]'}`
        }>
        
          {tips.map((tip) =>
        <button
          key={`${tip.poi_id_wsh || tip.address_wsh}-${tip.latitude_wsh}-${tip.longitude_wsh}`}
          type="button"
          onMouseDown={(event) => {
            event.preventDefault();
            selectTip(tip);
          }}
          className="grid w-full gap-[3px] break-words px-3 py-2.5 text-left transition-colors duration-fast ease-editorial hover:bg-cream focus-visible:bg-cream">
          
              <span className={resultTitleClass}>{tip.display_name_wsh || tip.address_wsh}</span>
              <span className={resultAddressClass}>{tip.address_wsh}</span>
              {tip.match_label_wsh ? <span className={resultMetaClass}>{tip.match_label_wsh}</span> : null}
            </button>
        )}
        </div> :
      null}

      {selectedText ? <p className="m-0 text-[12px] text-muted">{selectedText}</p> : null}

      {mapDialogVisible && typeof document !== 'undefined' ? createPortal(mapDialog, document.body) : null}
    </div>);

}