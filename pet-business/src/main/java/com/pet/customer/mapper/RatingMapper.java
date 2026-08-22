package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 评价数据访问接口，提供 Rating 实体的基础 CRUD 操作。
 * 映射表 rating_wsh。
 */
@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    /**
     * 按目标批量聚合评分：一次 IN 查询返回每个目标（分类维度用）的平均分与评价数。
     *
     * @param targetType 目标类型：service|merchant|keeper
     * @param targetIds  目标 ID 集合（禁止为空）
     * @return 每行 {targetId, avgScore, cnt}
     */
    @Select("<script>" +
            "SELECT target_id_wsh AS targetId, AVG(score_wsh) AS avgScore, COUNT(*) AS cnt " +
            "FROM rating_wsh " +
            "WHERE target_type_wsh = #{targetType} AND deleted_wsh = 0 " +
            "AND target_id_wsh IN " +
            "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY target_id_wsh" +
            "</script>")
    List<Map<String, Object>> aggregateByTargets(@Param("targetType") String targetType,
                                                 @Param("targetIds") Collection<Long> targetIds);

    /**
     * 批量查询已有评价的订单ID（用于订单列表展示"待反馈/已评价"状态）
     *
     * @param orderIds 订单ID集合
     * @return 已有评价的订单ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT order_id_wsh FROM rating_wsh " +
            "WHERE deleted_wsh = 0 AND order_id_wsh IS NOT NULL " +
            "AND order_id_wsh IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectOrderIdsWithRatings(@Param("orderIds") Collection<Long> orderIds);
}