import * as merchantService from '@/services/merchantService'
import * as ratingService from '@/services/ratingService'
import * as favoriteService from '@/services/favoriteService'

function computeAvgScore(ratings) {
  if (!ratings || ratings.length === 0) return '-'
  const sum = ratings.reduce((a, r) => a + (r.score_wsh || 0), 0)
  return (sum / ratings.length).toFixed(1)
}

function computeDayLabels() {
  return { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
}

export async function getFullProfile(id) {
  const [merchant, services, hours, ratings] = await Promise.all([
    merchantService.getById(id),
    merchantService.getServices(id),
    merchantService.getBusinessHours(id),
    ratingService.getByTarget(id, 'merchant'),
  ])

  return {
    merchant,
    services: services || [],
    hours: hours || [],
    ratings: ratings || [],
    dayLabels: computeDayLabels(),
    stats: {
      avgScore: computeAvgScore(ratings),
      serviceCount: (services || []).length,
      ratingCount: (ratings || []).length,
    },
    qualifications: merchant.qualifications_wsh || [],
  }
}

export async function toggleFavorite(id) {
  return favoriteService.toggle(id, 'merchant')
}

export async function getServices(id) {
  return merchantService.getServices(id)
}

export async function getBusinessHours(id) {
  return merchantService.getBusinessHours(id)
}

export async function getRatings(id) {
  return ratingService.getByTarget(id, 'merchant')
}
