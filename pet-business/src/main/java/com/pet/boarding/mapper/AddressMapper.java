package com.pet.boarding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.boarding.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址（Address）数据访问层。
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，提供地址表的基础 CRUD 操作。
 * <p>
 * <b>映射表：</b>address_wsh
 *
 * @author: wsh
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
