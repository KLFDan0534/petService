package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceMediaDTO;
import com.pet.boarding.dto.ServiceProductDetailVO;
import com.pet.boarding.dto.ServiceProductMediaVO;
import com.pet.boarding.entity.Merchant;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U2-4: 服务公开详情契约 —— 白名单投影、可信图册、评分聚合单次查询、
 * 可预约性标记（未来预约 + day/天 单位）、上架/商家/分类三层可见性校验。
 */
@ExtendWith(MockitoExtension.class)
class ServiceProductDetailTest {

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

    private ServiceItem enabled(Long id) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(101L);
        item.setCategory_id_wsh(1L);
        item.setName_wsh("标准寄养");
        item.setType_wsh("boarding");
        item.setDescription_wsh("含每日遛狗");
        item.setPrice_wsh(new BigDecimal("128.00"));
        item.setUnit_wsh("day");
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        item.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        return item;
    }

    private Merchant approved() {
        Merchant m = new Merchant();
        m.setId_wsh(101L);
        m.setName_wsh("爱宠之家");
        m.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        m.setFuture_booking_enabled_wsh(1);
        return m;
    }

    private ServiceCategory enabledCategory() {
        ServiceCategory c = new ServiceCategory();
        c.setId_wsh(1L);
        c.setName_wsh("寄养");
        c.setCode_wsh("boarding");
        c.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return c;
    }

    private ServiceMediaDTO media(Long fileId, int sort, String url) {
        ServiceMediaDTO m = new ServiceMediaDTO();
        m.setFile_id_wsh(fileId);
        m.setSort_order_wsh(sort);
        m.setUrl_wsh(url);
        return m;
    }

    private Map<String, Object> agg(Long targetId, BigDecimal avg, long cnt) {
        return Map.of("targetId", targetId, "avgScore", avg, "cnt", cnt);
    }

    private void stubVisibleContext() {
        when(serviceItemMapper.selectById(301L)).thenReturn(enabled(301L));
        when(merchantMapper.selectById(101L)).thenReturn(approved());
        when(categoryMapper.selectById(1L)).thenReturn(enabledCategory());
    }

    /**
     * DU-01: 详情仅暴露白名单字段；图册来自可信媒体行；评分聚合只查询 service 一类。
     */
    @Test
    void detailExposesWhitelistedFieldsWithTrustedMediaAndSingleAggregateQuery() {
        stubVisibleContext();
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of(
                media(11L, 0, "https://cdn/cover.jpg"),
                media(12L, 1, "https://cdn/photo2.jpg")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection()))
                .thenReturn(List.of(agg(301L, new BigDecimal("4.8"), 7L)));

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertEquals(301L, vo.getId_wsh());
        assertEquals(101L, vo.getMerchant_id_wsh());
        assertEquals("爱宠之家", vo.getMerchant_name_wsh());
        assertEquals("标准寄养", vo.getName_wsh());
        assertEquals("boarding", vo.getType_wsh());
        assertEquals(1L, vo.getCategory_id_wsh());
        assertEquals("寄养", vo.getCategory_name_wsh());
        assertEquals("含每日遛狗", vo.getDescription_wsh());
        assertEquals(new BigDecimal("128.00"), vo.getPrice_wsh());
        assertEquals("day", vo.getUnit_wsh());
        assertEquals("2026-08-11T13:45:20", vo.getService_version_wsh());
        assertEquals(new BigDecimal("4.8"), vo.getService_rating_wsh());
        assertEquals(7L, vo.getService_rating_count_wsh());
        assertTrue(vo.getBookable_wsh());
        assertNull(vo.getBookable_reason_wsh());
        assertEquals(2, vo.getMedia_wsh().size());
        assertEquals("https://cdn/cover.jpg", vo.getMedia_wsh().get(0).getUrl_wsh());
        assertEquals(0, vo.getMedia_wsh().get(0).getSort_order_wsh());

        verify(ratingMapper).aggregateByTargets(eq("service"), anyCollection());
        verify(ratingMapper, never()).aggregateByTargets(eq("merchant"), anyCollection());
    }

    /**
     * DU-02: 无评价时评分为 null、评价数为 0。
     */
    @Test
    void detailWithoutRatingsShowsZeroCountAndNullRating() {
        stubVisibleContext();
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null)).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertNull(vo.getService_rating_wsh());
        assertEquals(0L, vo.getService_rating_count_wsh());
    }

    /**
     * DU-03: 下架服务详情不可见（服务自身已下架）。
     */
    @Test
    void detailOfOffShelfServiceRejected() {
        ServiceItem offShelf = enabled(301L);
        offShelf.setStatus_wsh(StatusCode.SERVICE_DISABLED != null
                ? StatusCode.SERVICE_DISABLED.getValue() : 0);
        when(serviceItemMapper.selectById(301L)).thenReturn(offShelf);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPublicDetail(301L));

        assertEquals(400, ex.getCode());
        assertEquals(BookingErrorCode.SERVICE_OFF_SHELF, ex.getErrorCode());
    }

    /**
     * DU-04: 商家未通过审核的详情不可见。
     */
    @Test
    void detailOfUnapprovedMerchantRejected() {
        when(serviceItemMapper.selectById(301L)).thenReturn(enabled(301L));
        Merchant pending = approved();
        pending.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        when(merchantMapper.selectById(101L)).thenReturn(pending);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPublicDetail(301L));

        assertEquals(BookingErrorCode.MERCHANT_NOT_APPROVED, ex.getErrorCode());
    }

    /**
     * DU-05: 分类被下架视为服务不可见。
     */
    @Test
    void detailWithDisabledCategoryRejected() {
        when(serviceItemMapper.selectById(301L)).thenReturn(enabled(301L));
        when(merchantMapper.selectById(101L)).thenReturn(approved());
        ServiceCategory offShelf = enabledCategory();
        offShelf.setStatus_wsh(StatusCode.SERVICE_DISABLED != null
                ? StatusCode.SERVICE_DISABLED.getValue() : 0);
        when(categoryMapper.selectById(1L)).thenReturn(offShelf);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPublicDetail(301L));

        assertEquals(BookingErrorCode.SERVICE_OFF_SHELF, ex.getErrorCode());
    }

    /**
     * DU-06: 商家未开启未来预约时详情可见但标记不可预约。
     */
    @Test
    void detailNotBookableWhenFutureBookingDisabled() {
        stubVisibleContext();
        Merchant closed = approved();
        closed.setFuture_booking_enabled_wsh(0);
        when(merchantMapper.selectById(101L)).thenReturn(closed);
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null)).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertFalse(vo.getBookable_wsh());
        assertEquals(BookingErrorCode.FUTURE_BOOKING_DISABLED, vo.getBookable_reason_wsh());
    }

    /**
     * DU-07: session/hour 计费单位可预约（A2 多单位支持，不得显示“暂不支持预约”）。
     */
    @Test
    void detailBookableForHourUnit() {
        stubVisibleContext();
        ServiceItem hourly = enabled(301L);
        hourly.setUnit_wsh("hour");
        hourly.setDuration_minutes_wsh(120);
        when(serviceItemMapper.selectById(301L)).thenReturn(hourly);
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null)).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertTrue(vo.getBookable_wsh());
        assertNull(vo.getBookable_reason_wsh());
        assertEquals("hour", vo.getUnit_wsh());
        assertEquals(120, vo.getDuration_minutes_wsh());
        assertEquals("slot", vo.getBooking_mode_wsh());
    }

    /**
     * DU-07b: 真正未知/非受支持单位仍不可预约，返回 UNSUPPORTED_SERVICE_UNIT。
     */
    @Test
    void detailNotBookableForUnknownUnit() {
        stubVisibleContext();
        ServiceItem odd = enabled(301L);
        odd.setUnit_wsh("疗程");
        when(serviceItemMapper.selectById(301L)).thenReturn(odd);
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null)).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertFalse(vo.getBookable_wsh());
        assertEquals(BookingErrorCode.UNSUPPORTED_SERVICE_UNIT, vo.getBookable_reason_wsh());
    }

    /**
     * DU-08: 无媒体行时回退到可信历史图（外部/协议相对地址被过滤），首图标记为封面。
     */
    @Test
    void detailFallsBackToTrustedLegacyImagesWhenNoMediaRows() {
        stubVisibleContext();
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null))
                .thenReturn(List.of("https://cdn/legacy1.jpg", "https://cdn/legacy2.jpg"));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        ServiceProductDetailVO vo = service.getPublicDetail(301L);

        assertEquals(2, vo.getMedia_wsh().size());
        ServiceProductMediaVO first = vo.getMedia_wsh().get(0);
        assertEquals("https://cdn/legacy1.jpg", first.getUrl_wsh());
        assertEquals(0, first.getSort_order_wsh());
        assertEquals(1, first.getIs_cover_wsh());
        assertEquals(0, vo.getMedia_wsh().get(1).getIs_cover_wsh());
    }

    /**
     * DU-09: 非法 ID 与不存在与均被拒绝。
     */
    @Test
    void detailRejectsInvalidAndMissingIds() {
        BusinessException badId = assertThrows(BusinessException.class, () -> service.getPublicDetail(0L));
        assertEquals(BookingErrorCode.INVALID_PRODUCT_ID, badId.getErrorCode());

        when(serviceItemMapper.selectById(999L)).thenReturn(null);
        BusinessException missing = assertThrows(BusinessException.class, () -> service.getPublicDetail(999L));
        assertEquals(BookingErrorCode.SERVICE_NOT_FOUND, missing.getErrorCode());
    }

    /**
     * DU-10: 聚合以单次批量调用携带当前服务 ID，不用逐条查询。
     */
    @Test
    void detailAggregateCarriesSingleServiceIdInOneCall() {
        stubVisibleContext();
        when(serviceMediaService.listMedia(301L)).thenReturn(List.of());
        when(serviceMediaService.resolveTrustedLegacyImages(null)).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());

        service.getPublicDetail(301L);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(ratingMapper).aggregateByTargets(eq("service"), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains(301L));
        verify(ratingMapper, never()).aggregateByTargets(eq("merchant"), anyCollection());
    }
}