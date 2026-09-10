/**
 * 本地照片映射 —— 照片来自参考前端素材（frontend/public/photos/）。
 * 后端仍返回真实业务数据；这里只在前端把「展示图」替换成这批本地照片，
 * 确保离线/扫码访问时也能正常显示。map 里没有的 type 保持原图不替换。
 */
const p = (n) => `/photos/${n}.jpg`

export const HERO_IMAGE = p('273888e2-96f5-450c-a284-c40db4fd7f02') // 寄养套房主图
export const DETAIL_IMAGE = p('ad56f1e8-d106-4562-8a0d-06a83785c2c7')
export const BOOKING_IMAGE = p('bd3de1af-e7cc-4b3c-856e-111a6ded3568') // 休闲区

/** 照护现场 相册 6 张（不重复场景）。 */
export const GALLERY_IMAGES = [
  p('273888e2-96f5-450c-a284-c40db4fd7f02'), // 套房
  p('bd3de1af-e7cc-4b3c-856e-111a6ded3568'), // 休闲区
  p('81782d5d-3b9a-4987-941a-4131a76c3d9c'), // 美容
  p('181ac6a4-382b-4b33-881f-03ee1c6cd64b'), // 遛宠
  p('8c4e5ecc-5f89-4f3f-97fe-94b9c2d2d42c'), // 标准寄养
  p('323ee6ed-0b15-4923-949f-7bf087eb0f9f'), // 训练
]

const BY_TYPE = {
  // 后端真实类型（小写）
  boarding: p('273888e2-96f5-450c-a284-c40db4fd7f02'),
  grooming: p('81782d5d-3b9a-4987-941a-4131a76c3d9c'),
  walk: p('181ac6a4-382b-4b33-881f-03ee1c6cd64b'),
  training: p('323ee6ed-0b15-4923-949f-7bf087eb0f9f'),
  medical: p('39ab3e0e-f365-4364-8a72-22a3c467c987'),
  // 参考源码的大写细分键，兼容用
  BOARDING_VIP: p('273888e2-96f5-450c-a284-c40db4fd7f02'),
  BOARDING_STANDARD: p('8c4e5ecc-5f89-4f3f-97fe-94b9c2d2d42c'),
  BOARDING_SUPER: p('6fe0ab36-6a6d-4ee2-9e55-5f41ff36160d'),
  GROOMING_BASIC: p('81782d5d-3b9a-4987-941a-4131a76c3d9c'),
  GROOMING_VIP: p('d3f87364-d074-406f-a55e-cf1139990aa9'),
  WALK_STANDARD: p('181ac6a4-382b-4b33-881f-03ee1c6cd64b'),
  WALK_VIP: p('1d779025-2751-42d8-8846-cea961f39853'),
  TRAINING_BASIC: p('323ee6ed-0b15-4923-949f-7bf087eb0f9f'),
  MEDICAL_CHECKUP: p('39ab3e0e-f365-4364-8a72-22a3c467c987'),
  MEDICAL_CARE: p('0dc3cb03-4ca5-4256-a538-156f02a78f38'),
  DAYCARE_STANDARD: p('2b79085d-c936-4111-8ced-9f585106dcee'),
  DAYCARE_VIP: p('86b9a52e-731a-46bc-ba34-03d940450fe0'),
}

/** 有映射的类型返回本地图；没有的返回 undefined（调用方保留原图）。 */
export function photoForServiceType(type) {
  const raw = String(type || '')
  return BY_TYPE[raw] || BY_TYPE[raw.toUpperCase()] || undefined
}

/**
 * 服务的完整图集：列表页封面与详情页相册共用同一份结果，保证两处看到的是同一批图。
 *
 * 优先级（单一数据源，真实内容图优先，本地图仅兜底）：
 *   1. media_wsh   —— 后端媒体表 pet_service_media_wsh（含 is_cover 标记）
 *   2. images_wsh  —— 服务主表的历史图片字段（逗号分隔）
 *   3. 本地类型图   —— 按 type_wsh 映射的本地素材，仅在上面都为空时出现
 *
 * @returns {Array<{url: string, isCover: boolean}>} 至少包含 1 条（本地兜底图）
 */
export function serviceGallery(svc) {
  if (!svc) return [{ url: HERO_IMAGE, isCover: true }]
  const list = []
  const seen = new Set()
  const push = (raw, isCover) => {
    const url = String(raw || '').trim()
    if (!url || seen.has(url)) return
    seen.add(url)
    list.push({ url, isCover: !!isCover })
  }

  // 1) 后端媒体表
  const media = Array.isArray(svc.media_wsh) ? svc.media_wsh : []
  for (const m of media) {
    push(m?.url_wsh || m?.url, Number(m?.is_cover_wsh) === 1)
  }
  // 2) 服务主表历史图片字段
  const legacy = String(svc.images_wsh || '').split(',')
  legacy.forEach((u, i) => push(u, list.length === 0 && i === 0))
  if (!list.length && svc.firstImage) push(svc.firstImage, true)
  // 3) 本地兜底（真实图缺失时才用）
  if (!list.length) push(photoForServiceType(svc.type_wsh) || HERO_IMAGE, true)

  // 没有显式封面时，第一张即封面
  if (!list.some(item => item.isCover)) list[0].isCover = true
  return list
}

/**
 * 服务的展示图（封面）：取图集中标记为封面的那张，通常就是内容图的第一张。
 */
export function coverForService(svc) {
  const gallery = serviceGallery(svc)
  return (gallery.find(item => item.isCover) || gallery[0]).url
}

/**
 * 服务的本地兜底图：真实内容图加载失败时顶上，避免列表/详情出现空占位。
 */
export function localFallbackForService(svc) {
  return photoForServiceType(svc?.type_wsh) || HERO_IMAGE
}

/** 照护师/门店卡片占位头像池（宠物特写，供无头像时兜底，也可整卡替换）。 */
export const KEEPER_AVATARS = [
  p('9029988a-0b7d-47bf-ab21-c48582152fd2'),
  p('a0e52d88-ee3e-4d4b-b943-f7854c49ad9b'),
  p('98133d55-b54e-4f27-aa86-55d2d6d65d8d'),
  p('a727cfb4-25bb-43a1-8efa-6f5725fabe32'),
  p('ad12608e-deb6-42a4-bbe1-63744babac36'),
  p('965c45c5-ddd0-4854-a477-bd3687dec485'),
]