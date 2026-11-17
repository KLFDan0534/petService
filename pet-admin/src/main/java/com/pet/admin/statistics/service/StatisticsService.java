package com.pet.admin.statistics.service;

import com.pet.admin.statistics.vo.AdminDashboardVO;
import com.pet.admin.statistics.vo.MerchantDashboardVO;
import com.pet.admin.statistics.vo.ReputationStatsVO;
import com.pet.admin.statistics.vo.UserDashboardVO;

/**
 * 数据统计服务接口
 * <p>业务作用：提供平台各级别的数据统计看板功能，包括管理员全局看板、商家数据看板、用户个人看板和信誉统计看板。</p>
 * <p>核心数据：用户数、宠物数、商家数、看护人数、订单数、营收金额、订单状态分布、评价评分、投诉率、打赏数等。</p>
 */
public interface StatisticsService {

    /**
     * 管理员全局数据看板
     *
     * 业务作用：聚合平台全局统计数据，包括总用户数、总宠物数、总商家数、总看护人数、总订单数、总营收（已支付/进行中/已完成订单）、待处理订单数和已完成订单数。
     * 业务规则：营收统计包含状态为PAID/CONFIRMED/RECEIVED/IN_PROGRESS/COMPLETED的订单。
     * 注意事项：全量listAll()在大数据量下性能需关注；前端返回时间可能过长。
     *
     * @return 管理员看板数据VO
     */
    AdminDashboardVO getAdminDashboard();

    /**
     * 商家数据看板
     *
     * 业务作用：统计指定商家的经营数据，包括总订单数、总营收、待处理/进行中/已完成订单数、服务过的宠物种类数。
     * 业务规则：营收统计状态与Admin一致；待处理=PENDING，进行中=ACTIVE_ORDER_STATUSES。
     * 异常情况：merchantId不存在时返回全零数据。
     * 注意事项：仅MERCHANT和ADMIN角色可访问。
     *
     * @param merchantId 商家ID
     * @return 商家看板数据VO
     */
    MerchantDashboardVO getMerchantDashboard(Long merchantId);

    /**
     * 用户个人数据看板
     *
     * 业务作用：统计当前用户的个人数据，包括宠物数、进行中订单数、已完成订单数和累计消费金额。
     * 业务规则：营收统计使用final_amount字段。
     * 异常情况：userId不存在时返回全零数据。
     * 注意事项：需用户登录后查看自己的数据，不支持跨用户查询。
     *
     * @param userId 用户ID
     * @return 用户看板数据VO
     */
    UserDashboardVO getUserDashboard(Long userId);

    /**
     * 信誉统计看板
     *
     * 业务作用：统计指定商家或看护人的信誉数据，包括总评价数、平均评分、完成订单数、完成率、投诉率和打赏总数。
     * 业务规则：targetType仅支持"merchant"和"keeper"；完成率=已完成订单/总订单×100%；投诉率=投诉数/总订单×100%。
     * 异常情况：targetType不合法抛出IllegalArgumentException；targetId不存在时返回全零数据。
     * 注意事项：除法运算使用HALF_UP四舍五入保留1位小数；打赏统计通过订单ID关联查询。
     *
     * @param targetType 统计目标类型（"merchant"或"keeper"）
     * @param targetId   统计目标ID
     * @return 信誉统计数据VO
     */
    ReputationStatsVO getReputationStats(String targetType, Long targetId);
}

