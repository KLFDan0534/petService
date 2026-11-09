package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.FavoriteTargetType;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.dto.FavoriteTargetTypeDTO;
import com.pet.operation.entity.Favorite;
import com.pet.operation.mapper.FavoriteMapper;
import com.pet.operation.service.FavoriteService;
import com.pet.operation.service.FavoriteTargetResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final Map<String, FavoriteTargetResolver> resolverMap;

    /**
     * 【构造函数：装配收藏目标解析器】
     *
     * 业务作用：注入 FavoriteMapper，并把所有 FavoriteTargetResolver 按目标类型注册到 Map 中。
     *
     * 调用场景：容器启动创建 FavoriteServiceImpl Bean 时自动调用。
     *
     * 调用链：Spring IoC → FavoriteServiceImpl(构造) → FavoriteTargetType.requireSupported → resolverMap.putIfAbsent。
     *
     * 数据处理：遍历注入的解析器列表，用目标类型码作为 key 存入 LinkedHashMap，保持注入顺序。
     *
     * 业务规则：同一种目标类型不允许注册两个解析器。
     *
     * 状态影响：无。
     *
     * 异常情况：类型码不受支持时 requireSupported 抛异常；出现重复解析器抛 IllegalStateException("Duplicate favorite target resolver")，导致应用启动失败。
     *
     * 注意事项：解析器缺失不会导致启动失败，但该类型的收藏卡片会退化为占位内容。
     */
    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, List<FavoriteTargetResolver> targetResolvers) {
        this.favoriteMapper = favoriteMapper;
        this.resolverMap = new LinkedHashMap<>();
        for (FavoriteTargetResolver resolver : targetResolvers) {
            String key = FavoriteTargetType.requireSupported(resolver.targetType());
            FavoriteTargetResolver previous = this.resolverMap.putIfAbsent(key, resolver);
            if (previous != null) {
                throw new IllegalStateException("Duplicate favorite target resolver: " + key);
            }
        }
    }

    /**
     * 查询用户的收藏列表，支持按类型筛选
     */
    @Override
    public List<Favorite> listByUser(Long userId, String targetType) {
        log.info("listByUser() 被调用");
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUser_id_wsh, userId)
                .orderByDesc(Favorite::getCreated_at_wsh);
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(Favorite::getTarget_type_wsh, FavoriteTargetType.requireSupported(targetType));
        }
        return favoriteMapper.selectList(wrapper);
    }

    /**
     * 判断用户是否已收藏指定目标
     */
    @Override
    public boolean isFavorited(Long userId, Long targetId, String targetType) {
        log.info("isFavorited() 被调用");
        String normalizedType = FavoriteTargetType.requireSupported(targetType);
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, normalizedType));
        return count > 0;
    }

    /**
     * 切换收藏状态：已收藏则取消，未收藏则新增。
     *
     * 注意：deleteById 是软删除（UPDATE deleted_wsh=1），唯一索引 uk_user_target
     * 仍包含软删除的行。如果直接 INSERT 会唯一索引冲突（500 错误）。
     * 修复策略：插入前先检查是否有软删除记录并恢复（UPDATE deleted_wsh=0），
     * 避免唯一索引冲突。
     */
    @Override
    @Transactional
    public void toggle(Long userId, Long targetId, String targetType) {
        log.info("toggle() 被调用");
        String normalizedType = FavoriteTargetType.requireSupported(targetType);
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, normalizedType));
        if (existing != null) {
            // 有活跃记录 → 软删除
            favoriteMapper.deleteById(existing.getId_wsh());
            return;
        }
        // 无活跃记录 → 尝试恢复软删除记录（防止唯一索引冲突）。
        // 必须用原生 SQL 绕过 @TableLogic 过滤，见 FavoriteMapper.reactivate
        int reactivated = favoriteMapper.reactivate(userId, targetId, normalizedType);
        if (reactivated > 0) {
            return;
        }
        // 完全没有记录 → 插入新记录
        Favorite favorite = new Favorite();
        favorite.setUser_id_wsh(userId);
        favorite.setTarget_id_wsh(targetId);
        favorite.setTarget_type_wsh(normalizedType);
        favoriteMapper.insert(favorite);
    }

    /**
     * 分页查询收藏卡片，通过对应类型的 Resolver 组装摘要信息
     */
    @Override
    public PageResult<FavoriteCardDTO> pageByUser(Long userId, String targetType, int page, int size) {
        log.info("pageByUser() 被调用");
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedType = null;
        if (targetType != null && !targetType.isBlank()) {
            normalizedType = FavoriteTargetType.requireSupported(targetType);
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUser_id_wsh, userId)
                .orderByDesc(Favorite::getCreated_at_wsh);
        if (normalizedType != null) {
            wrapper.eq(Favorite::getTarget_type_wsh, normalizedType);
        }

        Page<Favorite> favoritePage = favoriteMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<FavoriteCardDTO> cards = buildCards(favoritePage.getRecords());
        PageResult<FavoriteCardDTO> result = new PageResult<>();
        result.setList(cards);
        result.copyPageInfo(favoritePage);
        return result;
    }

    /**
     * 获取所有支持的收藏目标类型列表
     */
    @Override
    public List<FavoriteTargetTypeDTO> listTargetTypes() {
        List<FavoriteTargetTypeDTO> result = new ArrayList<>();
        for (String code : FavoriteTargetType.codes()) {
            FavoriteTargetTypeDTO dto = new FavoriteTargetTypeDTO();
            dto.setCode_wsh(code);
            dto.setLabel_wsh(FavoriteTargetType.labelOf(code));
            result.add(dto);
        }
        return result;
    }

    /**
     * 管理员分页查询全部收藏数据，支持按目标类型精确筛选
     */
    @Override
    public IPage<Favorite> pageAll(PageRequestDTO pageParam, String targetType) {
        log.info("pageAll() 被调用");
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(StringUtils.hasText(targetType), Favorite::getTarget_type_wsh, targetType)
                .orderByDesc(Favorite::getCreated_at_wsh);
        return favoriteMapper.selectPage(new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);
    }

    /**
     * 【组装收藏卡片列表】
     *
     * 业务作用：把收藏记录按目标类型分组，调用对应解析器批量取摘要，再逐条拼成卡片。
     *
     * 调用场景：pageByUser 分页查询后组装返回数据。
     *
     * 调用链：pageByUser → buildCards → FavoriteTargetType.normalize → FavoriteTargetResolver.resolve → mergeSummary。
     *
     * 数据处理：先按 target_type_wsh 分组收集目标 ID，按类型批量调用解析器得到 targetId→卡片映射；再逐条填充收藏 ID、目标 ID、类型、类型标签与创建时间，命中摘要时合并，未命中时生成「类型 + #ID」的占位标题并标记目标缺失。
     *
     * 业务规则：没有注册解析器的类型不参与批量查询，直接走占位分支；记录为 null 时跳过。
     *
     * 状态影响：只读。
     *
     * 异常情况：入参为空返回 List.of()；解析器内部异常会向上抛出。
     *
     * 注意事项：按类型批量查询避免 N+1；占位描述为英文固定文案，与其它中文提示不一致。
     */
    private List<FavoriteCardDTO> buildCards(List<Favorite> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<String, List<Long>> idsByType = records.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Favorite::getTarget_type_wsh,
                        LinkedHashMap::new,
                        Collectors.mapping(Favorite::getTarget_id_wsh, Collectors.toList())));

        Map<String, Map<Long, FavoriteCardDTO>> summaryByType = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : idsByType.entrySet()) {
            String type = FavoriteTargetType.normalize(entry.getKey());
            FavoriteTargetResolver resolver = resolverMap.get(type);
            if (resolver != null) {
                summaryByType.put(type, resolver.resolve(entry.getValue()));
            }
        }

        List<FavoriteCardDTO> result = new ArrayList<>(records.size());
        for (Favorite favorite : records) {
            if (favorite == null) {
                continue;
            }
            String type = FavoriteTargetType.normalize(favorite.getTarget_type_wsh());
            FavoriteCardDTO card = new FavoriteCardDTO();
            card.setId_wsh(favorite.getId_wsh());
            card.setTarget_id_wsh(favorite.getTarget_id_wsh());
            card.setTarget_type_wsh(type);
            card.setTarget_type_label_wsh(FavoriteTargetType.labelOf(type));
            card.setCreated_at_wsh(favorite.getCreated_at_wsh());

            Map<Long, FavoriteCardDTO> summaryMap = summaryByType.get(type);
            FavoriteCardDTO summary = summaryMap == null ? null : summaryMap.get(favorite.getTarget_id_wsh());
            if (summary != null) {
                mergeSummary(card, summary);
            } else {
                card.setTitle_wsh(FavoriteTargetType.labelOf(type) + " #" + favorite.getTarget_id_wsh());
                card.setDescription_wsh("Target is missing or no resolver is registered");
                card.setDetail_url_wsh(FavoriteTargetType.detailPath(type, favorite.getTarget_id_wsh()));
            }
            result.add(card);
        }
        return result;
    }

    /**
     * 【合并目标摘要到卡片】
     *
     * 业务作用：把解析器返回的摘要字段覆盖到收藏卡片上。
     *
     * 调用场景：buildCards 命中摘要时调用。
     *
     * 调用链：buildCards → mergeSummary。
     *
     * 数据处理：拷贝标题、描述、图片地址、详情地址、主次信息、金额与金额单位后缀共 8 个字段。
     *
     * 业务规则：只覆盖展示字段，收藏自身的 ID、目标 ID、类型与创建时间保持不变。
     *
     * 状态影响：无。
     *
     * 异常情况：summary 为 null 时抛 NullPointerException，调用方已做判空。
     *
     * 注意事项：字段是逐个手写的，FavoriteCardDTO 新增展示字段时需同步维护此处。
     */
    private void mergeSummary(FavoriteCardDTO card, FavoriteCardDTO summary) {
        card.setTitle_wsh(summary.getTitle_wsh());
        card.setDescription_wsh(summary.getDescription_wsh());
        card.setImage_url_wsh(summary.getImage_url_wsh());
        card.setDetail_url_wsh(summary.getDetail_url_wsh());
        card.setPrimary_info_wsh(summary.getPrimary_info_wsh());
        card.setSecondary_info_wsh(summary.getSecondary_info_wsh());
        card.setAmount_wsh(summary.getAmount_wsh());
        card.setAmount_suffix_wsh(summary.getAmount_suffix_wsh());
    }
}
