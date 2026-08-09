package com.pet.operation.service.impl;

import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.MerchantService;
import com.pet.common.FavoriteTargetType;
import com.pet.operation.dto.FavoriteCardDTO;
import com.pet.operation.service.FavoriteTargetResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家（Merchant）收藏目标解析器，将收藏的商家解析为卡片展示信息。
 * <p>
 * 组装信息包括：商家名称、描述/地址、评分、地址详情。
 */
@Component
public class MerchantFavoriteTargetResolver implements FavoriteTargetResolver {

    private final MerchantService merchantService;

    public MerchantFavoriteTargetResolver(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 返回目标类型编码 {@link FavoriteTargetType#MERCHANT}
     */
    @Override
    public String targetType() {
        return FavoriteTargetType.MERCHANT;
    }

    /**
     * 批量解析商家收藏卡片数据
     */
    @Override
    public Map<Long, FavoriteCardDTO> resolve(Collection<Long> targetIds) {
        Map<Long, FavoriteCardDTO> result = new LinkedHashMap<>();
        List<Merchant> merchants = merchantService.listByIds(targetIds);
        for (Merchant merchant : merchants) {
            FavoriteCardDTO card = new FavoriteCardDTO();
            card.setTarget_id_wsh(merchant.getId_wsh());
            card.setTarget_type_wsh(FavoriteTargetType.MERCHANT);
            card.setTarget_type_label_wsh(FavoriteTargetType.labelOf(FavoriteTargetType.MERCHANT));
            card.setTitle_wsh(merchant.getName_wsh());
            card.setDescription_wsh(firstNonBlank(merchant.getDescription_wsh(), merchant.getAddress_wsh()));
            card.setPrimary_info_wsh(merchant.getRating_wsh() == null ? null : "Rating " + merchant.getRating_wsh());
            card.setSecondary_info_wsh(merchant.getAddress_wsh());
            card.setDetail_url_wsh(FavoriteTargetType.detailPath(FavoriteTargetType.MERCHANT, merchant.getId_wsh()));
            result.put(merchant.getId_wsh(), card);
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
