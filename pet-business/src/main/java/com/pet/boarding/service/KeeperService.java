package com.pet.boarding.service;

import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.dto.KeeperUpdateRequestDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.vo.KeeperVO;

import java.util.Collection;
import java.util.List;

/**
 * 看护者（Keeper）管理服务接口。
 * <p>
 * 看护者是宠物寄养服务的实际执行人，隶属于商家。本接口提供：
 * <ul>
 *   <li>看护者入驻申请、审核（平台审核 + 商家审核双通道）</li>
 *   <li>看护者信息查询、创建、更新、删除</li>
 *   <li>看护者离职（主动辞职 / 商家解雇）</li>
 *   <li>看护者在线状态切换（在线/离线/忙碌）</li>
 *   <li>看护者附近搜索（基于商家地理位置）</li>
 *   <li>看护者角色授予与回收</li>
 *   <li>商家店铺状态变更时同步看护者在线状态</li>
 * </ul>
 */
public interface KeeperService {

    /**
     * 离线来源：店铺同步 / 系统自动（如关店同步、订单完成后的容量刷新）。
     */
    int OFFLINE_SOURCE_SYSTEM = 0;

    /**
     * 离线来源：看护员主动离线。此来源在店铺开门同步时不会被覆盖回在线。
     */
    int OFFLINE_SOURCE_MANUAL = 1;

    /**
     * 业务作用：获取系统中所有看护者的全量列表。
     * 业务规则：返回所有状态的看护者（含待审核、已通过、已驳回、已离职等）。
     * 注意事项：数据量大时需要考虑分页。
     *
     * @return 所有看护者实体列表
     */
    List<Keeper> listAll();

    /**
     * 业务作用：查询所有待审核的看护者申请，供平台管理员审核使用。
     * 业务规则：仅返回待审核状态的看护者。
     *
     * @return 待审核看护者列表
     */
    List<Keeper> listPending();

    /**
     * 业务作用：根据主键ID查询看护者信息，是其他业务方法的底层依赖。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     *
     * @param id 看护者ID
     * @return 看护者实体
     * @throws BusinessException 如果看护者不存在
     */
    Keeper getById(Long id);

    /**
     * 业务作用：根据多个看护者ID批量查询，减少数据库查询次数。
     * 业务规则：ids 为 null 或空集合时返回空列表。
     *
     * @param ids 看护者ID集合
     * @return 看护者实体列表，不会返回 null
     */
    List<Keeper> listByIds(Collection<Long> ids);

    /**
     * 业务作用：查询指定商家旗下所有未逻辑删除的看护者。
     * 业务规则：仅返回未逻辑删除（deleted=0）的看护者。
     *
     * @param merchantId 商家ID
     * @return 看护者列表
     */
    List<Keeper> findByMerchantId(Long merchantId);

    /**
     * 业务作用：基于用户地理位置搜索附近已审核通过的商家下的活跃看护者。
     * 业务规则：仅搜索已审核通过商家下的活跃看护者（KEEPER_ACTIVE）。
     *
     * @param lat    用户纬度
     * @param lng    用户经度
     * @param radius 搜索半径（公里）
     * @return 附近看护者VO列表（含商家名称、坐标、距离）
     */
    List<KeeperVO> searchNearby(double lat, double lng, double radius);

    /**
     * 业务作用：用户提交看护者入驻申请，创建或复用看护者记录并同步提交资质审核材料。
     * 数据处理：校验商家存在性；如果用户已有被驳回/离职/解雇的历史记录，则复用并更新该记录， 否则新建。设置 status = KEEPER_PENDING，初始评分 5.0，同步创建或更新资质审核记录。
     * 业务规则：一个用户不可同时拥有多个待审核/已通过的申请；创建后需经过平台或商家审核才能 ACTIVE。
     * 异常情况：商家不存在时抛异常；已有待审核/已通过申请时抛异常。
     *
     * @param dto    看护者创建请求DTO
     * @param userId 操作人用户ID
     * @return 创建后的看护者实体
     * @throws BusinessException 如果已有待审核或已通过的申请、商家不存在等
     */
    Keeper create(KeeperCreateRequestDTO dto, Long userId);

    /**
     * 业务作用：修改看护者的基础信息（姓名、电话、头像、经验、定价等）。
     *
     * @param id  看护者ID
     * @param dto 看护者更新请求DTO
     * @return 更新后的看护者实体
     * @throws BusinessException 如果看护者不存在
     */
    Keeper update(Long id, KeeperUpdateRequestDTO dto);

    /**
     * 业务作用：从数据库中物理删除看护者记录并回收其 KEEPER 角色。
     * 业务规则：物理删除不可恢复；删除后同步回收角色权限。
     *
     * @param id 看护者ID
     * @throws BusinessException 如果看护者不存在
     */
    void delete(Long id);

    /**
     * 业务作用：看护者主动申请离职，状态变为辞职并回收 KEEPER 角色。
     * 异常情况：非本人操作抛 403；状态不允许抛 400；有未完成订单抛 400。
     *
     * @param id     看护者ID
     * @param userId 操作用户ID（必须是看护者本人）
     * @throws BusinessException 如果非本人操作、状态不允许或有未完成订单
     */
    void resign(Long id, Long userId);

    /**
     * 业务作用：商家 owner 解雇旗下看护者，状态变为解雇并回收 KEEPER 角色。
     * 异常情况：非本商家操作抛 403；状态不允许抛 400；有未完成订单抛 400。
     *
     * @param id             看护者ID
     * @param merchantUserId 商家的用户ID（用于校验商家的 owner 身份）
     * @throws BusinessException 如果非本商家操作、状态不允许或有未完成订单
     */
    void terminateByMerchant(Long id, Long merchantUserId);

    /**
     * 业务作用：平台管理员审核通过看护者入驻申请，使其成为活跃看护者。
     * 异常情况：不是待审核状态时抛 BusinessException。
     *
     * @param id 看护者ID
     * @throws BusinessException 如果不是待审核状态
     */
    void approve(Long id);

    /**
     * 业务作用：平台管理员驳回看护者入驻申请。
     * 异常情况：不是待审核状态时抛 BusinessException。
     *
     * @param id 看护者ID
     * @throws BusinessException 如果不是待审核状态
     */
    void reject(Long id);

    /**
     * 业务作用：将看护者实体转换为前端展示所需的 VO（含资质信息）。
     *
     * @param entity 看护者实体
     * @return 看护者VO，入参为 null 时返回 null
     */
    KeeperVO toDTO(Keeper entity);

    /**
     * 批量填充「所属商家在售服务的最低价」。
     * <p>
     * 价格属于服务而非看护者，看护者卡片只展示「¥xx 起」，具体金额以服务详情为准。
     * 一次查询覆盖整页看护者，避免 N+1。
     *
     * @param keepers 已转换的看护者 VO 列表（原地写入 min_price_wsh）
     */
    void fillMinServicePrice(List<KeeperVO> keepers);

    /**
     * 业务作用：看护者手动切换自己的在线/离线状态。
     * 数据处理：校验状态值合法性（仅 ACTIVE/OFFLINE/BUSY 有效）；校验当前状态是否可切换； BUSY 状态禁止手动切换，须等订单完成自动恢复。
     * 业务规则：忙碌状态不可手动切换；只有 ACTIVE/OFFLINE/BUSY 状态的看护者可以切换。
     * 异常情况：状态值无效抛 BusinessException；当前状态不可切换抛 BusinessException；忙碌状态抛 BusinessException。
     *
     * @param id     看护者ID
     * @param status 目标状态
     * @throws BusinessException 如果状态值无效、当前状态不允许切换、或忙碌状态手动切换
     */
    void setOnlineStatus(Long id, int status);

    /**
     * 业务作用：商家店铺开门/关门时同步通知旗下所有看护者的在线状态。
     * 业务规则：仅变更 OFFLINE→ACTIVE（开门时）或 ACTIVE→OFFLINE（关门时），BUSY 状态不受影响。
     *
     * @param merchantId 商家ID
     * @param storeOpen  true 表示开门，false 表示关门
     */
    void syncMerchantStoreStatus(Long merchantId, boolean storeOpen);

    /**
     * 业务作用：根据用户ID查询最新的看护者记录。
     * 业务规则：一个用户可以有多次看护者申请记录（如驳回后重新申请），按 ID 倒序取最新。
     *
     * @param userId 用户ID
     * @return 看护者实体，未找到时返回 null
     */
    Keeper findByUserId(Long userId);

    /**
     * 业务作用：商家 owner 查询归属本商家的所有待审核看护者申请。
     * 业务规则：必须先校验商家身份；仅返回待审核状态的看护者。
     *
     * @param merchantUserId 商家的用户ID
     * @return 待审核看护者列表
     * @throws BusinessException 如果该用户没有商家信息
     */
    List<Keeper> listPendingByMerchant(Long merchantUserId);

    /**
     * 业务作用：商家 owner 审核通过归属本商家的看护者入驻申请。
     * 数据处理：校验看护者 PENDING 状态；校验商家归属权（必须是本商家旗下的看护者）； 更新为 KEEPER_ACTIVE 并授予 KEEPER 角色。
     * 异常情况：不是待审核状态抛 BusinessException；无权限审核抛 BusinessException。
     *
     * @param id             看护者ID
     * @param merchantUserId 商家的用户ID（用于校验归属权）
     * @throws BusinessException 如果不是待审核状态、或无权限审核
     */
    void approveByMerchant(Long id, Long merchantUserId);

    /**
     * 业务作用：商家 owner 驳回归属本商家的看护者入驻申请。
     * 异常情况：不是待审核状态抛 BusinessException；无权限审核抛 BusinessException。
     *
     * @param id             看护者ID
     * @param merchantUserId 商家的用户ID（用于校验归属权）
     * @throws BusinessException 如果不是待审核状态、或无权限审核
     */
    void rejectByMerchant(Long id, Long merchantUserId);
}
