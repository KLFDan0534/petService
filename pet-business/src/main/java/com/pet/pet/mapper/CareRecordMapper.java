package com.pet.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.pet.entity.CareRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 护理记录数据访问层，基于 MyBatis-Plus 提供护理记录表的基础 CRUD。
 * 护理记录关联订单、宠物和看护者，记录宠物在寄养过程中的各项护理操作。
 *
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Mapper
public interface CareRecordMapper extends BaseMapper<CareRecord> {
}
