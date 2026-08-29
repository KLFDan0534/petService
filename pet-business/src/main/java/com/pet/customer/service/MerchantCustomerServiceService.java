package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
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
     * 【业务名称】申请成为客服
     * 业务作用：用户申请成为指定商家的客服。
     * 如果已存在被拒绝/辞职/终止的记录，会复用并重置为待审核状态。
     * 调用场景：用户向商家提交客服申请。
     * 调用链：apply() → 查重 → 构造/复用记录 → insert/update → 返回DTO。
     * 数据处理：查重后创建或复用申请记录，状态设为 pending。
     * 业务规则：商家需存在且状态正常；用户不可重复申请已生效的客服。
     * 状态影响：新增或更新一条申请记录（状态 pending）。
     * 异常情况：商家不存在或状态异常抛 BusinessException；用户已是该商家客服抛异常。
     * 注意事项：商家主人无需申请即可管理。
     *
     * @param userId 申请用户ID
     * @param request 申请请求，包含商家ID和申请备注
     * @return 申请记录DTO
     */
    MerchantCustomerServiceDTO apply(Long userId, MerchantCustomerServiceApplyRequestDTO request);

    /**
     * 【业务名称】查询我的申请记录
     * 业务作用：查询当前用户的所有客服申请记录，按创建时间倒序。
     * 调用场景：用户查看自己的申请记录。
     * 调用链：listMine() → Mapper.selectList()。
     * 数据处理：按 user_id 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：包含所有历史状态。
     *
     * @param userId 用户ID
     * @return 申请记录DTO列表，按创建时间倒序
     */
    List<MerchantCustomerServiceDTO> listMine(Long userId);

    /**
     * 【业务名称】商家查询待审核申请
     * 业务作用：商家查询自己商铺待审核的客服申请列表。
     * 调用场景：商家审核客服申请。
     * 调用链：listPendingForMerchant() → Mapper.selectList()。
     * 数据处理：按 merchant_user_id 和 status=pending 匹配。
     * 业务规则：仅商家主人可查询。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param merchantUserId 商家主人用户ID
     * @return 待审核申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> listPendingForMerchant(Long merchantUserId);

    /**
     * 【业务名称】商家查询已通过客服
     * 业务作用：商家查询自己商铺已通过的客服列表。
     * 调用场景：商家管理客服团队。
     * 调用链：listApprovedForMerchant() → Mapper.selectList()。
     * 数据处理：按 merchant_user_id 和 status=approved 匹配。
     * 业务规则：仅商家主人可查询。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param merchantUserId 商家主人用户ID
     * @return 已通过申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> listApprovedForMerchant(Long merchantUserId);

    /**
     * 【业务名称】通过客服申请
     * 业务作用：商家通过客服申请，自动为用户授予 CUSTOMER_SERVICE 角色。
     * 调用场景：商家审批通过客服申请。
     * 调用链：approve() → 查记录 → 校验 → update → 授予角色。
     * 数据处理：更新状态为 approved，设置审核备注；授予 CUSTOMER_SERVICE 角色。
     * 业务规则：仅 pending 状态可审批；仅商家主人可操作。
     * 状态影响：申请记录状态 approved；用户新增 CUSTOMER_SERVICE 角色。
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
     * 【业务名称】拒绝客服申请
     * 业务作用：商家拒绝客服申请。
     * 调用场景：商家拒绝客服申请。
     * 调用链：reject() → 查记录 → 校验 → update。
     * 数据处理：更新状态为 rejected。
     * 业务规则：仅 pending 状态可拒绝；仅商家主人可操作。
     * 状态影响：申请记录状态 rejected。
     * 异常情况：申请不存在或状态不对抛异常。
     * 注意事项：无。
     *
     * @param id 申请记录ID
     * @param merchantUserId 商家主人用户ID
     * @param request 审核请求（含审核备注）
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO reject(Long id, Long merchantUserId, MerchantCustomerServiceReviewRequestDTO request);

    /**
     * 【业务名称】客服辞职
     * 业务作用：客服自己申请辞职，自动回收 CUSTOMER_SERVICE 角色。
     * 调用场景：客服主动辞职。
     * 调用链：resign() → 查记录 → 校验 → update → 回收角色。
     * 数据处理：更新状态为 resigned；如果该用户再无其他商家客服角色则回收 CUSTOMER_SERVICE。
     * 业务规则：仅申请者本人可操作。
     * 状态影响：申请记录状态 resigned；可能回收 CUSTOMER_SERVICE 角色。
     * 异常情况：申请不存在或非本人抛异常。
     * 注意事项：自动判断是否回收角色。
     *
     * @param id 申请记录ID
     * @param userId 当前用户ID（必须是申请者本人）
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO resign(Long id, Long userId);

    /**
     * 【业务名称】商家终止客服
     * 业务作用：商家终止客服的合作关系，自动回收 CUSTOMER_SERVICE 角色。
     * 调用场景：商家终止客服。
     * 调用链：terminateByMerchant() → 查记录 → 校验 → update → 回收角色。
     * 数据处理：更新状态为 terminated；如果该用户再无其他商家客服角色则回收 CUSTOMER_SERVICE。
     * 业务规则：仅商家主人可操作。
     * 状态影响：申请记录状态 terminated；可能回收 CUSTOMER_SERVICE 角色。
     * 异常情况：申请不存在或非商家主人抛异常。
     * 注意事项：自动判断是否回收角色。
     *
     * @param id 申请记录ID
     * @param merchantUserId 商家主人用户ID
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO terminateByMerchant(Long id, Long merchantUserId);

    /**
     * 【业务名称】获取用户已通过的商家ID
     * 业务作用：获取用户所有已通过审核的商家ID列表。
     * 调用场景：查询用户可管理的商家范围。
     * 调用链：getApprovedMerchantIds() → Mapper.selectList()。
     * 数据处理：按 user_id 和 status=approved 筛选。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于权限判断。
     *
     * @param userId 用户ID
     * @return 商家ID集合
     */
    Set<Long> getApprovedMerchantIds(Long userId);

    /**
     * 【业务名称】判断是否指定商家的客服
     * 业务作用：判断用户是否为指定商家的已授权客服。
     * 调用场景：权限校验。
     * 调用链：isMerchantCustomerService() → Mapper.selectCount()。
     * 数据处理：按 userId + merchantId + approved 统计。
     * 业务规则：只统计 approved 状态。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于权限判断。
     *
     * @param userId 用户ID
     * @param merchantId 商家ID
     * @return 如果是该商家的客服返回 true
     */
    boolean isMerchantCustomerService(Long userId, Long merchantId);

    /**
     * 【业务名称】查询指定商家的已授权客服用户ID集合
     * 业务作用：获取服务指定商家的所有已通过审核客服的用户ID。
     * 调用场景：新工单/新投诉创建时通知服务该商家的客服。
     * 调用链：getApprovedCsUserIds() → Mapper.selectList()。
     * 数据处理：按 merchant_id + approved 状态筛选。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：用于提醒推送。
     *
     * @param merchantId 商家ID
     * @return 该商家的已授权客服用户ID集合
     */
    Set<Long> getApprovedCsUserIds(Long merchantId);

    /**
     * 【业务名称】管理员分页查询全平台客服申请
     * 业务作用：管理员查看全平台所有商家的客服申请列表（可按状态筛选）。
     * 调用场景：管理后台客服审核列表页。
     * 调用链：pageAll() → Mapper.selectPage()。
     * 数据处理：按状态精确筛选，按创建时间倒序分页。
     * 业务规则：status_wsh 为空时查询全部状态。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：仅供 ADMIN 角色调用。
     *
     * @param pageParam 分页参数
     * @param status_wsh 状态（可空，如 pending/approved/rejected）
     * @return 分页的申请记录实体
     */
    IPage<MerchantCustomerService> pageAll(PageRequestDTO pageParam, String status_wsh);

    /**
     * 【业务名称】管理员通过客服申请
     * 业务作用：管理员通过指定客服申请，自动为用户授予 CUSTOMER_SERVICE 角色。
     * 调用场景：管理后台审核客服申请通过。
     * 调用链：approveByAdmin() → 查记录 → 校验 → update → 授予角色。
     * 数据处理：仅 pending 可改为 approved，记录审核人、审核备注、审核时间。
     * 业务规则：不校验商家归属，管理员可审批全平台申请。
     * 状态影响：申请记录状态 approved；用户新增 CUSTOMER_SERVICE 角色。
     * 异常情况：申请不存在抛 404；状态非 pending 抛 400。
     * 注意事项：仅供 ADMIN 角色调用。
     *
     * @param id 申请记录ID
     * @param adminUserId 管理员用户ID
     * @param reviewNote 审核备注
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO approveByAdmin(Long id, Long adminUserId, String reviewNote);

    /**
     * 【业务名称】管理员驳回客服申请
     * 业务作用：管理员驳回指定客服申请。
     * 调用场景：管理后台审核客服申请驳回。
     * 调用链：rejectByAdmin() → 查记录 → 校验 → update。
     * 数据处理：仅 pending 可改为 rejected，记录审核人、审核备注、审核时间。
     * 业务规则：不校验商家归属，管理员可审批全平台申请。
     * 状态影响：申请记录状态 rejected。
     * 异常情况：申请不存在抛 404；状态非 pending 抛 400。
     * 注意事项：仅供 ADMIN 角色调用。
     *
     * @param id 申请记录ID
     * @param adminUserId 管理员用户ID
     * @param reviewNote 审核备注
     * @return 更新后的申请记录DTO
     */
    MerchantCustomerServiceDTO rejectByAdmin(Long id, Long adminUserId, String reviewNote);

    /**
     * 【业务名称】申请记录实体转DTO
     * 业务作用：将单个申请记录实体转换为DTO，关联商家名称和用户信息。
     * 调用场景：Controller 组装分页结果时批量转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝，关联查询商家名称、用户名和昵称。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param entity 申请记录实体
     * @return 申请记录DTO
     */
    MerchantCustomerServiceDTO toDTO(MerchantCustomerService entity);

    /**
     * 【业务名称】申请记录实体列表转DTO列表
     * 业务作用：将申请记录实体列表批量转换为DTO列表。
     * 调用场景：Controller 组装分页结果。
     * 调用链：toDTOList() → toDTO()。
     * 数据处理：遍历转换。
     * 业务规则：入参为 null 时返回空列表。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param list 申请记录实体列表
     * @return 申请记录DTO列表
     */
    List<MerchantCustomerServiceDTO> toDTOList(List<MerchantCustomerService> list);
}
