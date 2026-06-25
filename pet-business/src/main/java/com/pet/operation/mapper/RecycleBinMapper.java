package com.pet.operation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecycleBinMapper {

    /**
     * 查询指定表中已删除的记录
     * @param tableName 表名
     * @return 已删除记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Select("SELECT * FROM `${tableName}` WHERE deleted_wsh = 1 ORDER BY id_wsh DESC LIMIT 200")
    List<Map<String, Object>> selectDeleted(@Param("tableName") String tableName);

    /**
     * 恢复指定表中已删除的记录
     * @param tableName 表名
     * @param id 记录ID
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE `${tableName}` SET deleted_wsh = 0 WHERE id_wsh = #{id}")
    int restore(@Param("tableName") String tableName, @Param("id") Long id);

    /**
     * 软删除指定表中的记录
     * @param tableName 表名
     * @param id 记录ID
     * @return 影响行数
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @Update("UPDATE `${tableName}` SET deleted_wsh = 1 WHERE id_wsh = #{id}")
    int softDelete(@Param("tableName") String tableName, @Param("id") Long id);
}
