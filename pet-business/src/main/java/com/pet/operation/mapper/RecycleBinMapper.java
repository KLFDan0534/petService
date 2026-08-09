package com.pet.operation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 回收站 MyBatis Mapper 接口，使用原生 SQL 实现跨表的软删除查询和恢复操作
 */
@Mapper
public interface RecycleBinMapper {

    /**
     * 查询指定表中所有已软删除的记录（最多 200 条），按 ID 倒序排列
     *
     * @param tableName 表名（使用占位符注入，需确保表名受信任）
     * @return 已删除记录的字段名-值 Map 列表
     */
    @Select("SELECT * FROM `${tableName}` WHERE deleted_wsh = 1 ORDER BY id_wsh DESC LIMIT 200")
    List<Map<String, Object>> selectDeleted(@Param("tableName") String tableName);

    /**
     * 恢复指定表中一条已软删除的记录，将 deleted_wsh 置为 0
     *
     * @param tableName 表名
     * @param id        记录 ID
     * @return 受影响的行数（0 表示记录不存在或未删除）
     */
    @Update("UPDATE `${tableName}` SET deleted_wsh = 0 WHERE id_wsh = #{id}")
    int restore(@Param("tableName") String tableName, @Param("id") Long id);

    /**
     * 软删除指定表中的一条记录，将 deleted_wsh 置为 1
     *
     * @param tableName 表名
     * @param id        记录 ID
     * @return 受影响的行数（0 表示记录不存在）
     */
    @Update("UPDATE `${tableName}` SET deleted_wsh = 1 WHERE id_wsh = #{id}")
    int softDelete(@Param("tableName") String tableName, @Param("id") Long id);
}
