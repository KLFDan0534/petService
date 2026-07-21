package com.pet.customer.service;

import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingDTO;
import com.pet.customer.entity.Rating;

import java.util.List;

public interface RatingService {
    /**
     * 根据目标ID和类型获取评价列表
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 评价列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<RatingDTO> getRatingsByTarget(Long targetId, String targetType);
    /**
     * 创建评价
     * @param userId 用户ID
     * @param rating 评价实体
     * @return 创建后的评价数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    RatingDTO createRating(Long userId, RatingCreateRequestDTO request);
    /**
     * 回复评价
     * @param id 评价ID
     * @param reply 回复内容
     * @param merchantId 商家ID
     * @return 更新后的评价数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    RatingDTO replyRating(Long id, String reply, Long merchantId);
}

