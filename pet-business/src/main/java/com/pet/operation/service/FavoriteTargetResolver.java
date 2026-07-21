package com.pet.operation.service;

import com.pet.operation.dto.FavoriteCardDTO;

import java.util.Collection;
import java.util.Map;

public interface FavoriteTargetResolver {
    String targetType();

    Map<Long, FavoriteCardDTO> resolve(Collection<Long> targetIds);
}
