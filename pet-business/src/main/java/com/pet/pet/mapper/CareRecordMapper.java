package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.CareRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 护理记录数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface CareRecordMapper extends BaseMapper<CareRecord> {
}
