package com.pet.module.rating.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.rating.entity.Rating;
import com.pet.module.rating.mapper.RatingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {

    private final RatingMapper ratingMapper;
    private final OrderMapper orderMapper;

    public RatingService(RatingMapper ratingMapper, OrderMapper orderMapper) {
        this.ratingMapper = ratingMapper;
        this.orderMapper = orderMapper;
    }

    public List<Rating> getRatingsByTarget(Long targetId, String targetType) {
        return ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTargetId, targetId)
                        .eq(Rating::getTargetType, targetType)
                        .orderByDesc(Rating::getCreatedAt));
    }

    @Transactional
    public Rating createRating(Long userId, Rating rating) {
        PetOrder order = orderMapper.selectById(rating.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new BusinessException("订单未完成，无法评价");
        }
        Long count = ratingMapper.selectCount(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getOrderId, rating.getOrderId())
                        .eq(Rating::getUserId, userId));
        if (count > 0) {
            throw new BusinessException("已评价过此订单");
        }
        rating.setUserId(userId);
        ratingMapper.insert(rating);
        return rating;
    }
}
