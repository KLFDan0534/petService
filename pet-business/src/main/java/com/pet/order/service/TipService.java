package com.pet.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.order.dto.TipCreateRequestDTO;
import com.pet.order.dto.TipDTO;
import com.pet.order.entity.Tip;

import java.util.List;

/**
 * Service interface for tipping (gratuity) operations.
 * <p>
 * Tipping allows pet owners to reward keepers after an order is completed.
 * Tips are transferred directly from the owner's account to the keeper's account
 * in real-time via the accounting system. Only COMPLETED orders are eligible for tipping.
 */
public interface TipService {

    /**
     * 【创建打赏】
     *
     * 业务作用：
     * 宠物主人在订单完成后打赏看护者，金额通过财务系统从主人账户实时转账到接收人账户。
     *
     * 调用场景：
     * 宠物主人在已完成的订单中点击"打赏"。
     *
     * 调用链：
     * 主人端/Controller
     * ↓
     * create(userId, request)
     * ↓
     * 校验订单归属/状态 → insert打赏记录 → accountingService.transfer(主人→接收人)
     *
     * 业务规则：
     * 1. 仅订单主人可打赏
     * 2. 仅COMPLETED状态的订单可打赏
     * 3. 未指定接收人时默认看护者关联的用户
     * 4. 实时转账到接收人账户
     *
     * 状态影响：
     * 创建打赏记录并执行实时转账。
     *
     * @param userId  宠物主人用户ID
     * @param request 打赏请求
     */
    void create(Long userId, TipCreateRequestDTO request);

    /**
     * 【查询订单的打赏记录】
     *
     * 业务作用：
     * 查询指定订单的所有打赏记录。
     *
     * @param orderId 订单ID
     * @return 打赏记录列表
     */
    List<Tip> listByOrder(Long orderId);

    /**
     * 【查询用户的打赏记录】
     *
     * 业务作用：
     * 查询用户相关的所有打赏（包括发出的和收到的）。
     *
     * @param userId 用户ID
     * @return 打赏记录列表
     */
    List<Tip> listMyTips(Long userId);

    /**
     * 【管理员分页查询打赏记录】
     *
     * 业务作用：
     * 分页查询打赏记录，按创建时间倒序排列。
     *
     * 调用场景：
     * 管理员后台打赏记录列表。
     *
     * 调用链：
     * Controller
     * ↓
     * pageAll(pageParam)
     * ↓
     * tipMapper.selectPage(Page, LambdaQueryWrapper)
     *
     * 状态影响：
     * 只读操作。
     *
     * @param pageParam 分页参数
     * @return 分页的打赏记录
     */
    IPage<Tip> pageAll(PageRequestDTO pageParam);

    /**
     * 【打赏实体转DTO】
     *
     * 业务作用：
     * 将打赏实体映射为打赏DTO。
     *
     * @param entity 打赏实体
     * @return 打赏DTO
     */
    TipDTO toDTO(Tip entity);
}
