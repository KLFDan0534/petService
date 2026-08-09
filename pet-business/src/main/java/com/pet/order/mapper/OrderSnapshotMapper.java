package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.OrderSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for {@link OrderSnapshot} entity.
 * <p>
 * Provides CRUD operations on the {@code order_snapshot_wsh} table.
 * Snapshots preserve immutable copies of all order-related entity data
 * at the time of order creation for historical accuracy.
 */
@Mapper
public interface OrderSnapshotMapper extends BaseMapper<OrderSnapshot> {
}
