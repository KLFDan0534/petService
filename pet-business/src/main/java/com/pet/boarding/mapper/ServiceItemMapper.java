package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.ServiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务项目（ServiceItem）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供服务项目表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>pet_service_wsh
 *
 * @author: wsh
 */
@Mapper
public interface ServiceItemMapper extends BaseMapper<ServiceItem> {
}
