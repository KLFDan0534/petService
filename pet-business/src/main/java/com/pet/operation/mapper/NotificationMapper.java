package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户通知 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 批量插入通知，一条 SQL 插入多条记录，显著减少数据库往返次数。
     * <p>
     * 调用方需自行控制单批数量（建议 500~1000 条），避免超出 MySQL
     * max_allowed_packet 限制。
     *
     * @param list 待插入的通知列表，不能为空
     * @return 实际插入的行数
     */
    @Insert("""
            <script>
            INSERT INTO notification_wsh
                (user_id_wsh, title_wsh, content_wsh, type_wsh,
                 is_read_wsh, related_id_wsh, deleted_wsh, created_at_wsh)
            VALUES
            <foreach collection="list" item="item" separator=",">
                (#{item.user_id_wsh}, #{item.title_wsh}, #{item.content_wsh}, #{item.type_wsh},
                 #{item.is_read_wsh}, #{item.related_id_wsh}, 0, NOW())
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<Notification> list);
}
