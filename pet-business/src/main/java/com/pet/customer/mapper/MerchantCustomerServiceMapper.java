package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.MerchantCustomerService;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家客服申请数据访问接口，提供 MerchantCustomerService 实体的基础 CRUD 操作。
 * 映射表 merchant_customer_service_wsh。
 */
@Mapper
public interface MerchantCustomerServiceMapper extends BaseMapper<MerchantCustomerService> {
}
