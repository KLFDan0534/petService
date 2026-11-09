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
import com.pet.common.BusinessException;
import com.pet.common.BookingUnit;
import com.pet.common.ServiceVersions;
import com.pet.order.pricing.NightPricingRule;
import com.pet.order.pricing.OrderPricing;
import com.pet.pet.entity.Pet;
import com.pet.system.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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

    /**
     * 【订单快照服务构造器】
     *
     * 业务作用：注入快照 Mapper 与 JSON 序列化器，供快照读写使用。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 构造器 → 赋值 orderSnapshotMapper / objectMapper。
     *
     * 数据处理：仅字段赋值，不处理业务数据。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无；依赖缺失时由 Spring 启动期报错。
     *
     * 注意事项：ObjectMapper 为全局共享实例，序列化行为受全局 Jackson 配置影响。
     */
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
        // 各实体快照序列化后整体落库
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
     * 【回填看护者快照（实现）】
     *
     * 业务作用：
     * 待分配订单在抢单/指派/超时自动分配成功后，把负责人身份补进快照。
     *
     * 数据处理：
     * 按订单ID查出快照，只覆盖 keeper_snapshot_wsh；快照不存在时静默跳过（历史订单可能没有快照）。
     */
    @Override
    public void refreshKeeperSnapshot(Long orderId, Keeper keeper) {
        if (orderId == null || keeper == null) {
            return;
        }
        OrderSnapshot existing = orderSnapshotMapper.selectOne(
                new LambdaQueryWrapper<OrderSnapshot>().eq(OrderSnapshot::getOrder_id_wsh, orderId));
        if (existing == null) {
            return;
        }
        OrderSnapshot update = new OrderSnapshot();
        update.setId_wsh(existing.getId_wsh());
        update.setKeeper_snapshot_wsh(toJson(keeperSnapshot(keeper)));
        orderSnapshotMapper.updateById(update);
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
        // 一次批量查出，避免订单列表 N+1
        List<OrderSnapshot> snapshots = orderSnapshotMapper.selectList(
                new LambdaQueryWrapper<OrderSnapshot>().in(OrderSnapshot::getOrder_id_wsh, orderIds));
        return snapshots.stream()
                .map(this::toDTO)
                .collect(Collectors.toMap(OrderSnapshotDTO::getOrder_id_wsh, Function.identity(), (a, b) -> a));
    }

    /**
     * 【快照实体转 DTO】
     *
     * 业务作用：把 order_snapshot 表记录转换为 OrderSnapshotDTO，并将 7 个 JSON 字段还原为 Map。
     *
     * 调用场景：查询单个快照或批量查询快照后转换结果时调用。
     *
     * 调用链：getByOrderId → toDTO；findDTOMapByOrderIds → stream.map(this::toDTO)。
     *
     * 数据处理：逐字段拷贝主键、订单号、创建时间；owner/pet/merchant/keeper/service/address/price 七个字段经 fromJson 转 Map。
     *
     * 业务规则：入参为 null 时直接返回 null，不构造空对象。
     *
     * 状态影响：无（只读转换）。
     *
     * 异常情况：无；JSON 解析失败由 fromJson 内部吞掉并返回空 Map。
     *
     * 注意事项：DTO 中快照字段为 Map 类型，与实体的 String 存储结构不同。
     */
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

    /**
     * 【构建下单人快照】
     *
     * 业务作用：提取下单用户的关键字段，作为订单快照中的下单人信息留存。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → ownerSnapshot → toJson。
     *
     * 数据处理：取 id_wsh / username_wsh / nickname_wsh；phone_wsh 经 maskPhone 脱敏后写入。
     *
     * 业务规则：owner 为 null 时返回空 Map，不抛异常。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：手机号脱敏后入库，快照中不保留完整号码。
     */
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

    /**
     * 【构建宠物快照】
     *
     * 业务作用：留存下单时宠物的档案信息，供历史订单回显。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → petSnapshot → toJson。
     *
     * 数据处理：拷贝 id、名称、类型、品种、年龄、体重、性别、绝育、免疫、过敏、习性共 11 个字段。
     *
     * 业务规则：pet 为 null 时返回空 Map。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：为全量快照，之后修改宠物档案不影响已生成的订单。
     */
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

    /**
     * 【构建商家快照】
     *
     * 业务作用：留存下单时商家的名称、电话、地址、营业执照与评分。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → merchantSnapshot → toJson。
     *
     * 数据处理：拷贝 id_wsh / name_wsh / phone_wsh / address_wsh / business_license_wsh / rating_wsh。
     *
     * 业务规则：merchant 为 null（平台自营或未归属）时返回空 Map。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：商家电话未脱敏，与 owner 快照的处理方式不一致。
     */
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

    /**
     * 【构建看护者快照】
     *
     * 业务作用：留存下单时看护者的经验年限、评分、单价、接单上限与简介。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → keeperSnapshot → toJson。
     *
     * 数据处理：拷贝 8 个字段，含 price_per_day_wsh 与 max_pets_wsh。
     *
     * 业务规则：keeper 为 null（商家自营或尚未分配）时返回空 Map。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：单价以快照为准，看护者后续调价不影响历史订单。
     */
    private Map<String, Object> keeperSnapshot(Keeper keeper) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (keeper != null) {
            data.put("id_wsh", keeper.getId_wsh());
            data.put("name_wsh", keeper.getName_wsh());
            data.put("phone_wsh", keeper.getPhone_wsh());
            data.put("experience_years_wsh", keeper.getExperience_years_wsh());
            data.put("rating_wsh", keeper.getRating_wsh());
            data.put("max_pets_wsh", keeper.getMax_pets_wsh());
            data.put("bio_wsh", keeper.getBio_wsh());
        }
        return data;
    }

    /**
     * 【构建服务产品快照】
     *
     * 业务作用：留存下单时服务产品的价格、计价单位、预约模式与图片。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → serviceSnapshot → toJson。
     *
     * 数据处理：拷贝 8 个业务字段，并额外写入 version_wsh = ServiceVersions.format(updated_at_wsh)。
     *
     * 业务规则：service 为 null 时返回空 Map。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：version_wsh 由更新时间派生，可用于比对产品是否在下单后发生变更。
     */
    private Map<String, Object> serviceSnapshot(ServiceItem service) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (service != null) {
            data.put("id_wsh", service.getId_wsh());
            data.put("name_wsh", service.getName_wsh());
            data.put("description_wsh", service.getDescription_wsh());
            data.put("price_wsh", service.getPrice_wsh());
            data.put("unit_wsh", service.getUnit_wsh());
            data.put("booking_mode_wsh", service.getBooking_mode_wsh());
            data.put("duration_minutes_wsh", service.getDuration_minutes_wsh());
            data.put("images_wsh", service.getImages_wsh());
            data.put("version_wsh", ServiceVersions.format(service.getUpdated_at_wsh()));
        }
        return data;
    }

    /**
     * 【构建地址与时间快照】
     *
     * 业务作用：留存送接地址、时间窗与紧急联系人等履约信息。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → addressSnapshot → toJson。
     *
     * 数据处理：送/接地址经 firstText 回落到商家地址；时间窗与联系人字段直接取请求值。
     *
     * 业务规则：请求未填地址时使用商家默认地址；紧急联系人字段不做格式校验。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：request 不可为 null（上游 createOrder 已保证）；merchant 为 null 时回落值为 null。
     */
    private Map<String, Object> addressSnapshot(OrderCreateRequestDTO request, Merchant merchant) {
        Map<String, Object> data = new LinkedHashMap<>();
        // 未填地址时回落到商家地址
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

    /**
     * 【构建价格快照】
     *
     * 业务作用：留存下单时的计费口径、优惠明细与结算金额，作为后续对账依据。
     *
     * 调用场景：createForOrder 组装快照时调用。
     *
     * 调用链：createForOrder → priceSnapshot → toJson。
     *
     * 数据处理：共 21 个字段，金额类字段统一经 defaultMoney 把 null 归一为 0。
     *
     * 业务规则：优惠券、会员、平台补贴、结算金额分别独立记录，互不覆盖。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：final_amount_wsh 为实付金额，退款核算应以本快照为准。
     */
    private Map<String, Object> priceSnapshot(PetOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("billing_unit_wsh", order.getBilling_unit_wsh());
        data.put("quantity_wsh", order.getQuantity_wsh());
        data.put("unit_price_wsh", defaultMoney(order.getUnit_price_wsh()));
        data.put("duration_minutes_wsh", order.getDuration_minutes_wsh());
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
        putNightPricingSnapshot(data, order);
        return data;
    }

    /**
     * 按夜订单的计价审计快照：冻结夜价、预订夜数、锚点与提前/晚接规则版本。
     * <p>
     * 这些键是实际补费的唯一真相源（AE17：商家改价不影响原单）。
     * 初始金额以 initial_* 前缀单独保存，后续累计补费不回写这里。
     */
    private void putNightPricingSnapshot(Map<String, Object> data, PetOrder order) {
        if (!BookingUnit.NIGHT.equals(BookingUnit.normalize(order.getBilling_unit_wsh()))
                || order.getStart_date_wsh() == null) {
            return;
        }
        int nights = order.getQuantity_wsh() == null ? 0 : order.getQuantity_wsh();
        if (nights < 1) {
            return;
        }
        BigDecimal nightPrice = defaultMoney(order.getUnit_price_wsh());
        LocalDateTime anchor = OrderPricing.checkinAnchor(order.getStart_date_wsh());
        Duration early = OrderPricing.earlyDuration(anchor, order.getDelivery_time_wsh());
        data.put("pricing_rule_version", OrderPricing.PRICING_RULE_VERSION);
        data.put("booked_nights", nights);
        data.put("night_price", nightPrice);
        data.put("checkin_anchor_time", OrderPricing.CHECKIN_ANCHOR_TIME.toString());
        data.put("checkout_grace_minutes", OrderPricing.CHECKOUT_GRACE_MINUTES);
        data.put("early_limit_minutes", OrderPricing.EARLY_LIMIT_MINUTES);
        data.put("overtime_price_mode", OrderPricing.OVERTIME_PRICE_MODE);
        data.put("early_extra_nights", OrderPricing.earlyExtraNights(early));
        data.put("early_extra_hours", OrderPricing.earlyExtraHours(early));
        data.put("early_amount", OrderPricing.earlyAmount(nightPrice, nights, early));
        data.put("standard_checkout_at",
                OrderPricing.standardCheckoutAt(order.getStart_date_wsh(), nights).toString());
        data.put("free_checkout_until",
                OrderPricing.freeCheckoutUntil(order.getStart_date_wsh(), nights).toString());
        data.put("initial_total_amount", defaultMoney(order.getTotal_amount_wsh()));
        data.put("initial_final_amount", defaultMoney(order.getFinal_amount_wsh()));
        data.put("initial_settlement_amount", defaultMoney(order.getSettlement_amount_wsh()));
    }

    /**
     * 【读取并校验按夜计价快照（实现）】
     *
     * 业务作用：
     * 从 price_snapshot_wsh 读出冻结的夜价/夜数/锚点，供接走确认计算实际晚接费。
     * 快照缺失、版本不是 night-v1、关键字段缺失或无法解析时明确失败，
     * 不使用当前服务价格兜底（否则商家改价会改写历史订单）。
     */
    @Override
    public NightPricingRule requireNightPricingRule(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(500, "订单计价快照缺失，无法结算");
        }
        OrderSnapshot snapshot = orderSnapshotMapper.selectOne(
                new LambdaQueryWrapper<OrderSnapshot>().eq(OrderSnapshot::getOrder_id_wsh, orderId));
        if (snapshot == null) {
            throw new BusinessException(500, "订单计价快照缺失，无法结算");
        }
        Map<String, Object> price = fromJson(snapshot.getPrice_snapshot_wsh());
        Object version = price.get("pricing_rule_version");
        if (!OrderPricing.PRICING_RULE_VERSION.equals(version == null ? null : String.valueOf(version))) {
            throw new BusinessException(500, "订单计价快照版本不支持，无法结算");
        }
        try {
            BigDecimal nightPrice = new BigDecimal(String.valueOf(price.get("night_price")));
            int nights = Integer.parseInt(String.valueOf(price.get("booked_nights")));
            LocalDateTime standardCheckoutAt = LocalDateTime.parse(String.valueOf(price.get("standard_checkout_at")));
            LocalDateTime freeCheckoutUntil = LocalDateTime.parse(String.valueOf(price.get("free_checkout_until")));
            if (nights < 1) {
                throw new NumberFormatException("booked_nights");
            }
            return new NightPricingRule(nightPrice, nights, standardCheckoutAt, freeCheckoutUntil);
        } catch (RuntimeException e) {
            throw new BusinessException(500, "订单计价快照格式错误，无法结算");
        }
    }

    /**
     * 【金额空值归一】
     *
     * 业务作用：把可能为 null 的金额统一转换为 0，避免快照中出现 null。
     *
     * 调用场景：priceSnapshot 组装各金额字段时调用。
     *
     * 调用链：priceSnapshot → defaultMoney。
     *
     * 数据处理：null → BigDecimal.ZERO，其余原样返回。
     *
     * 业务规则：仅处理 null，不做精度调整或舍入。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回的是常量 ZERO 实例，调用方不应修改其值。
     */
    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 【Map 序列化为 JSON】
     *
     * 业务作用：把快照 Map 序列化为字符串，写入数据库的 text 字段。
     *
     * 调用场景：createForOrder 写入各 snapshot 字段前调用。
     *
     * 调用链：createForOrder → toJson → objectMapper.writeValueAsString。
     *
     * 数据处理：Map → JSON 字符串，字段顺序由 LinkedHashMap 保证。
     *
     * 业务规则：序列化失败直接抛异常，不写入半成品快照。
     *
     * 状态影响：无（尚未落库）。
     *
     * 异常情况：序列化失败 → IllegalStateException，事务回滚。
     *
     * 注意事项：异常未携带原始数据内容，排查时需结合上下文日志。
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize order snapshot", e);
        }
    }

    /**
     * 【JSON 反序列化为 Map】
     *
     * 业务作用：把数据库中的快照 JSON 还原为 Map，供 DTO 使用。
     *
     * 调用场景：toDTO 转换各快照字段时调用。
     *
     * 调用链：toDTO → fromJson → objectMapper.readValue。
     *
     * 数据处理：null 或空白串返回 Map.of()；解析失败同样返回 Map.of()。
     *
     * 业务规则：解析异常静默吞掉，保证订单详情不因历史脏数据整体失败。
     *
     * 状态影响：无。
     *
     * 异常情况：解析失败不抛异常，返回空 Map。
     *
     * 注意事项：返回的空 Map 为不可变对象，调用方不可写入。
     */
    private Map<String, Object> fromJson(String json) {
        // 脏数据解析失败返回空 Map，不阻断详情
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 【取首个非空文本】
     *
     * 业务作用：在请求值与兜底值之间取第一个非空白文本。
     *
     * 调用场景：addressSnapshot 处理送/接地址时调用。
     *
     * 调用链：addressSnapshot → firstText。
     *
     * 数据处理：value 为 null 或空白返回 fallback，否则返回 value。
     *
     * 业务规则：仅判断空白，不做格式校验。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：fallback 也可能为 null，调用方需自行判空。
     */
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
        // 号码过短无法脱敏，原样返回
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
