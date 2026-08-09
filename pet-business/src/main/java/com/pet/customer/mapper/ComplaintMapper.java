package com.pet.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.customer.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投诉数据访问接口，提供 Complaint 实体的基础 CRUD 操作。
 * 映射表 complaint_wsh。
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
