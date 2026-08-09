package com.pet.operation.service.impl;

import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceCategoryService;
import com.pet.boarding.service.ServiceItemService;
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
 * 服务（ServiceItem）收藏目标解析器，将收藏的服务项解析为卡片展示信息。
 * <p>
 * 组装信息包括：服务名称、描述、首张图片、所属商家、服务分类、单价及单位。
 */
@Component
public class ServiceFavoriteTargetResolver implements FavoriteTargetResolver {

    private final ServiceItemService serviceItemService;
    private final MerchantService merchantService;
    private final ServiceCategoryService serviceCategoryService;

    public ServiceFavoriteTargetResolver(ServiceItemService serviceItemService,
                                         MerchantService merchantService,
                                         ServiceCategoryService serviceCategoryService) {
        this.serviceItemService = serviceItemService;
        this.merchantService = merchantService;
        this.serviceCategoryService = serviceCategoryService;
    }

    /**
     * 返回目标类型编码 {@link FavoriteTargetType#SERVICE}
     */
    @Override
    public String targetType() {
        return FavoriteTargetType.SERVICE;
    }

    /**
     * 批量解析服务收藏卡片数据
     */
    @Override
    public Map<Long, FavoriteCardDTO> resolve(Collection<Long> targetIds) {
        Map<Long, FavoriteCardDTO> result = new LinkedHashMap<>();
        List<ServiceItem> items = serviceItemService.listByIds(targetIds);
        Map<Long, Merchant> merchants = merchantService.listByIds(
                        items.stream()
                                .map(ServiceItem::getMerchant_id_wsh)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, merchant -> merchant));
        Map<Long, String> categoryNames = serviceCategoryService.listAllEnabled().stream()
                .collect(Collectors.toMap(ServiceCategory::getId_wsh, ServiceCategory::getName_wsh));

        for (ServiceItem item : items) {
            Merchant merchant = merchants.get(item.getMerchant_id_wsh());
            FavoriteCardDTO card = new FavoriteCardDTO();
            card.setTarget_id_wsh(item.getId_wsh());
            card.setTarget_type_wsh(FavoriteTargetType.SERVICE);
            card.setTarget_type_label_wsh(FavoriteTargetType.labelOf(FavoriteTargetType.SERVICE));
            card.setTitle_wsh(item.getName_wsh());
            card.setDescription_wsh(item.getDescription_wsh());
            card.setImage_url_wsh(firstImage(item.getImages_wsh()));
            card.setDetail_url_wsh(FavoriteTargetType.detailPath(FavoriteTargetType.SERVICE, item.getId_wsh()));
            card.setPrimary_info_wsh(merchant == null ? null : merchant.getName_wsh());
            card.setSecondary_info_wsh(resolveCategoryName(item, categoryNames));
            card.setAmount_wsh(item.getPrice_wsh());
            card.setAmount_suffix_wsh("/" + (item.getUnit_wsh() == null || item.getUnit_wsh().isBlank() ? "unit" : item.getUnit_wsh()));
            result.put(item.getId_wsh(), card);
        }
        return result;
    }

    private String resolveCategoryName(ServiceItem item, Map<Long, String> categoryNames) {
        if (item.getCategory_id_wsh() != null && categoryNames.containsKey(item.getCategory_id_wsh())) {
            return categoryNames.get(item.getCategory_id_wsh());
        }
        return item.getType_wsh();
    }

    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        String[] parts = images.split(",");
        return parts.length == 0 ? null : parts[0].trim();
    }
}
