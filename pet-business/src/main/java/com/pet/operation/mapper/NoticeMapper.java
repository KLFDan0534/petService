package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    /**
     * 查询用户未读公告列表
     * @param userId 用户ID
     * @param type 公告类型
     * @param status 公告状态
     * @return 未读公告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
}
