package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.ServiceItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务项目数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface ServiceItemMapper extends BaseMapper<ServiceItem> {
}
