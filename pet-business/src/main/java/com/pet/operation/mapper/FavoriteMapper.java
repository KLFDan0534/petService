package com.pet.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.operation.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收藏 MyBatis-Plus Mapper 接口
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 恢复软删除的收藏记录（deleted_wsh=1 → 0）。
     * <p>
     * 注意：不能用 BaseMapper.update() 配合 LambdaUpdateWrapper 来实现——
     * 因为实体上标记了 {@code @TableLogic}，MyBatis-Plus 会自动给 UPDATE 的
     * WHERE 追加 {@code deleted_wsh = 0} 条件，导致永远匹配不到软删除的行，
     * 最终走 INSERT 触发 uk_user_target 唯一索引冲突（500）。此处使用原生 SQL
     * 绕过逻辑删除过滤，才能正确复活软删除记录。
     */
    @Update("UPDATE favorite_wsh SET deleted_wsh = 0 " +
            "WHERE user_id_wsh = #{userId} AND target_id_wsh = #{targetId} AND target_type_wsh = #{targetType}")
    int reactivate(@Param("userId") Long userId,
                   @Param("targetId") Long targetId,
                   @Param("targetType") String targetType);
}
