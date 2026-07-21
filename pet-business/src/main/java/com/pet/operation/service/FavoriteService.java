package com.pet.operation.service;

import com.pet.common.PageResult;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.dto.FavoriteTargetTypeDTO;
import com.pet.operation.entity.Favorite;

import java.util.List;

public interface FavoriteService {
    List<Favorite> listByUser(Long userId, String targetType);

    boolean isFavorited(Long userId, Long targetId, String targetType);

    void toggle(Long userId, Long targetId, String targetType);

    PageResult<FavoriteCardDTO> pageByUser(Long userId, String targetType, int page, int size);

    List<FavoriteTargetTypeDTO> listTargetTypes();
}
