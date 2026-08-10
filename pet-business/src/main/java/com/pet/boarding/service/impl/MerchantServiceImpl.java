package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.boarding.entity.BusinessHours;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.constant.MerchantStoreConstants;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 商家管理服务实现。
 * <p>
 * 负责商家入驻、审核、信息维护、店铺营业模式切换以及店铺营业状态的实时计算。
 * 店铺营业状态由 审核状态 + 营业模式 + 营业时间配置 三者联动决定。
 */
@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final QualificationService qualificationService;
    private final BusinessHoursMapper businessHoursMapper;
    private final KeeperService keeperService;
    private final UserMapper userMapper;

    public MerchantServiceImpl(MerchantMapper merchantMapper,
                               QualificationService qualificationService,
                               BusinessHoursMapper businessHoursMapper,
                               KeeperService keeperService,
                               UserMapper userMapper) {
        this.merchantMapper = merchantMapper;
        this.qualificationService = qualificationService;
        this.businessHoursMapper = businessHoursMapper;
        this.keeperService = keeperService;
        this.userMapper = userMapper;
    }

    /**
     * 【查询商家列表】
     *
     * 业务作用：
     * 获取系统中所有商家的全量列表。
     *
     * 调用场景：
     * 管理后台的商家管理页面加载时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * listAll
     * ↓
     * MerchantMapper.selectList（按创建时间倒序）
     *
     * 数据处理：
     * 直接查询 merchant 表全部数据，按 created_at_wsh 字段倒序排列。
     *
     * 业务规则：
     * 返回所有状态的商家（含待审核、已驳回、已通过），不做状态过滤。
     *
     * 状态影响：
     * 只读操作。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 数据量大时需要考虑分页。
     */
    @Override
    public List<Merchant> listAll() {
        log.info("listAll() called");
        return merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>().orderByDesc(Merchant::getCreated_at_wsh));
    }

    /**
     * 【根据ID查询商家】
     *
     * 业务作用：
     * 根据主键ID查询商家信息，是其他业务方法的底层依赖。
     *
     * 调用场景：
     * 被 update、approve、reject、updateStoreMode 等业务方法内部调用。
     *
     * 调用链：
     * 上层业务方法
     * ↓
     * getById
     * ↓
     * MerchantMapper.selectById
     *
     * 数据处理：
     * 按主键ID从 merchant 表查询单条记录。
     *
     * 业务规则：
     * 查询结果为 null 时抛出 BusinessException，确保调用方获得有效的商家实体。
     *
     * 状态影响：
     * 只读操作。
     *
     * 异常情况：
     * 商家不存在时抛出 BusinessException("商家不存在")。
     */
    @Override
    public Merchant getById(Long id) {
        log.info("getById() called");
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }

    /**
     * 【批量查询商家】
     *
     * 业务作用：
     * 根据多个商家ID批量查询，减少数据库查询次数。
     *
     * 调用场景：
     * 需要一次展示多个商家详情的场景。
     *
     * 调用链：
     * 上层业务方法
     * ↓
     * listByIds
     * ↓
     * MerchantMapper.selectBatchIds
     *
     * 数据处理：
     * 按ID集合从 merchant 表批量查询，入参为空时直接返回空列表。
     *
     * 业务规则：
     * ids 为 null 或空集合时返回空列表，不做数据库查询。
     *
     * 状态影响：
     * 只读操作。
     */
    @Override
    public List<Merchant> listByIds(Collection<Long> ids) {
        log.info("listByIds() called");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return merchantMapper.selectBatchIds(ids);
    }

    /**
     * 【搜索附近商家】
     *
     * 业务作用：
     * 基于用户地理位置和半径搜索附近已审核通过的商家。
     *
     * 调用场景：
     * 用户端首页或地图模式搜索附近宠物寄养商家时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * searchNearby
     * ↓
     * MerchantMapper.searchNearby（SQL Haversine） + calculateDistance（Java精确计算）
     *
     * 数据处理：
     * SQL 层使用 Haversine 公式过滤附近商家，Java 层再次精确计算距离并保留两位小数。
     *
     * 业务规则：
     * 仅搜索已审核通过且未逻辑删除的商家；radius 单位为公里。
     *
     * 状态影响：
     * 只读操作。
     */
    @Override
    public List<MerchantDTO> searchNearby(double lat, double lng, double radius) {
        log.info("searchNearby() called");
        List<Merchant> merchants = merchantMapper.searchNearby(lat, lng, radius);
        return merchants.stream().map(m -> {
            MerchantDTO dto = toDTO(m);
            double d = calculateDistance(lat, lng,
                    m.getLatitude_wsh().doubleValue(), m.getLongitude_wsh().doubleValue());
            dto.setDistance_wsh(Math.round(d * 100.0) / 100.0);
            return dto;
        }).toList();
    }

    /**
     * 【根据用户ID查找商家】
     *
     * 业务作用：
     * 查询用户是否已入驻成为商家。
     *
     * 调用场景：
     * 用户登录成功后查询商家身份；商家后台校验时调用。
     *
     * 调用链：
     * 身份校验模块
     * ↓
     * findByUserId
     * ↓
     * MerchantMapper.selectOne（按 user_id）
     *
     * 数据处理：
     * 按 user_id 字段从 merchant 表查询单条记录。
     *
     * 业务规则：
     * 一个用户最多拥有一个商家账号；未找到时返回 null 而非抛异常。
     *
     * 状态影响：
     * 只读操作。
     */
    @Override
    public Merchant findByUserId(Long userId) {
        log.info("findByUserId() called");
        return merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId));
    }

    /**
     * 使用 Haversine 公式计算两点之间的球面距离。
     *
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 距离（公里）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(
                Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        return s * 6371;
    }

    /**
     * 【创建商家入驻申请】
     *
     * 业务作用：
     * 提交商家入驻申请，新建商家记录并同步创建资质审核记录。
     *
     * 调用场景：
     * 用户填写入驻信息并提交时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * create @Transactional
     * ↓
     * MerchantMapper.insert + QualificationService.createPending
     *
     * 数据处理：
     * 将 DTO 字段映射到商家实体，初始状态 MERCHANT_PENDING，自动模式 MODE_AUTO，店铺状态 STATUS_CLOSED。
     * 资质图片从 business_license 和 qualification_image 中取第一个非空值。
     *
     * 业务规则：
     * 创建后必须经 admin 审核才能营业；创建即提交资质审核。
     *
     * 状态影响：
     * 新增商家记录（MERCHANT_PENDING）；新增资质审核记录。
     *
     * 事务边界：
     * 商家写入 + 资质创建在同一 @Transactional 事务中。
     */
    @Override
    @Transactional
    public Merchant create(MerchantCreateRequestDTO dto, Long userId) {
        log.info("create() called");
        Merchant merchant = new Merchant();
        merchant.setUser_id_wsh(userId);
        merchant.setName_wsh(dto.getName_wsh());
        merchant.setPhone_wsh(dto.getPhone_wsh());
        merchant.setAddress_wsh(dto.getAddress_wsh());
        merchant.setLatitude_wsh(dto.getLatitude_wsh());
        merchant.setLongitude_wsh(dto.getLongitude_wsh());
        merchant.setDescription_wsh(dto.getDescription_wsh());
        merchant.setBusiness_license_wsh(firstText(dto.getBusiness_license_wsh(), dto.getQualification_image_wsh()));
        merchant.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchant.setFuture_booking_enabled_wsh(dto.getFuture_booking_enabled_wsh() == null
                ? 1 : dto.getFuture_booking_enabled_wsh());
        merchantMapper.insert(merchant);
        qualificationService.createPending(
                QualificationService.OWNER_TYPE_MERCHANT,
                merchant.getId_wsh(),
                userId,
                QualificationService.QUAL_TYPE_BUSINESS_LICENSE,
                "营业执照",
                merchant.getBusiness_license_wsh(),
                "商户入驻资质");
        return merchant;
    }

    /**
     * 【更新商家基础信息】
     *
     * 业务作用：
     * 修改商家的基础信息（名称、电话、地址、描述等）。
     *
     * 调用场景：
     * 商家在后台管理页面编辑基本信息时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * update @Transactional
     * ↓
     * MerchantMapper.updateById
     *
     * 数据处理：
     * 仅更新 DTO 中非 null 的字段，null 字段保持原值。
     *
     * 业务规则：
     * 仅更新非 null 字段，不修改审核状态或营业模式。
     *
     * 状态影响：
     * 仅影响商家基础信息字段。
     *
     * 事务边界：
     * 查询 + 更新在同一事务中。
     */
    @Override
    @Transactional
    public Merchant update(Long id, MerchantUpdateRequestDTO dto) {
        log.info("update() called");
        Merchant existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getPhone_wsh() != null) existing.setPhone_wsh(dto.getPhone_wsh());
        if (dto.getAddress_wsh() != null) existing.setAddress_wsh(dto.getAddress_wsh());
        if (dto.getLatitude_wsh() != null) existing.setLatitude_wsh(dto.getLatitude_wsh());
        if (dto.getLongitude_wsh() != null) existing.setLongitude_wsh(dto.getLongitude_wsh());
        if (dto.getDescription_wsh() != null) existing.setDescription_wsh(dto.getDescription_wsh());
        if (dto.getFuture_booking_enabled_wsh() != null) {
            Integer enabled = dto.getFuture_booking_enabled_wsh();
            if (enabled != 0 && enabled != 1) {
                throw new BusinessException(400, "future_booking_enabled_wsh 仅允许 0 或 1");
            }
            existing.setFuture_booking_enabled_wsh(enabled);
        }
        merchantMapper.updateById(existing);
        return existing;
    }

    /**
     * 【商家实体转DTO】
     *
     * 业务作用：
     * 将商家实体转换为前端展示所需的 DTO（含资质信息和实时店铺状态）。
     *
     * 调用场景：
     * Controller 层返回商家信息前调用。
     *
     * 调用链：
     * 各查询 Controller
     * ↓
     * toDTO
     * ↓
     * resolveStoreStatus + qualificationService.listByOwner
     *
     * 数据处理：
     * 字段拷贝；实时计算店铺状态写入 store_status_wsh；加载资质列表。
     *
     * 业务规则：
     * 店铺状态实时计算而非直接使用 DB 值；仅返回启用状态的资质记录。
     *
     * 状态影响：
     * 只读操作。
     */
    @Override
    public MerchantDTO toDTO(Merchant entity) {
        if (entity == null) return null;
        MerchantDTO dto = new MerchantDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setPhone_wsh(entity.getPhone_wsh());
        dto.setAddress_wsh(entity.getAddress_wsh());
        dto.setLatitude_wsh(entity.getLatitude_wsh());
        dto.setLongitude_wsh(entity.getLongitude_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setBusiness_license_wsh(entity.getBusiness_license_wsh());
        dto.setRating_wsh(entity.getRating_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setStore_mode_wsh(entity.getStore_mode_wsh());
        dto.setStore_status_wsh(resolveStoreStatus(entity));
        dto.setFuture_booking_enabled_wsh(entity.getFuture_booking_enabled_wsh() == null ? 1 : entity.getFuture_booking_enabled_wsh());
        fillOwnerInfo(dto, entity);
        dto.setQualifications_wsh(qualificationService.listByOwner(
                QualificationService.OWNER_TYPE_MERCHANT, entity.getId_wsh(), true));
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * 【填充店主信息】
     *
     * 业务作用：
     * 查询该店铺归属用户（店主）的昵称、头像，并在店主同时是看护者时
     * 填充其看护者主页ID，便于前端展示“这家店铺属于谁”并提供店主主页入口。
     *
     * 业务规则：
     * 店主用户不存在时静默跳过；店主没有看护者档案时 owner_keeper_id_wsh 保持为空。
     */
    private void fillOwnerInfo(MerchantDTO dto, Merchant entity) {
        Long ownerUserId = entity.getUser_id_wsh();
        if (ownerUserId == null) {
            return;
        }
        User owner = userMapper.selectById(ownerUserId);
        if (owner != null) {
            dto.setOwner_name_wsh(owner.getNickname_wsh() != null && !owner.getNickname_wsh().isBlank()
                    ? owner.getNickname_wsh() : owner.getUsername_wsh());
            dto.setOwner_avatar_wsh(owner.getAvatar_wsh());
        }
        Keeper ownerKeeper = keeperService.findByUserId(ownerUserId);
        if (ownerKeeper != null) {
            dto.setOwner_keeper_id_wsh(ownerKeeper.getId_wsh());
        }
    }

    /**
     * 取两个字符串中第一个非空非空白的值，如果都为空则返回 null。
     */
    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    /**
     * 【审核通过商家入驻申请】
     *
     * 业务作用：
     * 平台管理员审核通过商家入驻申请。
     *
     * 调用场景：
     * 管理员后台审核通过时调用。
     *
     * 调用链：
     * AdminController
     * ↓
     * approve @Transactional
     * ↓
     * MerchantMapper.updateById + refreshStoreState（私有方法）
     *
     * 数据处理：
     * 状态从 PENDING 更新为 MERCHANT_APPROVED，营业模式设为 MODE_AUTO，然后刷新店铺营业状态。
     *
     * 业务规则：
     * 审核通过后进入自动模式；立即根据当前时间计算店铺是否应该开门。
     *
     * 状态影响：
     * status: PENDING → MERCHANT_APPROVED；store_mode → MODE_AUTO；store_status 可能变化。
     *
     * 事务边界：
     * 状态更新 + 店铺刷新在同一事务中。
     */
    @Override
    @Transactional
    public void approve(Long id) {
        log.info("approve() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchantMapper.updateById(merchant);
        refreshStoreState(merchant, LocalDateTime.now());
    }

    /**
     * 【驳回商家入驻申请】
     *
     * 业务作用：
     * 平台管理员驳回商家入驻申请，强制关店并通知旗下看护者下线。
     *
     * 调用场景：
     * 管理员后台驳回商家申请时调用。
     *
     * 调用链：
     * AdminController
     * ↓
     * reject @Transactional
     * ↓
     * MerchantMapper.updateById + KeeperService.syncMerchantStoreStatus
     *
     * 数据处理：
     * 状态更新为 MERCHANT_REJECTED，模式设为 MODE_MANUAL_CLOSED，店铺状态设为 STATUS_CLOSED，
     * 同步通知旗下所有看护者下线。
     *
     * 业务规则：
     * 驳回后商家强制关店，旗下所有看护者自动下线（storeOpen=false）。
     *
     * 状态影响：
     * status: PENDING → MERCHANT_REJECTED；store_mode → MODE_MANUAL_CLOSED；
     * store_status → STATUS_CLOSED；旗下看护者状态同步下线。
     *
     * 事务边界：
     * 状态更新 + 看护者同步通知在同一事务中。
     */
    @Override
    @Transactional
    public void reject(Long id) {
        log.info("reject() called");
        Merchant merchant = getById(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_REJECTED.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_MANUAL_CLOSED);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchantMapper.updateById(merchant);
        keeperService.syncMerchantStoreStatus(merchant.getId_wsh(), false);
    }

    /**
     * 【更新商家营业模式】
     *
     * 业务作用：
     * 切换商家店铺的营业模式（自动/手动开门/手动关店）。
     *
     * 调用场景：
     * 商家后台管理页面切换营业模式时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * updateStoreMode @Transactional
     * ↓
     * MerchantMapper.updateById + refreshStoreState（私有方法）
     *
     * 数据处理：
     * 校验模式参数合法性（null 检查 + 枚举值校验），更新 store_mode，触发店铺状态重新计算。
     *
     * 业务规则：
     * 仅支持 MODE_AUTO、MODE_MANUAL_OPEN、MODE_MANUAL_CLOSED 三种；非法参数直接抛异常。
     *
     * 状态影响：
     * store_mode 更新；store_status 根据新模式重新计算。
     *
     * 事务边界：
     * 模式更新 + 店铺刷新在同一事务中。
     *
     * 异常情况：
     * storeMode 为 null 或无效枚举值时抛 BusinessException。
     */
    @Override
    @Transactional
    public Merchant updateStoreMode(Long id, Integer storeMode) {
        log.info("updateStoreMode() called");
        Merchant merchant = getById(id);
        if (storeMode == null) {
            throw new BusinessException(400, "营业模式不能为空");
        }
        if (storeMode != MerchantStoreConstants.MODE_AUTO
                && storeMode != MerchantStoreConstants.MODE_MANUAL_OPEN
                && storeMode != MerchantStoreConstants.MODE_MANUAL_CLOSED) {
            throw new BusinessException(400, "营业模式无效");
        }
        merchant.setStore_mode_wsh(storeMode);
        merchantMapper.updateById(merchant);
        refreshStoreState(merchant, LocalDateTime.now());
        return merchant;
    }

    /**
     * 【刷新指定商家店铺营业状态】
     *
     * 业务作用：
     * 根据商家审核状态、营业模式、营业时间配置重新计算当前店铺营业状态。
     *
     * 调用场景：
     * BusinessHoursService 在营业时间变更时触发；审核通过/驳回后调用。
     *
     * 调用链：
     * BusinessHoursService / 审核方法
     * ↓
     * refreshStoreState @Transactional
     * ↓
     * refreshStoreState(merchant, now) → resolveStoreStatus → keeperService.syncMerchantStoreStatus
     *
     * 数据处理：
     * 查询商家信息，计算营业状态，若不一致则更新 DB 并同步看护者。
     *
     * 业务规则：
     * 状态变化时才更新 DB；变化时同步通知旗下所有看护者上线/下线。
     *
     * 状态影响：
     * 可能更新 store_status；可能同步变更旗下看护者在线状态。
     *
     * 事务边界：
     * DB 更新 + 看护者同步在同一事务中。
     */
    @Override
    @Transactional
    public void refreshStoreState(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        refreshStoreState(merchant, LocalDateTime.now());
    }

    /**
     * 【批量刷新所有商家店铺营业状态】
     *
     * 业务作用：
     * 遍历所有已审核通过的商家，逐个计算当前时间点的营业状态。
     *
     * 调用场景：
     * MerchantStoreSyncScheduler 定时任务每分钟执行一次。
     *
     * 调用链：
     * MerchantStoreSyncScheduler
     * ↓
     * refreshAllStoreStates @Transactional
     * ↓
     * 循环 → refreshStoreState(merchant, now)
     *
     * 数据处理：
     * 查询所有已审核通过且未逻辑删除的商家，逐个调用 refreshStoreState。
     *
     * 业务规则：
     * 仅处理已审核通过且未删除的商家；仅当状态变更时才更新 DB。
     *
     * 状态影响：
     * 每个状态变化的商家都会触发 store_status 更新和看护者状态同步。
     *
     * 注意事项：
     * 商家数量多时可能耗时较长，注意定时任务频率。
     */
    @Override
    @Transactional
    public void refreshAllStoreStates() {
        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus_wsh, StatusCode.MERCHANT_APPROVED.getValue())
                        .eq(Merchant::getDeleted_wsh, 0));
        LocalDateTime now = LocalDateTime.now();
        for (Merchant merchant : merchants) {
            refreshStoreState(merchant, now);
        }
    }

    /**
     * 【校验商家所有者】
     *
     * 业务作用：
     * 校验指定用户是否为指定商家的所有者，用于权限判断。
     *
     * 调用场景：
     * 各业务方法中校验当前操作人是否有权管理该商家时调用。
     *
     * 调用链：
     * 各业务方法
     * ↓
     * isOwner
     * ↓
     * MerchantMapper.selectOne（按 id + user_id 联合查询）
     *
     * 数据处理：
     * 按 merchantId 和 userId 联合查询，存在匹配记录返回 true。
     *
     * 业务规则：
     * 一个商家只能有一个 owner；查询不到返回 false。
     *
     * 状态影响：
     * 只读操作。
     */
    @Override
    public boolean isOwner(Long merchantId, Long userId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getId_wsh, merchantId)
                        .eq(Merchant::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        return merchant != null;
    }

    /**
     * 刷新指定商家在给定时间点的店铺营业状态。
     * <p>
     * <b>决策逻辑：</b>
     * <ol>
     *   <li>调用 {@link #resolveStoreStatus(Merchant, LocalDateTime)} 解析状态</li>
     *   <li>如果解析结果与当前 DB 中的 store_status 不一致，则更新 DB 并同步看护者</li>
     * </ol>
     * <p>
     * <b>状态联动：</b>店铺开门 → 通知该商家下所有看护者上线；
     * 店铺关门 → 通知该商家下所有看护者下线。
     *
     * @param merchant 商家实体（可为 null，此时直接返回）
     * @param now      当前时间点
     */
    private void refreshStoreState(Merchant merchant, LocalDateTime now) {
        if (merchant == null) {
            return;
        }
        int resolved = resolveStoreStatus(merchant, now);
        Integer current = merchant.getStore_status_wsh();
        if (current == null || current != resolved) {
            merchant.setStore_status_wsh(resolved);
            merchantMapper.updateById(merchant);
            keeperService.syncMerchantStoreStatus(merchant.getId_wsh(), resolved == MerchantStoreConstants.STATUS_OPEN);
        }
    }

    /**
     * 解析商家当前店铺营业状态（使用当前系统时间）。
     *
     * @param merchant 商家实体
     * @return {@link MerchantStoreConstants#STATUS_OPEN} 或 {@link MerchantStoreConstants#STATUS_CLOSED}
     */
    private int resolveStoreStatus(Merchant merchant) {
        return resolveStoreStatus(merchant, LocalDateTime.now());
    }

    /**
     * 解析商家在指定时间点的店铺营业状态。
     * <p>
     * <b>决策优先级：</b>
     * <ol>
     *   <li>商家为 null → 关闭</li>
     *   <li>审核状态非 APPROVED → 关闭（未审核通过不可营业）</li>
     *   <li>营业模式为 MODE_MANUAL_OPEN → 强制开门</li>
     *   <li>营业模式为 MODE_MANUAL_CLOSED → 强制关闭</li>
     *   <li>自动模式下（默认）：查询当天营业时间配置 → 当前时间是否落在营业时段内</li>
     *   <li>当天无营业时间配置或所有时段标记为休息 → 关闭</li>
     * </ol>
     *
     * @param merchant 商家实体
     * @param now      当前时间点（用于确定星期几并与营业时间比较）
     * @return {@link MerchantStoreConstants#STATUS_OPEN} 或 {@link MerchantStoreConstants#STATUS_CLOSED}
     */
    private int resolveStoreStatus(Merchant merchant, LocalDateTime now) {
        if (merchant == null) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        if (merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        Integer mode = merchant.getStore_mode_wsh();
        if (mode == null) {
            mode = MerchantStoreConstants.MODE_AUTO;
        }
        if (mode == MerchantStoreConstants.MODE_MANUAL_OPEN) {
            return MerchantStoreConstants.STATUS_OPEN;
        }
        if (mode == MerchantStoreConstants.MODE_MANUAL_CLOSED) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        if (now == null) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        List<BusinessHours> hours = businessHoursMapper.selectList(
                new LambdaQueryWrapper<BusinessHours>()
                        .eq(BusinessHours::getMerchant_id_wsh, merchant.getId_wsh())
                        .eq(BusinessHours::getDay_of_week_wsh, now.getDayOfWeek().getValue())
                        .eq(BusinessHours::getDeleted_wsh, 0));
        if (hours == null || hours.isEmpty()) {
            return MerchantStoreConstants.STATUS_CLOSED;
        }
        return hours.stream()
                .filter(h -> h.getIs_closed_wsh() == null || h.getIs_closed_wsh() == 0)
                .anyMatch(h -> isWithinRange(now.toLocalTime(), h.getOpen_time_wsh(), h.getClose_time_wsh()))
                ? MerchantStoreConstants.STATUS_OPEN
                : MerchantStoreConstants.STATUS_CLOSED;
    }

    /**
     * 判断当前时间是否落在指定的营业时段内。
     * <p>
     * <b>跨天处理：</b>如果 close 小于 open（如 22:00 - 02:00），视为跨天营业，
     * 当前时间在 open ~ 次日 close 之间或在 00:00 ~ close 之间均视为在时段内。
     * <p>
     * <b>边界规则：</b>open 等于 close 时视为全天营业（返回 true）。
     *
     * @param now   当前时间
     * @param open  开门时间
     * @param close 关门时间
     * @return true 表示当前时间在营业时段内
     */
    private boolean isWithinRange(LocalTime now, LocalTime open, LocalTime close) {
        if (open == null || close == null) {
            return false;
        }
        if (open.equals(close)) {
            return true;
        }
        if (open.isBefore(close)) {
            return !now.isBefore(open) && !now.isAfter(close);
        }
        return !now.isBefore(open) || !now.isAfter(close);
    }
}
