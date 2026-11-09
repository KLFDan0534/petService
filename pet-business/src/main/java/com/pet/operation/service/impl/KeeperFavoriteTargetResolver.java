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

    /**
     * 【构造方法】注入看护人与商家服务
     *
     * 业务作用：由 Spring 注入 KeeperService 与 MerchantService，供收藏卡片解析使用。
     *
     * 调用场景：Spring 容器启动创建该 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → 两个 final 字段赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
     * 注意事项：依赖为 final 字段。
     */
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
        // 一次性批量取商家，避免逐条查询
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
            card.setImage_url_wsh(keeper.getAvatar_wsh());
            card.setDetail_url_wsh(FavoriteTargetType.detailPath(FavoriteTargetType.KEEPER, keeper.getId_wsh()));
            result.put(keeper.getId_wsh(), card);
        }
        return result;
    }

    /**
     * 【拼装看护人描述】组合商家名、从业年限与每日价格
     *
     * 业务作用：为收藏卡片生成一段综合描述文案。
     *
     * 调用场景：resolve 组装 FavoriteCardDTO 时调用。
     *
     * 调用链：resolve → 本方法 → joinParts。
     *
     * 数据处理：商家名为空时置 null，年限拼成 "N years"，价格拼成 "Price N/day"，再由 joinParts 拼接。
     *
     * 业务规则：任一字段为空则不参与拼接；全部为空时返回 null。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：文案为英文（years / Price /day），与项目其它中文提示不一致；价格未做格式化，可能直接暴露原始小数位。
     */
    private String buildDescription(Keeper keeper, Merchant merchant) {
        String merchantName = merchant == null ? null : merchant.getName_wsh();
        String experience = keeper.getExperience_years_wsh() == null ? null : keeper.getExperience_years_wsh() + " years";
        return joinParts(merchantName, experience);
    }

    /**
     * 【拼接文本片段】用竖线连接非空片段
     *
     * 业务作用：把多个可选文案片段拼成一条描述。
     *
     * 调用场景：buildDescription 中调用。
     *
     * 调用链：buildDescription → 本方法。
     *
     * 数据处理：跳过 null 与空白片段，其余用 " | " 连接。
     *
     * 业务规则：所有片段都为空时返回 null 而非空串。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不做 trim，片段自带首尾空格时会被保留在分隔符两侧。
     */
    private String joinParts(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            // 跳过空片段，全部为空返回 null
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
