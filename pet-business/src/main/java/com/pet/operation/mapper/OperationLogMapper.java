package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
