package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
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
        c.setCode_wsh("boarding");
        c.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return c;
    }

    private ServiceItemCreateRequestDTO createDto() {
        ServiceItemCreateRequestDTO dto = new ServiceItemCreateRequestDTO();
        dto.setName_wsh("标准寄养");
        dto.setCategory_id_wsh(1L);
        dto.setDescription_wsh("含每日遛狗");
        dto.setPrice_wsh(new BigDecimal("128.00"));
        dto.setUnit_wsh("天");
        return dto;
    }

    private ServiceItem existing() {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(301L);
        item.setMerchant_id_wsh(101L);
        item.setCategory_id_wsh(1L);
        item.setName_wsh("标准寄养");
        item.setPrice_wsh(new BigDecimal("128.00"));
        item.setUnit_wsh("day");
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return item;
    }

    // ============ 创建 ============

    /**
     * MG-01: 创建校验名称/描述/价格与启用分类，type 取自分类编码，
     * 单位 天→day 归一化，默认上架，媒体随事务写入。
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
        assertEquals("boarding", created.getType_wsh());
        assertEquals("day", created.getUnit_wsh());
        assertEquals(StatusCode.SERVICE_ENABLED.getValue(), created.getStatus_wsh());
        ArgumentCaptor<ServiceItem> captor = ArgumentCaptor.forClass(ServiceItem.class);
        verify(serviceItemMapper).insert(captor.capture());
        assertEquals("day", captor.getValue().getUnit_wsh());
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
        assertEquals("boarding", updated.getType_wsh());
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

    /**
     * MG-08: 更新目标不存在或 ID 非法时拒绝。
     */
    @Test
    void updateRejectsMissingTarget() {
        assertEquals(BookingErrorCode.INVALID_PRODUCT_ID, assertThrows(BusinessException.class,
                () -> service.update(0L, new ServiceItemUpdateRequestDTO())).getErrorCode());

        when(serviceItemMapper.selectByIdForUpdate(999L)).thenReturn(null);
        assertEquals(BookingErrorCode.SERVICE_NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.update(999L, new ServiceItemUpdateRequestDTO())).getErrorCode());
    }
}