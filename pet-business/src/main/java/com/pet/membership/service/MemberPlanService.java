package com.pet.membership.service;

import com.pet.membership.dto.MemberPlanCreateRequestDTO;
import com.pet.membership.dto.MemberPlanDTO;
import com.pet.membership.dto.MemberPlanUpdateRequestDTO;
import com.pet.membership.entity.MemberPlan;

import java.util.List;

/**
 * 【业务模块】会员套餐管理
 * 业务作用：提供会员套餐的模板 CRUD 管理，包括创建、更新、启用/禁用、删除。
 * 套餐包含价格、时长、折扣率、优惠券配置和权益配置。
 */
public interface MemberPlanService {
    /**
     * 查询套餐列表
     *
     * 业务作用：查询会员套餐列表，支持按状态筛选和 activeOnly 模式。
     * 业务规则：activeOnly=true 时忽略 status 参数，只返回启用的套餐。
     *
     * @param status     可选状态筛选（0=禁用，1=启用），activeOnly=true 时忽略
     * @param activeOnly 为 true 时只返回启用套餐
     * @return 套餐 DTO 列表
     */
    List<MemberPlanDTO> listPlans(Integer status, boolean activeOnly);

    /**
     * 查询套餐详情
     *
     * 业务作用：根据 ID 查询单个套餐详情。
     * 业务规则：套餐不存在抛异常。
     * 异常情况：套餐不存在抛 BusinessException(404)。
     *
     * @param id 套餐 ID
     * @return 套餐 DTO
     */
    MemberPlanDTO getPlan(Long id);

    /**
     * 创建套餐
     *
     * 业务作用：创建新的会员套餐，含完整的字段校验。
     * 业务规则：code 必须唯一且符合大写字母数字下划线模式；默认启用状态。
     * 异常情况：code 重复抛 400；校验失败抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param request 创建请求
     * @return 创建成功的套餐 DTO
     */
    MemberPlanDTO createPlan(MemberPlanCreateRequestDTO request);

    /**
     * 更新套餐
     *
     * 业务作用：更新已有套餐的信息，仅更新非 null 字段。
     * 业务规则：仅非 null 字段生效；code 变更时校验唯一性。
     * 异常情况：code 重复抛 400；校验失败抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param id      套餐 ID
     * @param request 更新请求
     * @return 更新后的套餐 DTO
     */
    MemberPlanDTO updatePlan(Long id, MemberPlanUpdateRequestDTO request);

    /**
     * 更新套餐状态
     *
     * 业务作用：启用或禁用会员套餐。
     * 业务规则：仅支持 0（禁用）和 1（启用）。
     * 异常情况：不支持的 status 抛 400。
     * 注意事项：禁用后用户不可购买该套餐。
     *
     * @param id     套餐 ID
     * @param status 新状态（0=禁用，1=启用）
     * @return 更新后的套餐 DTO
     */
    MemberPlanDTO updateStatus(Long id, Integer status);

    /**
     * 删除套餐
     *
     * 业务作用：删除会员套餐。 如果套餐已关联会员或订单记录则不可删除（应改禁用）。
     * 业务规则：有关联记录时禁止删除，应改为禁用状态。
     * 异常情况：有关联记录抛 BusinessException(400)。
     * 注意事项：物理删除不可恢复，有关联时应禁用而非删除。
     *
     * @param id 套餐 ID
     */
    void deletePlan(Long id);

    /**
     * 套餐实体转 DTO
     *
     * 业务作用：将套餐实体转换为 DTO。
     * 业务规则：入参为 null 时返回 null。
     *
     * @param plan 套餐实体
     * @return 套餐 DTO
     */
    MemberPlanDTO toDTO(MemberPlan plan);
}
