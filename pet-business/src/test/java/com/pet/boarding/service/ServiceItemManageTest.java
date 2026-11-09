package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.dto.ServiceMediaItemDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.impl.ServiceItemServiceImpl;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U2-2: 服务聚合管理契约 —— 创建（名称/描述/价格/分类校验、单位归一化、
 * 默认上架、图册同事务写入）与更新（行锁、部分字段、分类同步 type、图册整体替换）。
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemManageTest {

    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private ServiceCategoryMapper categoryMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private com.pet.customer.mapper.RatingMapper ratingMapper;
    @Mock private ServiceMediaService serviceMediaService;

    private ServiceItemServiceImpl service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, ServiceItem.class);
    }

    @BeforeEach
    void setUp() {
        service = new ServiceItemServiceImpl(serviceItemMapper, categoryMapper, merchantMapper, ratingMapper,
                serviceMediaService);
    }

    private ServiceCategory enabledCat() {
        ServiceCategory c = new ServiceCategory();
        c.setId_wsh(1L);
        c.setName_wsh("寄养");
        c.setCode_wsh("GROOMING");
        c.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return c;
    }

    private ServiceItemCreateRequestDTO createDto() {
        ServiceItemCreateRequestDTO dto = new ServiceItemCreateRequestDTO();
        dto.setName_wsh("标准寄养");
        dto.setCategory_id_wsh(1L);
        dto.setDescription_wsh("含每日遛狗");
        dto.setPrice_wsh(new BigDecimal("128.00"));
        dto.setUnit_wsh("夜");
        return dto;
    }

    private ServiceItem existing() {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(301L);
        item.setMerchant_id_wsh(101L);
        item.setCategory_id_wsh(1L);
        item.setName_wsh("标准寄养");
        item.setPrice_wsh(new BigDecimal("128.00"));
        item.setUnit_wsh("night");
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return item;
    }

    // ============ 创建 ============

    /**
     * MG-01: 创建校验名称/描述/价格与启用分类，type 取自分类编码，
     * 单位 夜→night 归一化，默认上架，媒体随事务写入。
     */
    @Test
    void createValidatesAndWritesAggregate() {
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);
        ServiceItemCreateRequestDTO dto = createDto();
        ServiceMediaItemDTO mediaItem = new ServiceMediaItemDTO();
        mediaItem.setFile_id_wsh(11L);
        mediaItem.setSort_order_wsh(0);
        mediaItem.setIs_cover_wsh(1);
        dto.setMedia_wsh(List.of(mediaItem));

        ServiceItem created = service.create(99L, dto);

        assertEquals(99L, created.getMerchant_id_wsh());
        assertEquals("标准寄养", created.getName_wsh());
        assertEquals(1L, created.getCategory_id_wsh());
        assertEquals("GROOMING", created.getType_wsh());
        assertEquals("night", created.getUnit_wsh());
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), created.getStatus_wsh());
        ArgumentCaptor<ServiceItem> captor = ArgumentCaptor.forClass(ServiceItem.class);
        verify(serviceItemMapper).insert(captor.capture());
        assertEquals("night", captor.getValue().getUnit_wsh());
        verify(serviceMediaService).replaceMedia(captor.getValue().getId_wsh(), List.of(mediaItem));
    }

    /**
     * MG-02: 创建时无媒体则不触碰图册替换。
     */
    @Test
    void createWithoutMediaSkipsReplace() {
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);

        service.create(99L, createDto());

        verify(serviceMediaService, never()).replaceMedia(any(), any());
    }

    /**
     * MG-03: 创建校验价格（负数/超精度/超上限/空）。
     */
    @Test
    void createRejectsInvalidPrice() {
        ServiceItemCreateRequestDTO dto = createDto();
        dto.setPrice_wsh(new BigDecimal("-1.00"));
        assertEquals(BookingErrorCode.PRICE_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());

        dto.setPrice_wsh(new BigDecimal("1.234"));
        assertEquals(BookingErrorCode.PRICE_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());

        dto.setPrice_wsh(new BigDecimal("1000000.01"));
        assertEquals(BookingErrorCode.PRICE_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());

        dto.setPrice_wsh(null);
        assertEquals(BookingErrorCode.PRICE_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());
    }

    /**
     * MG-04: 创建校验名称与分类（缺分类/不存在/已禁用）。
     */
    @Test
    void createRejectsBlankNameAndUnavailableCategory() {
        ServiceItemCreateRequestDTO dto = createDto();
        dto.setName_wsh("  ");
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getCode());

        dto.setName_wsh("标准寄养");
        dto.setCategory_id_wsh(null);
        assertEquals(BookingErrorCode.CATEGORY_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());

        dto.setCategory_id_wsh(404L);
        when(categoryMapper.selectById(404L)).thenReturn(null);
        assertEquals(BookingErrorCode.CATEGORY_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());

        dto.setCategory_id_wsh(2L);
        ServiceCategory disabled = enabledCat();
        disabled.setId_wsh(2L);
        disabled.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(categoryMapper.selectById(2L)).thenReturn(disabled);
        assertEquals(BookingErrorCode.CATEGORY_DISABLED, assertThrows(BusinessException.class,
                () -> service.create(99L, dto)).getErrorCode());
    }

    // ============ 更新 ============

    /**
     * MG-05: 更新走行锁读取；仅非 null 字段生效；分类变化同步 type 编码。
     */
    @Test
    void updateAppliesNonNullFieldsUnderRowLock() {
        when(serviceItemMapper.selectByIdForUpdate(301L)).thenReturn(existing());
        when(serviceItemMapper.updateById(any(ServiceItem.class))).thenReturn(1);
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());

        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setName_wsh("豪华寄养");
        dto.setCategory_id_wsh(1L);
        dto.setPrice_wsh(new BigDecimal("188.00"));

        ServiceItem updated = service.update(301L, dto);

        assertEquals("豪华寄养", updated.getName_wsh());
        assertEquals(new BigDecimal("188.00"), updated.getPrice_wsh());
        assertEquals("GROOMING", updated.getType_wsh());
        ArgumentCaptor<ServiceItem> captor = ArgumentCaptor.forClass(ServiceItem.class);
        verify(serviceItemMapper).updateById(captor.capture());
        assertEquals("豪华寄养", captor.getValue().getName_wsh());
        assertNull(captor.getValue().getDescription_wsh());
        assertNull(captor.getValue().getImages_wsh());
    }

    /**
     * MG-06: 更新校验状态/价格/分类；只允许启用或禁用。
     */
    @Test
    void updateRejectsInvalidStatusPriceAndCategory() {
        when(serviceItemMapper.selectByIdForUpdate(301L)).thenReturn(existing());

        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setStatus_wsh(9);
        assertEquals(BookingErrorCode.INVALID_STATUS, assertThrows(BusinessException.class,
                () -> service.update(301L, dto)).getErrorCode());

        dto.setStatus_wsh(null);
        dto.setPrice_wsh(new BigDecimal("-5.00"));
        assertEquals(BookingErrorCode.PRICE_INVALID, assertThrows(BusinessException.class,
                () -> service.update(301L, dto)).getErrorCode());

        dto.setPrice_wsh(null);
        dto.setCategory_id_wsh(2L);
        ServiceCategory disabled = enabledCat();
        disabled.setId_wsh(2L);
        disabled.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(categoryMapper.selectById(2L)).thenReturn(disabled);
        assertEquals(BookingErrorCode.CATEGORY_DISABLED, assertThrows(BusinessException.class,
                () -> service.update(301L, dto)).getErrorCode());
    }

    /**
     * MG-07: 更新时 media_wsh 非 null（含空集合=清空）才触发整体替换；
     * 为 null 时不得触碰图册。
     */
    @Test
    void updateReplacesMediaOnlyWhenListPresent() {
        when(serviceItemMapper.selectByIdForUpdate(301L)).thenReturn(existing());
        when(serviceItemMapper.updateById(any(ServiceItem.class))).thenReturn(1);

        ServiceItemUpdateRequestDTO noMedia = new ServiceItemUpdateRequestDTO();
        noMedia.setName_wsh("改名");
        service.update(301L, noMedia);
        verify(serviceMediaService, never()).replaceMedia(any(), any());

        ServiceItemUpdateRequestDTO clearMedia = new ServiceItemUpdateRequestDTO();
        clearMedia.setMedia_wsh(List.of());
        service.update(301L, clearMedia);
        verify(serviceMediaService).replaceMedia(301L, List.of());
    }

    // ============ 多单位契约（session/hour） ============

    /**
     * MG-09: 创建 session 单位回填默认时长 60 并推导 slot 模式；
     * 显式 120 分钟生效。
     */
    @Test
    void createSessionUnitWritesDefaultDurationAndSlotMode() {
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);
        ServiceItemCreateRequestDTO dto = createDto();
        dto.setUnit_wsh("次");
        dto.setDuration_minutes_wsh(null);

        ServiceItem created = service.create(99L, dto);

        assertEquals("session", created.getUnit_wsh());
        assertEquals(60, created.getDuration_minutes_wsh());
        assertEquals("slot", created.getBooking_mode_wsh());
    }

    /**
     * MG-10: 创建 hour 单位的时长必须是 30 分钟粒度步长的整数倍
     * （30/90/120 通过，15/10 拒绝），低于 15 / 超过 1440 拒绝；booking_mode 为 slot。
     */
    @Test
    void createHourUnitEnforcesGranularityStep() {
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);

        ServiceItemCreateRequestDTO ok = createDto();
        ok.setUnit_wsh("hours");
        ok.setDuration_minutes_wsh(120);
        ServiceItem created = service.create(99L, ok);
        assertEquals("hour", created.getUnit_wsh());
        assertEquals(120, created.getDuration_minutes_wsh());
        assertEquals("slot", created.getBooking_mode_wsh());

        // 30 分钟粒度合法：一个计费单位就是半小时
        ServiceItemCreateRequestDTO halfHour = createDto();
        halfHour.setUnit_wsh("hour");
        halfHour.setDuration_minutes_wsh(30);
        assertEquals(30, service.create(99L, halfHour).getDuration_minutes_wsh());

        // 90 分钟 = 3 个半小时单位，同样合法
        ServiceItemCreateRequestDTO ninety = createDto();
        ninety.setUnit_wsh("hour");
        ninety.setDuration_minutes_wsh(90);
        assertEquals(90, service.create(99L, ninety).getDuration_minutes_wsh());

        // 非 30 的整数倍拒绝
        ServiceItemCreateRequestDTO badStep = createDto();
        badStep.setUnit_wsh("hour");
        badStep.setDuration_minutes_wsh(45);
        assertEquals(BookingErrorCode.DURATION_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, badStep)).getErrorCode());

        ServiceItemCreateRequestDTO tooShort = createDto();
        tooShort.setUnit_wsh("小时");
        tooShort.setDuration_minutes_wsh(10);
        assertEquals(BookingErrorCode.DURATION_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, tooShort)).getErrorCode());

        ServiceItemCreateRequestDTO tooLong = createDto();
        tooLong.setUnit_wsh("hour");
        tooLong.setDuration_minutes_wsh(1500);
        assertEquals(BookingErrorCode.DURATION_INVALID, assertThrows(BusinessException.class,
                () -> service.create(99L, tooLong)).getErrorCode());
    }

    /**
     * MG-11: 未知/超长单位创建拒绝，返回 UNSUPPORTED_SERVICE_UNIT。
     */
    @Test
    void createRejectsUnknownAndOversizedUnit() {
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());
        ServiceItemCreateRequestDTO unknown = createDto();
        unknown.setUnit_wsh("按疗程");
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, assertThrows(BusinessException.class,
                () -> service.create(99L, unknown)).getErrorCode());

        ServiceItemCreateRequestDTO oversized = createDto();
        oversized.setUnit_wsh("a".repeat(40));
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, assertThrows(BusinessException.class,
                () -> service.create(99L, oversized)).getErrorCode());
    }

    /**
     * MG-12: 更新单位或时长时重新落地契约；只传时长时沿用现有单位推导。
     */
    @Test
    void updateAppliesUnitContractWhenUnitOrDurationProvided() {
        ServiceItem base = existing();
        base.setUnit_wsh("night");
        when(serviceItemMapper.selectByIdForUpdate(301L)).thenReturn(base);
        when(serviceItemMapper.updateById(any(ServiceItem.class))).thenReturn(1);

        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setUnit_wsh("小时");
        dto.setDuration_minutes_wsh(120);
        ServiceItem updated = service.update(301L, dto);
        assertEquals("hour", updated.getUnit_wsh());
        assertEquals(120, updated.getDuration_minutes_wsh());
        assertEquals("slot", updated.getBooking_mode_wsh());

        ArgumentCaptor<ServiceItem> captor = ArgumentCaptor.forClass(ServiceItem.class);
        verify(serviceItemMapper).updateById(captor.capture());
        assertEquals("hour", captor.getValue().getUnit_wsh());
        assertEquals(120, captor.getValue().getDuration_minutes_wsh());
    }

    /**
     * MG-13: 更新传入非法单位拒绝；按夜服务显式传入非 1440 时长拒绝。
     */
    @Test
    void updateRejectsInvalidUnitAndDayDuration() {
        ServiceItem base = existing();
        base.setUnit_wsh("night");
        when(serviceItemMapper.selectByIdForUpdate(301L)).thenReturn(base);

        ServiceItemUpdateRequestDTO badUnit = new ServiceItemUpdateRequestDTO();
        badUnit.setUnit_wsh("疗程");
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, assertThrows(BusinessException.class,
                () -> service.update(301L, badUnit)).getErrorCode());

        ServiceItemUpdateRequestDTO badNight = new ServiceItemUpdateRequestDTO();
        badNight.setUnit_wsh("night");
        badNight.setDuration_minutes_wsh(720);
        assertEquals(BookingErrorCode.DURATION_INVALID, assertThrows(BusinessException.class,
                () -> service.update(301L, badNight)).getErrorCode());
    }

    /**
     * MG-14: 详情/管理列表回显规范单位、模式、时长与单价（不得显示“暂不支持预约”）。
     */
    @Test
    void detailAndListEchoCanonicalUnitFields() {
        ServiceItem entity = existing();
        entity.setUnit_wsh("session");
        entity.setDuration_minutes_wsh(60);
        entity.setBooking_mode_wsh("slot");
        when(categoryMapper.selectById(1L)).thenReturn(enabledCat());

        ServiceItemDTO dto = service.toDTO(entity);
        assertEquals("session", dto.getUnit_wsh());
        assertEquals(60, dto.getDuration_minutes_wsh());
        assertEquals("slot", dto.getBooking_mode_wsh());
        assertEquals(new BigDecimal("128.00"), dto.getPrice_wsh());
    }
    // ============ 寄养类服务的计价方式约束 ============

    /** 根分类 BOARDING 的叶子分类（非日间寄养） */
    private ServiceCategory boardingLeaf(String code) {
        ServiceCategory leaf = new ServiceCategory();
        leaf.setId_wsh(11L);
        leaf.setParent_id_wsh(1L);
        leaf.setName_wsh("标准寄养");
        leaf.setCode_wsh(code);
        leaf.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return leaf;
    }

    private ServiceCategory boardingRoot() {
        ServiceCategory root = new ServiceCategory();
        root.setId_wsh(1L);
        root.setName_wsh("寄养服务");
        root.setCode_wsh("BOARDING");
        root.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return root;
    }

    @Test
    void createRejectsLegacyUnitsForBoardingCategory() {
        when(categoryMapper.selectById(11L)).thenReturn(boardingLeaf("BOARDING_STANDARD"));
        // day/boarding 已下线：单位归一阶段就直接拒绝，不再进入分类匹配
        for (String unit : new String[]{"day", "boarding"}) {
            ServiceItemCreateRequestDTO dto = createDto();
            dto.setCategory_id_wsh(11L);
            dto.setUnit_wsh(unit);

            BusinessException error = assertThrows(BusinessException.class, () -> service.create(99L, dto));

            assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, error.getErrorCode());
        }
        verify(serviceItemMapper, never()).insert(any(ServiceItem.class));
    }

    @Test
    void createAllowsNightUnitForBoardingCategory() {
        when(categoryMapper.selectById(11L)).thenReturn(boardingLeaf("BOARDING_STANDARD"));
        when(categoryMapper.selectById(1L)).thenReturn(boardingRoot());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);
        ServiceItemCreateRequestDTO dto = createDto();
        dto.setCategory_id_wsh(11L);
        dto.setUnit_wsh("night");

        ServiceItem created = service.create(99L, dto);

        assertEquals("night", created.getUnit_wsh());
        // 一夜 24 小时只是单位元数据，不要求实际住满
        assertEquals(1440, created.getDuration_minutes_wsh());
    }

    @Test
    void createAllowsSessionUnitForDaycareCategory() {
        // 日间寄养：宠物夜间回家，按一次日托计费，时长沿用商家配置
        when(categoryMapper.selectById(13L)).thenReturn(boardingLeaf("BOARDING_DAYCARE"));
        when(categoryMapper.selectById(1L)).thenReturn(boardingRoot());
        when(serviceItemMapper.insert(any(ServiceItem.class))).thenReturn(1);
        ServiceItemCreateRequestDTO dto = createDto();
        dto.setCategory_id_wsh(13L);
        dto.setUnit_wsh("session");
        dto.setDuration_minutes_wsh(480);

        ServiceItem created = service.create(99L, dto);

        assertEquals("session", created.getUnit_wsh());
        // 不能默认改成 60 分钟
        assertEquals(480, created.getDuration_minutes_wsh());
    }
}
