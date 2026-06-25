package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.customer.entity.Rating;
import com.pet.customer.mapper.RatingMapper;
import com.pet.customer.service.RatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final OrderMapper orderMapper;

    public RatingServiceImpl(RatingMapper ratingMapper, OrderMapper orderMapper) {
        this.ratingMapper = ratingMapper;
        this.orderMapper = orderMapper;
    }

    public List<Rating> getRatingsByTarget(Long targetId, String targetType) {
        log.info("调用 getRatingsByTarget()");
        return ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTarget_id_wsh, targetId)
                        .eq(Rating::getTarget_type_wsh, targetType)
                        .orderByDesc(Rating::getCreated_at_wsh));
    }

    @Transactional
    public Rating createRating(Long userId, Rating rating) {
        log.info("调用 createRating()");
        if (rating.getTarget_id_wsh() == null || rating.getTarget_type_wsh() == null || rating.getTarget_type_wsh().isBlank()) {
            throw new BusinessException("评价目标不能为空");
        }
        if (rating.getScore_wsh() == null || rating.getScore_wsh() < 1 || rating.getScore_wsh() > 5) {
            throw new BusinessException("评分必须在 1-5 之间");
        }

        if ("service".equalsIgnoreCase(rating.getTarget_type_wsh())) {
            rating.setTarget_type_wsh("service");
            Long count = ratingMapper.selectCount(
                    new LambdaQueryWrapper<Rating>()
                            .eq(Rating::getTarget_id_wsh, rating.getTarget_id_wsh())
                            .eq(Rating::getTarget_type_wsh, "service")
                            .eq(Rating::getUser_id_wsh, userId));
            if (count > 0) {
                throw new BusinessException("已评价过该服务");
            }
            PetOrder order = orderMapper.selectOne(new LambdaQueryWrapper<PetOrder>()
                    .eq(PetOrder::getService_id_wsh, rating.getTarget_id_wsh())
                    .eq(PetOrder::getOwner_id_wsh, userId)
                    .eq(PetOrder::getStatus_wsh, OrderStatus.COMPLETED)
                    .orderByDesc(PetOrder::getCreated_at_wsh));
            if (order == null) {
                throw new BusinessException("订单未完成，无法评价");
            }
            rating.setOrder_id_wsh(order.getId_wsh());
            rating.setUser_id_wsh(userId);
            ratingMapper.insert(rating);
            return rating;
        }

        PetOrder order = orderMapper.selectById(rating.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            throw new BusinessException("订单未完成，无法评价");
        }
        Long count = ratingMapper.selectCount(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getOrder_id_wsh, rating.getOrder_id_wsh())
                        .eq(Rating::getUser_id_wsh, userId));
        if (count > 0) {
            throw new BusinessException("已评价过该订单");
        }
        rating.setUser_id_wsh(userId);
        ratingMapper.insert(rating);
        return rating;
    }

    @Transactional
    public Rating replyRating(Long id, String reply, Long merchantId) {
        log.info("调用 replyRating()");
        Rating rating = ratingMapper.selectById(id);
        if (rating == null) {
            throw new BusinessException("评价记录不存在");
        }
        rating.setReply_wsh(reply);
        rating.setReply_at_wsh(LocalDateTime.now());
        ratingMapper.updateById(rating);
        return rating;
    }
}
