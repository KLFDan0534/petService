package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.dto.FavoriteTargetTypeDTO;
import com.pet.operation.entity.Favorite;

import java.util.List;

/**
 * 用户收藏服务接口，提供收藏记录的查询、切换（收藏/取消）及分页卡片展示功能。
 * <p>
 * 可收藏的目标类型包括服务（service）、商家（merchant）、看护人（keeper）等，
 * 每种类型由对应的 {@link FavoriteTargetResolver} 实现类提供卡片摘要信息。
 */
public interface FavoriteService {
    /**
     * 查询用户的收藏记录列表，按收藏时间倒序
     *
     * @param userId     用户ID
     * @param targetType 目标类型，传 null 或空表示查询所有类型
     * @return 收藏记录列表
     */
    List<Favorite> listByUser(Long userId, String targetType);

    /**
     * 判断用户是否已收藏指定目标
     *
     * @param userId     用户ID
     * @param targetId   目标对象ID
     * @param targetType 目标类型（如 "service"、"merchant"、"keeper"）
     * @return true 表示已收藏
     */
    boolean isFavorited(Long userId, Long targetId, String targetType);

    /**
     * 切换收藏状态：如果已收藏则取消，未收藏则添加
     *
     * @param userId     用户ID
     * @param targetId   目标对象ID
     * @param targetType 目标类型
     */
    void toggle(Long userId, Long targetId, String targetType);

    /**
     * 分页查询用户的收藏卡片列表
     * <p>
     * 通过 {@link FavoriteTargetResolver} 解析每个收藏目标的摘要信息
     * （标题、描述、图片、价格等），封装为 {@link FavoriteCardDTO}。
     *
     * @param userId     用户ID
     * @param targetType 目标类型，传 null 或空表示所有类型
     * @param page       页码（从 1 开始）
     * @param size       每页大小（自动限制在 1~100 之间）
     * @return 分页收藏卡片结果
     */
    PageResult<FavoriteCardDTO> pageByUser(Long userId, String targetType, int page, int size);

    /**
     * 获取所有支持收藏的目标类型列表（含编码和显示名称）
     *
     * @return 目标类型 DTO 列表
     */
    List<FavoriteTargetTypeDTO> listTargetTypes();

    /**
     * 管理员分页查询全部收藏数据，支持按目标类型精确筛选
     *
     * @param pageParam   分页参数（页码、每页条数）
     * @param targetType  目标类型，精确匹配，为空时查询全部类型
     * @return 分页收藏记录，按创建时间倒序
     */
    IPage<Favorite> pageAll(PageRequestDTO pageParam, String targetType);
}
