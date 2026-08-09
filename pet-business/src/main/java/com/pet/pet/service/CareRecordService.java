package com.pet.pet.service;

import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordUpdateRequestDTO;
import com.pet.pet.entity.CareRecord;

import java.util.List;

public interface CareRecordService {
    /**
     * 【业务名称】按订单查询护理记录（无权限）
     * 业务作用：根据订单 ID 获取该订单的全部护理记录，按记录时间倒序排列。
     * 调用场景：内部或管理员查询护理时间线。
     * 调用链：CareRecordService.listByOrder(Long) → CareRecordMapper.selectList() → toDTOList()。
     * 数据处理：按 order_id 精确匹配，按 record_time 倒序。
     * 业务规则：无权限校验，适用于管理员和内部调用。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：不校验用户身份，谨慎暴露。
     *
     * @param orderId 订单 ID
     * @return 护理记录 DTO 列表
     */
    List<CareRecordDTO> listByOrder(Long orderId);

    /**
     * 【业务名称】按订单查询护理记录（带权限）
     * 业务作用：根据订单 ID 获取护理记录列表，非管理员时校验当前用户身份。
     * 调用场景：用户查看某订单的护理记录。
     * 调用链：CareRecordService.listByOrder(actorUserId, admin, orderId) → requireOrder() → requireReadableAccess() → listByOrder(orderId)。
     * 数据处理：同无权限版本，额外执行权限校验。
     * 业务规则：非管理员时，宠物主人、分配看护者或商家均可读取。
     * 状态影响：无。
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
     * 【业务名称】护理记录详情查询（无权限）
     * 业务作用：根据记录 ID 获取护理记录详情，不存在时抛异常。
     * 调用场景：内部调用或管理员查看。
     * 调用链：CareRecordService.getById(Long) → CareRecordMapper.selectById() → toDTO()。
     * 数据处理：主键查询。
     * 业务规则：无权限校验。
     * 状态影响：无。
     * 异常情况：不存在时抛出 BusinessException("记录不存在")。
     * 注意事项：无权限校验。
     *
     * @param id 记录 ID
     * @return 护理记录 DTO，不存在时抛出 BusinessException
     */
    CareRecordDTO getById(Long id);

    /**
     * 【业务名称】护理记录详情查询（带权限）
     * 业务作用：根据记录 ID 获取护理记录详情，非管理员时校验权限。
     * 调用场景：用户查看某条护理记录详情。
     * 调用链：CareRecordService.getById(actorUserId, admin, id) → getByIdRaw() → 权限校验 → toDTO()。
     * 数据处理：主键查询，权限校验。
     * 业务规则：非管理员时，用户必须是宠物主人、分配看护者或商家。
     * 状态影响：无。
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
     * 【业务名称】创建护理记录（无权限）
     * 业务作用：创建护理记录，订单 ID 必填，宠物 ID 和看护者 ID 从订单继承。
     * 调用场景：内部调用或管理员创建。
     * 调用链：CareRecordService.create(CareRecordCreateRequestDTO) → 校验 → CareRecordMapper.insert() → toDTO()。
     * 数据处理：从请求复制字段，宠物/看护者 ID 优先取请求，未指定时从订单继承；记录时间未指定时取当前时间。
     * 业务规则：无权限校验。
     * 状态影响：新增一条护理记录。
     * 异常情况：订单 ID 为空时抛 BusinessException("订单ID不能为空")；订单不存在时抛异常。
     * 注意事项：无权限校验。
     *
     * @param request 创建请求
     * @return 创建完成后的护理记录 DTO
     */
    CareRecordDTO create(CareRecordCreateRequestDTO request);

    /**
     * 【业务名称】创建护理记录（带权限与考勤）
     * 业务作用：创建护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者提交日常护理记录。
     * 调用链：CareRecordService.create(actorUserId, admin, request) → 校验权限 → 校验考勤 → 插入 → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可创建；看护者需处于值班状态；订单状态需为 received 或 in_progress。
     * 状态影响：新增一条护理记录。
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
     * 【业务名称】更新护理记录（无权限）
     * 业务作用：更新护理记录，仅更新请求中非 null 字段。
     * 调用场景：内部调用或管理员更新。
     * 调用链：CareRecordService.update(Long, CareRecordUpdateRequestDTO) → getByIdRaw() → applyUpdate() → CareRecordMapper.updateById() → toDTO()。
     * 数据处理：仅更新类型、内容、图片、记录时间中非 null 字段。
     * 业务规则：无权限校验。
     * 状态影响：更新护理记录。
     * 异常情况：记录不存在时抛异常。
     * 注意事项：无权限校验。
     *
     * @param id      记录 ID
     * @param request 更新的字段
     * @return 更新后的护理记录 DTO
     */
    CareRecordDTO update(Long id, CareRecordUpdateRequestDTO request);

    /**
     * 【业务名称】更新护理记录（带权限与考勤）
     * 业务作用：更新护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者修改护理记录。
     * 调用链：CareRecordService.update(actorUserId, admin, id, request) → getByIdRaw() → 权限校验 → 考勤校验 → applyUpdate() → updateById() → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可更新，且需处于可写入订单状态和值班中。
     * 状态影响：更新护理记录。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param id          记录 ID
     * @param request     更新的字段
     * @return 更新后的护理记录 DTO
     */
    CareRecordDTO update(Long actorUserId, boolean admin, Long id, CareRecordUpdateRequestDTO request);

    /**
     * 【业务名称】删除护理记录（无权限）
     * 业务作用：删除护理记录。
     * 调用场景：内部或管理员删除。
     * 调用链：CareRecordService.delete(Long) → CareRecordMapper.deleteById()。
     * 数据处理：物理删除。
     * 业务规则：无权限校验。
     * 状态影响：删除护理记录。
     * 异常情况：无。
     * 注意事项：无权限校验。
     *
     * @param id 记录 ID
     */
    void delete(Long id);

    /**
     * 【业务名称】删除护理记录（带权限与考勤）
     * 业务作用：删除护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者删除护理记录。
     * 调用链：CareRecordService.delete(actorUserId, admin, id) → getByIdRaw() → 权限校验 → 考勤校验 → deleteById()。
     * 数据处理：物理删除。
     * 业务规则：非管理员时仅分配看护者或商家可删除，且需处于可写入订单状态和值班中。
     * 状态影响：删除护理记录。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     *
     * @param actorUserId 当前操作用户 ID
     * @param admin       是否管理员
     * @param id          记录 ID
     */
    void delete(Long actorUserId, boolean admin, Long id);
}
