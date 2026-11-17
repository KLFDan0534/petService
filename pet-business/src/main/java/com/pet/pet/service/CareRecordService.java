package com.pet.pet.service;

import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordRequestDTO;

import java.util.List;

public interface CareRecordService {
    /**
     * 按订单查询护理记录（无权限）
     *
     * 业务作用：根据订单 ID 获取该订单的全部护理记录，按记录时间倒序排列。
     * 业务规则：无权限校验，适用于管理员和内部调用。
     * 注意事项：不校验用户身份，谨慎暴露。
     *
     * @param orderId 订单 ID
     * @return 护理记录 DTO 列表
     */
    List<CareRecordDTO> listByOrder(Long orderId);

    /**
     * 按订单查询护理记录（带权限）
     *
     * 业务作用：根据订单 ID 获取护理记录列表，非管理员时校验当前用户身份。
     * 业务规则：非管理员时，宠物主人、分配看护者或商家均可读取。
     * 异常情况：无权限时抛出 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员（true 跳过权限校验）
     * @param orderId     订单 ID
     * @return 护理记录 DTO 列表
     */
    List<CareRecordDTO> listByOrder(Long actorUserId, boolean admin, Long orderId);
    /**
     * 护理记录详情查询（无权限）
     *
     * 业务作用：根据记录 ID 获取护理记录详情，不存在时抛异常。
     * 业务规则：无权限校验。
     * 异常情况：不存在时抛出 BusinessException("记录不存在")。
     * 注意事项：无权限校验。
     *
     * @param id 记录 ID
     * @return 护理记录 DTO，不存在时抛出 BusinessException
     */
    CareRecordDTO getById(Long id);

    /**
     * 护理记录详情查询（带权限）
     *
     * 业务作用：根据记录 ID 获取护理记录详情，非管理员时校验权限。
     * 业务规则：非管理员时，用户必须是宠物主人、分配看护者或商家。
     * 异常情况：无权限时抛出 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param id          记录 ID
     * @return 护理记录 DTO
     */
    CareRecordDTO getById(Long actorUserId, boolean admin, Long id);
    /**
     * 创建护理记录（无权限）
     *
     * 业务作用：创建护理记录，订单 ID 必填，宠物 ID 和看护者 ID 从订单继承。
     * 业务规则：无权限校验。
     * 异常情况：订单 ID 为空时抛 BusinessException("订单ID不能为空")；订单不存在时抛异常。
     * 注意事项：无权限校验。
     *
     * @param request 创建请求
     * @return 创建完成后的护理记录 DTO
     */
    CareRecordDTO create(CareRecordCreateRequestDTO request);

    /**
     * 创建护理记录（带权限与考勤）
     *
     * 业务作用：创建护理记录，非管理员时校验权限和考勤状态。
     * 业务规则：非管理员时仅分配看护者或商家可创建；看护者需处于值班状态；订单状态需为 received 或 in_progress。
     * 异常情况：无权限时抛 BusinessException(403)；考勤不通过时抛异常。
     * 注意事项：管理员跳过权限和考勤校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param request     创建请求
     * @return 创建完成后的护理记录 DTO
     */
    CareRecordDTO create(Long actorUserId, boolean admin, CareRecordCreateRequestDTO request);
    /**
     * 更新护理记录（无权限）
     *
     * 业务作用：更新护理记录，仅更新请求中非 null 字段。
     * 业务规则：无权限校验。
     * 异常情况：记录不存在时抛异常。
     * 注意事项：无权限校验。
     *
     * @param id      记录 ID
     * @param request 更新的字段
     * @return 更新后的护理记录 DTO
     */
    CareRecordDTO update(Long id, CareRecordRequestDTO request);

    /**
     * 更新护理记录（带权限与考勤）
     *
     * 业务作用：更新护理记录，非管理员时校验权限和考勤状态。
     * 业务规则：非管理员时仅分配看护者或商家可更新，且需处于可写入订单状态和值班中。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param id          记录 ID
     * @param request     更新的字段
     * @return 更新后的护理记录 DTO
     */
    CareRecordDTO update(Long actorUserId, boolean admin, Long id, CareRecordRequestDTO request);

    /**
     * 删除护理记录（无权限）
     *
     * 业务作用：删除护理记录。
     * 业务规则：无权限校验。
     * 注意事项：无权限校验。
     *
     * @param id 记录 ID
     */
    void delete(Long id);

    /**
     * 删除护理记录（带权限与考勤）
     *
     * 业务作用：删除护理记录，非管理员时校验权限和考勤状态。
     * 业务规则：非管理员时仅分配看护者或商家可删除，且需处于可写入订单状态和值班中。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param id          记录 ID
     */
    void delete(Long actorUserId, boolean admin, Long id);
}
