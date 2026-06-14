package com.pet.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.order.dto.CreateOrderRequest;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;

    public OrderService(OrderMapper orderMapper, PetMapper petMapper, KeeperMapper keeperMapper) {
        this.orderMapper = orderMapper;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
    }

    public List<PetOrder> listByOwner(Long ownerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getOwnerId, ownerId)
                        .orderByDesc(PetOrder::getCreatedAt));
    }

    public List<PetOrder> listByMerchant(Long merchantId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getMerchantId, merchantId)
                        .orderByDesc(PetOrder::getCreatedAt));
    }

    public List<PetOrder> listByKeeper(Long keeperId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<PetOrder>()
                        .eq(PetOrder::getKeeperId, keeperId)
                        .orderByDesc(PetOrder::getCreatedAt));
    }

    public PetOrder getByOrderNo(String orderNo) {
        PetOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PetOrder>().eq(PetOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    public PetOrder getById(Long id) {
        PetOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Transactional
    public PetOrder createOrder(Long ownerId, CreateOrderRequest request) {
        Pet pet = petMapper.selectById(request.getPetId());
        if (pet == null) {
            throw new BusinessException("宠物不存在");
        }
        Keeper keeper = keeperMapper.selectById(request.getKeeperId());
        if (keeper == null) {
            throw new BusinessException("寄养员不存在");
        }
        if (keeper.getCurrentPets() >= keeper.getMaxPets()) {
            throw new BusinessException("该寄养员已满");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (days <= 0) {
            throw new BusinessException("结束日期必须大于开始日期");
        }
        BigDecimal totalAmount = keeper.getPricePerDay().multiply(BigDecimal.valueOf(days));
        BigDecimal discount = BigDecimal.ZERO;
        if (days >= 30) {
            discount = totalAmount.multiply(BigDecimal.valueOf(0.1));
        } else if (days >= 7) {
            discount = totalAmount.multiply(BigDecimal.valueOf(0.05));
        }

        PetOrder order = new PetOrder();
        order.setOrderNo(generateOrderNo());
        order.setOwnerId(ownerId);
        order.setPetId(request.getPetId());
        order.setKeeperId(request.getKeeperId());
        order.setMerchantId(request.getMerchantId());
        order.setServiceId(request.getServiceId());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setDays((int) days);
        order.setPricePerDay(keeper.getPricePerDay());
        order.setTotalAmount(totalAmount);
        order.setDiscount(discount);
        order.setFinalAmount(totalAmount.subtract(discount));
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        keeper.setCurrentPets(keeper.getCurrentPets() + 1);
        keeperMapper.updateById(keeper);

        return order;
    }

    @Transactional
    public void cancelOrder(Long ownerId, String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        if (!order.getOwnerId().equals(ownerId)) {
            throw new BusinessException("无权取消此订单");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);

        Keeper keeper = keeperMapper.selectById(order.getKeeperId());
        if (keeper != null && keeper.getCurrentPets() > 0) {
            keeper.setCurrentPets(keeper.getCurrentPets() - 1);
            keeperMapper.updateById(keeper);
        }
    }

    @Transactional
    public void completeOrder(String orderNo) {
        PetOrder order = getByOrderNo(orderNo);
        if (!"paid".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可完成");
        }
        order.setStatus("completed");
        orderMapper.updateById(order);
    }

    private String generateOrderNo() {
        String date = LocalDate.now().toString().replace("-", "");
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + date + uuid;
    }
}
