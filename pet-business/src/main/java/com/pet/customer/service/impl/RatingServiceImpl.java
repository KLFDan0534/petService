package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BookingErrorCode;
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
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【业务模块】评价管理（实现）
 * 业务作用：提供用户评价和商家回复的完整业务逻辑。
 * 支持三种评价目标类型：merchant（商家）、keeper（照看者）、service（服务）。
 * 评价后商家或照看者可进行回复。
 */
@Service
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingMapper ratingMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperMapper keeperMapper;
    private final ServiceItemMapper serviceItemMapper;

    public RatingServiceImpl(RatingMapper ratingMapper,
                             OrderMapper orderMapper,
                             MerchantMapper merchantMapper,
                             KeeperMapper keeperMapper,
                             ServiceItemMapper serviceItemMapper) {
        this.ratingMapper = ratingMapper;
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.keeperMapper = keeperMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 【业务名称】按目标查询评价列表（实现）
     * 业务作用：根据目标对象 ID 和类型查询评价列表。
     * 调用场景：用户或商家查看某目标的评价。
     * 调用链：getRatingsByTarget() → RatingMapper.selectList()。
     * 数据处理：按 targetId + targetType 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<RatingDTO> getRatingsByTarget(Long targetId, String targetType) {
        log.info("调用 getRatingsByTarget()");
        return toDTOList(ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getTarget_id_wsh, targetId)
                        .eq(Rating::getTarget_type_wsh, targetType)
                        .orderByDesc(Rating::getCreated_at_wsh)));
    }

    @Override
    public List<RatingDTO> getMyRatingsByOrder(Long userId, Long orderId) {
        log.info("调用 getMyRatingsByOrder()");
        if (orderId == null) {
            return List.of();
        }
        return toDTOList(ratingMapper.selectList(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getOrder_id_wsh, orderId)
                        .eq(Rating::getUser_id_wsh, userId)));
    }

    /**
     * 【业务名称】创建评价（实现）
     * 业务作用：用户为指定订单或服务创建评价。
     * 调用场景：订单完成后用户评价。
     * 调用链：createRating() → 校验订单状态、归属与重复评价 → insert()。
     * 数据处理：校验订单已完成、属于当前用户、评价目标与订单归属一致，且未重复评价 → 插入评价记录。
     * 业务规则：
     * - service 类型允许不带订单ID，自动归属到当前用户最近一次使用该服务的已完成订单；
     * - 评分 1-5；
     * - 评价目标必须与订单归属一致（merchant/keeper/service 分别对应订单的 merchant/keeper/service）；
     * - 同一订单同一维度（order_id + user_id + target_type）仅允许一条评价；
     * - 订单必须属于当前用户，禁止利用他人订单刷评。
     * 状态影响：新增一条评价记录。
     * 异常情况：目标为空抛异常；评分越界抛异常；订单未完成抛异常；订单不属于当前用户抛异常；
     * 目标与订单归属不一致抛异常；重复评价抛异常。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
    public RatingDTO createRating(Long userId, RatingCreateRequestDTO request) {
        log.info("调用 createRating()");
        Rating rating = new Rating();
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

        PetOrder order;
        if ("service".equalsIgnoreCase(rating.getTarget_type_wsh())) {
            rating.setTarget_type_wsh("service");
            if (request.getOrder_id_wsh() == null) {
                order = orderMapper.selectOne(new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getService_id_wsh, rating.getTarget_id_wsh())
                        .eq(PetOrder::getOwner_id_wsh, userId)
                        .eq(PetOrder::getStatus_wsh, OrderStatus.COMPLETED)
                        .orderByDesc(PetOrder::getCreated_at_wsh));
                if (order == null) {
                    throw new BusinessException(400, BookingErrorCode.ORDER_NOT_COMPLETED, "订单未完成，无法评价");
                }
            } else {
                order = orderMapper.selectById(request.getOrder_id_wsh());
                if (order == null) {
                    throw new BusinessException("订单不存在");
                }
            }
        } else {
            order = orderMapper.selectById(request.getOrder_id_wsh());
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
        }

        if (!OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            throw new BusinessException(400, BookingErrorCode.ORDER_NOT_COMPLETED, "订单未完成，无法评价");
        }
        if (!Long.valueOf(userId).equals(order.getOwner_id_wsh())) {
            throw new BusinessException(400, BookingErrorCode.RATING_TARGET_MISMATCH,
                    "订单不属于当前用户，禁止评价他人订单");
        }
        assertTargetBelongsOrder(rating.getTarget_id_wsh(), rating.getTarget_type_wsh(), order);

        Long count = ratingMapper.selectCount(
                new LambdaQueryWrapper<Rating>()
                        .eq(Rating::getOrder_id_wsh, order.getId_wsh())
                        .eq(Rating::getUser_id_wsh, userId)
                        .eq(Rating::getTarget_type_wsh, rating.getTarget_type_wsh()));
        if (count > 0) {
            throw new BusinessException(400, BookingErrorCode.RATING_ALREADY_EXISTS, "已评价过该订单的该维度");
        }

        rating.setOrder_id_wsh(order.getId_wsh());
        rating.setUser_id_wsh(userId);
        try {
            ratingMapper.insert(rating);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(400, BookingErrorCode.RATING_ALREADY_EXISTS,
                    "已评价过该订单的该维度");
        }
        return toDTO(rating);
    }

    /**
     * 校验评价目标与订单的实际归属一致（防止对订单无关对象刷评）。
     *
     * @param targetId   评价目标ID
     * @param targetType 评价目标类型 merchant/keeper/service
     * @param order      已完成且属于当前用户的订单
     * @throws BusinessException 目标与订单归属不一致时抛出 RATING_TARGET_MISMATCH
     */
    private void assertTargetBelongsOrder(Long targetId, String targetType, PetOrder order) {
        boolean matched;
        switch (targetType) {
            case "merchant" -> matched = Long.valueOf(order.getMerchant_id_wsh()).equals(targetId);
            case "keeper" -> matched = Long.valueOf(order.getKeeper_id_wsh()).equals(targetId);
            case "service" -> matched = Long.valueOf(order.getService_id_wsh()).equals(targetId);
            default -> throw new BusinessException("不支持的评价目标类型");
        }
        if (!matched) {
            throw new BusinessException(400, BookingErrorCode.RATING_TARGET_MISMATCH, "评价目标与订单不一致");
        }
    }

    /**
     * 【业务名称】回复评价（实现）
     * 业务作用：商家或照看者回复一条评价。
     * 调用场景：商家/照看者回复用户评价。
     * 调用链：replyRating() → 权限校验 → updateById()。
     * 数据处理：更新 reply 和 reply_at。
     * 业务规则：商家只能回复 merchant 类型且归属自己商家的评价；照看者只能回复 keeper 类型且属于自己的评价。
     * 状态影响：评价的 reply 和 reply_at 字段更新。
     * 异常情况：评价不存在抛异常；无权回复抛 BusinessException(403)。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    @Override
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
        } else if ("service".equals(rating.getTarget_type_wsh())) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId));
            ServiceItem service = serviceItemMapper.selectById(rating.getTarget_id_wsh());
            if (merchant == null || service == null || !merchant.getId_wsh().equals(service.getMerchant_id_wsh())) {
                throw new BusinessException(403, "无权回复该评价");
            }
        }
        rating.setReply_wsh(reply);
        rating.setReply_at_wsh(LocalDateTime.now());
        ratingMapper.updateById(rating);
        return toDTO(rating);
    }

    /**
     * 【业务名称】评价实体转DTO
     * 业务作用：将评价实体转换为DTO。
     * 调用场景：对外暴露评价信息。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为null时返回null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
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
