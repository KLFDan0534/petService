import request from '@/utils/request'

export async function getGeoConfig() {
  const res = await request.get('/api/geo/config')
  return res.data
}



