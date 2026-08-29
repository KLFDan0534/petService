package com.pet.boarding.service;

import com.pet.boarding.dto.KeeperLeaveCreateRequestDTO;
import com.pet.boarding.dto.KeeperLeaveDTO;
import com.pet.boarding.entity.KeeperLeave;
import com.pet.common.PageRequestDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDate;
import java.util.List;

/**
 * 看护者休假管理服务接口。
 * <p>
 * 商家为其旗下的看护者设置休假安排，休假期间看护者不能进行签到打卡和接单。
 * 休假以天为单位，支持跨天休假。系统提供休假重叠检测，防止重复设置。
 * <p>
 * <b>核心关系：</b>休假 → 考勤：休假中的看护者签到被拒绝；
 * 休假 → 订单：下订单时校验看护者在服务日期内是否可用。
 */
public interface KeeperLeaveService {

    /**
     * 查询某个商家下的所有休假记录。
     * <p>
     * <b>业务说明：</b>商家后台查看员工休假安排使用。
     *
     * @param merchantUserId 商家的用户ID
     * @return 休假记录DTO列表（含看护者名称、商家名称）
     */
    List<KeeperLeaveDTO> listByMerchant(Long merchantUserId);

    /**
     * 商家为其看护者创建休假安排。
     * <p>
     * <b>前置条件：</b>
     * <ul>
     *   <li>商家只能为归属本商家的看护者设置休假</li>
     *   <li>休假日期不能与已有休假重叠</li>
     * </ul>
     * <b>请求校验：</b>
     * <ul>
     *   <li>看护者ID、开始/结束日期不能为空</li>
     *   <li>结束日期不能早于开始日期</li>
     * </ul>
     *
     * @param merchantUserId 商家的用户ID（用于校验商家身份和归属权）
     * @param request        休假创建请求DTO
     * @return 创建后的休假记录DTO
     * @throws BusinessException 如果商家不存在、非本商家看护者、日期重叠或参数校验失败
     */
    KeeperLeaveDTO createByMerchant(Long merchantUserId, KeeperLeaveCreateRequestDTO request);

    /**
     * 商家删除休假记录。
     * <p>
     * <b>前置条件：</b>只能删除本商家下的休假记录。
     *
     * @param merchantUserId 商家的用户ID
     * @param id             休假记录ID
     * @throws BusinessException 如果休假记录不存在或不属于该商家
     */
    void deleteByMerchant(Long merchantUserId, Long id);

    /**
     * 分页查询全部请假记录（管理员）。
     * <p>
     * <b>业务说明：</b>管理员后台审批请假使用。可按照审批状态精确过滤，
     * 状态为空时返回全部记录；按创建时间倒序排列。
     *
     * @param pageParam 分页参数
     * @param status    审批状态（pending/approved/rejected），可为 null 表示不过滤
     * @return 请假实体分页结果
     */
    IPage<KeeperLeave> pageAll(PageRequestDTO pageParam, String status);

    /**
     * 管理员审批通过请假申请。
     * <p>
     * <b>前置条件：</b>仅当请假状态为 {@code pending} 时才可将其改为 {@code approved}。
     *
     * @param id          请假记录ID
     * @param adminUserId 管理员用户ID
     * @param reason      审批备注（可为 null，仅记录日志）
     * @return 审批后的请假实体
     * @throws BusinessException 如果请假记录不存在（404）或非待审批状态（400）
     */
    KeeperLeave approve(Long id, Long adminUserId, String reason);

    /**
     * 管理员驳回请假申请。
     * <p>
     * <b>前置条件：</b>仅当请假状态为 {@code pending} 时才可将其改为 {@code rejected}。
     *
     * @param id          请假记录ID
     * @param adminUserId 管理员用户ID
     * @param reason      驳回原因（可为 null，仅记录日志）
     * @return 驳回后的请假实体
     * @throws BusinessException 如果请假记录不存在（404）或非待审批状态（400）
     */
    KeeperLeave reject(Long id, Long adminUserId, String reason);

    /**
     * 将请假实体转换为 DTO，并关联填充看护者名称和商家名称。
     * <p>
     * <b>业务说明：</b>供 Controller 在分页列表和审批结果返回时，将实体转换为带名称的 DTO。
     *
     * @param entity 请假实体
     * @return 请假DTO（含看护者名称、商家名称、审批状态）
     */
    KeeperLeaveDTO toDTO(KeeperLeave entity);

    /**
     * 判断指定看护者在指定日期是否处于休假状态。
     * <p>
     * <b>业务说明：</b>被考勤服务和其他服务调用，用于校验看护者是否可操作。
     *
     * @param keeperId 看护者ID
     * @param date     目标日期
     * @return true 表示该看护者在当天休假
     */
    boolean isKeeperOnLeave(Long keeperId, LocalDate date);

    /**
     * 检测指定看护者在指定日期范围内是否存在休假重叠。
     * <p>
     * <b>区间规则：</b>[startDate, endExclusive) 左闭右开区间，
     * 内部转换为 [startDate, endInclusive] 左闭右闭与休假记录比较。
     *
     * @param keeperId     看护者ID
     * @param startDate    开始日期（包含）
     * @param endExclusive 结束日期（不包含）
     * @return true 表示存在重叠的休假
     */
    boolean hasLeaveOverlap(Long keeperId, LocalDate startDate, LocalDate endExclusive);

    /**
     * 校验看护者在指定日期范围内是否可用，不可用时抛出异常。
     * <p>
     * <b>业务说明：</b>订单服务在接单或创建订单时调用，确保分配看护者在服务期内可用。
     *
     * @param keeperId     看护者ID
     * @param startDate    服务开始日期（包含）
     * @param endExclusive 服务结束日期（不包含）
     * @throws BusinessException 如果看护者在服务日期内有休假安排
     */
    void requireKeeperAvailable(Long keeperId, LocalDate startDate, LocalDate endExclusive);
}
