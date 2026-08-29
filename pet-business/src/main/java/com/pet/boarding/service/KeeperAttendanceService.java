package com.pet.boarding.service;

import com.pet.boarding.dto.AttendanceCheckRequestDTO;
import com.pet.boarding.dto.KeeperAttendanceDTO;
import com.pet.boarding.entity.KeeperAttendance;
import com.pet.common.PageRequestDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 看护者签到/签退服务接口。
 * <p>
 * 看护者每天上班时需要在商家附近指定范围内进行地理定位签到（check-in），
 * 下班时签退（check-out）。系统通过 GPS 坐标校验看护者是否在商家附近。
 * 签到/签退记录用于追踪看护者的考勤状态。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *   <li>上下班打卡（含地理围栏校验）</li>
 *   <li>查询当前班次状态</li>
 *   <li>查询当天考勤记录</li>
 *   <li>校验看护者是否在岗（被订单系统等调用）</li>
 * </ul>
 */
public interface KeeperAttendanceService {

    /**
     * 看护者上班签到（check-in）。
     * <p>
     * <b>前置条件：</b>
     * <ul>
     *   <li>看护者须已通过审核（ACTIVE / OFFLINE / BUSY）</li>
     *   <li>看护者所属商家须已通过审核</li>
     *   <li>看护者当天未被商家设置休假</li>
     *   <li>看护者当前没有未签退的班次</li>
     *   <li>签到位置须在商家设定的打卡半径范围内</li>
     * </ul>
     * <p>
     * <b>数据流：</b>
     * <ol>
     *   <li>校验用户身份 → 查找看护者</li>
     *   <li>校验商家状态和打卡范围</li>
     *   <li>校验休假和已有班次</li>
     *   <li>生成签到记录（含定位坐标、地址、精度、距离）</li>
     * </ol>
     *
     * @param userId  操作用户ID
     * @param request 签到请求（含经纬度、地址、定位精度）
     * @return 签到详情DTO
     * @throws BusinessException 如果未登录、看护者未审核、商家未审核、超出打卡范围等
     */
    KeeperAttendanceDTO checkIn(Long userId, AttendanceCheckRequestDTO request);

    /**
     * 看护者下班签退（check-out）。
     * <p>
     * <b>前置条件：</b>
     * <ul>
     *   <li>看护者当前须有未签退的班次</li>
     *   <li>签退位置须在商家设定的打卡半径范围内</li>
     * </ul>
     * <p>
     * <b>数据流：</b>
     * <ol>
     *   <li>查找当前活跃班次（check_out_at IS NULL）</li>
     *   <li>校验定位信息</li>
     *   <li>更新签退时间和定位信息</li>
     * </ol>
     *
     * @param userId  操作用户ID
     * @param request 签退请求（含经纬度、地址、定位精度）
     * @return 签到详情DTO（含签到和签退信息）
     * @throws BusinessException 如果无活跃班次、超出打卡范围等
     */
    KeeperAttendanceDTO checkOut(Long userId, AttendanceCheckRequestDTO request);

    /**
     * 查询看护者当前的活跃班次（尚未签退）。
     * <p>
     * <b>业务说明：</b>如果看护者已签到但未签退，返回当前班次信息；否则返回 null。
     *
     * @param userId 用户ID
     * @return 当前签到DTO（含在岗状态 on_duty=true），无活跃班次时返回 null
     */
    KeeperAttendanceDTO current(Long userId);

    /**
     * 查询看护者当天所有的签到/签退记录。
     * <p>
     * <b>时间范围：</b>当天 00:00:00 ~ 次日 00:00:00，按签到时间倒序排列。
     *
     * @param userId 用户ID
     * @return 当天考勤记录DTO列表
     */
    List<KeeperAttendanceDTO> today(Long userId);

    /**
     * 查询某个商家下所有看护者当天的考勤记录。
     * <p>
     * <b>业务说明：</b>商家后台查看员工考勤使用。
     *
     * @param merchantUserId 商家的用户ID
     * @return 当天该商家所有看护者的考勤记录（含看护者名称）
     */
    List<KeeperAttendanceDTO> listMerchantToday(Long merchantUserId);

    /**
     * 分页查询全部考勤记录（管理员）。
     * <p>
     * <b>业务说明：</b>管理员后台查看出勤情况使用。可按照商家精确过滤，
     * 商家ID为空时返回全部记录；按创建时间倒序排列。
     *
     * @param pageParam  分页参数
     * @param merchantId 商家ID，可为 null 表示不过滤
     * @return 考勤实体分页结果
     */
    IPage<KeeperAttendance> pageAll(PageRequestDTO pageParam, Long merchantId);

    /**
     * 将考勤实体转换为 DTO，并关联填充看护者名称和商家名称。
     * <p>
     * <b>业务说明：</b>供 Controller 在分页列表返回时，将实体转换为带名称和上岗状态的 DTO。
     *
     * @param entity 考勤实体
     * @return 考勤DTO（含看护者名称、商家名称、上岗状态）
     */
    KeeperAttendanceDTO toDTO(KeeperAttendance entity);

    /**
     * 判断指定看护者在指定商家是否有活跃班次（已签到未签退）。
     * <p>
     * <b>业务说明：</b>被订单系统等其他服务调用，用于校验看护者是否在岗。
     *
     * @param keeperId   看护者ID
     * @param merchantId 商家ID
     * @return true 表示该看护者当前在岗
     */
    boolean hasActiveShift(Long keeperId, Long merchantId);

    /**
     * 校验看护者是否在岗且未休假，否则抛出异常。
     * <p>
     * <b>业务说明：</b>用于订单接单等操作前的权限校验。
     *
     * @param keeperId   看护者ID
     * @param merchantId 商家ID
     * @throws BusinessException 如果看护者未打卡上班或处于休假状态
     */
    void requireKeeperOnDuty(Long keeperId, Long merchantId);
}
