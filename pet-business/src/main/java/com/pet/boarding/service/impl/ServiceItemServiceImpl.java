package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.dto.ServiceManageDetailVO;
import com.pet.boarding.dto.ServiceMediaDTO;
import com.pet.boarding.dto.ServiceProductDetailVO;
import com.pet.boarding.dto.ServiceProductMediaVO;
import com.pet.boarding.dto.ServiceQueryResultVO;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.ServiceItemService;
import com.pet.boarding.service.ServiceMediaService;
import com.pet.common.BookingErrorCode;
import com.pet.common.BookingUnit;
import com.pet.common.BusinessException;
import com.pet.common.ServiceVersions;
import com.pet.common.StatusCode;
import com.pet.common.geo.GeoDistanceUtils;
import com.pet.customer.mapper.RatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 服务项目管理服务实现。
 * <p>
 * 负责商家服务项目的 CRUD、启用/禁用切换、图片更新等。
 * 创建和更新时根据分类ID自动关联分类编码（code）写入 type 字段，用于前端归类展示。
 */
@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    private static final Set<String> SUPPORTED_SORTS = Set.of("default", "price_asc", "rating_desc", "distance_asc");
    private static final int MAX_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000");

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceCategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final RatingMapper ratingMapper;
    private final ServiceMediaService serviceMediaService;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper,
                                  ServiceCategoryMapper categoryMapper,
                                  MerchantMapper merchantMapper,
                                  RatingMapper ratingMapper,
                                  ServiceMediaService serviceMediaService) {
        this.serviceItemMapper = serviceItemMapper;
        this.categoryMapper = categoryMapper;
        this.merchantMapper = merchantMapper;
        this.ratingMapper = ratingMapper;
        this.serviceMediaService = serviceMediaService;
    }

    /**
     * 【查询商家下所有服务项目（含禁用）】
     *
     * 业务作用：查询某个商家下所有服务项目（商家后台管理用）。
     * 调用场景：商家在后台管理服务项目列表时调用。
     * 调用链：ServiceItemController → listByMerchantForManage → ServiceItemMapper.selectList
     * 数据处理：按 merchant_id 查询全部，按创建时间倒序。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByMerchantForManage(Long merchantId) {
        log.info("listByMerchantForManage() 被调用");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .orderByDesc(ServiceItem::getCreated_at_wsh));
    }

    /**
     * 【根据ID查询服务项目】
     *
     * 业务作用：根据主键ID查询服务项目。
     * 调用场景：被 update、delete 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → ServiceItemMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛 BusinessException。
     * 状态影响：只读操作。
     * 异常情况：服务项目不存在时抛 BusinessException。
     */
    @Override
    @Cacheable(value = "serviceItem", key = "#id", unless = "#result == null || #result == ''")
    public ServiceItem getById(Long id) {
        log.info("getById() 被调用");
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("服务项目不存在");
        }
        return item;
    }

    /**
     * 【批量查询服务项目】
     *
     * 业务作用：根据多个ID批量查询服务项目。
     * 调用链：上层方法 → listByIds → ServiceItemMapper.selectBatchIds
     * 数据处理：按ID集合批量查询，参为空返回空列表。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByIds(Collection<Long> ids) {
        log.info("listByIds() 被调用");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return serviceItemMapper.selectBatchIds(ids);
    }

    /**
     * 【创建服务项目（聚合）】
     *
     * 业务作用：商家新增一个服务项目，标量与图册在同一事务内写入。
     * 调用场景：商家在后台新增服务项目时调用。
     * 业务规则：归属商家由服务端派生（调用方已完成作用域解析）；
     * 分类必须存在且启用；名称/描述/价格/单位均校验；图册非空时整体替换校验。
     * 状态影响：新增服务项目记录（默认已启用）+ 图册行。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-item-page", "serviceItem"},
            allEntries = true)
    public ServiceItem create(Long merchantId, ServiceItemCreateRequestDTO dto) {
        log.info("create() 被调用");
        validateName(dto.getName_wsh());
        validateDescription(dto.getDescription_wsh());
        validatePrice(dto.getPrice_wsh());
        ServiceCategory cat = requireEnabledCategory(dto.getCategory_id_wsh());

        ServiceItem item = new ServiceItem();
        item.setMerchant_id_wsh(merchantId);
        item.setName_wsh(dto.getName_wsh());
        item.setCategory_id_wsh(cat.getId_wsh());
        item.setType_wsh(cat.getCode_wsh());
        item.setDescription_wsh(dto.getDescription_wsh());
        item.setPrice_wsh(dto.getPrice_wsh());
        applyUnitContract(item, dto.getUnit_wsh(), dto.getDuration_minutes_wsh());
        item.setImages_wsh(dto.getImages_wsh());
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        serviceItemMapper.insert(item);
        if (dto.getMedia_wsh() != null && !dto.getMedia_wsh().isEmpty()) {
            serviceMediaService.replaceMedia(item.getId_wsh(), dto.getMedia_wsh());
        }
        return item;
    }

    /**
     * 【更新服务项目（聚合）】
     *
     * 业务作用：修改服务项目信息；仅更新非 null 字段；更新分类时同步
     * 更新 type 编码；media_wsh 非 null 时整体替换图册（空集合表示清空）。
     * 业务规则：更新前对聚合根加行锁（SELECT ... FOR UPDATE），与图册替换
     * 串行化；名称/描述/价格/状态/单位/分类均校验。
     * 状态影响：更新服务项目字段，可选整体替换图册。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-item-page", "serviceItem"},
            allEntries = true)
    public ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto) {
        log.info("update() 被调用");
        assertPositiveId(id);
        ServiceItem existing = serviceItemMapper.selectByIdForUpdate(id);
        if (existing == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "服务项目不存在");
        }
        if (dto.getName_wsh() != null) {
            validateName(dto.getName_wsh());
            existing.setName_wsh(dto.getName_wsh());
        }
        if (dto.getCategory_id_wsh() != null) {
            ServiceCategory cat = requireEnabledCategory(dto.getCategory_id_wsh());
            existing.setCategory_id_wsh(cat.getId_wsh());
            existing.setType_wsh(cat.getCode_wsh());
        }
        if (dto.getDescription_wsh() != null) {
            validateDescription(dto.getDescription_wsh());
            existing.setDescription_wsh(dto.getDescription_wsh());
        }
        if (dto.getPrice_wsh() != null) {
            validatePrice(dto.getPrice_wsh());
            existing.setPrice_wsh(dto.getPrice_wsh());
        }
        if (dto.getUnit_wsh() != null || dto.getDuration_minutes_wsh() != null) {
            applyUnitContract(existing, dto.getUnit_wsh(), dto.getDuration_minutes_wsh());
        }
        if (dto.getImages_wsh() != null) {
            existing.setImages_wsh(dto.getImages_wsh());
        }
        if (dto.getStatus_wsh() != null) {
            validateStatus(dto.getStatus_wsh());
            existing.setStatus_wsh(dto.getStatus_wsh());
        }
        serviceItemMapper.updateById(existing);
        if (dto.getMedia_wsh() != null) {
            serviceMediaService.replaceMedia(id, dto.getMedia_wsh());
        }
        return existing;
    }

    /**
     * 【删除服务项目】
     *
     * 业务作用：物理删除服务项目。
     * 调用场景：商家在后台删除服务项目时调用。
     * 调用链：ServiceItemController → delete @Transactional → ServiceItemMapper.deleteById
     * 数据处理：先查询存在性，再物理删除。
     * 状态影响：物理删除服务项目记录。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-item-page", "serviceItem"},
            allEntries = true)
    public void delete(Long id) {
        log.info("delete() 被调用");
        assertPositiveId(id);
        getById(id);
        serviceItemMapper.deleteById(id);
    }

    /**
     * 【切换服务项目启用/禁用】
     *
     * 业务作用：切换服务项目的上架/下架状态。
     * 调用场景：商家在后台启用或禁用服务项目时调用。
     * 调用链：ServiceItemController → toggleStatus @Transactional → ServiceItemMapper.updateById
     * 数据处理：ENABLED ↔ DISABLED 相互切换。
     * 状态影响：更新服务项目的 status 字段。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-item-page", "serviceItem"}, allEntries = true)
    public void toggleStatus(Long id) {
        log.info("toggleStatus() 被调用");
        assertPositiveId(id);
        ServiceItem item = getById(id);
        item.setStatus_wsh(item.getStatus_wsh() == null || item.getStatus_wsh() == StatusCode.SERVICE_DISABLED.getValue()
                ? StatusCode.SERVICE_ENABLED.getValue()
                : StatusCode.SERVICE_DISABLED.getValue());
        serviceItemMapper.updateById(item);
    }

    /**
     * 【服务项目实体转DTO】
     *
     * 业务作用：转换为前端展示 DTO（含分类名称）。
     * 调用场景：Controller 层返回服务项目信息前调用。
     * 调用链：各查询 Controller → toDTO → ServiceCategoryMapper.selectById
     * 数据处理：字段拷贝 + 查询分类名称。
     * 状态影响：只读操作。
     */
    @Override
    public ServiceItemDTO toDTO(ServiceItem entity) {
        if (entity == null) return null;
        ServiceItemDTO dto = copyFields(entity);
        if (entity.getCategory_id_wsh() != null) {
            ServiceCategory cat = categoryMapper.selectById(entity.getCategory_id_wsh());
            dto.setCategory_name_wsh(cat != null ? cat.getName_wsh() : null);
        }
        return dto;
    }

    /** 复制实体字段到DTO */
    private ServiceItemDTO copyFields(ServiceItem entity) {
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setCategory_id_wsh(entity.getCategory_id_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setPrice_wsh(entity.getPrice_wsh());
        dto.setUnit_wsh(entity.getUnit_wsh());
        dto.setDuration_minutes_wsh(entity.getDuration_minutes_wsh());
        dto.setBooking_mode_wsh(entity.getBooking_mode_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        return dto;
    }

    /**
     * 【公开查询服务项目列表】
     *
     * 业务作用：对外提供可预约服务的分页查询，支持按分类、商家、关键字筛选，
     * 支持按价格、评分、距离排序，支持基于用户经纬度计算距离。
     *
     * 调用场景：用户端服务列表页、商家详情页服务列表。
     *
     * 调用链：ServiceItemController → queryPublic → ServiceItemMapper.selectList
     *
     * 数据处理流程：
     * 1. 参数校验（分页、排序、坐标）
     * 2. 数据库查询（按条件筛选已启用服务）
     * 3. 批量加载关联数据（商家、分类、评分、媒体）
     * 4. 可见性过滤（只显示已审核商家 + 启用分类的服务）
     * 5. 内存排序（价格/评分/距离）
     * 6. 分页截取
     *
     * 状态影响：只读操作，结果缓存（key 由 ServiceItemQueryDTO.cacheKey_wsh 生成，仅含非空字段）。
     *
     * @param query 查询参数（分类、商家、关键字、排序、经纬度、分页）
     * @return 分页结果（服务列表 + 总数 + 分页信息）
     */
    @Override
    @Cacheable(value = "service-item-page",
            key = "#query == null ? 'all' : #query.cacheKey_wsh()",
            unless = "#result == null || #result.items_wsh == null || #result.items_wsh.isEmpty()")
    public ServiceQueryResultVO queryPublic(ServiceItemQueryDTO query) {
        // 参数空值处理：无参数时返回全部上架服务
        if (query == null) query = new ServiceItemQueryDTO();

        // ========== 分页参数校验 ==========
        int page = query.getPage_wsh();
        int size = query.getSize_wsh();
        if (page < 1) throw new BusinessException(400, "page must be >= 1");
        if (size < 1) throw new BusinessException(400, "size must be >= 1");
        if (size > MAX_SIZE) size = MAX_SIZE;  // 防止恶意请求过大分页

        // ========== 排序参数校验 ==========
        // 支持的排序方式：default（默认）、price_asc（价格升序）、rating_desc（评分降序）、distance_asc（距离升序）
        String sort = query.getSort_wsh() == null || query.getSort_wsh().isBlank() ? "default" : query.getSort_wsh();
        if (!SUPPORTED_SORTS.contains(sort)) {
            throw new BusinessException(400, BookingErrorCode.INVALID_SORT_PARAM, "unsupported sort: " + sort);
        }

        // ========== 经纬度参数校验 ==========
        // 用于计算用户与服务提供商家之间的距离（基于高德 GCJ02 坐标系）
        Double lat = toDouble(query.getLatitude_wsh());
        Double lng = toDouble(query.getLongitude_wsh());
        boolean hasCoords = lat != null || lng != null;
        // 经纬度必须成对提供，不能只提供其中一个
        if (hasCoords && (lat == null || lng == null)) {
            throw new BusinessException(400, BookingErrorCode.INVALID_SORT_PARAM, "latitude and longitude must be provided together");
        }
        // 坐标范围校验：纬度 [-90, 90]，经度 [-180, 180]
        if (hasCoords && (lat < -90 || lat > 90 || lng < -180 || lng > 180)) {
            throw new BusinessException(400, BookingErrorCode.INVALID_SORT_PARAM, "coordinate out of range");
        }
        // 距离排序必须提供经纬度参数
        if ("distance_asc".equals(sort) && !hasCoords) {
            throw new BusinessException(400, BookingErrorCode.INVALID_SORT_PARAM, "distance sort requires coordinates");
        }

        // ========== 构建数据库查询条件 ==========
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue());  // 只查询已上架服务
        // 分类筛选
        if (query.getCategory_id_wsh() != null) {
            wrapper.eq(ServiceItem::getCategory_id_wsh, query.getCategory_id_wsh());
        }
        // 商家筛选（商家维度列表用）
        if (query.getMerchant_id_wsh() != null) {
            wrapper.eq(ServiceItem::getMerchant_id_wsh, query.getMerchant_id_wsh());
        }
        // 关键字模糊匹配（服务名称或描述）
        if (query.getKeyword_wsh() != null && !query.getKeyword_wsh().isBlank()) {
            String keyword = query.getKeyword_wsh().trim();
            wrapper.and(w -> w.like(ServiceItem::getName_wsh, keyword)
                    .or().like(ServiceItem::getDescription_wsh, keyword));
        }
        // 数据库层排序：价格升序或创建时间倒序（默认）
        // 注意：评分排序和距离排序需要在内存中进行，因为涉及关联表计算
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(ServiceItem::getPrice_wsh);
        } else {
            wrapper.orderByDesc(ServiceItem::getCreated_at_wsh);  // 默认按创建时间倒序
        }
        // 执行数据库查询
        List<ServiceItem> services = serviceItemMapper.selectList(wrapper);
        if (services.isEmpty()) {
            return new ServiceQueryResultVO(new ArrayList<>(), 0, page, size);
        }

        // ========== 批量加载关联数据 ==========
        // 性能优化：一次性批量加载所有关联数据，避免 N+1 查询问题（SVC-U-08）
        Map<Long, Merchant> merchants = new HashMap<>(batchMerchants(services));      // 批量加载商家信息
        Map<Long, ServiceCategory> categories = new HashMap<>(batchCategories(services));  // 批量加载分类信息

        // ========== 可见性过滤 ==========
        // 只显示满足以下条件的服务：
        // 1. 服务已上架（已在 SQL 过滤）
        // 2. 商家已审核通过
        // 3. 分类已启用（如果服务有关联分类）
        List<ServiceItem> visible = services.stream()
                .filter(s -> isPubliclyVisible(s, merchants, categories))
                .toList();
        if (visible.isEmpty()) {
            return new ServiceQueryResultVO(new ArrayList<>(), 0, page, size);
        }

        // ========== 批量加载扩展数据 ==========
        Set<Long> serviceIds = visible.stream().map(ServiceItem::getId_wsh).collect(Collectors.toSet());
        // 图册批量解析：单次媒体查询 + 单次文件记录查询，列表只展示媒体行可信 URL
        Map<Long, List<ServiceMediaDTO>> mediaByService = serviceMediaService.listMediaByServiceIds(serviceIds);
        // 聚合服务评分统计
        Map<Long, RatingStats> serviceStats = aggregate(serviceIds, "service");
        // 聚合商家评分统计
        Set<Long> merchantIds = visible.stream().map(ServiceItem::getMerchant_id_wsh)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, RatingStats> merchantStats = aggregate(merchantIds, "merchant");

        // ========== 转换为 DTO ==========
        // 包含：服务基本信息 + 商家名称 + 分类名称 + 评分统计 + 距离计算（如果提供了用户坐标）
        List<ServiceItemDTO> dtos = visible.stream()
                .map(s -> toPublicDTO(s, merchants, categories, serviceStats, merchantStats, lat, lng,
                        mediaByService.get(s.getId_wsh())))
                .collect(Collectors.toList());

        // ========== 内存排序 ==========
        // 评分排序和距离排序需要在内存中进行，因为涉及关联表计算
        if ("price_asc".equals(sort)) {
            // 价格升序：空值视为 0
            dtos = dtos.stream()
                    .sorted(Comparator.comparing(d -> d.getPrice_wsh() == null
                            ? BigDecimal.ZERO : d.getPrice_wsh()))
                    .collect(Collectors.toList());
        } else if ("rating_desc".equals(sort)) {
            // 评分降序：高分在前，空值排在最后
            dtos = dtos.stream()
                    .sorted(Comparator.comparing(ServiceItemDTO::getService_rating_wsh,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } else if ("distance_asc".equals(sort)) {
            // 距离升序：近的在前，空值（无商家坐标或无用户坐标）排在最后
            dtos = dtos.stream()
                    .sorted(Comparator.comparing(ServiceItemDTO::getDistance_km_wsh,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }

        // ========== 分页截取 ==========
        int total = dtos.size();
        int from = (page - 1) * size;
        List<ServiceItemDTO> items = from >= total ? new ArrayList<>()
                : new ArrayList<>(dtos.subList(from, Math.min(from + size, total)));
        return new ServiceQueryResultVO(items, total, page, size);
    }

    /** 判断服务是否对外可见（商家已审核+分类已启用） */
    private boolean isPubliclyVisible(ServiceItem service, Map<Long, Merchant> merchants,
                                      Map<Long, ServiceCategory> categories) {
        Merchant merchant = merchants.get(service.getMerchant_id_wsh());
        if (merchant == null || merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            return false;
        }
        if (service.getCategory_id_wsh() != null) {
            ServiceCategory category = categories.get(service.getCategory_id_wsh());
            if (category == null || category.getStatus_wsh() == null
                    || category.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** 批量加载商家信息 */
    private Map<Long, Merchant> batchMerchants(List<ServiceItem> services) {
        Set<Long> merchantIds = services.stream().map(ServiceItem::getMerchant_id_wsh)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (merchantIds.isEmpty()) return Map.of();
        return merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId_wsh, Function.identity()));
    }

    /** 批量加载分类信息 */
    private Map<Long, ServiceCategory> batchCategories(List<ServiceItem> services) {
        Set<Long> categoryIds = services.stream().map(ServiceItem::getCategory_id_wsh)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return Map.of();
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ServiceCategory::getId_wsh, Function.identity()));
    }

    /** 聚合评分统计（平均分+评价数） */
    private Map<Long, RatingStats> aggregate(Set<Long> targetIds, String targetType) {
        if (targetIds.isEmpty()) return Map.of();
        Map<Long, RatingStats> stats = new HashMap<>();
        for (Map<String, Object> row : ratingMapper.aggregateByTargets(targetType, targetIds)) {
            Object targetId = row.get("targetId");
            if (targetId == null) continue;
            long count = toLong(row.get("cnt"));
            BigDecimal avg = row.get("avgScore") == null
                    ? null : BigDecimal.valueOf(((Number) row.get("avgScore")).doubleValue());
            stats.put(((Number) targetId).longValue(), new RatingStats(avg, count));
        }
        return stats;
    }

    /** 转换为公开DTO（含商家名称、分类名称、评分、距离、可信图册） */
    private ServiceItemDTO toPublicDTO(ServiceItem entity, Map<Long, Merchant> merchants,
                                       Map<Long, ServiceCategory> categories,
                                       Map<Long, RatingStats> serviceStats,
                                       Map<Long, RatingStats> merchantStats,
                                       Double lat, Double lng,
                                       List<ServiceMediaDTO> media) {
        ServiceItemDTO dto = copyFields(entity);
        // 公共输出只允许可信图册 URL（外部/data/协议相对地址一律不下发）
        dto.setImages_wsh(media == null || media.isEmpty() ? null
                : media.stream().map(ServiceMediaDTO::getUrl_wsh)
                        .filter(Objects::nonNull).collect(Collectors.joining(",")));
        Merchant merchant = merchants.get(entity.getMerchant_id_wsh());
        if (merchant != null) {
            dto.setMerchant_name_wsh(merchant.getName_wsh());
        }
        if (entity.getCategory_id_wsh() != null && categories.containsKey(entity.getCategory_id_wsh())) {
            dto.setCategory_name_wsh(categories.get(entity.getCategory_id_wsh()).getName_wsh());
        }
        RatingStats serviceStat = serviceStats.get(entity.getId_wsh());
        if (serviceStat != null) {
            dto.setService_rating_wsh(serviceStat.avgScore());
            dto.setService_rating_count_wsh(serviceStat.count());
        } else {
            dto.setService_rating_count_wsh(0L);
        }
        if (entity.getMerchant_id_wsh() != null) {
            RatingStats merchantStat = merchantStats.get(entity.getMerchant_id_wsh());
            if (merchantStat != null) {
                dto.setMerchant_rating_wsh(merchantStat.avgScore());
                dto.setMerchant_rating_count_wsh(merchantStat.count());
            } else {
                dto.setMerchant_rating_count_wsh(0L);
            }
        }
        if (lat != null && lng != null && merchant != null
                && merchant.getLatitude_wsh() != null && merchant.getLongitude_wsh() != null) {
            dto.setDistance_km_wsh(BigDecimal.valueOf(GeoDistanceUtils.distanceKm(lat, lng,
                    merchant.getLatitude_wsh().doubleValue(), merchant.getLongitude_wsh().doubleValue())).setScale(1, RoundingMode.HALF_UP));
        }
        dto.setService_version_wsh(ServiceVersions.format(entity.getUpdated_at_wsh()));
        return dto;
    }

    /** BigDecimal转Double */
    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /** 对象转long */
    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /** 获取公开服务详情（兼容旧客户端） */
    @Override
    public ServiceItemDTO getByIdPublic(Long id) {
        assertPositiveId(id);
        PublicContext ctx = requirePublicService(id);
        ServiceItemDTO dto = toDTO(ctx.service);
        dto.setImages_wsh(resolvePublicImages(ctx.service));
        return dto;
    }

    /** 获取服务产品公开详情（权威数据源） */
    @Override
    public ServiceProductDetailVO getPublicDetail(Long id) {
        assertPositiveId(id);
        PublicContext ctx = requirePublicService(id);
        ServiceItem service = ctx.service;
        Merchant merchant = ctx.merchant;
        ServiceCategory category = ctx.category;

        ServiceProductDetailVO vo = new ServiceProductDetailVO();
        vo.setId_wsh(service.getId_wsh());
        vo.setMerchant_id_wsh(service.getMerchant_id_wsh());
        vo.setMerchant_name_wsh(merchant != null ? merchant.getName_wsh() : null);
        vo.setName_wsh(service.getName_wsh());
        vo.setType_wsh(service.getType_wsh());
        vo.setCategory_id_wsh(service.getCategory_id_wsh());
        vo.setCategory_name_wsh(category != null ? category.getName_wsh() : null);
        vo.setDescription_wsh(service.getDescription_wsh());
        vo.setPrice_wsh(service.getPrice_wsh());
        vo.setUnit_wsh(service.getUnit_wsh());
        String detailUnit = BookingUnit.normalize(service.getUnit_wsh());
        if (detailUnit == null) {
            detailUnit = BookingUnit.DAY;
        }
        vo.setDuration_minutes_wsh(service.getDuration_minutes_wsh() != null
                ? service.getDuration_minutes_wsh()
                : BookingUnit.resolveDurationMinutes(detailUnit, null));
        vo.setBooking_mode_wsh(service.getBooking_mode_wsh() != null
                ? service.getBooking_mode_wsh()
                : BookingUnit.bookingMode(detailUnit));
        vo.setService_version_wsh(ServiceVersions.format(service.getUpdated_at_wsh()));
        vo.setMedia_wsh(resolvePublicMedia(service));

        RatingStats stats = aggregate(Set.of(service.getId_wsh()), "service").get(service.getId_wsh());
        if (stats != null) {
            vo.setService_rating_wsh(stats.avgScore());
            vo.setService_rating_count_wsh(stats.count());
        } else {
            vo.setService_rating_count_wsh(0L);
        }

        String reason = bookableReason(service, merchant);
        vo.setBookable_wsh(reason == null);
        vo.setBookable_reason_wsh(reason);
        return vo;
    }

    /** 获取服务管理详情（含全部字段+图册） */
    @Override
    public ServiceManageDetailVO getManageDetail(Long id) {
        assertPositiveId(id);
        ServiceManageDetailVO vo = new ServiceManageDetailVO();
        vo.setService_wsh(toDTO(getById(id)));
        vo.setMedia_wsh(serviceMediaService.listMedia(id));
        return vo;
    }

    /**
     * 公共可见性不变量：上架服务 + 已审核商家 + （存在分类时）启用分类。
     * 与列表过滤（isPubliclyVisible）语义一致，保证详情不会暴露列表隐藏的数据。
     */
    /** 校验服务公共可见性并返回上下文 */
    private PublicContext requirePublicService(Long id) {
        ServiceItem service = serviceItemMapper.selectById(id);
        if (service == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "服务不存在");
        }
        if (service.getStatus_wsh() == null
                || service.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_OFF_SHELF, "服务已下架");
        }
        Merchant merchant = merchantMapper.selectById(service.getMerchant_id_wsh());
        if (merchant == null || merchant.getStatus_wsh() == null
                || merchant.getStatus_wsh() != StatusCode.MERCHANT_APPROVED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.MERCHANT_NOT_APPROVED, "服务所属商家未通过审核");
        }
        ServiceCategory category = service.getCategory_id_wsh() == null
                ? null : categoryMapper.selectById(service.getCategory_id_wsh());
        if (service.getCategory_id_wsh() != null
                && (category == null || category.getStatus_wsh() == null
                || category.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue())) {
            throw new BusinessException(400, BookingErrorCode.SERVICE_OFF_SHELF, "服务所属分类已下架");
        }
        return new PublicContext(service, merchant, category);
    }

    /**
     * 详情图片：优先媒体行可信 URL；无媒体行时仅回退到能解析到内部
     * 文件记录的历史值（外部/data/协议相对地址丢弃，仅审计记录）。
     */
    /** 解析公开图片（优先媒体行，回退历史可信图片） */
    private String resolvePublicImages(ServiceItem service) {
        List<ServiceMediaDTO> media = serviceMediaService.listMedia(service.getId_wsh());
        if (!media.isEmpty()) {
            return media.stream().map(ServiceMediaDTO::getUrl_wsh)
                    .filter(Objects::nonNull).collect(Collectors.joining(","));
        }
        List<String> trusted = serviceMediaService.resolveTrustedLegacyImages(service.getImages_wsh());
        return trusted.isEmpty() ? null : String.join(",", trusted);
    }

    /** 解析公开媒体列表 */
    private List<ServiceProductMediaVO> resolvePublicMedia(ServiceItem service) {
        List<ServiceProductMediaVO> result = new ArrayList<>();
        for (ServiceMediaDTO m : serviceMediaService.listMedia(service.getId_wsh())) {
            ServiceProductMediaVO vo = new ServiceProductMediaVO();
            vo.setFile_id_wsh(m.getFile_id_wsh());
            vo.setSort_order_wsh(m.getSort_order_wsh());
            vo.setIs_cover_wsh(m.getIs_cover_wsh());
            vo.setUrl_wsh(m.getUrl_wsh());
            result.add(vo);
        }
        if (result.isEmpty()) {
            List<String> trusted = serviceMediaService.resolveTrustedLegacyImages(service.getImages_wsh());
            for (int i = 0; i < trusted.size(); i++) {
                ServiceProductMediaVO vo = new ServiceProductMediaVO();
                vo.setSort_order_wsh(i);
                vo.setIs_cover_wsh(i == 0 ? 1 : 0);
                vo.setUrl_wsh(trusted.get(i));
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 可预约性：商家开放未来预约且服务计费单位是受支持的规范单位（day/session/hour）。
     * 未知/无法解析的单位仍视为不可预约（UNSUPPORTED_SERVICE_UNIT）。
     */
    /** 判断服务是否可预约（返回不可预约原因或null） */
    private String bookableReason(ServiceItem service, Merchant merchant) {
        boolean futureBookingOn = merchant != null && merchant.getFuture_booking_enabled_wsh() != null
                && merchant.getFuture_booking_enabled_wsh() == 1;
        if (!futureBookingOn) {
            return BookingErrorCode.FUTURE_BOOKING_DISABLED;
        }
        if (service.getUnit_wsh() == null || BookingUnit.normalize(service.getUnit_wsh()) == null) {
            return BookingErrorCode.UNSUPPORTED_SERVICE_UNIT;
        }
        return null;
    }


    /** 校验ID是否为正数 */
    private void assertPositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, BookingErrorCode.INVALID_PRODUCT_ID, "服务ID必须为正数");
        }
    }

    /** 校验服务名称 */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "服务名称不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(400, "服务名称不能超过" + MAX_NAME_LENGTH + "个字符");
        }
    }

    /** 校验服务描述 */
    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(400, "服务描述不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
        }
    }

    /** 校验价格 */
    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException(400, BookingErrorCode.PRICE_INVALID, "价格不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, BookingErrorCode.PRICE_INVALID, "价格不能为负数");
        }
        if (price.scale() > 2) {
            throw new BusinessException(400, BookingErrorCode.PRICE_INVALID, "价格最多保留两位小数");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException(400, BookingErrorCode.PRICE_INVALID, "价格不能超过上限");
        }
    }

    /** 校验服务状态 */
    private void validateStatus(Integer status) {
        if (status == null
                || (status != StatusCode.SERVICE_ENABLED.getValue()
                && status != StatusCode.SERVICE_DISABLED.getValue())) {
            throw new BusinessException(400, BookingErrorCode.INVALID_STATUS, "服务状态仅允许启用或禁用");
        }
    }

    /** 获取已启用的分类（不存在或禁用时抛异常） */
    private ServiceCategory requireEnabledCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(400, BookingErrorCode.CATEGORY_NOT_FOUND, "服务分类不能为空");
        }
        ServiceCategory cat = categoryMapper.selectById(categoryId);

        if (cat == null) {
            throw new BusinessException(400, BookingErrorCode.CATEGORY_NOT_FOUND, "服务分类不存在");
        }
        if (cat.getStatus_wsh() == null
                || cat.getStatus_wsh() != StatusCode.SERVICE_ENABLED.getValue()) {
            throw new BusinessException(400, BookingErrorCode.CATEGORY_DISABLED, "服务分类已禁用");
        }
        return cat;
    }

    /** 应用单位契约（规范化单位+时长+预约模式） */
    private void applyUnitContract(ServiceItem item, String unit, Integer durationMinutes) {
        String canonical;
        if (unit == null || unit.isBlank()) {
            canonical = item.getUnit_wsh();
        } else {
            canonical = BookingUnit.normalize(unit);
            if (canonical == null) {
                throw new BusinessException(400, BookingErrorCode.UNSUPPORTED_SERVICE_UNIT,
                        "不支持的计费单位: " + unit.trim());
            }
        }
        int duration = BookingUnit.resolveDurationMinutes(canonical, durationMinutes);
        item.setUnit_wsh(canonical);
        item.setDuration_minutes_wsh(duration);
        item.setBooking_mode_wsh(BookingUnit.bookingMode(canonical));
    }

    /** 公共服务上下文（服务+商家+分类） */
    private record PublicContext(ServiceItem service, Merchant merchant, ServiceCategory category) {
    }

    /** 评分统计（平均分+评价数） */
    private record RatingStats(BigDecimal avgScore, long count) {
    }
}
