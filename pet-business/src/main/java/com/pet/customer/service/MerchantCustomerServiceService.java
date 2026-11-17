package com.pet.customer.service;

import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;
import com.pet.customer.entity.MerchantCustomerService;

import java.util.List;
import java.util.Set;

/**
 * 【业务模块】商家客服申请管理
 * 业务作用：管理用户向商家申请成为客服人员的全流程。
 * 用户可向已批准的商家提交客服申请，商家审核通过后用户获得 CUSTOMER_SERVICE 角色，
 * 从而可以协助管理工单和投诉。支持申请、审批、辞职、终止等操作。
 */
public interface MerchantCustomerServiceService {
    /** 申请状态：待审核 */
    String STATUS_PENDING = "pending";
    /** 申请状态：已通过 */
    String STATUS_APPROVED = "approved";
    /** 申请状态：已拒绝 */
    String STATUS_REJECTED = "rejected";
    /** 申请状态：已辞职 */
    String STATUS_RESIGNED = "resigned";
    /** 申请状态：已被商家终止 */
    String STATUS_TERMINATED = "terminated";

    /**
     * 申请成为客服
     *
     * 业务作用：用户申请成为指定商家的客服。 如果已存在被拒绝/辞职/终止的记录，会复用并重置为待审核状态。
     * 业务规则：商家需存在且状态正常；用户不可重复申请已生效的客服。
     * 异常情况：商家不存在或状态异常抛 BusinessException；用户已是该商家客服抛异常。
     * 注意事项：商家主人无需申请即可管理。
     *
     * @param userId 申请用户ID
     * @param request 申请请求，包含商家ID和申请备注
     * @return 申请记录DTO
     */
    MerchantCustomerServiceDTO apply(Long userId, MerchantCustomerServiceApplyRequestDTO request);

    /**
     * 查询我的申请记录
     *
     * 业务作用：查询当前用户的所有客服申请记录，按创建时间倒序。
     * 注意事项：包含所有历史状态。
     *
     * @param userId 用户ID
     * @return 申请记录DTO列表，按创建时间倒序
     */
    List<MerchantCustomerServiceDTO> listMine(Long userId);

    /**
     * 商家查询待审核申请
     *
     * 业务作用：商家查询自己商铺待审核的客服申请列表。
     * 业务规则：仅商家主人可查询。
     *
     * @param merchantUserId 商家主人用户ID
     * @return 待审核申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> listPendingForMerchant(Long merchantUserId);

    /**
     * 商家查询已通过客服
     *
     * 业务作用：商家查询自己商铺已通过的客服列表。
     * 业务规则：仅商家主人可查询。
     *
     * @param merchantUserId 商家主人用户ID
     * @return 已通过申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> listApprovedForMerchant(Long merchantUserId);

    /**
     * 通过客服申请
     *
     * 业务作用：商家通过客服申请，自动为用户授予 CUSTOMER_SERVICE 角色。
     * 业务规则：仅 pending 状态可审批；仅商家主人可操作。
     * 异常情况：申请不存在或状态不对抛异常。
     * 注意事项：附带角色授权。
     *
     * @param id 申请记录ID
     * @param merchantUserId 商家主人用户ID
     * @param request 审核请求（含审核备注）
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO approve(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request);

    /**
     * 拒绝客服申请
     *
     * 业务作用：商家拒绝客服申请。
     * 业务规则：仅 pending 状态可拒绝；仅商家主人可操作。
     * 异常情况：申请不存在或状态不对抛异常。
     *
     * @param id 申请记录ID
     * @param merchantUserId 商家主人用户ID
     * @param request 审核请求（含审核备注）
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO reject(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request);

    /**
     * 超时自动拒绝客服申请
     *
     * 业务作用：
     * 为「商家长期不处理」提供平台兜底。客服申请的唯一审核人是被申请的商家，
     * 商家若一直不处理，申请会永久停留在 pending，申请人拿不到结果。
     * 本方法把停留超过 timeoutDays 天的 pending 申请批量置为 rejected。
     * 业务规则：仅 pending 可被自动拒绝；不校验商家归属（系统行为）； reviewer_id_wsh 保持为 null，表示无人工审核人。
     * 异常情况：timeoutDays 非正数抛 400。
     * 注意事项：申请人可重新提交申请（apply() 会复用该记录并重置为 pending）。
     *
     * @param timeoutDays 允许商家处理的天数上限，必须为正数
     * @return 本次自动拒绝的记录条数
     */
    int autoRejectStalePending(int timeoutDays);

    /**
     * 客服辞职
     *
     * 业务作用：客服自己申请辞职，自动回收 CUSTOMER_SERVICE 角色。
     * 业务规则：仅申请者本人可操作。
     * 异常情况：申请不存在或非本人抛异常。
     * 注意事项：自动判断是否回收角色。
     *
     * @param id 申请记录ID
     * @param userId 当前用户ID（必须是申请者本人）
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO resign(Long id, Long userId);

    /**
     * 商家终止客服
     *
     * 业务作用：商家终止客服的合作关系，自动回收 CUSTOMER_SERVICE 角色。
     * 业务规则：仅商家主人可操作。
     * 异常情况：申请不存在或非商家主人抛异常。
     * 注意事项：自动判断是否回收角色。
     *
     * @param id 申请记录ID
     * @param merchantUserId 商家主人用户ID
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO terminateByMerchant(Long id, Long merchantUserId);

    /**
     * 获取用户已通过的商家ID
     *
     * 业务作用：获取用户所有已通过审核的商家ID列表。
     * 注意事项：用于权限判断。
     *
     * @param userId 用户ID
     * @return 商家ID集合
     */
    Set<Long> getApprovedMerchantIds(Long userId);

    /**
     * 判断是否指定商家的客服
     *
     * 业务作用：判断用户是否为指定商家的已授权客服。
     * 业务规则：只统计 approved 状态。
     * 注意事项：用于权限判断。
     *
     * @param userId 用户ID
     * @param merchantId 商家ID
     * @return 如果是该商家的客服返回 true
     */
    boolean isMerchantCustomerService(Long userId, Long merchantId);

    /**
     * 查询指定商家的已授权客服用户ID集合
     *
     * 业务作用：获取服务指定商家的所有已通过审核客服的用户ID。
     * 注意事项：用于提醒推送。
     *
     * @param merchantId 商家ID
     * @return 该商家的已授权客服用户ID集合
     */
    Set<Long> getApprovedCsUserIds(Long merchantId);

    /**
     * 申请记录实体转DTO
     *
     * 业务作用：将单个申请记录实体转换为DTO，关联商家名称和用户信息。
     * 业务规则：入参为 null 时返回 null。
     *
     * @param entity 申请记录实体
     * @return 申请记录DTO
     */
    MerchantCustomerServiceDTO toDTO(MerchantCustomerService entity);

    /**
     * 申请记录实体列表转DTO列表
     *
     * 业务作用：将申请记录实体列表批量转换为DTO列表。
     * 业务规则：入参为 null 时返回空列表。
     *
     * @param list 申请记录实体列表
     * @return 申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> toDTOList(List<MerchantCustomerService> list);
}
