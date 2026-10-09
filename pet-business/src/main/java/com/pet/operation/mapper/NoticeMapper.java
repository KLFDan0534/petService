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

    /**
     * 查询用户尚未关闭的弹窗公告列表
     * <p>
     * 懒加载模型：notice_read_wsh 中「有记录 = 该用户已关闭该弹窗」，无记录才需要弹。
     * 因此用 NOT EXISTS 排除已有阅读记录的公告，不对 is_read_wsh 做状态判断。
     *
     * @param userId 用户 ID
     * @param status 公告状态码
     * @return 用户未关闭的弹窗公告列表
     */

    //根据用户查询尚未关闭的弹窗公告,且投递方式包含popup
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
