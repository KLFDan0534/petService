package com.pet.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.order.entity.Tip;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小费数据访问层
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface TipMapper extends BaseMapper<Tip> {
}
