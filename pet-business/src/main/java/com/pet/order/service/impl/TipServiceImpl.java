package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
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

/**
 * Implementation of {@link TipService} for tip/gratuity processing.
 * <p>
 * Tips allow pet owners to reward keepers after order completion. Tips are
 * transferred directly in real-time via the accounting system. Only the pet
 * owner of a COMPLETED order can send a tip. If no recipient is specified,
 * the tip defaults to the keeper's associated user account.
 */
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

    /**
     * 【创建打赏（实现）】
     *
     * 业务作用：
     * 创建打赏记录并实时转账：校验订单归属和完成状态 → insert打赏记录 → accountingService.transfer实时转账。
     *
     * 调用链：
     * TipService.create()
     * ↓
     * 查询订单 → 校验归属/状态 → insert打赏记录 → resolveReceiverUserId()
     * → accountingService.transfer(主人→接收人, 即时转账)
     *
     * @param userId  主人用户ID
     * @param request 打赏请求
     */
    @Transactional
    @Override
    public void create(Long userId, TipCreateRequestDTO request) {
        log.info("为订单创建打赏: {}", request.getOrder_id_wsh());
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

    /**
     * Retrieves all tips associated with a specific order.
     *
     * @param orderId the order ID
     * @return list of tips for this order
     */
    @Override
    public List<Tip> listByOrder(Long orderId) {
        log.info("查询订单打赏记录: {}", orderId);
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>().eq(Tip::getOrder_id_wsh, orderId));
    }

    /**
     * Retrieves all tips where the user is either the sender (from_user_id)
     * or the recipient (to_user_id).
     *
     * @param userId the user ID
     * @return list of tips involving this user
     */
    @Override
    public List<Tip> listMyTips(Long userId) {
        log.info("查询用户打赏记录: {}", userId);
        return tipMapper.selectList(
                new LambdaQueryWrapper<Tip>()
                        .eq(Tip::getFrom_user_id_wsh, userId)
                        .or()
                        .eq(Tip::getTo_user_id_wsh, userId));
    }

    /**
     * 【管理员分页查询打赏记录（实现）】
     *
     * 业务作用：
     * 分页查询打赏记录，按创建时间倒序排列。
     *
     * 调用链：
     * TipService.pageAll()
     * ↓
     * tipMapper.selectPage(Page, LambdaQueryWrapper)
     *
     * 状态影响：
     * 只读操作。
     *
     * @param pageParam 分页参数
     * @return 分页的打赏记录
     */
    @Override
    public IPage<Tip> pageAll(PageRequestDTO pageParam) {
        log.info("分页查询打赏记录, page: {}, size: {}", pageParam.getPage(), pageParam.getSize());
        Page<Tip> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return tipMapper.selectPage(page,
                new LambdaQueryWrapper<Tip>().orderByDesc(Tip::getCreated_at_wsh));
    }

    /**
     * Converts a Tip entity to a TipDTO.
     *
     * @param entity the Tip entity, may be null
     * @return the corresponding TipDTO, or null if input is null
     */
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

    /**
     * Resolves the recipient user ID for a tip. If the caller specified a
     * recipient, returns it directly. Otherwise, defaults to the keeper's
     * associated user account.
     *
     * @param order          the order (used to look up the keeper)
     * @param requestedUserId the user-specified recipient, may be null
     * @return the resolved recipient user ID
     * @throws BusinessException if no recipient specified and keeper cannot be determined
     */
    private Long resolveReceiverUserId(PetOrder order, Long requestedUserId) {
        if (requestedUserId != null) return requestedUserId;
        Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
        if (keeper == null || keeper.getUser_id_wsh() == null) {
            throw new BusinessException(400, "无法确定打赏接收人");
        }
        return keeper.getUser_id_wsh();
    }
}
