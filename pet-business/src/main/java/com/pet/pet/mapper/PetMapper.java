package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.Pet;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {
}
