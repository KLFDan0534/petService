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

    /**
     * 【构造服务收藏解析器】
     *
     * 业务作用：注入服务项、商家、服务分类三个业务服务。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值三个 service 字段。
     *
     * 数据处理：无。
     *
     * 业务规则：依赖均为 final 字段。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本解析器由收藏列表按目标类型路由调用。
     */
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
        // 批量查服务项，再一次带出商家与分类
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
            // 单位缺失时用 unit 占位
            card.setAmount_suffix_wsh("/" + (item.getUnit_wsh() == null || item.getUnit_wsh().isBlank() ? "unit" : item.getUnit_wsh()));
            result.put(item.getId_wsh(), card);
        }
        return result;
    }

    /**
     * 【解析服务分类名称】
     *
     * 业务作用：取服务项所属分类的名称，取不到时回退到服务的 type 字段。
     *
     * 调用场景：resolve 组装收藏卡片次要信息时。
     *
     * 调用链：resolve() → resolveCategoryName()。
     *
     * 数据处理：先看 category_id_wsh 是否在已加载的分类名 Map 中，命中则返回分类名，否则返回 item.type_wsh。
     *
     * 业务规则：只使用「已启用」分类构建的 Map，已下架分类查不到。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：回退值 type_wsh 存的是分类编码，与分类名称展示效果不一致。
     */
    private String resolveCategoryName(ServiceItem item, Map<Long, String> categoryNames) {
        if (item.getCategory_id_wsh() != null && categoryNames.containsKey(item.getCategory_id_wsh())) {
            return categoryNames.get(item.getCategory_id_wsh());
        }
        // 分类已下架时回退到服务类型编码
        return item.getType_wsh();
    }

    /**
     * 【取首张图片地址】
     *
     * 业务作用：从逗号分隔的图片串中取出第一张图片地址。
     *
     * 调用场景：resolve 设置收藏卡片封面图时。
     *
     * 调用链：resolve() → firstImage()。
     *
     * 数据处理：空值或空白返回 null，否则按逗号切分后取第 0 段并 trim。
     *
     * 业务规则：无。
     *
     * 异常情况：无。
     *
     * 注意事项：未过滤空串，若图片串以逗号开头会返回空字符串而非 null。
     */
    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        String[] parts = images.split(",");
        return parts.length == 0 ? null : parts[0].trim();
    }
}
