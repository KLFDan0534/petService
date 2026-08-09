package com.pet.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.marketing.entity.CouponUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券使用记录数据访问接口，提供 CouponUsage 实体的基础 CRUD 操作。
 * 映射表 coupon_usage_wsh。
 */
@Mapper
public interface CouponUsageMapper extends BaseMapper<CouponUsage> {
}
