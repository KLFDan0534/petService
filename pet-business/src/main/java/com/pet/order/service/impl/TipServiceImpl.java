package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.finance.service.AccountingService;
import com.pet.order.dto.TipCreateRequestDTO;
import com.pet.order.dto.TipDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Tip;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.TipMapper;
import com.pet.order.service.TipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class TipServiceImpl implements TipService {

    private final TipMapper tipMapper;
    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;
    private final AccountingService accountingService;

    public TipServiceImpl(TipMapper tipMapper,
                          OrderMapper orderMapper,
                          KeeperMapper keeperMapper,
                          AccountingService accountingService) {
        this.tipMapper = tipMapper;
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.accountingService = accountingService;
    }

    @Transactional
    @Override
    public void create(Long userId, TipCreateRequestDTO request) {
        log.info("调用 create()");
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!userId.equals(order.getOwner_id_wsh())) throw new BusinessException(403, "只有宠物主人可以打赏此订单");
        if (!OrderStatus.COMPLETED.equals(order.getStatus_wsh())) throw new BusinessException(400, "只有已完成的订单可以打赏");

        Tip tip = new Tip();
        tip.setOrder_id_wsh(request.getOrder_id_wsh());
        tip.setAmount_wsh(request.getAmount_wsh());
        tip.setMessage_wsh(request.getMessage_wsh());
        tip.setFrom_user_id_wsh(userId);
        tip.setTo_user_id_wsh(resolveReceiverUserId(order, request.getTo_user_id_wsh()));
        tipMapper.insert(tip);

        accountingService.transfer(userId, tip.getTo_user_id_wsh(), tip.getAmount_wsh(),
                "tip", order.getId_wsh(), "tip", String.valueOf(tip.getId_wsh()),
                "tip:" + tip.getId_wsh(), "订单打赏 - " + order.getOrder_no_wsh());
    }

    @Override
    public List<Tip> listByOrder(Long orderId) {
        log.info("调用 listByOrder()");
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>().eq(Tip::getOrder_id_wsh, orderId));
    }

    @Override
    public List<Tip> listMyTips(Long userId) {
        log.info("调用 listMyTips()");
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>()
                        .eq(Tip::getFrom_user_id_wsh, userId)
                        .or()
                        .eq(Tip::getTo_user_id_wsh, userId));
    }

    @Override
    public TipDTO toDTO(Tip entity) {
        if (entity == null) return null;
        TipDTO dto = new TipDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setFrom_user_id_wsh(entity.getFrom_user_id_wsh());
        dto.setTo_user_id_wsh(entity.getTo_user_id_wsh());
        dto.setAmount_wsh(entity.getAmount_wsh());
        dto.setMessage_wsh(entity.getMessage_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private Long resolveReceiverUserId(PetOrder order, Long requestedUserId) {
        if (requestedUserId != null) return requestedUserId;
        Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
        if (keeper == null || keeper.getUser_id_wsh() == null) {
            throw new BusinessException(400, "无法确定打赏接收人");
        }
        return keeper.getUser_id_wsh();
    }
}
