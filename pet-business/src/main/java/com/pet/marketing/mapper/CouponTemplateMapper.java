package com.pet.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.marketing.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板数据访问接口，提供 CouponTemplate 实体的基础 CRUD 操作
 * 以及安全的库存发行量递增方法（含库存上限校验）。
 * 映射表 coupon_template_wsh。
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
    /**
     * 原子递增优惠券模板的已发行数量。
     * <p>在增加之前校验总发行量限制（total_quantity_wsh），
     * 确保不会超发。返回 0 表示库存不足或模板不存在。</p>
     *
     * @param templateId 模板ID
     * @param quantity 增加数量
     * @return 受影响的行数，0 表示库存不足或模板已被删除
     */
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
