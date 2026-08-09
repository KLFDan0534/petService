package com.pet.marketing.service;

import com.pet.marketing.dto.CouponDiscountResult;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.dto.CouponQuoteDTO;
import com.pet.marketing.dto.CouponQuoteRequestDTO;
import com.pet.marketing.dto.CouponTemplateCreateRequestDTO;
import com.pet.marketing.dto.CouponTemplateDTO;
import com.pet.marketing.dto.UserCouponDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口，提供优惠券模板管理、优惠券发放（手动/全量/条件）、
 * 用户领券、订单优惠预览、锁定/使用/释放等完整生命周期管理。
 */
public interface CouponService {
    /**
     * 查询优惠券模板列表。
     *
     * @param activeOnly 是否仅返回当前有效的模板（启用 + 在有效期内）
     * @return 模板DTO列表，按创建时间倒序
     */
    List<CouponTemplateDTO> listTemplates(boolean activeOnly);

    /**
     * 创建优惠券模板。
     * <p>支持两种类型：amount（固定金额减免）和 percent（折扣率减免）。
     * 适用范围支持 platform（全平台）和 merchant（指定商家）。</p>
     *
     * @param adminId 创建人（管理员）ID
     * @param request 模板创建请求
     * @return 创建成功的模板实体
     */
    CouponTemplate createTemplate(Long adminId, CouponTemplateCreateRequestDTO request);

    /**
     * 更新优惠券模板的启用/禁用状态。
     *
     * @param templateId 模板ID
     * @param status 状态值（1=启用，0=禁用）
     * @return 更新后的模板实体
     */
    CouponTemplate updateTemplateStatus(Long templateId, Integer status);

    /**
     * 向指定用户发放优惠券。
     *
     * @param adminId 操作人（管理员）ID
     * @param templateId 模板ID
     * @param request 发放请求，包含用户ID和数量
     * @return 实际成功发放的数量
     */
    int grantToUser(Long adminId, Long templateId, CouponGrantRequestDTO request);

    /**
     * 向所有活跃用户发放优惠券。
     *
     * @param adminId 操作人（管理员）ID
     * @param templateId 模板ID
     * @param request 发放请求，包含数量
     * @return 实际成功发放的总数量
     */
    int grantToAllUsers(Long adminId, Long templateId, CouponGrantRequestDTO request);

    /**
     * 按条件筛选用户并发放优惠券。
     * <p>支持按注册时间、累计消费金额、订单数、宠物数、最后下单时间、
     * 包含/排除指定用户ID等条件维度进行筛选。</p>
     *
     * @param adminId 操作人（管理员）ID
     * @param templateId 模板ID
     * @param request 发放请求，包含筛选条件和数量
     * @return 实际成功发放的总数量
     */
    int grantByCondition(Long adminId, Long templateId, CouponGrantRequestDTO request);

    /**
     * 用户自助领取优惠券。
     * <p>每用户限领数量由模板的 perUserLimit 控制。</p>
     *
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 领取成功的用户优惠券实体
     * @throws BusinessException 如果已达领取上限或库存不足
     */
    UserCoupon claim(Long userId, Long templateId);

    /**
     * 查询用户拥有的所有优惠券（含已用、已过期等）。
     *
     * @param userId 用户ID
     * @return 用户优惠券DTO列表
     */
    List<UserCouponDTO> listMyCoupons(Long userId);

    /**
     * 查询用户在指定商家处可用于某订单金额的可用优惠券。
     * <p>过滤条件：状态可用、未过期、模板有效、适用范围匹配、满足使用门槛。</p>
     *
     * @param userId 用户ID
     * @param merchantId 商家ID（用于匹配适用的优惠券范围）
     * @param orderAmount 订单金额（用于匹配使用门槛）
     * @return 可用的用户优惠券DTO列表
     */
    List<UserCouponDTO> listAvailableCoupons(Long userId, Long merchantId, BigDecimal orderAmount);

    /**
     * 报价查询：预览优惠券在指定订单上的优惠效果。
     *
     * @param userId 用户ID
     * @param request 报价请求，包含用户优惠券ID和订单金额信息
     * @return 优惠报价DTO，包含各阶段金额明细
     */
    CouponQuoteDTO quote(Long userId, CouponQuoteRequestDTO request);

    /**
     * 预览优惠券在订单上的优惠金额。
     * <p>考虑长期入住折扣后的结算金额，计算优惠券减免金额和最终支付金额。
     * 校验优惠券状态、有效期、适用范围和使用门槛。</p>
     *
     * @param userId 用户ID
     * @param userCouponId 用户优惠券ID，为空则返回无优惠的结果
     * @param totalAmount 订单总金额
     * @param longStayDiscount 长期入住折扣金额
     * @param merchantId 商家ID
     * @param serviceId 服务ID
     * @return 优惠折扣结果，包含各阶段金额和快照
     * @throws BusinessException 如果优惠券不可用或不满足条件
     */
    CouponDiscountResult previewForOrder(Long userId,
                                         Long userCouponId,
                                         BigDecimal totalAmount,
                                         BigDecimal longStayDiscount,
                                         Long merchantId,
                                         Long serviceId);

    /**
     * 为订单锁定优惠券。
     * <p>将优惠券状态从 "available" 变为 "locked"，
     * 并记录关联的订单号和优惠金额。</p>
     *
     * @param userId 用户ID
     * @param userCouponId 用户优惠券ID
     * @param orderId 订单ID
     * @param orderNo 订单号
     * @param discountAmount 优惠金额
     * @throws BusinessException 如果优惠金额无效或优惠券不再可用
     */
    void lockForOrder(Long userId, Long userCouponId, Long orderId, String orderNo, BigDecimal discountAmount);

    /**
     * 将已锁定优惠券标记为已使用。
     * <p>创建优惠券使用记录（CouponUsage），记录使用详情。</p>
     *
     * @param orderId 订单ID
     * @param orderNo 订单号
     */
    void markUsedForOrder(Long orderId, String orderNo);

    /**
     * 释放订单关联的已锁定优惠券（订单取消时使用）。
     * <p>将优惠券状态从 "locked" 恢复为 "available"，释放锁定。</p>
     *
     * @param orderId 订单ID
     */
    void releaseForOrder(Long orderId);
}
