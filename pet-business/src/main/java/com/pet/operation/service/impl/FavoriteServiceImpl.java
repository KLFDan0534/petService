package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.FavoriteTargetType;
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

    @Override
    public List<Favorite> listByUser(Long userId, String targetType) {
        log.info("listByUser() called");
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUser_id_wsh, userId)
                .orderByDesc(Favorite::getCreated_at_wsh);
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(Favorite::getTarget_type_wsh, FavoriteTargetType.requireSupported(targetType));
        }
        return favoriteMapper.selectList(wrapper);
    }

    @Override
    public boolean isFavorited(Long userId, Long targetId, String targetType) {
        log.info("isFavorited() called");
        String normalizedType = FavoriteTargetType.requireSupported(targetType);
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, normalizedType));
        return count > 0;
    }

    @Override
    @Transactional
    public void toggle(Long userId, Long targetId, String targetType) {
        log.info("toggle() called");
        String normalizedType = FavoriteTargetType.requireSupported(targetType);
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, normalizedType));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId_wsh());
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUser_id_wsh(userId);
        favorite.setTarget_id_wsh(targetId);
        favorite.setTarget_type_wsh(normalizedType);
        favoriteMapper.insert(favorite);
    }

    @Override
    public PageResult<FavoriteCardDTO> pageByUser(Long userId, String targetType, int page, int size) {
        log.info("pageByUser() called");
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

    @Override
    public List<FavoriteTargetTypeDTO> listTargetTypes() {
        List<FavoriteTargetTypeDTO> list = new ArrayList<>();
        for (String code : FavoriteTargetType.codes()) {
            FavoriteTargetTypeDTO dto = new FavoriteTargetTypeDTO();
            dto.setCode_wsh(code);
            dto.setLabel_wsh(FavoriteTargetType.labelOf(code));
            list.add(dto);
        }
        return list;
    }

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
