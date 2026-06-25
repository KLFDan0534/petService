package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
