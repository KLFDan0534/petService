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
 * 服务的展示图：优先本地映射，其次后端首图，最后给一张兜底本地图。
 */
export function coverForService(svc) {
  if (!svc) return HERO_IMAGE
  const local = photoForServiceType(svc.type_wsh)
  if (local) return local
  const fromBackend = String(svc.images_wsh || svc.firstImage || '').split(',')[0]?.trim()
  return fromBackend || HERO_IMAGE
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