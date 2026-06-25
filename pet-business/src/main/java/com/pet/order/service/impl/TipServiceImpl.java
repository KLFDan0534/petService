package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Tip;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.TipMapper;
import com.pet.order.service.TipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 小费服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class TipServiceImpl implements TipService {

    private final TipMapper tipMapper;
    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;

    public TipServiceImpl(TipMapper tipMapper, OrderMapper orderMapper, KeeperMapper keeperMapper) {
        this.tipMapper = tipMapper;
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
    }

    /**
     * 创建小费
     * @param userId 用户ID
     * @param tip 小费信息
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    @Override
    public void create(Long userId, Tip tip) {
        log.info("调用 create()");
        PetOrder order = orderMapper.selectById(tip.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getOwner_id_wsh())) {
            throw new BusinessException("只有宠物主人可以打赏此订单");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus_wsh())) {
            throw new BusinessException("只有已完成的订单可以打赏");
        }
        tip.setId_wsh(null);
        tip.setFrom_user_id_wsh(userId);
        if (tip.getTo_user_id_wsh() == null) {
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            if (keeper == null || keeper.getUser_id_wsh() == null) {
                throw new BusinessException("无法确定打赏接收人");
            }
            tip.setTo_user_id_wsh(keeper.getUser_id_wsh());
        }
        tipMapper.insert(tip);
    }

    /**
     * 根据订单ID获取小费列表
     * @param orderId 订单ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Tip> listByOrder(Long orderId) {
        log.info("调用 listByOrder()");
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>().eq(Tip::getOrder_id_wsh, orderId));
    }

    /**
     * 获取当前用户的小费列表
     * @param userId 用户ID
     * @return 小费列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public List<Tip> listMyTips(Long userId) {
        log.info("调用 listMyTips()");
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>()
                        .eq(Tip::getFrom_user_id_wsh, userId)
                        .or()
                        .eq(Tip::getTo_user_id_wsh, userId));
    }
}
