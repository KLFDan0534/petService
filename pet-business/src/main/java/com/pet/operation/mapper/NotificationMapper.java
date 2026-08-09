package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
