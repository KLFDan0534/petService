package com.pet.operation.service;

import com.pet.operation.dto.FavoriteCardDTO;

import java.util.Collection;
import java.util.Map;

/**
 * 收藏目标解析器接口，每种可收藏的目标类型需实现该接口。
 * <p>
 * 根据目标 ID 集合批量查询目标详情并组装为 {@link FavoriteCardDTO} 卡片数据，
 * 用于在收藏列表中展示标题、描述、图片、价格等信息。
 */
public interface FavoriteTargetResolver {
    /**
     * 返回此解析器支持的目标类型编码，与 {@link com.pet.common.FavoriteTargetType} 中的定义一致
     *
     * @return 目标类型编码（如 "service"、"merchant"、"keeper"）
     */
    String targetType();

    /**
     * 根据目标 ID 集合批量解析收藏卡片数据
     *
     * @param targetIds 目标 ID 集合
     * @return Map，key 为目标 ID，value 为对应的收藏卡片 DTO
     */
    Map<Long, FavoriteCardDTO> resolve(Collection<Long> targetIds);
}
