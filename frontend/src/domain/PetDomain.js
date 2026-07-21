import * as petService from '@/services/petService'
import * as orderService from '@/services/orderService'
import * as fileService from '@/services/fileService'
import { getPetReports } from '@/api/ai'

const TYPE_MAP = { dog: '狗', cat: '猫', rabbit: '兔子', bird: '鸟', fish: '鱼', hamster: '仓鼠', other: '其他' }
const ICON_MAP = { dog: '犬', cat: '猫', rabbit: '兔', bird: '鸟', fish: '鱼', hamster: '鼠', other: '宠' }

function normalizeType(type) {
  return String(type || 'other').toLowerCase()
}

export function petTypeLabel(type) {
  return TYPE_MAP[normalizeType(type)] || type || '其他'
}

export function petTypeIcon(type) {
  return ICON_MAP[normalizeType(type)] || '宠'
}

export function genderLabel(g) {
  const v = Number(g)
  return v === 1 ? '公' : v === 2 ? '母' : '未知'
}

export function formatAge(a) {
  return a ? a + '个月' : '-'
}

function extract(res) {
  if (!res) throw new Error('无响应')
  if (res.code !== 200) throw new Error(res.message || '请求失败')
  return res.data ?? res
}

export async function getFullProfile(id) {
  const [pet, orders, reportsRes] = await Promise.all([
    petService.getById(id),
    orderService.getByPetId(id),
    getPetReports(id).catch(() => ({ code: 200, data: [] })),
  ])

  const reports = extract(reportsRes)

  return {
    pet,
    orders: orders || [],
    reports: reports || [],
    canGenerateReport: (orders || []).some(o => o.status_wsh === 'completed'),
  }
}

export async function updateAvatar(petId, file) {
  const uploaded = await fileService.uploadToDirectory('pets', file)
  if (!uploaded || !uploaded.url_wsh) throw new Error('上传失败')
  await petService.updateAvatar(petId, uploaded.url_wsh)
  return uploaded.url_wsh
}

export async function updateProfile(id, data) {
  return petService.update(id, data)
}
