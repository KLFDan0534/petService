/**
 * 预约窗口与批量下单的共享配置。
 *
 * 权威约定：
 * - 实际可预约窗口由后端 booking.max-booking-days 决定（默认 91 = 今天起 90 天），
 *   可用性接口响应字段 booking_window_days_wsh 为准；本文件只定义请求上界，
 *   后端会把超窗请求收敛到实际窗口，避免前后端各自维护一套窗口数字。
 * - 批量下单宠物数上限与后端 OrderBatchCreateRequestDTO.MIN_ITEMS/MAX_ITEMS 对齐。
 */

/** 可用性查询请求上界（今天起的偏移天数）；后端按 booking.max-booking-days 收敛实际窗口 */
export const AVAILABILITY_REQUEST_WINDOW_DAYS = 365

/** 批量下单最少宠物数（少于该数提示走单笔下单） */
export const BATCH_MIN_PETS = 2

/** 批量下单最多宠物数 */
export const BATCH_MAX_PETS = 10
