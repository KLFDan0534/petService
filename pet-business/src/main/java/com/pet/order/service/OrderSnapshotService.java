package com.pet.order.service;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderSnapshotDTO;
import com.pet.order.entity.OrderSnapshot;
import com.pet.order.entity.PetOrder;
import com.pet.order.pricing.NightPricingRule;
import com.pet.pet.entity.Pet;
import com.pet.system.entity.User;

import java.util.Map;
import java.util.Set;

/**
 * Service interface for order snapshot management.
 * <p>
 * Order snapshots capture immutable copies of all related entities (owner, pet,
 * merchant, keeper, service, address, pricing) at the time of order creation.
 * This ensures that historical order data remains accurate even if the referenced
 * entities are later updated. Snapshots are stored as JSON blobs.
 */
public interface OrderSnapshotService {

    /**
     * 快照内容：
     * - 主人信息（ID、账号、昵称、脱敏手机号）
     * - 宠物信息（名称、类型、品种、年龄、体重、性别、绝育/疫苗/过敏标记）
     * - 商家信息（名称、电话、地址、营业执照、评分）
     * - 看护者信息（名称、电话、经验、评分、价格、容量、简介）
     * - 服务信息（名称、描述、价格、单位）
     * - 地址信息（配送/接收地址、时间、紧急联系人）
     * - 价格明细（天数、单价、总价、折扣、优惠券、会员、最终金额）
     *
     * 业务作用：在订单创建时生成所有相关实体的不可变快照（JSON格式），确保历史订单数据不受后续实体变更影响。
     *
     * @param order   新创建的订单
     * @param owner   宠物主人用户
     * @param pet     宠物
     * @param merchant 商家
     * @param keeper  看护者
     * @param service 服务项（可为null）
     * @param request 原始创建请求
     * @return 创建的订单快照
     */
    OrderSnapshot createForOrder(PetOrder order,
                                 User owner,
                                 Pet pet,
                                 Merchant merchant,
                                 Keeper keeper,
                                 ServiceItem service,
                                 OrderCreateRequestDTO request);

    /**
     * 业务作用：根据一组订单ID批量查询快照DTO，用于列表页批量加载快照避免N+1查询。
     *
     * @param orderIds 订单ID集合
     * @return 订单ID到快照DTO的映射，无匹配时返回空Map
     */
    Map<Long, OrderSnapshotDTO> findDTOMapByOrderIds(Set<Long> orderIds);

    /**
     * 业务作用：下单时负责人可能为空（待分配），分配成功后把看护者身份写回快照， 否则新流程的订单快照里永远没有负责人，纠纷与结算取证链断裂。
     * 调用场景：OrderServiceImpl.assignTo 抢单/指派/超时自动分配成功后。
     *
     * @param orderId 订单ID
     * @param keeper  分配到的看护者
     */
    void refreshKeeperSnapshot(Long orderId, Keeper keeper);

    /**
     * 【读取并校验按夜计价快照】
     *
     * 业务作用：
     * 接走确认时按订单读取冻结的夜价、预订夜数与离店/免费锚点，用于计算实际晚接费。
     * 快照缺失、版本不符或字段格式错误时明确失败——绝不回读当前服务价兜底，
     * 否则商家改价会篡改历史订单（AE17）。
     *
     * @param orderId 订单ID
     * @return 冻结的按夜计价规则
     * @throws com.pet.common.BusinessException 快照缺失或格式错误
     */
    NightPricingRule requireNightPricingRule(Long orderId);
}
