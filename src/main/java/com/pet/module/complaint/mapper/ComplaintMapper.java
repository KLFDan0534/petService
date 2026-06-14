package com.pet.module.complaint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.module.complaint.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
