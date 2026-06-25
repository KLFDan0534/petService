package com.pet.adoption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.adoption.entity.AdoptionApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 领养申请数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface AdoptionApplicationMapper extends BaseMapper<AdoptionApplication> {
}
