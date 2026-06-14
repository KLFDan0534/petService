package com.pet.module.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.module.favorite.entity.Favorite;
import com.pet.module.favorite.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    public FavoriteService(FavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    public List<Favorite> listByUser(Long userId, String targetType) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId);
        if (targetType != null) {
            wrapper.eq(Favorite::getTargetType, targetType);
        }
        return favoriteMapper.selectList(wrapper);
    }

    public boolean isFavorited(Long userId, Long targetId, String targetType) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTargetId, targetId)
                        .eq(Favorite::getTargetType, targetType));
        return count > 0;
    }

    @Transactional
    public void toggle(Long userId, Long targetId, String targetType) {
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTargetId, targetId)
                        .eq(Favorite::getTargetType, targetType));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTargetId(targetId);
            favorite.setTargetType(targetType);
            favoriteMapper.insert(favorite);
        }
    }
}
