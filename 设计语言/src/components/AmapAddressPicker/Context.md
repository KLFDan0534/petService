# AmapAddressPicker

Amap (高德地图) address field: debounced keyword search with a suggestion panel, one-tap browser geolocation, and a full map-picking dialog with click / marker-drag reverse geocoding.

Ported from the product's `src/components/common/AmapAddressPicker.vue` (structure, behaviour, copy and result shapes kept 1:1). Its global `.form-control` / `.btn` / `--color-*` styling doesn't exist in this artifact, so that chrome is expressed with Editorial Warm tokens (`bg-surface`, `border-line`, `rounded-control`, `text-brand`, `shadow-lift`) matching the warm override the product applies in `views/user/Addresses.vue`.

## Usage

```tsx
import { AmapAddressPicker } from 'components/AmapAddressPicker'
import type { AmapAddressSelection } from 'components/AmapAddressPicker'

const [selection, setSelection] = useState<AmapAddressSelection>({
  address: '', latitude: null, longitude: null, source: '',
})

<AmapAddressPicker
  value={selection.address}
  latitude={selection.latitude}
  longitude={selection.longitude}
  source={selection.source}
  onChange={setSelection}
  amapConfig={{ apiKey: GAO_MAP_API_KEY, securityCode: GAO_MAP_SECURITY_CODE }}
  onNotify={(message, level) => toast(message, level)}
/>
```

Omit `value` to run the component uncontrolled (it keeps the selection internally and still calls `onChange`).

## Props

| Prop | Type | Default | Notes |
| --- | --- | --- | --- |
| `value` | `string` | – | Address text. Omit for uncontrolled mode. |
| `latitude` / `longitude` | `number \| string \| null` | `null` | Current coordinate; cleared whenever the user types. |
| `source` | `string` | `''` | `merchant` \| `amap_geolocation` \| `amap_map` \| `amap_tip` … drives the confirmation caption. |
| `placeholder` | `string` | `'搜索地址或打开地图选址'` | |
| `showMapButton` / `mapButtonText` | `boolean` / `string` | `true` / `'地图选址'` | Opens the map dialog. |
| `showLocateButton` / `locateButtonText` | `boolean` / `string` | `true` / `'定位'` | Browser geolocation. |
| `disabled` | `boolean` | `false` | Locks the field and both actions. |
| `amapConfig` | `{ apiKey: string; securityCode?: string }` | – | Required for live search / map. In the product this comes from `/api/geo/config`. |
| `searchAddress` | `(keyword, options) => Promise<AmapPlace[]>` | built-in | Override for mocks or a server proxy. |
| `getCurrentAddress` | `() => Promise<AmapPlace>` | built-in | Override geolocation. |
| `reverseGeocodeDetail` | `(lng, lat, options) => Promise<AmapPlace>` | built-in | Used by map click / marker drag. |
| `loadAmap` | `() => Promise<AmapNamespace>` | built-in | Override the JS SDK loader. |
| `onChange` | `(selection: AmapAddressSelection) => void` | – | Replaces the Vue `update:modelValue / latitude / longitude / source` emits. |
| `onSelected` | `(place: AmapPlace) => void` | – | Fires with the full Amap record on every confirmed pick. |
| `onNotify` | `(message: string, level: 'error' \| 'warning') => void` | – | Replaces `appStore.addToast`. |
| `className` | `string` | `''` | Extra classes on the wrapper. |

## Behaviour

- Typing clears the coordinate and re-searches after 500ms; suggestions need at least 2 characters.
- Suggestions show name, full address, and a distance chip (`match_label_wsh`); picking one emits address + coordinate + `location_source_wsh`.
- Map dialog: keyword search (debounced, Enter to search immediately), result list, click or drag the marker to reverse geocode, and 确认选择 is enabled only once an address is resolved. Escape or the scrim closes it.
- States covered: loading (`地图加载中...`, `搜索中`, `定位中`), empty (`未找到匹配地点`), error (map load failure inside the map area plus `onNotify`), and reverse-geocode failure (`当前选中` falls back to the error text).
- Below `md` the field stacks above its buttons, the result list collapses above the map, and the footer becomes a column.

## Helpers

`./amapLocation` exports the ported Amap layer — `loadAmap`, `searchAmapAddress`, `reverseGeocodeDetail`, `getCurrentAddress` — plus the `AmapPlace` / `AmapPoint` / `AmapConfig` types. REST calls use `fetch` (no axios) and take the key from `amapConfig` instead of the product's `/api/geo/config` endpoint.
