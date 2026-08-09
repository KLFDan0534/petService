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
     * 【业务名称】查询套餐列表
     * 业务作用：查询会员套餐列表，支持按状态筛选和 activeOnly 模式。
     * 调用场景：套餐管理列表展示。
     * 调用链：listPlans() → selectList() → toDTO()。
     * 数据处理：按状态和 activeOnly 过滤，按 sort_order、level、id 排序。
     * 业务规则：activeOnly=true 时忽略 status 参数，只返回启用的套餐。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param status     可选状态筛选（0=禁用，1=启用），activeOnly=true 时忽略
     * @param activeOnly 为 true 时只返回启用套餐
     * @return 套餐 DTO 列表
     */
    List<MemberPlanDTO> listPlans(Integer status, boolean activeOnly);

    /**
     * 【业务名称】查询套餐详情
     * 业务作用：根据 ID 查询单个套餐详情。
     * 调用场景：套餐详情展示。
     * 调用链：getPlan() → selectById() → toDTO()。
     * 数据处理：按 ID 精确查询。
     * 业务规则：套餐不存在抛异常。
     * 状态影响：无。
     * 异常情况：套餐不存在抛 BusinessException(404)。
     * 注意事项：无。
     *
     * @param id 套餐 ID
     * @return 套餐 DTO
     */
    MemberPlanDTO getPlan(Long id);

    /**
     * 【业务名称】创建套餐
     * 业务作用：创建新的会员套餐，含完整的字段校验。
     * 调用场景：管理员新增套餐。
     * 调用链：createPlan() → 校验 → ensureCodeAvailable() → insert()。
     * 数据处理：校验 code 唯一性、价格、时长、折扣率、JSON 配置等。
     * 业务规则：code 必须唯一且符合大写字母数字下划线模式；默认启用状态。
     * 状态影响：新增一条套餐记录。
     * 异常情况：code 重复抛 400；校验失败抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param request 创建请求
     * @return 创建成功的套餐 DTO
     */
    MemberPlanDTO createPlan(MemberPlanCreateRequestDTO request);

    /**
     * 【业务名称】更新套餐
     * 业务作用：更新已有套餐的信息，仅更新非 null 字段。
     * 调用场景：管理员编辑套餐。
     * 调用链：updatePlan() → requirePlan() → 按需更新字段 → updateById()。
     * 数据处理：校验 code 唯一性（如变更）→ 逐字段更新。
     * 业务规则：仅非 null 字段生效；code 变更时校验唯一性。
     * 状态影响：更新套餐记录。
     * 异常情况：code 重复抛 400；校验失败抛 400。
     * 注意事项：@Transactional 保证事务一致性。
     *
     * @param id      套餐 ID
     * @param request 更新请求
     * @return 更新后的套餐 DTO
     */
    MemberPlanDTO updatePlan(Long id, MemberPlanUpdateRequestDTO request);

    /**
     * 【业务名称】更新套餐状态
     * 业务作用：启用或禁用会员套餐。
     * 调用场景：管理员上架/下架套餐。
     * 调用链：updateStatus() → requirePlan() → updateById()。
     * 数据处理：更新状态字段。
     * 业务规则：仅支持 0（禁用）和 1（启用）。
     * 状态影响：套餐启用/禁用状态变更。
     * 异常情况：不支持的 status 抛 400。
     * 注意事项：禁用后用户不可购买该套餐。
     *
     * @param id     套餐 ID
     * @param status 新状态（0=禁用，1=启用）
     * @return 更新后的套餐 DTO
     */
    MemberPlanDTO updateStatus(Long id, Integer status);

    /**
     * 【业务名称】删除套餐
     * 业务作用：删除会员套餐。
     * 如果套餐已关联会员或订单记录则不可删除（应改禁用）。
     * 调用场景：管理员删除套餐。
     * 调用链：deletePlan() → requirePlan() → 检查关联 → deleteById()。
     * 数据处理：检查是否有关联的会员或订单记录。
     * 业务规则：有关联记录时禁止删除，应改为禁用状态。
     * 状态影响：物理删除套餐记录。
     * 异常情况：有关联记录抛 BusinessException(400)。
     * 注意事项：物理删除不可恢复，有关联时应禁用而非删除。
     *
     * @param id 套餐 ID
     */
    void deletePlan(Long id);

    /**
     * 【业务名称】套餐实体转 DTO
     * 业务作用：将套餐实体转换为 DTO。
     * 调用场景：内部转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝。
     * 业务规则：入参为 null 时返回 null。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param plan 套餐实体
     * @return 套餐 DTO
     */
    MemberPlanDTO toDTO(MemberPlan plan);
}
