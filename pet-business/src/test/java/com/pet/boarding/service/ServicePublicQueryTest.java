package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceQueryResultVO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U1 RED: 服务列表公开查询契约 —— 同分类不同商家的服务必须按不同 service_id 返回，
 * 附带商家名、服务/商家评分聚合、评价数、距离和版本；禁止 N+1 查询；
 * 只公开上架服务 + 已审核商家 + 启用分类。
 */
@ExtendWith(MockitoExtension.class)
class ServicePublicQueryTest {

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

    private ServiceItem enabled(Long id, Long merchantId, Long categoryId, String name, BigDecimal price) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        item.setCategory_id_wsh(categoryId);
        item.setName_wsh(name);
        item.setPrice_wsh(price);
        item.setUnit_wsh("day");
        item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        item.setUpdated_at_wsh(LocalDateTime.of(2026, 8, 11, 13, 45, 20));
        return item;
    }

    private Merchant approved(Long id, String name) {
        Merchant m = new Merchant();
        m.setId_wsh(id);
        m.setName_wsh(name);
        m.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        return m;
    }

    private ServiceCategory enabledCategory(Long id, String name) {
        ServiceCategory c = new ServiceCategory();
        c.setId_wsh(id);
        c.setName_wsh(name);
        c.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        return c;
    }

    private Map<String, Object> agg(Long targetId, BigDecimal avg, long cnt) {
        return Map.of("targetId", targetId, "avgScore", avg, "cnt", cnt);
    }

    /**
     * SVC-U-01/02: 同分类、同名服务 S1/M1 与 S2/M2 以不同 service_id 返回，商家名不混淆。
     */
    @Test
    void sameCategoryServicesFromDifferentMerchantsStayDistinct() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s2 = enabled(302L, 102L, 1L, "标准寄养", new BigDecimal("168.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), approved(102L, "萌宠乐园")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertEquals(2, vo.getTotal_wsh());
        assertEquals(301L, vo.getItems_wsh().get(0).getId_wsh());
        assertEquals(302L, vo.getItems_wsh().get(1).getId_wsh());
        assertEquals("爱宠之家", vo.getItems_wsh().get(0).getMerchant_name_wsh());
        assertEquals("萌宠乐园", vo.getItems_wsh().get(1).getMerchant_name_wsh());
        assertEquals(new BigDecimal("128.00"), vo.getItems_wsh().get(0).getPrice_wsh());
        assertEquals(new BigDecimal("168.00"), vo.getItems_wsh().get(1).getPrice_wsh());
        assertEquals("寄养", vo.getItems_wsh().get(0).getCategory_name_wsh());
    }

    /**
     * SVC-U-03: 下架服务不出现在公开查询中。
     */
    @Test
    void disabledServiceHiddenFromPublicQuery() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s4 = enabled(304L, 101L, 1L, "已下架寄养", new BigDecimal("99.00"));
        s4.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertEquals(1, vo.getTotal_wsh());
        assertEquals(301L, vo.getItems_wsh().get(0).getId_wsh());
    }

    /**
     * SVC-U-04: 未审核商家的服务不出现在公开查询中。
     */
    @Test
    void unapprovedMerchantServicesHidden() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s5 = enabled(305L, 103L, 1L, "待审商家寄养", new BigDecimal("88.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s5));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        Merchant pending = approved(103L, "未审商家");
        pending.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), pending));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertEquals(1, vo.getTotal_wsh());
        assertEquals(301L, vo.getItems_wsh().get(0).getId_wsh());
    }

    /**
     * SVC-U-05: 禁用分类下的服务不公开；按禁用分类筛选返回空。
     */
    @Test
    void disabledCategoryServicesHidden() {
        ServiceItem s6 = enabled(306L, 101L, 3L, "游泳体验", new BigDecimal("60.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s6));
        ServiceCategory disabled = enabledCategory(3L, "游泳");
        disabled.setStatus_wsh(StatusCode.SERVICE_DISABLED.getValue());
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(disabled));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertEquals(0, vo.getTotal_wsh());
    }

    /**
     * SVC-U-06: 服务评分与商家评分按不同 target type 聚合。
     */
    @Test
    void ratingAggregationSplitByTargetType() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection()))
                .thenReturn(List.of(agg(301L, new BigDecimal("4.8"), 36L)));
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection()))
                .thenReturn(List.of(agg(101L, new BigDecimal("4.7"), 120L)));

        ServiceQueryResultVO vo = service.queryPublic(query());

        ServiceItemDTO dto = vo.getItems_wsh().get(0);
        assertEquals(0, new BigDecimal("4.8").compareTo(dto.getService_rating_wsh()));
        assertEquals(36L, dto.getService_rating_count_wsh());
        assertEquals(0, new BigDecimal("4.7").compareTo(dto.getMerchant_rating_wsh()));
        assertEquals(120L, dto.getMerchant_rating_count_wsh());
        assertNotNull(dto.getService_version_wsh());
        assertEquals("2026-08-11T13:45:20", dto.getService_version_wsh());
    }

    /**
     * SVC-U-05 附加：无评价的目标评分为空、评价数为 0。
     */
    @Test
    void noRatingsYieldsNullScoreAndZeroCount() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        ServiceItemDTO dto = vo.getItems_wsh().get(0);
        assertNull(dto.getService_rating_wsh());
        assertEquals(0L, dto.getService_rating_count_wsh());
        assertNull(dto.getMerchant_rating_wsh());
        assertEquals(0L, dto.getMerchant_rating_count_wsh());
    }

    /**
     * SVC-U-07: 无定位参数 -> distance_km_wsh 为 null，不伪造 0。
     */
    @Test
    void distanceNullWithoutCoordinates() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertNull(vo.getItems_wsh().get(0).getDistance_km_wsh());
    }

    /**
     * SVC-U-07b: 提供定位参数且商家有坐标 -> 距离为真实计算结果。
     */
    @Test
    void distanceComputedWithCoordinates() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        Merchant m1 = approved(101L, "爱宠之家");
        m1.setLatitude_wsh(new BigDecimal("31.2304"));
        m1.setLongitude_wsh(new BigDecimal("121.4737"));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(m1));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceItemQueryDTO q = query();
        q.setLatitude_wsh(new BigDecimal("31.2504"));
        q.setLongitude_wsh(new BigDecimal("121.4737"));
        ServiceQueryResultVO vo = service.queryPublic(q);

        assertNotNull(vo.getItems_wsh().get(0).getDistance_km_wsh());
        assertTrue(vo.getItems_wsh().get(0).getDistance_km_wsh().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * SVC-U-08: 列表页面不随行数增长而线性查询 —— 分类/商家批量查询各一次，
     * 评分聚合每个维度一次，禁止逐行 selectById。
     */
    @Test
    void publicQueryDoesNotIssueNPlusOneLookups() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s2 = enabled(302L, 102L, 1L, "标准寄养", new BigDecimal("168.00"));
        ServiceItem s3 = enabled(303L, 101L, 2L, "基础训练", new BigDecimal("99.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2, s3));
        when(categoryMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(enabledCategory(1L, "寄养"), enabledCategory(2L, "训练")));
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), approved(102L, "萌宠乐园")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection())).thenReturn(List.of());
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection())).thenReturn(List.of());

        service.queryPublic(query());

        verify(categoryMapper, never()).selectById(any());
        verify(categoryMapper).selectBatchIds(anyCollection());
        verify(merchantMapper).selectBatchIds(anyCollection());
        verify(ratingMapper).aggregateByTargets(eq("service"), anyCollection());
        verify(ratingMapper).aggregateByTargets(eq("merchant"), anyCollection());
    }

    /**
     * SVC-U-08b: 空列表不再发起聚合查询。
     */
    @Test
    void emptyListSkipsBatchLookups() {
        when(serviceItemMapper.selectList(any())).thenReturn(List.of());

        ServiceQueryResultVO vo = service.queryPublic(query());

        assertEquals(0, vo.getTotal_wsh());
        verify(categoryMapper, never()).selectBatchIds(anyCollection());
        verify(merchantMapper, never()).selectBatchIds(anyCollection());
        verify(ratingMapper, never()).aggregateByTargets(any(), anyCollection());
    }

    /**
     * 关键字过滤：SQL 层包装器必须包含 LIKE 关键字谓词，返回结果只保留匹配行。
     */
    @Test
    void keywordFiltersByName() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceItemQueryDTO q = query();
        q.setKeyword_wsh("标准");

        service.queryPublic(q);
        service.queryPublic(q);
        verify(serviceItemMapper, org.mockito.Mockito.times(2)).selectList(any());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ServiceItem>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(serviceItemMapper, org.mockito.Mockito.times(2)).selectList(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIKE"),
                "keyword must be applied as SQL LIKE predicate");
        assertTrue(captor.getValue().getParamNameValuePairs().values().stream()
                        .anyMatch(v -> String.valueOf(v).contains("标准")),
                "keyword value must be carried into the query");

        List<ServiceItemDTO> dtos = service.queryPublic(q).getItems_wsh();
        assertEquals(1, dtos.size());
        assertEquals(301L, dtos.get(0).getId_wsh());
    }

    /**
     * 排序白名单：price_asc 生效，非法 sort 返回 400 稳定错误码。
     */
    @Test
    void priceAscSortAppliedAndInvalidSortRejected() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("168.00"));
        ServiceItem s2 = enabled(302L, 102L, 1L, "特惠寄养", new BigDecimal("128.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), approved(102L, "萌宠乐园")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceItemQueryDTO q = query();
        q.setSort_wsh("price_asc");
        ServiceQueryResultVO vo = service.queryPublic(q);

        assertEquals(302L, vo.getItems_wsh().get(0).getId_wsh());
        assertEquals(301L, vo.getItems_wsh().get(1).getId_wsh());

        ServiceItemQueryDTO bad = query();
        bad.setSort_wsh("price; DROP TABLE services");
        BusinessException error = assertThrows(BusinessException.class, () -> service.queryPublic(bad));
        assertEquals(400, error.getCode());
        assertEquals(BookingErrorCode.INVALID_SORT_PARAM, error.getErrorCode());
    }

    /**
     * 分页：page/size 切片正确，非法页码拒绝。
     */
    @Test
    void paginationSlicesResponseAndBoundsAreValidated() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s2 = enabled(302L, 101L, 1L, "特惠寄养", new BigDecimal("168.00"));
        ServiceItem s3 = enabled(303L, 101L, 1L, "安心寄养", new BigDecimal("188.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2, s3));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection())).thenReturn(List.of(approved(101L, "爱宠之家")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        ServiceItemQueryDTO q = query();
        q.setPage_wsh(2);
        q.setSize_wsh(1);
        ServiceQueryResultVO vo = service.queryPublic(q);

        assertEquals(3, vo.getTotal_wsh());
        assertEquals(302L, vo.getItems_wsh().get(0).getId_wsh());
        assertEquals(2, vo.getPage_wsh());
        assertEquals(1, vo.getSize_wsh());

        ServiceItemQueryDTO badPage = query();
        badPage.setPage_wsh(0);
        assertThrows(BusinessException.class, () -> service.queryPublic(badPage));

        ServiceItemQueryDTO badSize = query();
        badSize.setSize_wsh(0);
        assertThrows(BusinessException.class, () -> service.queryPublic(badSize));
    }

    /**
     * rating_desc：评分更高的服务排前面。
     */
    @Test
    void ratingDescSortOrdersByAggregatedScore() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s2 = enabled(302L, 102L, 1L, "特惠寄养", new BigDecimal("168.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), approved(102L, "萌宠乐园")));
        when(ratingMapper.aggregateByTargets(eq("service"), anyCollection()))
                .thenReturn(List.of(agg(301L, new BigDecimal("4.2"), 5L), agg(302L, new BigDecimal("4.9"), 50L)));
        when(ratingMapper.aggregateByTargets(eq("merchant"), anyCollection())).thenReturn(List.of());

        ServiceItemQueryDTO q = query();
        q.setSort_wsh("rating_desc");
        ServiceQueryResultVO vo = service.queryPublic(q);

        assertEquals(302L, vo.getItems_wsh().get(0).getId_wsh());
        assertEquals(301L, vo.getItems_wsh().get(1).getId_wsh());
    }

    private ServiceItemQueryDTO query() {
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setPage_wsh(1);
        q.setSize_wsh(20);
        q.setSort_wsh("default");
        return q;
    }

    /**
     * 聚合查询必须批量接收 ID，禁止 IN 子句为空导致 SQL 报错。
     */
    @Test
    void aggregateCallCarriesAllServiceIdsInSingleInvocation() {
        ServiceItem s1 = enabled(301L, 101L, 1L, "标准寄养", new BigDecimal("128.00"));
        ServiceItem s2 = enabled(302L, 102L, 1L, "特惠寄养", new BigDecimal("168.00"));
        when(serviceItemMapper.selectList(any())).thenReturn(List.of(s1, s2));
        when(categoryMapper.selectBatchIds(anyCollection())).thenReturn(List.of(enabledCategory(1L, "寄养")));
        when(merchantMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(approved(101L, "爱宠之家"), approved(102L, "萌宠乐园")));
        when(ratingMapper.aggregateByTargets(any(), anyCollection())).thenReturn(List.of());

        service.queryPublic(query());

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(ratingMapper).aggregateByTargets(eq("service"), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains(301L));
        assertTrue(captor.getValue().contains(302L));
    }
}