package com.pet.operation.service.impl;

import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.common.FavoriteTargetType;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.service.FavoriteTargetResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 看护人（Keeper）收藏目标解析器，将收藏的看护人解析为卡片展示信息。
 * <p>
 * 组装信息包括：看护人姓名、简介、从业年限、所属商家、每日价格、头像。
 */
@Component
public class KeeperFavoriteTargetResolver implements FavoriteTargetResolver {

    private final KeeperService keeperService;
    private final MerchantService merchantService;

    public KeeperFavoriteTargetResolver(KeeperService keeperService, MerchantService merchantService) {
        this.keeperService = keeperService;
        this.merchantService = merchantService;
    }

    /**
     * 返回目标类型编码 {@link FavoriteTargetType#KEEPER}
     */
    @Override
    public String targetType() {
        return FavoriteTargetType.KEEPER;
    }

    /**
     * 批量解析看护人收藏卡片数据
     */
    @Override
    public Map<Long, FavoriteCardDTO> resolve(Collection<Long> targetIds) {
        Map<Long, FavoriteCardDTO> result = new LinkedHashMap<>();
        List<Keeper> keepers = keeperService.listByIds(targetIds);
        Map<Long, Merchant> merchants = merchantService.listByIds(
                        keepers.stream()
                                .map(Keeper::getMerchant_id_wsh)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, merchant -> merchant));

        for (Keeper keeper : keepers) {
            Merchant merchant = merchants.get(keeper.getMerchant_id_wsh());
            FavoriteCardDTO card = new FavoriteCardDTO();
            card.setTarget_id_wsh(keeper.getId_wsh());
            card.setTarget_type_wsh(FavoriteTargetType.KEEPER);
            card.setTarget_type_label_wsh(FavoriteTargetType.labelOf(FavoriteTargetType.KEEPER));
            card.setTitle_wsh(keeper.getName_wsh());
            card.setDescription_wsh(buildDescription(keeper, merchant));
            card.setPrimary_info_wsh(keeper.getExperience_years_wsh() == null ? null : keeper.getExperience_years_wsh() + " years");
            card.setSecondary_info_wsh(merchant == null ? null : merchant.getName_wsh());
            card.setAmount_wsh(keeper.getPrice_per_day_wsh());
            card.setAmount_suffix_wsh("/day");
            card.setImage_url_wsh(keeper.getAvatar_wsh());
            card.setDetail_url_wsh(FavoriteTargetType.detailPath(FavoriteTargetType.KEEPER, keeper.getId_wsh()));
            result.put(keeper.getId_wsh(), card);
        }
        return result;
    }

    private String buildDescription(Keeper keeper, Merchant merchant) {
        String merchantName = merchant == null ? null : merchant.getName_wsh();
        String experience = keeper.getExperience_years_wsh() == null ? null : keeper.getExperience_years_wsh() + " years";
        String price = keeper.getPrice_per_day_wsh() == null ? null : "Price " + keeper.getPrice_per_day_wsh() + "/day";
        return joinParts(merchantName, experience, price);
    }

    private String joinParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(part);
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
