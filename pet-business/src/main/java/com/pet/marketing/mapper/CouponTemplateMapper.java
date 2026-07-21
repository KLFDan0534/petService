package com.pet.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.marketing.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
    @Update("""
            UPDATE coupon_template_wsh
            SET issued_quantity_wsh = COALESCE(issued_quantity_wsh, 0) + #{quantity},
                updated_at_wsh = NOW()
            WHERE id_wsh = #{templateId}
              AND deleted_wsh = 0
              AND (
                  total_quantity_wsh IS NULL
                  OR total_quantity_wsh = 0
                  OR COALESCE(issued_quantity_wsh, 0) + #{quantity} <= total_quantity_wsh
              )
            """)
    int increaseIssuedQuantity(@Param("templateId") Long templateId, @Param("quantity") int quantity);
}
