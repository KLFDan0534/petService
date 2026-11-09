package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告 MyBatis-Plus Mapper 接口，提供自定义的未读公告和弹窗公告查询
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    /**
     * 查询用户未读的公告列表，使用 NOT EXISTS 子查询排除已读记录
     *
     * @param userId 用户 ID
     * @param type   公告类型（小写）
     * @param status 公告状态码（启用状态）
     * @return 用户未读公告列表，按排序字段升序、创建时间降序
     */
    @Select("""
            SELECT n.*
            FROM notice_wsh n
            WHERE n.deleted_wsh = 0
              AND n.status_wsh = #{status}
              AND LOWER(n.type_wsh) = #{type}
              AND NOT EXISTS (
                  SELECT 1
                  FROM notice_read_wsh r
                  WHERE r.notice_id_wsh = n.id_wsh
                    AND r.user_id_wsh = #{userId}
                    AND r.deleted_wsh = 0
              )
            ORDER BY n.sort_order_wsh ASC, n.created_at_wsh DESC
            """)
    List<Notice> selectUnreadByUser(@Param("userId") Long userId,
                                    @Param("type") String type,
                                    @Param("status") Integer status);

    //根据用户查询尚未关闭的弹窗公告,且投递方式包含popup
    /**
     * 【查询待弹窗公告】返回该用户尚未关闭的弹窗类公告
     *
     * 业务作用：用户端登录后拉取需要主动弹出展示的公告列表。
     *
     * 调用场景：前端启动或进入首页时调用公告服务，由服务层转调本方法。
     *
     * 调用链：公告服务 → 本方法 → notice_wsh 与 notice_read_wsh 联查。
     *
     * 数据处理：过滤 deleted_wsh=0、status_wsh=入参状态、type_wsh 小写为 notice、delivery_type_wsh 含 popup 的公告，并用 NOT EXISTS 排除 notice_read_wsh 中已有该用户阅读记录的公告，按 sort_order_wsh 升序、created_at_wsh 降序返回。
     *
     * 业务规则：懒加载模型——notice_read_wsh 中「有记录」即视为该用户已关闭该弹窗；不判断 is_read_wsh 状态。
     *
     * 状态影响：无，只读查询。
     *
     * 异常情况：SQL 执行异常由 MyBatis 抛出 DataAccessException，本接口未做包装。
     *
     * 注意事项：delivery_type_wsh 使用 LIKE '%popup%' 模糊匹配，字段中任意位置出现 popup 都会被命中；type_wsh 用 LOWER() 转换后与常量 'notice' 比较，因此依赖数据库函数，无法命中 type 索引。
     */
    @Select("""
            select n.*
            from notice_wsh n
            where n.deleted_wsh = 0
            and n.status_wsh = #{status}
            and LOWER(n.type_wsh) = 'notice'
            and n.delivery_type_wsh like '%popup%'
            and not exists (
                select 1
                from notice_read_wsh r
                where r.notice_id_wsh = n.id_wsh
                and r.user_id_wsh = #{userId}
                and r.deleted_wsh = 0
            )
            order by n.sort_order_wsh asc, n.created_at_wsh desc
            """)
    List<Notice> selectPopupByUser(@Param("userId") Long userId,
                                   @Param("status") Integer status);
}
