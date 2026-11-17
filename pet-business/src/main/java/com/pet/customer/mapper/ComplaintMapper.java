package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 投诉数据访问接口，提供 Complaint 实体的基础 CRUD 操作。
 * 映射表 complaint_wsh。
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {

    /**
     * 业务作用：把投诉从 expectedStatus 原子地迁到 status，并写入处理结论。 只有当前状态仍等于 expectedStatus 时才会更新，返回受影响行数。
     * 业务规则：SQL 里带 {@code status_wsh = #{expectedStatus}} 是并发保护的关键—— 两个管理员同时处理时只有一个能更新成功，另一个拿到 0 行。
     * 注意事项：
     * 显式写 SQL 而不是 LambdaUpdateWrapper，是因为 Lambda 包装器依赖
     * MyBatis-Plus 的实体元数据缓存，单元测试里没有该缓存会直接抛异常；
     * 显式 SQL 既能单测，也把并发条件写在最显眼的位置。
     *
     * @param id             投诉ID
     * @param expectedStatus 期望的前态（CAS 条件）
     * @param status         目标状态
     * @param result         处理结论，可为 null
     * @return 受影响行数，1 表示迁移成功，0 表示状态已变化
     */
    @Update("UPDATE complaint_wsh SET status_wsh = #{status}, result_wsh = #{result}, "
            + "updated_at_wsh = NOW() "
            + "WHERE id_wsh = #{id} AND status_wsh = #{expectedStatus} AND deleted_wsh = 0")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("status") String status,
                        @Param("result") String result);
}
