package com.pet.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 业务作用：
 * 投诉与工单共用的「内部人员可管理商家范围」计算与可见性判定。
 * 在此之前这段逻辑在 ComplaintServiceImpl / TicketServiceImpl / CsWorkbenchServiceImpl
 * 各写了一遍（三份逐字相同），任何一处改了规则其余两处不会同步。
 * 业务规则：
 * <ol>
 * <li>本类只做判定，不抛异常——错误码与文案留在各自业务层，投诉和工单的提示语可以不同；</li>
 * <li>管理员不看商家范围，直接放行；</li>
 * <li>商家 ID 为空时非管理员一律拒绝（沿用既有行为，不因合并而放宽）。</li>
 * </ol>
 * 异常情况：无显式抛出；数据库异常向上传播。
 * 注意事项：每次判定都重新查库、无缓存。调用方在一次请求内多次判定时可自行复用返回的集合。
 */
@Slf4j
@Component
public class IssueAccess {

    private final MerchantMapper merchantMapper;
    private final MerchantCustomerServiceService merchantCustomerServiceService;

        public IssueAccess(MerchantMapper merchantMapper,
                       MerchantCustomerServiceService merchantCustomerServiceService) {
        this.merchantMapper = merchantMapper;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
    }

    /**
     * 【计算内部人员可管理的商家范围】
     *
     * <p>业务作用：汇总当前用户作为商家所有者或已授权客服可管理的商家 ID 集合。</p>
     *
     * <p>业务规则：
     * <ul>
     *   <li>userId 为空返回空集合（不是 null，调用方无需判空）；</li>
     *   <li>merchant 为真：按 user_id_wsh 查本人商家，**只取一条**（LIMIT 1）——这是既有语义，
     *       一个账号拥有多个商家时只会取到其中一个，本次未改变；</li>
     *   <li>customerService 为真：追加已授权服务的商家；</li>
     *   <li>结果去重且保持插入顺序。</li>
     * </ul>
     *
     * @param userId          当前用户ID
     * @param merchant        是否具备商家角色
     * @param customerService 是否具备客服角色
     * @return 可管理商家ID集合，可能为空
     */
    public Set<Long> staffMerchantIds(Long userId, boolean merchant, boolean customerService) {
        if (userId == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (merchant) {
            Merchant owned = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                    .eq(Merchant::getUser_id_wsh, userId)
                    .last("LIMIT 1"));
            if (owned != null && owned.getId_wsh() != null) {
                ids.add(owned.getId_wsh());
            }
        }
        if (customerService) {
            Set<Long> approved = merchantCustomerServiceService.getApprovedMerchantIds(userId);
            if (approved != null) {
                ids.addAll(approved);
            }
        }
        return ids;
    }

    /**
     * 【能否管理某商家下的问题单】
     *
     * <p>业务作用：受理、处理、驳回、指派、解决、关闭等**写操作**前的权限判定。</p>
     *
     * <p>业务规则：管理员直接通过；否则问题单的 merchant_id_wsh 必须落在可管理商家集合内。
     * 问题单未关联商家（merchant_id_wsh 为空）时，只有管理员能管理——沿用既有行为。</p>
     *
     * @param merchantId      问题单归属商家ID，可为 null
     * @param userId          操作人ID
     * @param admin           是否管理员
     * @param merchant        是否商家角色
     * @param customerService 是否客服角色
     * @return 有权限返回 true
     */
    public boolean canManageMerchant(Long merchantId, Long userId,
                                     boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return true;
        }
        return merchantId != null
                && staffMerchantIds(userId, merchant, customerService).contains(merchantId);
    }

    /**
     * 【能否查看某问题单】
     *
     * <p>业务作用：详情与消息列表等**读操作**前的可见性判定。</p>
     *
     * <p>业务规则：管理员通过；发起人本人通过；否则按商家范围判定。
     * 与 {@link #canManageMerchant} 的唯一区别是「发起人本人可读但不可写」。</p>
     *
     * @param ownerId         问题单发起人ID
     * @param merchantId      归属商家ID，可为 null
     * @param userId          当前用户ID
     * @param admin           是否管理员
     * @param merchant        是否商家角色
     * @param customerService 是否客服角色
     * @return 可见返回 true
     */
    public boolean canView(Long ownerId, Long merchantId, Long userId,
                           boolean admin, boolean merchant, boolean customerService) {
        if (admin) {
            return true;
        }
        if (ownerId != null && ownerId.equals(userId)) {
            return true;
        }
        return canManageMerchant(merchantId, userId, false, merchant, customerService);
    }
}
