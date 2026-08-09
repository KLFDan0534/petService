package com.pet.boarding.service;

import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.boarding.entity.Merchant;

import java.util.Collection;
import java.util.List;

/**
 * 商家管理服务接口。
 * <p>
 * 提供商户/商家的全生命周期管理，包括：
 * <ul>
 *   <li>商家入驻申请、审核（通过/驳回）</li>
 *   <li>商家信息查询、创建、更新</li>
 *   <li>店铺营业模式切换（自动/手动开门/手动关店）</li>
 *   <li>店铺营业状态刷新（基于营业时间+模式联动计算）</li>
 *   <li>附近商家搜索（LBS地理围栏）</li>
 *   <li>商家归属权校验</li>
 * </ul>
 * 审核通过的商家才能上架服务并营业。店铺状态依赖于营业时间配置和手动模式的组合。
 */
public interface MerchantService {

    /**
     * 【查询商家列表】
     *
     * 业务作用：
     * 获取系统中所有商家的全量列表，用于管理后台展示。
     *
     * 调用场景：
     * 管理后台的商家管理页面加载时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * listAll
     * ↓
     * MerchantMapper.selectList
     *
     * 数据处理：
     * 直接查询 merchant 表全部数据，按创建时间倒序排列。
     *
     * 业务规则：
     * 返回所有状态的商家（含待审核、已驳回、已通过），不做状态过滤。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 数据量大时需要考虑分页。
     *
     * @return 所有商家实体列表
     */
    List<Merchant> listAll();

    /**
     * 【根据ID查询商家】
     *
     * 业务作用：
     * 根据主键ID精确查询商家信息，是其他业务方法的底层依赖方法。
     *
     * 调用场景：
     * 被 update、approve、reject、updateStoreMode 等业务方法内部调用，用于获取待操作的商家实体。
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
     * 如果查询结果为 null，直接抛出 BusinessException。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 商家不存在时抛出 BusinessException("商家不存在")。
     *
     * 注意事项：
     * 此方法会被事务方法调用，注意事务传播行为。
     *
     * @param id 商家ID
     * @return 商家实体
     * @throws BusinessException 如果商家不存在
     */
    Merchant getById(Long id);

    /**
     * 【批量查询商家】
     *
     * 业务作用：
     * 根据多个商家ID批量查询商家信息，减少数据库查询次数。
     *
     * 调用场景：
     * 在需要一次展示多个商家详情的场景下调用，如订单关联的商家信息加载。
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
     * ids 为 null 或空集合时返回空列表，不会抛异常。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 返回结果顺序与 ids 传入顺序不一定一致。
     *
     * @param ids 商家ID集合
     * @return 商家实体列表，不会返回 null
     */
    List<Merchant> listByIds(Collection<Long> ids);

    /**
     * 【搜索附近商家】
     *
     * 业务作用：
     * 基于用户当前地理位置和搜索半径，查找附近已审核通过的商家，用于用户端发现附近服务。
     *
     * 调用场景：
     * 用户在首页或地图模式搜索附近宠物寄养商家时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * searchNearby
     * ↓
     * MerchantMapper.searchNearby（SQL Haversine 公式计算距离）
     *
     * 数据处理：
     * 使用 Haversine 球面距离公式在 SQL 层过滤附近商家，Java 层再次计算精确距离并保留两位小数。
     *
     * 业务规则：
     * 仅搜索 status == MERCHANT_APPROVED 且未逻辑删除的商家；radius 单位为公里。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 返回结果附加上距离字段 distance_wsh；SQL 层和 Java 层两次计算距离确保精度。
     *
     * @param lat    用户纬度
     * @param lng    用户经度
     * @param radius 搜索半径（公里）
     * @return 附近商家DTO列表，每个DTO包含距离信息
     */
    List<MerchantDTO> searchNearby(double lat, double lng, double radius);

    /**
     * 【根据用户ID查找商家】
     *
     * 业务作用：
     * 根据用户ID查询该用户是否已入驻成为商家，用于登录后判断用户身份。
     *
     * 调用场景：
     * 用户登录成功后，查询用户是否拥有商家账号；或商家后台校验商家身份时调用。
     *
     * 调用链：
     * 身份校验/用户模块
     * ↓
     * findByUserId
     * ↓
     * MerchantMapper.selectOne
     *
     * 数据处理：
     * 按 user_id 字段从 merchant 表查询单条记录。
     *
     * 业务规则：
     * 一个用户最多拥有一个商家账号（user_id 唯一约束）；未找到时返回 null 而非抛异常。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 无特殊异常，未找到返回 null。
     *
     * 注意事项：
     * 返回 null 时应由调用方处理"尚未入驻"的逻辑。
     *
     * @param userId 用户ID
     * @return 商家实体，未找到时返回 null
     */
    Merchant findByUserId(Long userId);

    /**
     * 【创建商家入驻申请】
     *
     * 业务作用：
     * 用户提交商家入驻申请，创建商家记录并同步提交资质审核材料。
     *
     * 调用场景：
     * 用户在入驻页面填写商家信息并提交时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * create
     * ↓
     * MerchantMapper.insert + QualificationService.createPending
     *
     * 数据处理：
     * 将 DTO 字段映射到商家实体，初始状态设为待审核（MERCHANT_PENDING），
     * 营业模式设为自动（MODE_AUTO），店铺状态设为关闭（STATUS_CLOSED），
     * 写入数据库后同步创建资质审核记录。
     *
     * 业务规则：
     * 商家创建后必须经过 admin 审核（approve/reject）后才能正常营业；创建即提交资质审核申请。
     *
     * 状态影响：
     * 新增商家记录，状态为 MERCHANT_PENDING；同步创建资质审核记录。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 事务性操作——商家记录和资质审核记录在同一事务中写入。
     *
     * @param dto    商家创建请求DTO
     * @param userId 操作人用户ID（即该商家的所有者）
     * @return 创建后的商家实体
     */
    Merchant create(MerchantCreateRequestDTO dto, Long userId);

    /**
     * 【更新商家基础信息】
     *
     * 业务作用：
     * 修改商家的基础信息（名称、电话、地址、描述等），不影响审核状态和营业模式。
     *
     * 调用场景：
     * 商家在后台管理页面编辑基本信息时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * update
     * ↓
     * MerchantMapper.updateById
     *
     * 数据处理：
     * 仅更新 DTO 中非 null 的字段，null 字段保持数据库原值。
     *
     * 业务规则：
     * 仅更新非 null 字段，不会修改状态、模式等业务字段。
     *
     * 状态影响：
     * 仅影响商家基础信息字段，不修改审核状态或营业状态。
     *
     * 异常情况：
     * 商家不存在时抛出 BusinessException。
     *
     * 注意事项：
     * 此方法不处理资质信息的更新，资质更新需单独调用资质服务。
     *
     * @param id  商家ID
     * @param dto 商家更新请求DTO
     * @return 更新后的商家实体
     * @throws BusinessException 如果商家不存在
     */
    Merchant update(Long id, MerchantUpdateRequestDTO dto);

    /**
     * 【商家实体转DTO】
     *
     * 业务作用：
     * 将商家实体转换为前端展示所需的 DTO，包含资质信息和实时计算的店铺营业状态。
     *
     * 调用场景：
     * 在 Controller 层返回商家信息前调用，确保返回给前端的数据包含完整的资质和营业状态。
     *
     * 调用链：
     * 各查询业务的 Controller
     * ↓
     * toDTO
     * ↓
     * resolveStoreStatus + qualificationService.listByOwner
     *
     * 数据处理：
     * 字段拷贝至 DTO；调用 resolveStoreStatus 解析实时店铺状态写入 store_status_wsh；
     * 调用 qualificationService.listByOwner 加载商家资质列表。
     *
     * 业务规则：
     * 店铺状态实时计算而非直接使用 DB 值；资质信息仅返回启用状态的记录。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 入参为 null 时返回 null。
     *
     * 注意事项：
     * 店铺状态是实时计算的，可能和 DB 中的 store_status 值不一致。
     *
     * @param entity 商家实体
     * @return 商家DTO（含资质信息和解析后的店铺状态），入参为 null 时返回 null
     */
    MerchantDTO toDTO(Merchant entity);

    /**
     * 【审核通过商家入驻申请】
     *
     * 业务作用：
     * 平台管理员审核通过商家的入驻申请，使其具备营业资格。
     *
     * 调用场景：
     * 管理员在后台审核商家入驻申请时调用。
     *
     * 调用链：
     * AdminController
     * ↓
     * approve
     * ↓
     * MerchantMapper.updateById + refreshStoreState
     *
     * 数据处理：
     * 将商家状态从 PENDING 更新为 MERCHANT_APPROVED，营业模式设为自动模式（MODE_AUTO），
     * 然后重新计算当前店铺营业状态。
     *
     * 业务规则：
     * 审核通过后店铺自动进入自动模式；触发店铺状态刷新以决定当前是否开门。
     *
     * 状态影响：
     * status: PENDING → MERCHANT_APPROVED；store_mode 设为 MODE_AUTO；store_status 可能变化。
     *
     * 异常情况：
     * 商家不存在时抛出 BusinessException。
     *
     * 注意事项：
     * 审核通过后商家才能发布服务项目和正常营业。
     *
     * @param id 商家ID
     * @throws BusinessException 如果商家不存在
     */
    void approve(Long id);

    /**
     * 【驳回商家入驻申请】
     *
     * 业务作用：
     * 平台管理员驳回商家的入驻申请，同时强制关店并通知旗下看护者下线。
     *
     * 调用场景：
     * 管理员在后台审核商家入驻申请时驳回操作时调用。
     *
     * 调用链：
     * AdminController
     * ↓
     * reject
     * ↓
     * MerchantMapper.updateById + KeeperService.syncMerchantStoreStatus
     *
     * 数据处理：
     * 将商家状态从 PENDING 更新为 MERCHANT_REJECTED，营业模式设为手动关店（MODE_MANUAL_CLOSED），
     * 店铺状态设为关闭（STATUS_CLOSED），同时通知该商家旗下所有看护者下线。
     *
     * 业务规则：
     * 驳回后商家强制关店，旗下所有看护者自动下线。
     *
     * 状态影响：
     * status: PENDING → MERCHANT_REJECTED；store_mode 设为 MODE_MANUAL_CLOSED；
     * store_status 设为 STATUS_CLOSED；旗下看护者状态同步更新为离线。
     *
     * 异常情况：
     * 商家不存在时抛出 BusinessException。
     *
     * 注意事项：
     * 被驳回的商家可以重新提交入驻申请。
     *
     * @param id 商家ID
     * @throws BusinessException 如果商家不存在
     */
    void reject(Long id);

    /**
     * 【更新商家营业模式】
     *
     * 业务作用：
     * 切换商家的店铺营业模式，支持自动模式、手动开门、手动关店三种模式。
     *
     * 调用场景：
     * 商家在后台管理页面切换店铺营业模式时调用。
     *
     * 调用链：
     * MerchantController
     * ↓
     * updateStoreMode
     * ↓
     * MerchantMapper.updateById + refreshStoreState
     *
     * 数据处理：
     * 校验模式参数合法性后更新 store_mode 字段，然后触发店铺状态重新计算。
     *
     * 业务规则：
     * 仅支持 MODE_AUTO（自动）、MODE_MANUAL_OPEN（手动开门）、MODE_MANUAL_CLOSED（手动关店）三种模式；
     * 更新模式后立即重新计算店铺营业状态。
     *
     * 状态影响：
     * store_mode 更新为指定值；store_status 根据新模式重新计算更新。
     *
     * 异常情况：
     * 模式参数为 null 或无效时抛出 BusinessException；商家不存在时抛出 BusinessException。
     *
     * 注意事项：
     * 手动开门模式下店铺将强制保持开门状态，不受营业时间限制。
     *
     * @param id        商家ID
     * @param storeMode 目标营业模式
     * @return 更新后的商家实体
     * @throws BusinessException 如果模式参数为 null 或无效，或商家不存在
     */
    Merchant updateStoreMode(Long id, Integer storeMode);

    /**
     * 【刷新指定商家店铺营业状态】
     *
     * 业务作用：
     * 根据商家的审核状态、营业模式、营业时间配置，重新计算当前店铺营业状态。
     *
     * 调用场景：
     * 营业时间变更时由 BusinessHoursService 触发调用；审核通过/驳回后调用。
     *
     * 调用链：
     * BusinessHoursService / 审核方法
     * ↓
     * refreshStoreState
     * ↓
     * refreshStoreState(Merchant, LocalDateTime) → resolveStoreStatus → keeperService.syncMerchantStoreStatus
     *
     * 数据处理：
     * 查询商家当前信息，调用 resolveStoreStatus 解析营业状态，如果状态发生变化则更新 DB 并同步通知看护者。
     *
     * 业务规则：
     * 状态变化时更新 DB 并通过 keeperService 同步通知旗下所有看护者上线/下线。
     *
     * 状态影响：
     * 如果计算出的营业状态与 DB 不一致，则更新 store_status；看护者在线状态同步变更。
     *
     * 异常情况：
     * 无特殊异常（商家不存在时不做处理）。
     *
     * 注意事项：
     * 此为底层刷新方法，被定时任务和业务方法共同调用。
     *
     * @param merchantId 商家ID
     */
    void refreshStoreState(Long merchantId);

    /**
     * 【批量刷新所有商家店铺营业状态】
     *
     * 业务作用：
     * 遍历所有已审核通过的商家，逐个重新计算当前时间点的营业状态，确保营业状态始终与营业时间配置一致。
     *
     * 调用场景：
     * 由 MerchantStoreSyncScheduler 定时任务每分钟执行一次，用于自动模式下店铺状态的定期同步。
     *
     * 调用链：
     * MerchantStoreSyncScheduler（定时任务）
     * ↓
     * refreshAllStoreStates
     * ↓
     * for 循环 → refreshStoreState(Merchant, LocalDateTime)
     *
     * 数据处理：
     * 查询所有 status == MERCHANT_APPROVED 且未逻辑删除的商家，逐个调用 refreshStoreState 计算当前状态。
     *
     * 业务规则：
     * 仅处理已审核通过且未删除的商家；仅当状态与 DB 不一致时才更新。
     *
     * 状态影响：
     * 对每个状态发生变化的商家，更新 store_status 并同步通知旗下看护者上线/下线。
     *
     * 异常情况：
     * 单个商家处理异常不影响其他商家（循环内逐条处理）。
     *
     * 注意事项：
     * 商家数量较多时可能耗时较长，注意定时任务执行频率与数据量的平衡。
     */
    void refreshAllStoreStates();

    /**
     * 【校验商家所有者】
     *
     * 业务作用：
     * 校验指定用户是否为指定商家的所有者，用于权限判断。
     *
     * 调用场景：
     * 在商家后台操作（如管理看护者、编辑服务项目等）前进行权限校验时调用。
     *
     * 调用链：
     * 权限校验拦截器/各业务方法
     * ↓
     * isOwner
     * ↓
     * MerchantMapper.selectOne（按 id + user_id 查询）
     *
     * 数据处理：
     * 按 merchantId 和 userId 联合查询，存在匹配记录则返回 true。
     *
     * 业务规则：
     * 一个商家只能有一个 owner（user_id）；查询不到匹配记录返回 false。
     *
     * 状态影响：
     * 只读操作，不修改任何业务状态。
     *
     * 异常情况：
     * 无特殊异常。
     *
     * 注意事项：
     * 返回 false 时调用方应拦截操作并提示无权限。
     *
     * @param merchantId 商家ID
     * @param userId     用户ID
     * @return true 表示该用户是该商家的所有者
     */
    boolean isOwner(Long merchantId, Long userId);
}
