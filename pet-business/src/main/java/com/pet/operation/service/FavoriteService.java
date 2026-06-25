package com.pet.operation.service;

import com.pet.operation.entity.Favorite;

import java.util.List;

public interface FavoriteService {
    /**
     * 根据用户ID和目标类型获取收藏列表
     * @param userId 用户ID
     * @param targetType 目标类型
     * @return 收藏列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Favorite> listByUser(Long userId, String targetType);
    /**
     * 判断是否已收藏
     * @param userId 用户ID
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 是否已收藏
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    boolean isFavorited(Long userId, Long targetId, String targetType);
    /**
     * 切换收藏状态
     * @param userId 用户ID
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void toggle(Long userId, Long targetId, String targetType);
}

