package com.pet.admin.statistics.service;

import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.ReputationStatsVO;
import com.pet.admin.statistics.vo.UserDashboardVO;

/**
 * 【业务名称】数据统计服务接口
 * <p>业务作用：提供平台各级别的数据统计看板功能，包括管理员全局看板、商家数据看板、用户个人看板和信誉统计看板。</p>
 * <p>核心数据：用户数、宠物数、商家数、看护人数、订单数、营收金额、订单状态分布、评价评分、投诉率、打赏数等。</p>
 */
public interface StatisticsService {

    /**
     * 【业务名称】管理员全局数据看板
     * <p>业务作用：聚合平台全局统计数据，包括总用户数、总宠物数、总商家数、总看护人数、总订单数、总营收（已支付/进行中/已完成订单）、待处理订单数和已完成订单数。</p>
     * <p>调用场景：管理员登录后台首页查看平台运营概览。</p>
     * <p>调用链：StatisticsController.getAdminDashboard() → StatisticsService.getAdminDashboard() → UserService.listAll()/PetService.listAll()/MerchantService.listAll()/KeeperService.listAll()/OrderService.listAll() → 流式计算统计 → 返回AdminDashboardVO</p>
     * <p>数据处理：分别调用各Service的listAll()获取全量数据；size()计数；订单流过滤isRevenueOrder统计营收；根据状态过滤pending/completed订单。</p>
     * <p>业务规则：营收统计包含状态为PAID/CONFIRMED/DELIVERED/RECEIVED/IN_PROGRESS/COMPLETED的订单。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：数据量大时可能导致查询缓慢（已标记TODO需优化）。</p>
     * <p>注意事项：全量listAll()在大数据量下性能需关注；前端返回时间可能过长。</p>
     *
     * @return 管理员看板数据VO
     */
    AdminDashboardVO getAdminDashboard();

    /**
     * 【业务名称】商家数据看板
     * <p>业务作用：统计指定商家的经营数据，包括总订单数、总营收、待处理/进行中/已完成订单数、服务过的宠物种类数。</p>
     * <p>调用场景：商家登录后台查看自己店铺的经营概览。</p>
     * <p>调用链：StatisticsController.getMerchantDashboard() → StatisticsService.getMerchantDashboard(merchantId) → OrderService.listByMerchant() → 流式计算 → 返回MerchantDashboardVO</p>
     * <p>数据处理：按merchantId查关联订单；过滤isRevenueOrder计算营收；按订单状态分组统计；distinct统计服务过的宠物数。</p>
     * <p>业务规则：营收统计状态与Admin一致；待处理=PENDING，进行中=ACTIVE_ORDER_STATUSES。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：merchantId不存在时返回全零数据。</p>
     * <p>注意事项：仅MERCHANT和ADMIN角色可访问。</p>
     *
     * @param merchantId 商家ID
     * @return 商家看板数据VO
     */
    MerchantDashboardVO getMerchantDashboard(Long merchantId);

    /**
     * 【业务名称】用户个人数据看板
     * <p>业务作用：统计当前用户的个人数据，包括宠物数、进行中订单数、已完成订单数和累计消费金额。</p>
     * <p>调用场景：用户在个人中心查看自己的数据概览。</p>
     * <p>调用链：StatisticsController.getUserDashboard() → StatisticsService.getUserDashboard(userId) → PetService.getPetsByOwner()/OrderService.listByOwner() → 流式计算 → 返回UserDashboardVO</p>
     * <p>数据处理：按userId查宠物列表size()；查关联订单；过滤isRevenueOrder计算累计消费；按状态分组统计。</p>
     * <p>业务规则：营收统计使用final_amount字段。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：userId不存在时返回全零数据。</p>
     * <p>注意事项：需用户登录后查看自己的数据，不支持跨用户查询。</p>
     *
     * @param userId 用户ID
     * @return 用户看板数据VO
     */
    UserDashboardVO getUserDashboard(Long userId);

    /**
     * 【业务名称】信誉统计看板
     * <p>业务作用：统计指定商家或看护人的信誉数据，包括总评价数、平均评分、完成订单数、完成率、投诉率和打赏总数。</p>
     * <p>调用场景：商家/看护人查看自己的信誉评分；管理员查看商家/看护人的信誉表现。</p>
     * <p>调用链：StatisticsController.getReputationStats() → StatisticsService.getReputationStats(targetType, targetId) → RatingMapper/OrderMapper/ComplaintMapper/TipMapper查询 → 计算百分比 → 返回ReputationStatsVO</p>
     * <p>数据处理：按targetType+targetId查Rating表计算平均值和总数；查PetOrder表统计已完成订单数和完成率；查Complaint表统计投诉率；查Tip表统计打赏数。</p>
     * <p>业务规则：targetType仅支持"merchant"和"keeper"；完成率=已完成订单/总订单×100%；投诉率=投诉数/总订单×100%。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：targetType不合法抛出IllegalArgumentException；targetId不存在时返回全零数据。</p>
     * <p>注意事项：除法运算使用HALF_UP四舍五入保留1位小数；打赏统计通过订单ID关联查询。</p>
     *
     * @param targetType 统计目标类型（"merchant"或"keeper"）
     * @param targetId   统计目标ID
     * @return 信誉统计数据VO
     */
    ReputationStatsVO getReputationStats(String targetType, Long targetId);
}

