package com.pet.admin;

import com.pet.ai.dto.AiReportCreateRequestDTO;
import com.pet.ai.entity.AiReport;
import com.pet.ai.mapper.AiReportMapper;
import com.pet.ai.service.AiReportService;
import com.pet.ai.service.ChromaService;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.impl.AiReportServiceImpl;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.mapper.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AI 报告只按 metadata 取回，不做向量语义检索。
 * <p>
 * 背景：Chroma v2 的 add 接口强制要求 embeddings 字段（实测缺字段直接 400
 * {@code missing field embeddings}），但 get 接口支持只用 where 过滤 metadata。
 * 因此报告写库时携带一个固定 4 维零向量，读取时完全不使用向量。
 * <p>
 * 本测试锁死三件事：
 * 1. 读路径只用 where + documents/metadatas，include 里绝不出现 embeddings；
 * 2. MySQL 命中时不触碰 Chroma（原有降级顺序不变）；
 * 3. 写路径写入的是 4 维零向量，不再有任何「生成 embedding」的调用点。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 报告 · metadata 检索契约")
class AiReportMetadataQueryTest {

    private static final Long USER_ID = 7L;
    private static final Long ORDER_ID = 100L;

    @Mock private ChromaService chromaService;
    @Mock private PetMapper petMapper;
    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private CareRecordMapper careRecordMapper;
    @Mock private LlmChatService aiChatService;
    @Mock private AiReportMapper aiReportMapper;
    @Mock private RoleMapper roleMapper;

    private AiReportService service;

    @BeforeEach
    void setUp() {
        service = new AiReportServiceImpl(chromaService, petMapper, keeperMapper, merchantMapper,
                orderMapper, careRecordMapper, aiChatService, aiReportMapper, roleMapper);
    }

    @Test
    @DisplayName("MySQL 命中时直接返回，完全不查 Chroma")
    void mysqlHitDoesNotTouchChroma() {
        stubOwnerAccess();
        AiReport stored = report(1L, "数据库里的报告");
        when(aiReportMapper.selectList(any())).thenReturn(List.of(stored));

        List<AiReport> result = service.getReportsByOrder(USER_ID, ORDER_ID);

        assertEquals(1, result.size());
        assertEquals("数据库里的报告", result.get(0).getContent_wsh());
        verifyNoInteractions(chromaService);
    }

    @Test
    @DisplayName("MySQL 未命中时按 order_id 元数据取回，且不请求 embeddings")
    void chromaFallbackUsesMetadataOnly() {
        stubOwnerAccess();
        when(aiReportMapper.selectList(any())).thenReturn(List.of());
        when(chromaService.getOrCreateCollection(anyString())).thenReturn("cid");
        when(chromaService.get(anyString(), isNull(), anyMap(), anyList()))
                .thenReturn(new ChromaService.ChromaGetResult(List.of(), List.of(), List.of()));

        service.getReportsByOrder(USER_ID, ORDER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> includeCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> whereCaptor = ArgumentCaptor.forClass(Map.class);
        verify(chromaService).get(eq("cid"), isNull(), whereCaptor.capture(), includeCaptor.capture());

        assertEquals(Map.of("order_id_wsh", String.valueOf(ORDER_ID)), whereCaptor.getValue(),
                "必须按 order_id 元数据精确过滤");
        assertEquals(List.of("documents", "metadatas"), includeCaptor.getValue(),
                "只取正文与元数据，绝不请求向量");
        assertTrue(!includeCaptor.getValue().contains("embeddings"), "读路径不允许取 embedding");
    }

    @Test
    @DisplayName("Chroma 结果能还原成报告实体并按创建时间倒序")
    void chromaResultIsMappedBack() {
        stubOwnerAccess();
        when(aiReportMapper.selectList(any())).thenReturn(List.of());
        when(chromaService.getOrCreateCollection(anyString())).thenReturn("cid");

        Map<String, Object> older = Map.of(
                "order_id_wsh", String.valueOf(ORDER_ID),
                "pet_id_wsh", "11",
                "keeper_id_wsh", "31",
                "type_wsh", "boarding",
                "created_at_wsh", "2026-11-01 10:00:00");
        Map<String, Object> newer = Map.of(
                "order_id_wsh", String.valueOf(ORDER_ID),
                "pet_id_wsh", "11",
                "keeper_id_wsh", "31",
                "type_wsh", "care",
                "created_at_wsh", "2026-11-02 10:00:00");
        when(chromaService.get(anyString(), isNull(), anyMap(), anyList()))
                .thenReturn(new ChromaService.ChromaGetResult(
                        List.of("1", "2"),
                        List.of("旧报告", "新报告"),
                        List.of(older, newer)));

        List<AiReport> result = service.getReportsByOrder(USER_ID, ORDER_ID);

        assertEquals(2, result.size());
        assertEquals("新报告", result.get(0).getContent_wsh(), "按创建时间倒序");
        assertEquals("旧报告", result.get(1).getContent_wsh());
        assertEquals(ORDER_ID, result.get(0).getOrder_id_wsh());
        assertEquals("care", result.get(0).getType_wsh());
    }

    @Test
    @DisplayName("写入报告时用固定 4 维零向量满足 Chroma add，不依赖任何 embedding 服务")
    void storeUsesFixedZeroVector() {
        when(roleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of("ADMIN"));
        when(chromaService.getOrCreateCollection(anyString())).thenReturn("cid");

        AiReportCreateRequestDTO request = new AiReportCreateRequestDTO();
        request.setOrder_id_wsh(ORDER_ID);
        request.setPet_id_wsh(11L);
        request.setKeeper_id_wsh(31L);
        request.setContent_wsh("手工录入的报告");
        request.setType_wsh("boarding");

        service.createReport(USER_ID, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<float[]>> embeddingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(chromaService).add(eq("cid"), anyList(), embeddingsCaptor.capture(), anyList(), anyList());

        assertEquals(1, embeddingsCaptor.getValue().size());
        float[] vector = embeddingsCaptor.getValue().get(0);
        assertEquals(4, vector.length, "集合是按 4 维建立的，维度不能变");
        for (float v : vector) {
            assertEquals(0.0f, v, "报告不做语义检索，写入的必须是零向量");
        }
        assertNotNull(vector);
        verify(aiReportMapper).insert(any(AiReport.class));
        verify(chromaService, never()).get(anyString(), any(), anyMap(), anyList());
    }

    // ── 工具 ────────────────────────────────────────────────

    /** 让 requireOrderAccess 走「宠物主本人」分支：订单 owner 就是当前用户。 */
    private void stubOwnerAccess() {
        when(roleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of());
        PetOrder order = new PetOrder();
        order.setId_wsh(ORDER_ID);
        order.setOrder_no_wsh("ORD-1");
        order.setOwner_id_wsh(USER_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
    }

    private AiReport report(Long id, String content) {
        AiReport report = new AiReport();
        report.setId_wsh(id);
        report.setOrder_id_wsh(ORDER_ID);
        report.setPet_id_wsh(11L);
        report.setKeeper_id_wsh(31L);
        report.setContent_wsh(content);
        report.setType_wsh("boarding");
        report.setCreated_at_wsh(LocalDateTime.of(2026, 11, 1, 10, 0));
        return report;
    }
}
