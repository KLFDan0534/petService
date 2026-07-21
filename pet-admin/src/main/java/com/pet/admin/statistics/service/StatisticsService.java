package com.pet.admin.statistics.service;

import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.ReputationStatsVO;
import com.pet.admin.statistics.vo.UserDashboardVO;

public interface StatisticsService {

    AdminDashboardVO getAdminDashboard();

    MerchantDashboardVO getMerchantDashboard(Long merchantId);

    UserDashboardVO getUserDashboard(Long userId);

    ReputationStatsVO getReputationStats(String targetType, Long targetId);
}

