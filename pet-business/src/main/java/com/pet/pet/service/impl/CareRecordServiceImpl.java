package com.pet.pet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.BusinessException;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordUpdateRequestDTO;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.pet.service.CareRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CareRecordServiceImpl implements CareRecordService {

    private final CareRecordMapper careRecordMapper;
    private final OrderMapper orderMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final KeeperAttendanceService keeperAttendanceService;

    /**
     * 【护理记录服务实现的构造注入】
     *
     * 业务作用：注入护理记录、订单、寄养师、商家 Mapper 与考勤服务。
     *
     * 调用场景：Spring 容器创建 CareRecordServiceImpl 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 5 个 final 成员。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public CareRecordServiceImpl(CareRecordMapper careRecordMapper,
                                 OrderMapper orderMapper,
                                 KeeperMapper keeperMapper,
                                 MerchantMapper merchantMapper,
                                 KeeperAttendanceService keeperAttendanceService) {
        this.careRecordMapper = careRecordMapper;
        this.orderMapper = orderMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.keeperAttendanceService = keeperAttendanceService;
    }

    /**
     * 【业务名称】按订单查询护理记录（实现-无权限）
     * 业务作用：根据订单 ID 查询全部护理记录，按记录时间倒序。
     * 调用场景：内部、管理员查询。
     * 调用链：listByOrder(Long) → CareRecordMapper.selectList() → toDTOList()。
     * 数据处理：按 order_id 精确匹配，按 record_time 倒序。
     * 业务规则：无权限校验。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Override
    public List<CareRecordDTO> listByOrder(Long orderId) {
        return toDTOList(careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByDesc(CareRecord::getRecord_time_wsh)));
    }

    /**
     * 【业务名称】按订单查询护理记录（实现-带权限）
     * 业务作用：查询护理记录，非管理员时校验用户可读权限。
     * 调用场景：用户查看订单护理记录。
     * 调用链：listByOrder(Long, boolean, Long) → requireOrder() → requireReadableAccess() → listByOrder(orderId)。
     * 数据处理：委派给无权限版本。
     * 业务规则：非管理员时，宠物主人、分配看护者或商家可读取。
     * 状态影响：无。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
    @Override
    public List<CareRecordDTO> listByOrder(Long actorUserId, boolean admin, Long orderId) {
        PetOrder order = requireOrder(orderId);
        if (!admin) {
            requireReadableAccess(actorUserId, order);
        }
        return listByOrder(orderId);
    }

    /**
     * 【业务名称】护理记录详情查询（实现-无权限）
     * 业务作用：根据 ID 查询单条护理记录。
     * 调用场景：内部调用。
     * 调用链：getById(Long) → CareRecordMapper.selectById() → toDTO()。
     * 数据处理：主键查询。
     * 业务规则：不存在时抛异常。
     * 状态影响：无。
     * 异常情况：不存在时抛出 BusinessException("记录不存在")。
     * 注意事项：无。
     */
    @Override
    public CareRecordDTO getById(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return toDTO(record);
    }

    /**
     * 【业务名称】护理记录详情查询（实现-带权限）
     * 业务作用：根据 ID 查询单条护理记录，非管理员时校验权限。
     * 调用场景：用户查看护理记录详情。
     * 调用链：getById(Long, boolean, Long) → getByIdRaw() → requireReadableAccess() → toDTO()。
     * 数据处理：主键查询，权限校验。
     * 业务规则：非管理员时需满足可读权限（主人、看护者或商家）。
     * 状态影响：无。
     * 异常情况：无权限时抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
    @Override
    public CareRecordDTO getById(Long actorUserId, boolean admin, Long id) {
        CareRecord record = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(record.getOrder_id_wsh());
            requireReadableAccess(actorUserId, order);
        }
        return toDTO(record);
    }

    /**
     * 【业务名称】创建护理记录（实现-无权限）
     * 业务作用：创建护理记录，自动从订单继承宠物 ID 和看护者 ID。
     * 调用场景：内部、管理员创建。
     * 调用链：create(CareRecordCreateRequestDTO) → 校验订单 → 构造 → CareRecordMapper.insert() → toDTO()。
     * 数据处理：订单 ID 必填；宠物 ID 和看护者 ID 优先取请求，未指定时从订单继承；记录时间默认当前时间。
     * 业务规则：无权限校验。
     * 状态影响：新增一条护理记录。
     * 异常情况：订单 ID 为空抛 BusinessException；订单不存在抛 BusinessException。
     * 注意事项：无权限校验。
     */
    @Transactional
    @Override
    public CareRecordDTO create(CareRecordCreateRequestDTO request) {
        if (request.getOrder_id_wsh() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(request.getOrder_id_wsh());
        record.setPet_id_wsh(request.getPet_id_wsh() != null ? request.getPet_id_wsh() : order.getPet_id_wsh());
        record.setKeeper_id_wsh(request.getKeeper_id_wsh() != null ? request.getKeeper_id_wsh() : order.getKeeper_id_wsh());
        record.setType_wsh(request.getType_wsh());
        record.setContent_wsh(request.getContent_wsh());
        record.setImages_wsh(request.getImages_wsh());
        record.setRecord_time_wsh(request.getRecord_time_wsh() != null ? request.getRecord_time_wsh() : LocalDateTime.now());
        careRecordMapper.insert(record);
        return toDTO(record);
    }

    /**
     * 【业务名称】创建护理记录（实现-带权限与考勤）
     * 业务作用：创建护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者提交日常护理记录。
     * 调用链：create(Long, boolean, CareRecordCreateRequestDTO) → 校验订单 → requireWritableAccess() → requireKeeperOnDuty() → 插入 → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可创建；看护者需处于值班状态。
     * 状态影响：新增一条护理记录。
     * 异常情况：无权限抛 BusinessException(403)；考勤不通过抛异常。
     * 注意事项：管理员跳过权限和考勤校验。
     */
    @Transactional
    @Override
    public CareRecordDTO create(Long actorUserId, boolean admin, CareRecordCreateRequestDTO request) {
        if (request.getOrder_id_wsh() == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }
        PetOrder order = orderMapper.selectById(request.getOrder_id_wsh());
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        // 非管理员需过写权限与在岗校验
        if (!admin) {
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(request.getOrder_id_wsh());
        record.setPet_id_wsh(request.getPet_id_wsh() != null ? request.getPet_id_wsh() : order.getPet_id_wsh());
        record.setKeeper_id_wsh(request.getKeeper_id_wsh() != null ? request.getKeeper_id_wsh() : order.getKeeper_id_wsh());
        record.setType_wsh(request.getType_wsh());
        record.setContent_wsh(request.getContent_wsh());
        record.setImages_wsh(request.getImages_wsh());
        record.setRecord_time_wsh(request.getRecord_time_wsh() != null ? request.getRecord_time_wsh() : LocalDateTime.now());
        careRecordMapper.insert(record);
        return toDTO(record);
    }

    /**
     * 【校验护理记录写权限】
     *
     * 业务作用：确认操作人可维护该订单的护理记录，且订单状态允许写入。
     *
     * 调用场景：create、update、delete 的带权限版本在非管理员分支调用。
     *
     * 调用链：create()/update()/delete() → requireWritableAccess() → isAssignedKeeperOrMerchant() → isOrderServiceWritable()。
     *
     * 数据处理：先判断操作人是否为订单分配的寄养师或所属商家，再判断订单状态。
     *
     * 业务规则：订单状态仅 received 与 in_progress 可写护理记录；宠物主人不在可写范围内。
     *
     * 状态影响：无。
     *
     * 异常情况：非分配寄养师/商家抛 BusinessException(403)；订单状态不允许抛 BusinessException(400)。
     *
     * 注意事项：权限提示为英文，与项目其它中文提示不一致。
     */
    private void requireWritableAccess(Long actorUserId, PetOrder order) {
        if (!isAssignedKeeperOrMerchant(actorUserId, order)) {
            throw new BusinessException(403, "no permission to maintain care records for this order");
        }
        // 仅已接单和进行中可写护理记录
        if (!isOrderServiceWritable(order)) {
            throw new BusinessException(400, "当前订单状态不能维护护理记录");
        }
    }

    /**
     * 【校验护理记录读权限】
     *
     * 业务作用：确认操作人可查看该订单的护理记录。
     *
     * 调用场景：listByOrder 与 getById 的带权限版本在非管理员分支调用。
     *
     * 调用链：listByOrder()/getById() → requireReadableAccess() → isAssignedKeeperOrMerchant()。
     *
     * 数据处理：操作人为订单 owner_id_wsh 时放行；否则判断是否为分配寄养师或商家。
     *
     * 业务规则：未登录、非订单相关方均不可读；不校验订单状态。
     *
     * 状态影响：无。
     *
     * 异常情况：actorUserId 为空抛 BusinessException(401)；无权限抛 BusinessException(403)。
     *
     * 注意事项：读权限包含宠物主人，写权限不包含，两者范围不同。
     */
    private void requireReadableAccess(Long actorUserId, PetOrder order) {
        if (actorUserId == null) {
            throw new BusinessException(401, "please sign in");
        }
        // 宠物主本人直接放行
        if (actorUserId.equals(order.getOwner_id_wsh())) {
            return;
        }
        if (isAssignedKeeperOrMerchant(actorUserId, order)) {
            return;
        }
        throw new BusinessException(403, "no permission to access care records for this order");
    }

    /**
     * 【判断是否为订单分配方】
     *
     * 业务作用：判断操作人是否为该订单的寄养师本人或商家主人。
     *
     * 调用场景：requireWritableAccess() 与 requireReadableAccess() 的权限判断。
     *
     * 调用链：requireWritableAccess()/requireReadableAccess() → isAssignedKeeperOrMerchant() → keeperMapper.selectById() / merchantMapper.selectById()。
     *
     * 数据处理：按订单 keeper_id_wsh 查寄养师并比对 user_id_wsh；按 merchant_id_wsh 查商家并比对 user_id_wsh。
     *
     * 业务规则：任一命中即返回 true；参数为空返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：比较的是业务实体的 user_id_wsh（账号 ID），不是 keeper/merchant 主键，调用方传入的必须是登录用户 ID。
     */
    private boolean isAssignedKeeperOrMerchant(Long actorUserId, PetOrder order) {
        if (actorUserId == null || order == null) {
            return false;
        }
        // 先比对照看者账号，再比对商家账号
        if (order.getKeeper_id_wsh() != null) {
            Keeper keeper = keeperMapper.selectById(order.getKeeper_id_wsh());
            if (keeper != null && actorUserId.equals(keeper.getUser_id_wsh())) {
                return true;
            }
        }
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectById(order.getMerchant_id_wsh());
            if (merchant != null && actorUserId.equals(merchant.getUser_id_wsh())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 【判断订单是否可写护理记录】
     *
     * 业务作用：判断订单当前状态是否允许维护护理记录。
     *
     * 调用场景：requireWritableAccess() 内的状态判断。
     *
     * 调用链：requireWritableAccess() → isOrderServiceWritable()。
     *
     * 数据处理：读取订单 status_wsh 并比对白名单。
     *
     * 业务规则：仅 received 与 in_progress 两个状态可写。
     *
     * 状态影响：无。
     *
     * 异常情况：order 为 null 时返回 false。
     *
     * 注意事项：状态值为硬编码字符串，与订单状态机定义需保持同步。
     */
    private boolean isOrderServiceWritable(PetOrder order) {
        String status = order == null ? null : order.getStatus_wsh();
        return "received".equals(status) || "in_progress".equals(status);
    }

    /**
     * 【业务名称】更新护理记录（实现-无权限）
     * 业务作用：更新护理记录，仅更新非 null 字段。
     * 调用场景：内部、管理员更新。
     * 调用链：update(Long, CareRecordUpdateRequestDTO) → getByIdRaw() → applyUpdate() → updateById() → toDTO()。
     * 数据处理：仅更新类型、内容、图片、记录时间中非 null 字段。
     * 业务规则：无权限校验。
     * 状态影响：更新护理记录。
     * 异常情况：记录不存在时抛 BusinessException。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public CareRecordDTO update(Long id, CareRecordUpdateRequestDTO request) {
        CareRecord existing = getByIdRaw(id);
        applyUpdate(existing, request);
        careRecordMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 【业务名称】更新护理记录（实现-带权限与考勤）
     * 业务作用：更新护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者修改护理记录。
     * 调用链：update(Long, boolean, Long, CareRecordUpdateRequestDTO) → getByIdRaw() → requireWritableAccess() → requireKeeperOnDuty() → applyUpdate() → updateById() → toDTO()。
     * 数据处理：同无权限版本。
     * 业务规则：非管理员时仅分配看护者或商家可更新，且需处于值班状态。
     * 状态影响：更新护理记录。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：管理员跳过权限和考勤校验。
     */
    @Transactional
    @Override
    public CareRecordDTO update(Long actorUserId, boolean admin, Long id, CareRecordUpdateRequestDTO request) {
        CareRecord existing = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(existing.getOrder_id_wsh());
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        applyUpdate(existing, request);
        careRecordMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 【按非空字段覆盖护理记录】
     *
     * 业务作用：把更新请求中非 null 的字段写入已有护理记录实体。
     *
     * 调用场景：update 的两个重载版本。
     *
     * 调用链：update() → applyUpdate() → careRecordMapper.updateById()。
     *
     * 数据处理：仅当 type_wsh、content_wsh、images_wsh、record_time_wsh 非 null 时覆盖。
     *
     * 业务规则：null 表示不修改该字段，无法通过此方法把字段置空。
     *
     * 状态影响：无（仅修改内存对象，落库由调用方负责）。
     *
     * 异常情况：request 为 null 时抛 NullPointerException。
     *
     * 注意事项：order_id、pet_id、keeper_id 不可通过更新接口修改。
     */
    private void applyUpdate(CareRecord existing, CareRecordUpdateRequestDTO request) {
        // 仅覆盖非空字段，null 视为不修改
        if (request.getType_wsh() != null) existing.setType_wsh(request.getType_wsh());
        if (request.getContent_wsh() != null) existing.setContent_wsh(request.getContent_wsh());
        if (request.getImages_wsh() != null) existing.setImages_wsh(request.getImages_wsh());
        if (request.getRecord_time_wsh() != null) existing.setRecord_time_wsh(request.getRecord_time_wsh());
    }

    /**
     * 【业务名称】删除护理记录（实现-无权限）
     * 业务作用：删除护理记录。
     * 调用场景：内部、管理员删除。
     * 调用链：delete(Long) → CareRecordMapper.deleteById()。
     * 数据处理：物理删除。
     * 业务规则：无权限校验。
     * 状态影响：删除护理记录。
     * 异常情况：无。
     * 注意事项：无。
     */
    @Transactional
    @Override
    public void delete(Long id) {
        careRecordMapper.deleteById(id);
    }

    /**
     * 【业务名称】删除护理记录（实现-带权限与考勤）
     * 业务作用：删除护理记录，非管理员时校验权限和考勤状态。
     * 调用场景：看护者删除护理记录。
     * 调用链：delete(Long, boolean, Long) → getByIdRaw() → requireWritableAccess() → requireKeeperOnDuty() → deleteById()。
     * 数据处理：物理删除。
     * 业务规则：非管理员时仅分配看护者或商家可删除，且需处于值班状态。
     * 状态影响：删除护理记录。
     * 异常情况：无权限抛 BusinessException(403)。
     * 注意事项：管理员跳过权限校验。
     */
    @Transactional
    @Override
    public void delete(Long actorUserId, boolean admin, Long id) {
        CareRecord existing = getByIdRaw(id);
        if (!admin) {
            PetOrder order = requireOrder(existing.getOrder_id_wsh());
            requireWritableAccess(actorUserId, order);
            keeperAttendanceService.requireKeeperOnDuty(order.getKeeper_id_wsh(), order.getMerchant_id_wsh());
        }
        careRecordMapper.deleteById(id);
    }

    /**
     * 【加载订单并校验存在】
     *
     * 业务作用：按 ID 加载订单，不存在或 ID 为空时抛异常。
     *
     * 调用场景：listByOrder、getById、update、delete 的带权限版本。
     *
     * 调用链：各业务方法 → requireOrder() → orderMapper.selectById()。
     *
     * 数据处理：按主键查询 pet_order 单条记录。
     *
     * 业务规则：订单 ID 必填。
     *
     * 状态影响：无。
     *
     * 异常情况：ID 为空抛 BusinessException(400)；订单不存在抛 BusinessException(404)。
     *
     * 注意事项：提示语为英文。
     */
    private PetOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }
        PetOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        return order;
    }

    /**
     * 【加载护理记录并校验存在】
     *
     * 业务作用：按 ID 加载护理记录实体，不存在则抛异常。
     *
     * 调用场景：getById、update、delete 的带权限版本与无权限更新版本。
     *
     * 调用链：各业务方法 → getByIdRaw() → careRecordMapper.selectById()。
     *
     * 数据处理：按主键查询 care_record 单条记录。
     *
     * 业务规则：记录必须存在。
     *
     * 状态影响：无。
     *
     * 异常情况：记录为空抛 BusinessException("记录不存在")。
     *
     * 注意事项：使用无状态码构造，默认状态码由 BusinessException 决定，与其它方法的 404 写法不一致。
     */
    private CareRecord getByIdRaw(Long id) {
        CareRecord record = careRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    /**
     * 【护理记录实体转 DTO】
     *
     * 业务作用：把 CareRecord 实体转换为 CareRecordDTO。
     *
     * 调用场景：查询、创建、更新返回前。
     *
     * 调用链：各业务方法 → toDTO()。
     *
     * 数据处理：逐字段拷贝 id、订单 ID、宠物 ID、寄养师 ID、类型、内容、图片、记录时间与创建时间。
     *
     * 业务规则：实体为 null 时返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不补全宠物名、寄养师名等展示字段。
     */
    private CareRecordDTO toDTO(CareRecord record) {
        if (record == null) return null;
        CareRecordDTO dto = new CareRecordDTO();
        dto.setId_wsh(record.getId_wsh());
        dto.setOrder_id_wsh(record.getOrder_id_wsh());
        dto.setPet_id_wsh(record.getPet_id_wsh());
        dto.setKeeper_id_wsh(record.getKeeper_id_wsh());
        dto.setType_wsh(record.getType_wsh());
        dto.setContent_wsh(record.getContent_wsh());
        dto.setImages_wsh(record.getImages_wsh());
        dto.setRecord_time_wsh(record.getRecord_time_wsh());
        dto.setCreated_at_wsh(record.getCreated_at_wsh());
        return dto;
    }

    /**
     * 【护理记录列表转 DTO】
     *
     * 业务作用：批量把 CareRecord 列表转换为 DTO 列表。
     *
     * 调用场景：listByOrder(Long) 返回前。
     *
     * 调用链：listByOrder(Long) → toDTOList() → toDTO()。
     *
     * 数据处理：入参为 null 时返回空列表，否则逐条转换。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与同类中 toDTO() 的 null 处理保持一致。
     */
    private List<CareRecordDTO> toDTOList(List<CareRecord> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
