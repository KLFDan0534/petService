import { useState } from 'react';
import { AmapAddressPicker } from './index';
import type { AmapAddressSelection, AmapPlace } from './index';
import type { ComponentPreviewModule } from '../previewTypes';

const MOCK_PLACES: AmapPlace[] = [
{
  latitude_wsh: 31.2304,
  longitude_wsh: 121.4737,
  address_wsh: '上海市 黄浦区 南京东路 300 号 宠悦宠物生活馆',
  display_name_wsh: '宠悦宠物生活馆（南京东路店）',
  district_wsh: '上海市 黄浦区',
  distance_wsh: 320,
  match_label_wsh: '距离320米',
  location_source_wsh: 'amap_tip',
  poi_id_wsh: 'poi-1'
},
{
  latitude_wsh: 31.2249,
  longitude_wsh: 121.4642,
  address_wsh: '上海市 黄浦区 人民大道 200 号',
  display_name_wsh: '人民广场',
  district_wsh: '上海市 黄浦区',
  distance_wsh: 1400,
  match_label_wsh: '距离1.4公里',
  location_source_wsh: 'amap_place',
  poi_id_wsh: 'poi-2'
},
{
  latitude_wsh: 31.2397,
  longitude_wsh: 121.4998,
  address_wsh: '上海市 浦东新区 陆家嘴环路 1000 号 毛孩子上门喂养服务点',
  display_name_wsh: '毛孩子上门喂养服务点',
  district_wsh: '上海市 浦东新区',
  distance_wsh: 4200,
  match_label_wsh: '距离4.2公里',
  location_source_wsh: 'amap_nearby',
  poi_id_wsh: 'poi-3'
}];


const mockSearch = async (keyword: string) => {
  await new Promise((resolve) => window.setTimeout(resolve, 220));
  const text = keyword.trim();
  if (!text) return [];
  return MOCK_PLACES;
};

const mockCurrentAddress = async (): Promise<AmapPlace> => {
  await new Promise((resolve) => window.setTimeout(resolve, 400));
  return {
    latitude_wsh: 31.2304,
    longitude_wsh: 121.4737,
    address_wsh: '上海市 黄浦区 南京东路 300 号',
    display_name_wsh: '当前位置',
    accuracy_wsh: 42,
    location_source_wsh: 'amap_geolocation'
  };
};

function PickerHarness({
  initial,
  ...rest






}: {initial?: AmapAddressSelection;showLocateButton?: boolean;showMapButton?: boolean;disabled?: boolean;placeholder?: string;}) {
  const [selection, setSelection] = useState<AmapAddressSelection>(
    initial ?? { address: '', latitude: null, longitude: null, source: '' }
  );

  return (
    <div className="w-full max-w-xl bg-canvas p-6">
      <p className="mb-2 text-eyebrow uppercase text-muted">服务地址</p>
      <AmapAddressPicker
        value={selection.address}
        latitude={selection.latitude}
        longitude={selection.longitude}
        source={selection.source}
        onChange={setSelection}
        searchAddress={mockSearch}
        getCurrentAddress={mockCurrentAddress}
        onNotify={(message) => window.console.warn(message)}
        {...rest} />
      
      <p className="mt-3 text-meta text-muted" data-numeric="true">
        {selection.latitude != null && selection.longitude != null ?
        `坐标 ${Number(selection.longitude).toFixed(4)}, ${Number(selection.latitude).toFixed(4)}` :
        '尚未确认坐标'}
      </p>
    </div>);

}

const previews: ComponentPreviewModule = {
  componentName: 'AmapAddressPicker',
  importPath: 'components/AmapAddressPicker',
  previews: [
  {
    name: 'Default',
    description: 'Empty field with search, map picker and locate actions. Type 2+ characters to see suggestions.',
    render: () => <PickerHarness />
  },
  {
    name: 'Confirmed selection',
    description: 'Address already resolved from a search suggestion — shows the confirmation caption.',
    render: () =>
    <PickerHarness
      initial={{
        address: '上海市 黄浦区 南京东路 300 号 宠悦宠物生活馆',
        latitude: 31.2304,
        longitude: 121.4737,
        source: 'amap_tip'
      }} />


  },
  {
    name: 'Search only',
    description: 'Both action buttons hidden — the field spans the full row for compact forms.',
    render: () => <PickerHarness showLocateButton={false} showMapButton={false} placeholder="搜索地址" />
  },
  {
    name: 'Disabled',
    description: 'Read-only state while a form is submitting or the address is locked to a merchant.',
    render: () =>
    <PickerHarness
      disabled
      initial={{
        address: '上海市 浦东新区 陆家嘴环路 1000 号',
        latitude: 31.2397,
        longitude: 121.4998,
        source: 'merchant'
      }} />


  }]

};

export default previews;