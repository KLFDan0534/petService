package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阶段 F / P0：服务重新上架的计价单位校验契约。
 *
 * 覆盖授权书 §9.2 的 12 项要求：
 * 合法单位可上架；day/boarding/time/NULL/空串不能上架；非法单位仍可下架；
 * toggleStatus 与 update 的 status 分支均不能绕过校验；
 * night→interval、session/hour→slot；ID 29 / ID 36 不能绕过校验上架。
 *
 * 全部通过 ServiceItemServiceImpl 的公开方法驱动，复用 BookingUnit，
 * 不新建第二套单位常量，不触碰数据库。
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemStatusUnitTest {

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

    /** 构造一条待上架（下架状态）的服务；unit/mode/duration 由用例指定。 */
    private ServiceItem offShelf(long id, String unit, String mode, Integer duration) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(101L);
        item.setName_wsh("测试服务");
        item.setPrice_wsh(new BigDecimal("100.00"));
        item.setUnit_wsh(unit);
        item.setBooking_mode_wsh(mode);
        item.setDuration_minutes_wsh(duration);
        item.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        return item;
    }

    private void stubGetById(ServiceItem item) {
        when(serviceItemMapper.selectById(item.getId_wsh())).thenReturn(item);
    }

    private void stubSelectForUpdate(ServiceItem item) {
        when(serviceItemMapper.selectByIdForUpdate(item.getId_wsh())).thenReturn(item);
    }

    private static BusinessException assertUnsupported(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, ex.getErrorCode());
        return ex;
    }

    // ============ 1. 合法单位可以上架 ============

    @Test
    void legalNightUnitCanBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(1L, "night", "interval", 1440);
        stubGetById(item);
        assertDoesNotThrow(() -> service.toggleStatus(1L));
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper).updateById(any(ServiceItem.class));
    }

    @Test
    void legalSessionUnitCanBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(3L, "session", "slot", 30);
        stubGetById(item);
        assertDoesNotThrow(() -> service.toggleStatus(3L));
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), item.getStatus_wsh());
    }

    @Test
    void legalSession540MinutesCanBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(9L, "session", "slot", 540);
        stubGetById(item);
        assertDoesNotThrow(() -> service.toggleStatus(9L));
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), item.getStatus_wsh());
    }

    @Test
    void legalHourUnitCanBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(4L, "hour", "slot", 60);
        stubGetById(item);
        assertDoesNotThrow(() -> service.toggleStatus(4L));
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), item.getStatus_wsh());
    }

    // ============ 2~5. 非法单位不能上架 ============

    @Test
    void dayUnitCannotBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(29L, "day", "date_range", 1440);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(29L));
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void boardingUnitCannotBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(1L, "boarding", "interval", 1440);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(1L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void timeUnitCannotBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(3L, "time", "time_slot", 30);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(3L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void nullUnitCannotBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(5L, null, "slot", 60);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(5L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void blankUnitCannotBePublishedViaToggleStatus() {
        ServiceItem item = offShelf(6L, "   ", "slot", 60);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(6L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    // ============ 6. 非法单位仍然可以正常下架 ============

    @Test
    void illegalUnitCanStillBeUnpublished() {
        ServiceItem item = offShelf(36L, "day", "date_range", 480);
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());  // 当前上架
        stubGetById(item);
        assertDoesNotThrow(() -> service.toggleStatus(36L));       // 切为下架，不应受阻
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper).updateById(any(ServiceItem.class));
    }

    // ============ 7~8. 两个入口都不能绕过校验 ============

    @Test
    void toggleStatusCannotBypassValidation() {
        // 与 dayUnitCannotBePublished 呼应：显式确认 toggleStatus 这一入口本身被拦
        ServiceItem item = offShelf(7L, "boarding", "interval", 1440);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(7L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void updateStatusBranchCannotBypassValidation() {
        ServiceItem item = offShelf(29L, "day", "date_range", 1440);
        stubSelectForUpdate(item);
        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        assertUnsupported(() -> service.update(29L, dto));
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void updateStatusBranchAcceptsLegalUnit() {
        ServiceItem item = offShelf(1L, "night", "interval", 1440);
        stubSelectForUpdate(item);
        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        assertDoesNotThrow(() -> service.update(1L, dto));
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper).updateById(any(ServiceItem.class));
    }

    // ============ 9~10. 单位与预约模式必须匹配 ============

    @Test
    void nightMustMatchIntervalMode() {
        ServiceItem item = offShelf(8L, "night", "slot", 1440);
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(8L));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void sessionAndHourMustMatchSlotMode() {
        ServiceItem sessionWrongMode = offShelf(9L, "session", "interval", 540);
        stubGetById(sessionWrongMode);
        assertUnsupported(() -> service.toggleStatus(9L));

        ServiceItem hourWrongMode = offShelf(10L, "hour", "interval", 60);
        stubGetById(hourWrongMode);
        assertUnsupported(() -> service.toggleStatus(10L));
    }

    // ============ 11~12. ID 29 / ID 36 不能绕过校验上架 ============

    @Test
    void id29CannotBePublishedViaUpdateStatusBranch() {
        // ID 29 术后护理：day / 1440 / date_range / 120.00，冻结中
        ServiceItem item = offShelf(29L, "day", "date_range", 1440);
        item.setPrice_wsh(new BigDecimal("120.00"));
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());  // 真实库中当前为上架，但不得通过校验
        stubSelectForUpdate(item);
        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        assertUnsupported(() -> service.update(29L, dto));
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    @Test
    void id36CannotBePublishedViaToggleStatus() {
        // ID 36 临时暂停服务：day / 480 / date_range / 150.00 / status=0，必须保持下架
        ServiceItem item = offShelf(36L, "day", "date_range", 480);
        item.setPrice_wsh(new BigDecimal("150.00"));
        stubGetById(item);
        assertUnsupported(() -> service.toggleStatus(36L));
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), item.getStatus_wsh());
        verify(serviceItemMapper, never()).updateById(any(ServiceItem.class));
    }

    // ============ 补充：时长校验与「不偷偷上架」 ============

    @Test
    void durationIsValidatedOnPublish() {
        ServiceItem tooShort = offShelf(11L, "session", "slot", 10);
        stubGetById(tooShort);
        assertThrows(BusinessException.class, () -> service.toggleStatus(11L));

        ServiceItem nightWrongDuration = offShelf(12L, "night", "interval", 60);
        stubGetById(nightWrongDuration);
        assertThrows(BusinessException.class, () -> service.toggleStatus(12L));
    }

    @Test
    void updateWithoutStatusDoesNotPublishIllegalUnitService() {
        ServiceItem item = offShelf(36L, "day", "date_range", 480);
        stubSelectForUpdate(item);
        ServiceItemUpdateRequestDTO dto = new ServiceItemUpdateRequestDTO();
        dto.setPrice_wsh(new BigDecimal("150.00"));  // 只改价格，不改状态
        assertDoesNotThrow(() -> service.update(36L, dto));
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), item.getStatus_wsh());
        ArgumentCaptor<ServiceItem> captor = ArgumentCaptor.forClass(ServiceItem.class);
        verify(serviceItemMapper).updateById(captor.capture());
        assertEquals(StatusCode.SERVICE_DISABLED.getValue(), captor.getValue().getStatus_wsh());
    }
}
