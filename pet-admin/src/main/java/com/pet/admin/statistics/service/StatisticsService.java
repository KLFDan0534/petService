package com.pet.admin.statistics.service;

import java.util.Map;

public interface StatisticsService {

    /**
     * 获取管理员仪表盘数据
     * @return 仪表盘数据Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> getAdminDashboard();

    /**
     * 获取商家仪表盘数据
     * @param merchantId 商家ID
     * @return 仪表盘数据Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> getMerchantDashboard(Long merchantId);

    /**
     * 获取用户仪表盘数据
     * @param userId 用户ID
     * @return 仪表盘数据Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> getUserDashboard(Long userId);
}

