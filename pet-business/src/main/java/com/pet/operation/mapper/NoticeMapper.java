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
     * 弹窗公告的判断条件：投递方式字段包含 "popup" 且用户未读。
     *
     * @param userId 用户 ID
     * @param status 公告状态码
     * @return 用户未关闭的弹窗公告列表
     */
    @Select("""
            SELECT n.*
            FROM notice_wsh n
            WHERE n.deleted_wsh = 0
              AND n.status_wsh = #{status}
              AND LOWER(n.type_wsh) = 'notice'
              AND n.delivery_type_wsh LIKE '%popup%'
              AND NOT EXISTS (
                  SELECT 1
                  FROM notice_read_wsh r
                  WHERE r.notice_id_wsh = n.id_wsh
                    AND r.user_id_wsh = #{userId}
                    AND r.deleted_wsh = 0
              )
            ORDER BY n.sort_order_wsh ASC, n.created_at_wsh DESC
            """)
    List<Notice> selectPopupByUser(@Param("userId") Long userId,
                                   @Param("status") Integer status);
}
