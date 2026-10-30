import request from '@/utils/request'

// 取地图服务的配置（比如 key 和默认城市），前端定位、算距离之前先拿一次
export async function getGeoConfig() {
  const res = await request.get('/api/geo/config')
  return res.data
}



