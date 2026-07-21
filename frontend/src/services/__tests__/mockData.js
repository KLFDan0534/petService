export const mockMerchant = {
  id_wsh: 1,
  name_wsh: 'PetCare宠物生活馆',
  phone_wsh: '13800138000',
  address_wsh: '北京市朝阳区建国路88号',
  description_wsh: '专业的宠物寄养与美容服务',
  status_wsh: 1,
  rating_wsh: 4.5,
  created_at_wsh: '2025-01-15T08:00:00Z',
  qualifications_wsh: [
    { id_wsh: 1, title_wsh: '宠物美容师资格证', file_url_wsh: '/uploads/qual1.jpg' },
  ],
}

export const mockKeeper = {
  id_wsh: 10,
  name_wsh: '张三',
  avatar_wsh: '/uploads/keeper1.jpg',
  status_wsh: 1,
  rating_wsh: 4.8,
  merchant_id_wsh: 1,
  merchant_name_wsh: 'PetCare宠物生活馆',
  experience_years_wsh: 5,
  price_per_day_wsh: 200,
  current_pets_wsh: 2,
  max_pets_wsh: 4,
  completion_rate_wsh: 98,
  complaint_rate_wsh: 1,
  bio_wsh: '5年宠物照护经验，擅长老年犬护理',
  qualifications_wsh: [
    { id_wsh: 2, title_wsh: '宠物护理资格证', file_url_wsh: '/uploads/qual2.jpg' },
  ],
}

export const mockPet = {
  id_wsh: 100,
  name_wsh: '旺财',
  avatar_wsh: null,
  type_wsh: 'dog',
  breed_wsh: '金毛',
  age_wsh: 24,
  weight_wsh: 25,
  gender_wsh: 1,
  vaccinated_wsh: 1,
  sterilized_wsh: 0,
  description_wsh: '性格温顺，喜欢与人互动',
  allergies_wsh: '对鸡肉过敏',
  habits_wsh: '每天早晚各遛一次',
  owner_name_wsh: '李四',
}

export const mockRatings = [
  { id_wsh: 1, user_id_wsh: 1001, score_wsh: 5, content_wsh: '非常满意！', reply_wsh: '感谢您的支持', created_at_wsh: '2026-01-10T10:00:00Z' },
  { id_wsh: 2, user_id_wsh: 1002, score_wsh: 4, content_wsh: '服务态度很好', reply_wsh: null, created_at_wsh: '2026-02-15T14:00:00Z' },
]

export const mockServices = [
  { id_wsh: 1, name_wsh: '日间寄养', type_wsh: 'boarding', category_name_wsh: '寄养服务', description_wsh: '日间专业寄养', price_wsh: 80, unit_wsh: '天', images_wsh: '/img/svc1.jpg', status_wsh: 1 },
  { id_wsh: 2, name_wsh: '宠物美容', type_wsh: 'grooming', category_name_wsh: '美容服务', description_wsh: '全宠物美容', price_wsh: 150, unit_wsh: '次', images_wsh: null, status_wsh: 1 },
]

export const mockBusinessHours = [
  { day_of_week_wsh: 1, open_time_wsh: '09:00:00', close_time_wsh: '18:00:00', is_closed_wsh: false },
  { day_of_week_wsh: 2, open_time_wsh: '09:00:00', close_time_wsh: '18:00:00', is_closed_wsh: false },
  { day_of_week_wsh: 6, open_time_wsh: '10:00:00', close_time_wsh: '17:00:00', is_closed_wsh: false },
  { day_of_week_wsh: 7, open_time_wsh: null, close_time_wsh: null, is_closed_wsh: true },
]

export const mockOrders = [
  { id_wsh: 1000, pet_id_wsh: 100, service_name_wsh: '日间寄养', start_date_wsh: '2026-06-01', end_date_wsh: '2026-06-05', status_wsh: 'completed' },
  { id_wsh: 1001, pet_id_wsh: 100, service_name_wsh: '宠物美容', start_date_wsh: '2026-06-10', end_date_wsh: '2026-06-10', status_wsh: 'in_progress' },
]

export const mockReports = [
  { id_wsh: 1, pet_id_wsh: 100, type_wsh: 'daily', content_wsh: '今日食欲正常，精神状态良好，完成了常规运动量。', created_at_wsh: '2026-06-02T18:00:00Z' },
]
