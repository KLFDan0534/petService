package com.pet.module.rating.controller;

import com.pet.common.Result;
import com.pet.module.rating.entity.Rating;
import com.pet.module.rating.service.RatingService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public Result<List<Rating>> getRatings(@RequestParam Long targetId,
                                            @RequestParam String targetType) {
        return Result.success(ratingService.getRatingsByTarget(targetId, targetType));
    }

    @PostMapping
    public Result<Rating> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                  @RequestBody Rating rating) {
        return Result.success(ratingService.createRating(token.getUserId(), rating));
    }
}
