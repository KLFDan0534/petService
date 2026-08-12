package com.pet.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.order.dto.OrderCreateRequestDTO;
import com.pet.order.dto.OrderSnapshotDTO;
import com.pet.order.entity.OrderSnapshot;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderSnapshotMapper;
import com.pet.order.service.OrderSnapshotService;
import com.pet.common.ServiceVersions;
import com.pet.pet.entity.Pet;
import com.pet.system.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OrderSnapshotService} that creates immutable JSON snapshots
 * of all related entities at order creation time. Snapshots preserve the exact state
 * of owner, pet, merchant, keeper, service, address, and pricing data so that historical
 * order records remain accurate even if the underlying entities change later.
 */
@Service
public class OrderSnapshotServiceImpl implements OrderSnapshotService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final OrderSnapshotMapper orderSnapshotMapper;
    private final ObjectMapper objectMapper;

    public OrderSnapshotServiceImpl(OrderSnapshotMapper orderSnapshotMapper, ObjectMapper objectMapper) {
        this.orderSnapshotMapper = orderSnapshotMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 【创建订单快照（实现）】
     *
     * 业务作用：
     * 组装各实体关键字段快照（owner/pet/merchant/keeper/service/address/price），
     * 序列化为JSON后写入order_snapshot表。
     *
     * @param order    订单
     * @param owner    主人
     * @param pet      宠物
     * @param merchant 商家
     * @param keeper   看护者
     * @param service  服务
     * @param request  创建请求
     * @return 快照实体
     */
    @Override
    public OrderSnapshot createForOrder(PetOrder order,
                                        User owner,
                                        Pet pet,
                                        Merchant merchant,
                                        Keeper keeper,
                                        ServiceItem service,
                                        OrderCreateRequestDTO request) {
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setOrder_id_wsh(order.getId_wsh());
        snapshot.setOrder_no_wsh(order.getOrder_no_wsh());
        snapshot.setOwner_snapshot_wsh(toJson(ownerSnapshot(owner)));
        snapshot.setPet_snapshot_wsh(toJson(petSnapshot(pet)));
        snapshot.setMerchant_snapshot_wsh(toJson(merchantSnapshot(merchant)));
        snapshot.setKeeper_snapshot_wsh(toJson(keeperSnapshot(keeper)));
        snapshot.setService_snapshot_wsh(toJson(serviceSnapshot(service)));
        snapshot.setAddress_snapshot_wsh(toJson(addressSnapshot(request, merchant)));
        snapshot.setPrice_snapshot_wsh(toJson(priceSnapshot(order)));
        orderSnapshotMapper.insert(snapshot);
        return snapshot;
    }

    /**
     * 【查询订单快照（实现）】
     *
     * @param orderId 订单ID
     * @return 快照DTO，不存在返回null
     */
    @Override
    public OrderSnapshotDTO getByOrderId(Long orderId) {
        OrderSnapshot snapshot = orderSnapshotMapper.selectOne(
                new LambdaQueryWrapper<OrderSnapshot>()
                        .eq(OrderSnapshot::getOrder_id_wsh, orderId)
                        .last("LIMIT 1"));
        return toDTO(snapshot);
    }

    /**
     * 【批量查询订单快照（实现）】
     *
     * 业务作用：
     * 用于批量订单列表加载快照，避免N+1查询问题。
     *
     * @param orderIds 订单ID集合
     * @return 订单ID → 快照DTO 的映射
     */
    @Override
    public Map<Long, OrderSnapshotDTO> findDTOMapByOrderIds(Set<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderSnapshot> snapshots = orderSnapshotMapper.selectList(
                new LambdaQueryWrapper<OrderSnapshot>().in(OrderSnapshot::getOrder_id_wsh, orderIds));
        return snapshots.stream()
                .map(this::toDTO)
                .collect(Collectors.toMap(OrderSnapshotDTO::getOrder_id_wsh, Function.identity(), (a, b) -> a));
    }

    private OrderSnapshotDTO toDTO(OrderSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        OrderSnapshotDTO dto = new OrderSnapshotDTO();
        dto.setId_wsh(snapshot.getId_wsh());
        dto.setOrder_id_wsh(snapshot.getOrder_id_wsh());
        dto.setOrder_no_wsh(snapshot.getOrder_no_wsh());
        dto.setOwner_snapshot_wsh(fromJson(snapshot.getOwner_snapshot_wsh()));
        dto.setPet_snapshot_wsh(fromJson(snapshot.getPet_snapshot_wsh()));
        dto.setMerchant_snapshot_wsh(fromJson(snapshot.getMerchant_snapshot_wsh()));
        dto.setKeeper_snapshot_wsh(fromJson(snapshot.getKeeper_snapshot_wsh()));
        dto.setService_snapshot_wsh(fromJson(snapshot.getService_snapshot_wsh()));
        dto.setAddress_snapshot_wsh(fromJson(snapshot.getAddress_snapshot_wsh()));
        dto.setPrice_snapshot_wsh(fromJson(snapshot.getPrice_snapshot_wsh()));
        dto.setCreated_at_wsh(snapshot.getCreated_at_wsh());
        return dto;
    }

    private Map<String, Object> ownerSnapshot(User owner) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (owner != null) {
            data.put("id_wsh", owner.getId_wsh());
            data.put("username_wsh", owner.getUsername_wsh());
            data.put("nickname_wsh", owner.getNickname_wsh());
            data.put("phone_wsh", maskPhone(owner.getPhone_wsh()));
        }
        return data;
    }

    private Map<String, Object> petSnapshot(Pet pet) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (pet != null) {
            data.put("id_wsh", pet.getId_wsh());
            data.put("name_wsh", pet.getName_wsh());
            data.put("type_wsh", pet.getType_wsh());
            data.put("breed_wsh", pet.getBreed_wsh());
            data.put("age_wsh", pet.getAge_wsh());
            data.put("weight_wsh", pet.getWeight_wsh());
            data.put("gender_wsh", pet.getGender_wsh());
            data.put("sterilized_wsh", pet.getSterilized_wsh());
            data.put("vaccinated_wsh", pet.getVaccinated_wsh());
            data.put("allergies_wsh", pet.getAllergies_wsh());
            data.put("habits_wsh", pet.getHabits_wsh());
        }
        return data;
    }

    private Map<String, Object> merchantSnapshot(Merchant merchant) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (merchant != null) {
            data.put("id_wsh", merchant.getId_wsh());
            data.put("name_wsh", merchant.getName_wsh());
            data.put("phone_wsh", merchant.getPhone_wsh());
            data.put("address_wsh", merchant.getAddress_wsh());
            data.put("business_license_wsh", merchant.getBusiness_license_wsh());
            data.put("rating_wsh", merchant.getRating_wsh());
        }
        return data;
    }

    private Map<String, Object> keeperSnapshot(Keeper keeper) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (keeper != null) {
            data.put("id_wsh", keeper.getId_wsh());
            data.put("name_wsh", keeper.getName_wsh());
            data.put("phone_wsh", keeper.getPhone_wsh());
            data.put("experience_years_wsh", keeper.getExperience_years_wsh());
            data.put("rating_wsh", keeper.getRating_wsh());
            data.put("price_per_day_wsh", keeper.getPrice_per_day_wsh());
            data.put("max_pets_wsh", keeper.getMax_pets_wsh());
            data.put("bio_wsh", keeper.getBio_wsh());
        }
        return data;
    }

    private Map<String, Object> serviceSnapshot(ServiceItem service) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (service != null) {
            data.put("id_wsh", service.getId_wsh());
            data.put("name_wsh", service.getName_wsh());
            data.put("description_wsh", service.getDescription_wsh());
            data.put("price_wsh", service.getPrice_wsh());
            data.put("unit_wsh", service.getUnit_wsh());
            data.put("version_wsh", ServiceVersions.format(service.getUpdated_at_wsh()));
        }
        return data;
    }

    private Map<String, Object> addressSnapshot(OrderCreateRequestDTO request, Merchant merchant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("delivery_address_wsh", firstText(request.getDelivery_address_wsh(), merchant != null ? merchant.getAddress_wsh() : null));
        data.put("delivery_time_wsh", request.getDelivery_time_wsh());
        data.put("receiver_available_start_wsh", request.getReceiver_available_start_wsh());
        data.put("receiver_available_end_wsh", request.getReceiver_available_end_wsh());
        data.put("pickup_address_wsh", firstText(request.getPickup_address_wsh(), merchant != null ? merchant.getAddress_wsh() : null));
        data.put("pickup_time_wsh", request.getPickup_time_wsh());
        data.put("emergency_contact_name_wsh", request.getEmergency_contact_name_wsh());
        data.put("emergency_contact_phone_wsh", request.getEmergency_contact_phone_wsh());
        return data;
    }

    private Map<String, Object> priceSnapshot(PetOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days_wsh", order.getDays_wsh());
        data.put("price_per_day_wsh", defaultMoney(order.getPrice_per_day_wsh()));
        data.put("total_amount_wsh", defaultMoney(order.getTotal_amount_wsh()));
        data.put("discount_wsh", defaultMoney(order.getDiscount_wsh()));
        data.put("coupon_id_wsh", order.getCoupon_id_wsh());
        data.put("coupon_template_id_wsh", order.getCoupon_template_id_wsh());
        data.put("coupon_discount_wsh", defaultMoney(order.getCoupon_discount_wsh()));
        data.put("membership_id_wsh", order.getMembership_id_wsh());
        data.put("membership_plan_id_wsh", order.getMembership_plan_id_wsh());
        data.put("membership_discount_wsh", defaultMoney(order.getMembership_discount_wsh()));
        data.put("membership_snapshot_wsh", order.getMembership_snapshot_wsh());
        data.put("platform_subsidy_wsh", defaultMoney(order.getPlatform_subsidy_wsh()));
        data.put("settlement_amount_wsh", defaultMoney(order.getSettlement_amount_wsh()));
        data.put("promotion_snapshot_wsh", order.getPromotion_snapshot_wsh());
        data.put("final_amount_wsh", defaultMoney(order.getFinal_amount_wsh()));
        data.put("start_date_wsh", order.getStart_date_wsh());
        data.put("end_date_wsh", order.getEnd_date_wsh());
        return data;
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize order snapshot", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Masks the middle digits of a phone number for privacy (e.g., 138****1234).
     *
     * @param phone the raw phone number
     * @return the masked phone number, or the original if too short to mask
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
