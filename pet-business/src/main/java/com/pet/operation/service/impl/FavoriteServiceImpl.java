package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.operation.entity.Favorite;
import com.pet.operation.mapper.FavoriteMapper;
import com.pet.operation.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    public List<Favorite> listByUser(Long userId, String targetType) {
        log.info("listByUser() called");
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUser_id_wsh, userId);
        if (targetType != null) {
            wrapper.eq(Favorite::getTarget_type_wsh, targetType);
        }
        return favoriteMapper.selectList(wrapper);
    }

    public boolean isFavorited(Long userId, Long targetId, String targetType) {
        log.info("isFavorited() called");
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, targetType));
        return count > 0;
    }

    @Transactional
    public void toggle(Long userId, Long targetId, String targetType) {
        log.info("toggle() called");
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUser_id_wsh, userId)
                        .eq(Favorite::getTarget_id_wsh, targetId)
                        .eq(Favorite::getTarget_type_wsh, targetType));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId_wsh());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUser_id_wsh(userId);
            favorite.setTarget_id_wsh(targetId);
            favorite.setTarget_type_wsh(targetType);
            favoriteMapper.insert(favorite);
        }
    }
}
