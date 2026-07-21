package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingDTO;
import com.pet.customer.entity.Rating;
import com.pet.customer.mapper.RatingMapper;
import com.pet.customer.service.RatingService;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.KeeperMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;

    public RatingServiceImpl(RatingMapper ratingMapper,
                             OrderMapper orderMapper,
                             MerchantMapper merchantMapper,
                             KeeperMapper keeperMapper) {
        this.ratingMapper = ratingMapper;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
    }

    public List<RatingDTO> getRatingsByTarget(Long targetId, String targetType) {
        log.info("调用 getRatingsByTarget()");
        return toDTOList(ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTarget_id_wsh, targetId)
                        .eq(Rating::getTarget_type_wsh, targetType)
                        .orderByDesc(Rating::getCreated_at_wsh)));
    }

    @Transactional
    public RatingDTO createRating(Long userId, RatingCreateRequestDTO request) {
        log.info("调用 createRating()");
        Rating rating = new Rating();
        rating.setOrder_id_wsh(request.getOrder_id_wsh());
        rating.setTarget_id_wsh(request.getTarget_id_wsh());
        rating.setTarget_type_wsh(request.getTarget_type_wsh());
        rating.setScore_wsh(request.getScore_wsh());
        rating.setContent_wsh(request.getContent_wsh());
        rating.setImages_wsh(request.getImages_wsh());
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
            return toDTO(rating);
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
        return toDTO(rating);
    }

    @Transactional
    public RatingDTO replyRating(Long id, String reply, Long userId) {
        log.info("调用 replyRating()");
        Rating rating = ratingMapper.selectById(id);
        if (rating == null) {
            throw new BusinessException("评价记录不存在");
        }
        if ("merchant".equals(rating.getTarget_type_wsh())) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId));
            if (merchant == null || !merchant.getId_wsh().equals(rating.getTarget_id_wsh())) {
                throw new BusinessException(403, "无权回复该评价");
            }
        } else if ("keeper".equals(rating.getTarget_type_wsh())) {
            Keeper keeper = keeperMapper.selectOne(
                    new LambdaQueryWrapper<Keeper>().eq(Keeper::getUser_id_wsh, userId));
            if (keeper == null || !keeper.getId_wsh().equals(rating.getTarget_id_wsh())) {
                throw new BusinessException(403, "无权回复该评价");
            }
        }
        rating.setReply_wsh(reply);
        rating.setReply_at_wsh(LocalDateTime.now());
        ratingMapper.updateById(rating);
        return toDTO(rating);
    }

    private RatingDTO toDTO(Rating rating) {
        if (rating == null) return null;
        RatingDTO dto = new RatingDTO();
        dto.setId_wsh(rating.getId_wsh());
        dto.setOrder_id_wsh(rating.getOrder_id_wsh());
        dto.setUser_id_wsh(rating.getUser_id_wsh());
        dto.setTarget_id_wsh(rating.getTarget_id_wsh());
        dto.setTarget_type_wsh(rating.getTarget_type_wsh());
        dto.setScore_wsh(rating.getScore_wsh());
        dto.setContent_wsh(rating.getContent_wsh());
        dto.setImages_wsh(rating.getImages_wsh());
        dto.setReply_wsh(rating.getReply_wsh());
        dto.setReply_at_wsh(rating.getReply_at_wsh());
        dto.setCreated_at_wsh(rating.getCreated_at_wsh());
        return dto;
    }

    private List<RatingDTO> toDTOList(List<Rating> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
