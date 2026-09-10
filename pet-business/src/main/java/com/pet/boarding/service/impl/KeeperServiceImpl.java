package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.dto.KeeperUpdateRequestDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.vo.KeeperVO;
import com.pet.common.BusinessException;
import com.pet.common.OrderStatus;
import com.pet.common.StatusCode;
import com.pet.common.geo.GeoDistanceUtils;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 看护者（Keeper）管理服务实现。
 * <p>
 * 负责看护者从入驻申请 → 审核（平台/商家双通道）→ 在职（ACTIVE/OFFLINE/BUSY）→ 离职（辞职/解雇）的全生命周期管理。
 * 同时维护看护者与商家之间的归属关系，以及看护者在系统中的角色权限。
 */
@Service
@Slf4j
public class KeeperServiceImpl implements KeeperService {

    /**
     * 阻碍离职/解雇操作的进行中订单状态集合。
     * 包含：待付款、已付款、已确认、配送中、已接单、进行中、退款中
     */
    private static final Set<String> BLOCKING_ORDER_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.CONFIRMED,
            OrderStatus.DELIVERED,
            OrderStatus.RECEIVED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.REFUNDING);

    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final QualificationService qualificationService;
    private final OrderMapper orderMapper;

    public KeeperServiceImpl(KeeperMapper keeperMapper, MerchantMapper merchantMapper,
                             RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                             QualificationService qualificationService,
                             OrderMapper orderMapper) {
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.qualificationService = qualificationService;
        this.orderMapper = orderMapper;
    }

    /**
     * 【查询看护者列表】
     *
     * 业务作用：获取系统中所有看护者的全量列表。
     * 调用场景：管理后台看护者管理页面加载时调用。
     * 调用链：KeeperController → listAll → KeeperMapper.selectList（按创建时间倒序）
     * 数据处理：直接查询 keeper 表全部数据，按创建时间倒序。
     * 业务规则：返回所有状态的看护者（含待审核、已通过、已驳回等）。
     * 状态影响：只读操作。
     */
    @Override
    public List<Keeper> listAll() {
        log.info("listAll() 被调用");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().orderByDesc(Keeper::getCreated_at_wsh));
    }

    /**
     * 【查询待审核看护者列表】
     *
     * 业务作用：查询所有待审核的看护者申请。
     * 调用场景：平台管理员后台查看待审核看护者申请时调用。
     * 调用链：AdminController → listPending → KeeperMapper.selectList（按 KEEPER_PENDING 过滤）
     * 数据处理：查询 keeper 表中 status == KEEPER_PENDING 的所有记录。
     * 业务规则：仅返回待审核状态的看护者。
     * 状态影响：只读操作。
     */
    @Override
    public List<Keeper> listPending() {
        log.info("listPending() 被调用");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>().eq(Keeper::getStatus_wsh, StatusCode.KEEPER_PENDING.getValue()));
    }

    /**
     * 【根据ID查询看护者】
     *
     * 业务作用：根据主键ID查询看护者信息，是其他业务方法的底层依赖。
     * 调用场景：被 update、delete、resign 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → KeeperMapper.selectById
     * 数据处理：按主键ID从 keeper 表查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     * 异常情况：看护者不存在时抛 BusinessException("看护者不存在")。
     */
    @Override
    public Keeper getById(Long id) {
        log.info("getById() 被调用");
        Keeper keeper = keeperMapper.selectById(id);
        if (keeper == null) {
            throw new BusinessException("看护者不存在");
        }
        return keeper;
    }

    /**
     * 【批量查询看护者】
     *
     * 业务作用：根据多个看护者ID批量查询。
     * 调用场景：需要一次展示多个看护者详情的场景。
     * 调用链：上层业务方法 → listByIds → KeeperMapper.selectBatchIds
     * 数据处理：按ID集合批量查询，入参为空时返回空列表。
     * 业务规则：ids 为 null 或空集合时返回空列表。
     * 状态影响：只读操作。
     */
    @Override
    public List<Keeper> listByIds(Collection<Long> ids) {
        log.info("listByIds() 被调用");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return keeperMapper.selectBatchIds(ids);
    }

    /**
     * 【根据商家ID查询看护者】
     *
     * 业务作用：查询指定商家旗下所有未逻辑删除的看护者。
     * 调用场景：商家在后台查看自己名下的看护者列表时调用。
     * 调用链：MerchantKeeperController → findByMerchantId → KeeperMapper.selectList（按 merchant_id + deleted=0）
     * 数据处理：按 merchant_id 查询 keeper 表，过滤已逻辑删除的记录。
     * 业务规则：仅返回未逻辑删除（deleted=0）的看护者。
     * 状态影响：只读操作。
     */
    @Override
    public List<Keeper> findByMerchantId(Long merchantId) {
        log.info("findByMerchantId() 被调用");
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchantId)
                        .eq(Keeper::getDeleted_wsh, 0));
    }

    /**
     * 【搜索附近活跃看护者】
     *
     * 业务作用：基于用户地理位置搜索附近商家下的活跃看护者。
     * 调用场景：用户端搜索附近可提供服务的看护者时调用。
     * 调用链：KeeperController → searchNearby → MerchantMapper.searchNearby → KeeperMapper.selectList
     * 数据处理：两阶段查询——先搜附近商家，再查这些商家下 KEEPER_ACTIVE 的看护者，附上距离。
     * 业务规则：仅搜索已审核通过商家下的活跃看护者；距离使用 Haversine 公式。
     * 状态影响：只读操作。
     */
    @Override
    public List<KeeperVO> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() 被调用");
        List<Merchant> nearbyMerchants = merchantMapper.searchNearby(lat, lng, radius);
        if (nearbyMerchants.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Merchant> merchantMap = nearbyMerchants.stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, m -> m));
        List<Long> merchantIds = nearbyMerchants.stream().map(Merchant::getId_wsh).collect(Collectors.toList());

        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .in(Keeper::getMerchant_id_wsh, merchantIds)
                        .eq(Keeper::getStatus_wsh, StatusCode.KEEPER_ACTIVE.getValue()));

        return keepers.stream().map(k -> {
            KeeperVO vo = toKeeperVO(k);
            Merchant m = merchantMap.get(k.getMerchant_id_wsh());
            if (m != null) {
                vo.setMerchant_name_wsh(m.getName_wsh());
                vo.setMerchant_latitude_wsh(m.getLatitude_wsh());
                vo.setMerchant_longitude_wsh(m.getLongitude_wsh());
                vo.setDistance_wsh(GeoDistanceUtils.distanceKm(lat, lng,
                        m.getLatitude_wsh().doubleValue(), m.getLongitude_wsh().doubleValue()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 【创建看护者入驻申请】
     *
     * 业务作用：提交看护者入驻申请，创建或复用看护者记录并同步提交资质审核材料。
     * 调用场景：用户在看护者入驻页面提交时调用。
     * 调用链：KeeperController → create @Transactional → KeeperMapper.insert/updateById + QualificationService.createOrUpdatePending
     * 数据处理：校验商家存在；若用户已有被驳回/离职/解雇的历史记录则复用并更新，否则新建。初始状态 KEEPER_PENDING，评分 5.0。
     * 业务规则：一个用户不可同时拥有多个待审核/已通过申请；创建后需审核才能 ACTIVE。
     * 状态影响：新增/更新看护者记录（KEEPER_PENDING）；创建/更新资质审核记录。
     * 事务边界：看护者写入 + 资质创建在同一 @Transactional 事务中。
     */
    @Override
    @Transactional
    public Keeper create(KeeperCreateRequestDTO dto, Long userId) {
        log.info("create() 被调用");
        if (dto.getMerchant_id_wsh() == null) {
            throw new BusinessException("必须选择所属商家才能成为看护人");
        }
        Merchant merchant = merchantMapper.selectById(dto.getMerchant_id_wsh());
        if (merchant == null) {
            throw new BusinessException("所选商家不存在");
        }
        Keeper existing = keeperMapper.selectOne(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .orderByDesc(Keeper::getId_wsh)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getStatus_wsh() != StatusCode.KEEPER_REJECTED.getValue()
                    && existing.getStatus_wsh() != StatusCode.KEEPER_RESIGNED.getValue()
                    && existing.getStatus_wsh() != StatusCode.KEEPER_TERMINATED.getValue()) {
                throw new BusinessException("您已有待审核或已通过的看护者申请，不能重复申请");
            }
            existing.setMerchant_id_wsh(dto.getMerchant_id_wsh());
            existing.setName_wsh(dto.getName_wsh());
            existing.setPhone_wsh(dto.getPhone_wsh());
            existing.setAvatar_wsh(dto.getAvatar_wsh());
            existing.setExperience_years_wsh(dto.getExperience_years_wsh());
            existing.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
            existing.setMax_pets_wsh(dto.getMax_pets_wsh());
            existing.setBio_wsh(dto.getBio_wsh());
            existing.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());
            existing.setRating_wsh(java.math.BigDecimal.valueOf(5.0));
            keeperMapper.updateById(existing);
            qualificationService.createOrUpdatePending(
                    QualificationService.OWNER_TYPE_KEEPER,
                    existing.getId_wsh(),
                    userId,
                    QualificationService.QUAL_TYPE_KEEPER_CERTIFICATE,
                    "看护资质",
                    dto.getQualification_image_wsh(),
                    "看护者申请资质");
            return existing;
        }
        Keeper keeper = new Keeper();
        keeper.setUser_id_wsh(userId);
        keeper.setMerchant_id_wsh(dto.getMerchant_id_wsh());
        keeper.setName_wsh(dto.getName_wsh());
        keeper.setPhone_wsh(dto.getPhone_wsh());
        keeper.setAvatar_wsh(dto.getAvatar_wsh());
        keeper.setExperience_years_wsh(dto.getExperience_years_wsh());
        keeper.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
        keeper.setMax_pets_wsh(dto.getMax_pets_wsh());
        keeper.setBio_wsh(dto.getBio_wsh());
        keeper.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());
        keeper.setRating_wsh(java.math.BigDecimal.valueOf(5.0));
        keeperMapper.insert(keeper);
        qualificationService.createOrUpdatePending(
                QualificationService.OWNER_TYPE_KEEPER,
                keeper.getId_wsh(),
                userId,
                QualificationService.QUAL_TYPE_KEEPER_CERTIFICATE,
                "看护资质",
                dto.getQualification_image_wsh(),
                "看护者申请资质");
        return keeper;
    }

    /**
     * 【更新看护者基础信息】
     *
     * 业务作用：修改看护者的基础信息（姓名、电话、头像、经验、定价等）。
     * 调用场景：看护者在个人中心编辑基本信息时调用。
     * 调用链：KeeperController → update @Transactional → KeeperMapper.updateById
     * 数据处理：仅更新 DTO 中非 null 的字段，null 字段保持原值。
     * 业务规则：仅更新非 null 字段，不修改审核状态。
     * 状态影响：仅影响看护者基础信息字段。
     * 事务边界：查询 + 更新在同一事务中。
     */
    @Override
    @Transactional
    public Keeper update(Long id, KeeperUpdateRequestDTO dto) {
        log.info("update() 被调用");
        Keeper existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getPhone_wsh() != null) existing.setPhone_wsh(dto.getPhone_wsh());
        if (dto.getAvatar_wsh() != null) existing.setAvatar_wsh(dto.getAvatar_wsh());
        if (dto.getExperience_years_wsh() != null) existing.setExperience_years_wsh(dto.getExperience_years_wsh());
        if (dto.getPrice_per_day_wsh() != null) existing.setPrice_per_day_wsh(dto.getPrice_per_day_wsh());
        if (dto.getMax_pets_wsh() != null) existing.setMax_pets_wsh(dto.getMax_pets_wsh());
        if (dto.getBio_wsh() != null) existing.setBio_wsh(dto.getBio_wsh());
        keeperMapper.updateById(existing);
        return existing;
    }

    /**
     * 【物理删除看护者】
     *
     * 业务作用：物理删除看护者记录并回收 KEEPER 角色。
     * 调用场景：管理后台强制删除看护者时调用。
     * 调用链：AdminController → delete @Transactional → KeeperMapper.deleteById + revokeKeeperRole
     * 数据处理：先查询看护者存在，然后物理删除，最后回收 KEEPER 角色。
     * 业务规则：物理删除不可恢复；删除后同步回收角色权限。
     * 状态影响：删除 keeper 记录；删除 user_role 关联。
     * 事务边界：删除 + 角色回收在同一事务中。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() 被调用");
        Keeper keeper = getById(id);
        keeperMapper.deleteById(id);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    /**
     * 【看护者主动辞职】
     *
     * 业务作用：看护者主动申请离职，状态变为辞职并回收 KEEPER 角色。
     * 调用场景：看护者在个人中心点击辞职时调用。
     * 调用链：KeeperController → resign @Transactional → assertEmploymentActionAllowed → KeeperMapper.updateById → revokeKeeperRole
     * 数据处理：校验操作人须为本人；校验状态允许（ACTIVE/OFFLINE/BUSY）；校验无未完成订单；更新状态为 KEEPER_RESIGNED 并回收角色。
     * 状态影响：status → KEEPER_RESIGNED；回收 KEEPER 角色。
     * 事务边界：状态变更 + 角色回收在同一事务中。
     * 异常情况：非本人抛 403；状态不允许抛 400；有未完成订单抛 400。
     */
    @Override
    @Transactional
    public void resign(Long id, Long userId) {
        log.info("resign() 被调用");
        Keeper keeper = getById(id);
        if (keeper.getUser_id_wsh() == null || !keeper.getUser_id_wsh().equals(userId)) {
            throw new BusinessException(403, "Only the keeper can resign this profile");
        }
        assertEmploymentActionAllowed(keeper);
        keeper.setStatus_wsh(StatusCode.KEEPER_RESIGNED.getValue());
        keeperMapper.updateById(keeper);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    /**
     * 【商家解雇看护者】
     *
     * 业务作用：商家 owner 解雇旗下看护者，状态变为解雇并回收 KEEPER 角色。
     * 调用场景：商家在后台管理看护者时执行解雇操作。
     * 调用链：MerchantKeeperController → terminateByMerchant @Transactional → requireMerchantByUserId → assertEmploymentActionAllowed → KeeperMapper.updateById → revokeKeeperRole
     * 数据处理：校验商家身份和归属权；校验状态允许（ACTIVE/OFFLINE/BUSY）；校验无未完成订单；更新为 KEEPER_TERMINATED 并回收角色。
     * 状态影响：status → KEEPER_TERMINATED；回收 KEEPER 角色。
     * 事务边界：状态变更 + 角色回收在同一事务中。
     * 异常情况：非本商家抛 403；状态不允许抛 400；有未完成订单抛 400。
     */
    @Override
    @Transactional
    public void terminateByMerchant(Long id, Long merchantUserId) {
        log.info("terminateByMerchant() 被调用");
        Keeper keeper = getById(id);
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException(403, "Only the owning merchant can terminate this keeper");
        }
        assertEmploymentActionAllowed(keeper);
        keeper.setStatus_wsh(StatusCode.KEEPER_TERMINATED.getValue());
        keeperMapper.updateById(keeper);
        revokeKeeperRole(keeper.getUser_id_wsh());
    }

    /**
     * 【平台审核通过看护者申请】
     *
     * 业务作用：平台管理员审核通过看护者入驻申请，授予 KEEPER 角色。
     * 调用场景：平台管理员后台审核通过时调用。
     * 调用链：AdminController → approve @Transactional → KeeperMapper.updateById → grantKeeperRole
     * 数据处理：校验 PENDING 状态，更新为 KEEPER_ACTIVE，授予 KEEPER 角色。
     * 状态影响：status: KEEPER_PENDING → KEEPER_ACTIVE；新增 user_role 记录。
     * 事务边界：状态变更 + 角色授予在同一事务中。
     * 异常情况：不是待审核状态抛 BusinessException。
     */
    @Override
    @Transactional
    public void approve(Long id) {
        log.info("approve() 被调用");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护者不在待审核状态");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeperMapper.updateById(keeper);
        grantKeeperRole(keeper.getUser_id_wsh());
    }

    /**
     * 【平台驳回看护者申请】
     *
     * 业务作用：平台管理员驳回看护者入驻申请。
     * 调用场景：平台管理员后台驳回时调用。
     * 调用链：AdminController → reject @Transactional → KeeperMapper.updateById
     * 数据处理：校验 PENDING 状态，更新为 KEEPER_REJECTED。
     * 状态影响：status: KEEPER_PENDING → KEEPER_REJECTED。
     * 异常情况：不是待审核状态抛 BusinessException。
     */
    @Override
    @Transactional
    public void reject(Long id) {
        log.info("reject() 被调用");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护者不在待审核状态");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_REJECTED.getValue());
        keeperMapper.updateById(keeper);
    }

    /**
     * 【设置看护者在线状态】
     *
     * 业务作用：看护者手动切换自己的在线/离线状态。
     * 调用场景：看护者在 APP 上点击上线/下线时调用。
     * 调用链：KeeperController → setOnlineStatus @Transactional → KeeperMapper.updateById
     * 数据处理：校验状态值合法性；校验当前状态是否可切换；BUSY 状态禁止手动切换。
     * 业务规则：仅 ACTIVE/OFFLINE/BUSY 状态可切换；BUSY 禁止手动切换。
     * 状态影响：更新 keeper 的 status 字段。
     * 异常情况：无效状态值抛 BusinessException；BUSY 状态抛 BusinessException。
     */
    @Override
    @Transactional
    public void setOnlineStatus(Long id, int status) {
        log.info("setOnlineStatus() 被调用");
        if (status != StatusCode.KEEPER_ACTIVE.getValue() && status != StatusCode.KEEPER_OFFLINE.getValue()
                && status != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("无效的在线状态");
        }
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_ACTIVE.getValue() && keeper.getStatus_wsh() != StatusCode.KEEPER_OFFLINE.getValue()
                && keeper.getStatus_wsh() != StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("当前状态不可切换在线状态");
        }
        if (keeper.getStatus_wsh() == StatusCode.KEEPER_BUSY.getValue()) {
            throw new BusinessException("忙碌状态不可手动切换，请等待当前订单完成后自动恢复");
        }
        keeper.setStatus_wsh(status);
        // 记录离线来源：主动离线标记为人工（offline_source_wsh=1），上线则清除来源，
        // 保证关店同步不会把主动离线覆盖回在线。
        if (status == StatusCode.KEEPER_OFFLINE.getValue()) {
            keeper.setOffline_source_wsh(OFFLINE_SOURCE_MANUAL);
        } else {
            keeper.setOffline_source_wsh(OFFLINE_SOURCE_SYSTEM);
        }
        keeperMapper.updateById(keeper);
    }

    /**
     * 【同步商家店铺状态至看护者】
     *
     * 业务作用：商家店铺开门/关门时同步通知旗下所有看护者的在线状态。
     * 调用场景：MerchantServiceImpl.refreshStoreState 在店铺状态变更时触发。
     * 调用链：MerchantServiceImpl → syncMerchantStoreStatus @Transactional → 循环更新看护者在线状态
     * 数据处理：遍历该商家下所有看护者，开门时 OFFLINE→ACTIVE，关门时 ACTIVE→OFFLINE，BUSY 保持不变。
     * 业务规则：仅变更 OFFLINE↔ACTIVE，BUSY 状态不受影响。
     * 来源保护：开门时仅恢复"非主动离线"（offline_source_wsh != 1）的看护者；主动离线看护者保持离线，
     * 避免店铺同步覆盖看护员主动离线的意图。关门置 OFFLINE 时来源统一标记为系统同步（0）。
     * 状态影响：批量更新旗下看护者的在线状态。
     */
    @Override
    @Transactional
    public void syncMerchantStoreStatus(Long merchantId, boolean storeOpen) {
        log.info("syncMerchantStoreStatus() 被调用, merchantId={}, storeOpen={}", merchantId, storeOpen);
        List<Keeper> keepers = keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchantId)
                        .eq(Keeper::getDeleted_wsh, 0));
        for (Keeper keeper : keepers) {
            Integer currentStatus = keeper.getStatus_wsh();
            Integer targetStatus = currentStatus;
            boolean changed = false;
            if (storeOpen) {
                if (currentStatus == null || currentStatus == StatusCode.KEEPER_OFFLINE.getValue()) {
                    // 主动离线（offline_source_wsh == 1）不被店铺开门同步覆盖
                    boolean manualOffline = keeper.getOffline_source_wsh() != null
                            && keeper.getOffline_source_wsh() == OFFLINE_SOURCE_MANUAL;
                    if (!manualOffline) {
                        targetStatus = StatusCode.KEEPER_ACTIVE.getValue();
                        keeper.setOffline_source_wsh(OFFLINE_SOURCE_SYSTEM);
                        changed = true;
                    }
                }
            } else {
                if (currentStatus == null || currentStatus == StatusCode.KEEPER_ACTIVE.getValue()) {
                    targetStatus = StatusCode.KEEPER_OFFLINE.getValue();
                    keeper.setOffline_source_wsh(OFFLINE_SOURCE_SYSTEM);
                    changed = true;
                }
            }
            if (changed && targetStatus != null && !targetStatus.equals(currentStatus)) {
                keeper.setStatus_wsh(targetStatus);
                keeperMapper.updateById(keeper);
            }
        }
    }

    /**
     * 【看护者实体转VO】
     *
     * 业务作用：将看护者实体转换为前端展示所需的 VO（含资质信息）。
     * 调用场景：Controller 层返回看护者信息前调用。
     * 调用链：各查询 Controller → toDTO → qualificationService.listByOwner
     * 数据处理：字段拷贝 + 调用资质服务加载看护者资质列表。
     * 状态影响：只读操作。
     */
    @Override
    public KeeperVO toDTO(Keeper entity) {
        if (entity == null) return null;
        return toKeeperVO(entity);
    }

    /**
     * 【根据用户ID查找看护者】
     *
     * 业务作用：根据用户ID查询最新的看护者记录（支持多次申请场景）。
     * 调用场景：用户登录后查询看护者身份；创建看护者时判断是否已有历史记录。
     * 调用链：身份校验/入驻申请 → findByUserId → KeeperMapper.selectOne（按 user_id 倒序取最新）
     * 数据处理：按 user_id 查询，按 ID 倒序取最新一条记录。
     * 业务规则：一个用户可以有多次看护者申请记录，按 ID 倒序取最新。
     * 状态影响：只读操作。
     */
    @Override
    public Keeper findByUserId(Long userId) {
        log.info("findByUserId() 被调用");
        return keeperMapper.selectOne(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getUser_id_wsh, userId)
                        .orderByDesc(Keeper::getId_wsh)
                        .last("LIMIT 1"));
    }

    /**
     * 【商家查询旗下待审核看护者】
     *
     * 业务作用：商家 owner 查询归属本商家的所有待审核看护者申请。
     * 调用场景：商家在后台审核看护者入驻申请时调用。
     * 调用链：MerchantKeeperController → listPendingByMerchant → requireMerchantByUserId → KeeperMapper.selectList
     * 数据处理：根据 merchantUserId 查询商家信息，再查该商家下所有 KEEPER_PENDING 的看护者。
     * 业务规则：必须先校验商家身份；仅返回待审核状态的看护者。
     * 状态影响：只读操作。
     */
    @Override
    public List<Keeper> listPendingByMerchant(Long merchantUserId) {
        log.info("listPendingByMerchant() 被调用");
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (merchant == null) {
            throw new BusinessException("您没有商家信息");
        }
        return keeperMapper.selectList(
                new LambdaQueryWrapper<Keeper>()
                        .eq(Keeper::getMerchant_id_wsh, merchant.getId_wsh())
                        .eq(Keeper::getStatus_wsh, StatusCode.KEEPER_PENDING.getValue()));
    }

    /**
     * 【商家审核通过看护者申请】
     *
     * 业务作用：商家 owner 审核通过归属本商家的看护者入驻申请。
     * 调用场景：商家在后台审核通过看护者申请时调用。
     * 调用链：MerchantKeeperController → approveByMerchant @Transactional → KeeperMapper.updateById → grantKeeperRole
     * 数据处理：校验 PENDING 状态；校验商家归属权；更新为 KEEPER_ACTIVE 并授予角色。
     * 状态影响：status: KEEPER_PENDING → KEEPER_ACTIVE；新增 user_role 记录。
     * 事务边界：状态变更 + 角色授予在同一事务中。
     * 异常情况：不是待审核状态抛 BusinessException；无权限审核抛 BusinessException。
     */
    @Override
    @Transactional
    public void approveByMerchant(Long id, Long merchantUserId) {
        log.info("approveByMerchant() 被调用");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护者不在待审核状态");
        }
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException("无权审核此看护者申请");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        keeperMapper.updateById(keeper);
        grantKeeperRole(keeper.getUser_id_wsh());
    }

    /**
     * 【商家驳回看护者申请】
     *
     * 业务作用：商家 owner 驳回归属本商家的看护者入驻申请。
     * 调用场景：商家在后台驳回看护者申请时调用。
     * 调用链：MerchantKeeperController → rejectByMerchant @Transactional → KeeperMapper.updateById
     * 数据处理：校验 PENDING 状态；校验商家归属权；更新为 KEEPER_REJECTED。
     * 状态影响：status: KEEPER_PENDING → KEEPER_REJECTED。
     * 事务边界：状态变更在同一事务中。
     * 异常情况：不是待审核状态抛 BusinessException；无权限审核抛 BusinessException。
     */
    @Override
    @Transactional
    public void rejectByMerchant(Long id, Long merchantUserId) {
        log.info("rejectByMerchant() 被调用");
        Keeper keeper = getById(id);
        if (keeper.getStatus_wsh() != StatusCode.KEEPER_PENDING.getValue()) {
            throw new BusinessException("该看护者不在待审核状态");
        }
        Merchant merchant = requireMerchantByUserId(merchantUserId);
        if (!merchant.getId_wsh().equals(keeper.getMerchant_id_wsh())) {
            throw new BusinessException("无权审核此看护者申请");
        }
        keeper.setStatus_wsh(StatusCode.KEEPER_REJECTED.getValue());
        keeperMapper.updateById(keeper);
    }

    /**
     * 校验是否允许执行离职或解雇操作。
     * <p>
     * <b>前置条件：</b>
     * <ul>
     *   <li>看护者状态必须为 ACTIVE、OFFLINE 或 BUSY 之一</li>
     *   <li>看护者名下没有未完成的订单（包含 PENDING / PAID / CONFIRMED 等阻塞状态）</li>
     * </ul>
     *
     * @param keeper 看护者实体
     * @throws BusinessException 如果状态不允许或有未完成订单
     */
    private void assertEmploymentActionAllowed(Keeper keeper) {
        Integer status = keeper.getStatus_wsh();
        if (status == null
                || (status != StatusCode.KEEPER_ACTIVE.getValue()
                && status != StatusCode.KEEPER_OFFLINE.getValue()
                && status != StatusCode.KEEPER_BUSY.getValue())) {
            throw new BusinessException(400, "Keeper status does not allow resignation or termination");
        }
        Long blockingOrders = orderMapper.selectCount(new LambdaQueryWrapper<PetOrder>()
                .eq(PetOrder::getKeeper_id_wsh, keeper.getId_wsh())
                .in(PetOrder::getStatus_wsh, BLOCKING_ORDER_STATUSES));
        if (blockingOrders != null && blockingOrders > 0) {
            throw new BusinessException(400, "Keeper has unfinished orders");
        }
    }

    /**
     * 根据用户ID查询商家并校验其存在性。
     *
     * @param merchantUserId 商家的用户ID
     * @return 商家实体
     * @throws BusinessException 如果商家不存在
     */
    private Merchant requireMerchantByUserId(Long merchantUserId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getUser_id_wsh, merchantUserId)
                        .last("LIMIT 1"));
        if (merchant == null) {
            throw new BusinessException(404, "Merchant not found");
        }
        return merchant;
    }

    /**
     * 授予用户 KEEPER 角色。
     * <p>
     * <b>数据流：</b>
     * <ol>
     *   <li>查找角色表中 code = "KEEPER" 的角色</li>
     *   <li>如果用户尚无此角色，先尝试恢复已软删除的角色记录，否则新建</li>
     * </ol>
     *
     * @param userId 用户ID
     */
    private void grantKeeperRole(Long userId) {
        Role keeperRole = findKeeperRole();
        if (keeperRole != null && userId != null) {
            LambdaQueryWrapper<UserRole> qw = new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUser_id_wsh, userId)
                    .eq(UserRole::getRole_id_wsh, keeperRole.getId_wsh());
            if (userRoleMapper.selectCount(qw) == 0) {
                if (userRoleMapper.restoreUserRole(userId, keeperRole.getId_wsh()) > 0) {
                    return;
                }
                UserRole ur = new UserRole();
                ur.setUser_id_wsh(userId);
                ur.setRole_id_wsh(keeperRole.getId_wsh());
                userRoleMapper.insert(ur);
            }
        }
    }

    /**
     * 回收用户的 KEEPER 角色。
     *
     * @param userId 用户ID
     */
    private void revokeKeeperRole(Long userId) {
        Role keeperRole = findKeeperRole();
        if (keeperRole != null && userId != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUser_id_wsh, userId)
                    .eq(UserRole::getRole_id_wsh, keeperRole.getId_wsh()));
        }
    }

    /**
     * 查找系统中 KEEPER 角色的定义。
     *
     * @return 角色实体，未配置时返回 null
     */
    private Role findKeeperRole() {
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode_wsh, "KEEPER"));
    }

    /**
     * 将看护者实体转换为 VO（含资质信息）。
     *
     * @param k 看护者实体
     * @return 看护者VO
     */
    private KeeperVO toKeeperVO(Keeper k) {
        KeeperVO vo = new KeeperVO();
        vo.setId_wsh(k.getId_wsh());
        vo.setMerchant_id_wsh(k.getMerchant_id_wsh());
        vo.setUser_id_wsh(k.getUser_id_wsh());
        vo.setName_wsh(k.getName_wsh());
        vo.setPhone_wsh(k.getPhone_wsh());
        vo.setAvatar_wsh(k.getAvatar_wsh());
        vo.setExperience_years_wsh(k.getExperience_years_wsh());
        vo.setRating_wsh(k.getRating_wsh());
        vo.setCompletion_rate_wsh(k.getCompletion_rate_wsh());
        vo.setComplaint_rate_wsh(k.getComplaint_rate_wsh());
        vo.setPrice_per_day_wsh(k.getPrice_per_day_wsh());
        vo.setMax_pets_wsh(k.getMax_pets_wsh());
        vo.setCurrent_pets_wsh(k.getCurrent_pets_wsh());
        vo.setBio_wsh(k.getBio_wsh());
        vo.setStatus_wsh(k.getStatus_wsh());
        vo.setQualifications_wsh(qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_KEEPER, k.getId_wsh(), true));
        return vo;
    }
}
