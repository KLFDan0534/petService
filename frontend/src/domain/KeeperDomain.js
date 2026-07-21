import * as keeperService from '@/services/keeperService'
import * as ratingService from '@/services/ratingService'
import * as favoriteService from '@/services/favoriteService'

function computeAvgScore(ratings) {
  if (!ratings || ratings.length === 0) return '-'
  const sum = ratings.reduce((a, r) => a + (r.score_wsh || 0), 0)
  return (sum / ratings.length).toFixed(1)
}

export async function getFullProfile(id) {
  const [keeper, ratings] = await Promise.all([
    keeperService.getById(id),
    ratingService.getByTarget(id, 'keeper'),
  ])

  return {
    keeper,
    ratings: ratings || [],
    stats: {
      avgScore: computeAvgScore(ratings),
      ratingCount: (ratings || []).length,
    },
    qualifications: keeper.qualifications_wsh || [],
  }
}

export async function toggleFavorite(id) {
  return favoriteService.toggle(id, 'keeper')
}

export async function getRatings(id) {
  return ratingService.getByTarget(id, 'keeper')
}
