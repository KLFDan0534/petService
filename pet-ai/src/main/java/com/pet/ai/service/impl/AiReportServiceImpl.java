package com.pet.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.pet.ai.entity.AiReport;
import com.pet.ai.service.AiChatService;
import com.pet.ai.service.AiReportService;
import com.pet.ai.service.ChromaService;
import com.pet.ai.service.ChromaService.ChromaGetResult;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.entity.Pet;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.PetMapper;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.common.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AiReportServiceImpl implements AiReportService {

    private static final String COLLECTION_NAME = "ai_reports";
    private static final int EMBEDDING_DIM = 4;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChromaService chromaService;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;
    private final OrderMapper orderMapper;
    private final CareRecordMapper careRecordMapper;
    private final AiChatService aiChatService;

    private String collectionId;

    public AiReportServiceImpl(ChromaService chromaService, PetMapper petMapper,
                                KeeperMapper keeperMapper, OrderMapper orderMapper,
                                CareRecordMapper careRecordMapper,
                                AiChatService aiChatService) {
        this.chromaService = chromaService;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
        this.orderMapper = orderMapper;
        this.careRecordMapper = careRecordMapper;
        this.aiChatService = aiChatService;
    }

    private String getCollectionId() {
        if (collectionId == null) {
            collectionId = chromaService.getOrCreateCollection(COLLECTION_NAME);
        }
        return collectionId;
    }

    @Override
    public List<AiReport> getReportsByOrder(Long orderId) {
        log.info("调用 getReportsByOrder()");
        Map<String, Object> where = Map.of("order_id_wsh", String.valueOf(orderId));
        ChromaGetResult result = chromaService.get(getCollectionId(), null, where,
                List.of("documents", "metadatas"));
        return toAiReports(result);
    }

    @Override
    public List<AiReport> getReportsByPet(Long petId) {
        log.info("调用 getReportsByPet()");
        Map<String, Object> where = Map.of("pet_id_wsh", String.valueOf(petId));
        ChromaGetResult result = chromaService.get(getCollectionId(), null, where,
                List.of("documents", "metadatas"));
        return toAiReports(result);
    }

    @Override
    public AiReport createReport(AiReport report) {
        log.info("调用 createReport()");
        String id = storeInChroma(report);
        report.setId_wsh(parseLongSafely(id));
        return report;
    }

    @Override
    public AiReport generateCareSuggestion(Long petId, Long keeperId, Long orderId) {
        log.info("调用 generateCareSuggestion()");
        if (petId == null && orderId == null) {
            throw new BusinessException("请提供宠物ID或订单ID");
        }

        PetOrder order = findOrder(orderId);
        if (order != null) {
            if (petId == null) petId = order.getPet_id_wsh();
            if (keeperId == null) keeperId = order.getKeeper_id_wsh();
        }

        Pet pet = findPet(petId);
        Keeper keeper = findKeeper(keeperId);
        List<CareRecord> careRecords = findCareRecords(orderId);

        AiReport report = new AiReport();
        report.setOrder_id_wsh(safeId(orderId));
        report.setPet_id_wsh(safeId(petId));
        report.setKeeper_id_wsh(safeId(keeperId));
        report.setType_wsh("care");

        String fallback = buildCareSuggestion(pet, keeper, order, petId, careRecords);
        String aiContent = aiChatService.chat(
                "你是宠物寄养平台的专业宠物护理顾问。请用中文输出结构清晰、可直接给用户阅读的护理建议。",
                buildCarePrompt(pet, keeper, order, petId, careRecords));
        report.setContent_wsh(useAiOrFallback(aiContent, fallback));
        report.setCreated_at_wsh(LocalDateTime.now());
        storeInChroma(report);
        return report;
    }

    @Override
    public AiReport generateBoardingReport(Long petId, Long keeperId, Long orderId) {
        log.info("调用 generateBoardingReport()");
        if (orderId == null) {
            throw new BusinessException("请提供订单ID");
        }

        PetOrder order = findOrder(orderId);
        if (order != null) {
            if (petId == null) petId = order.getPet_id_wsh();
            if (keeperId == null) keeperId = order.getKeeper_id_wsh();
        }

        Pet pet = findPet(petId);
        Keeper keeper = findKeeper(keeperId);
        List<CareRecord> careRecords = findCareRecords(orderId);

        AiReport report = new AiReport();
        report.setOrder_id_wsh(safeId(orderId));
        report.setPet_id_wsh(safeId(petId));
        report.setKeeper_id_wsh(safeId(keeperId));
        report.setType_wsh("final");

        String fallback = buildBoardingReport(pet, keeper, order, orderId, careRecords);
        String aiContent = aiChatService.chat(
                "你是宠物寄养平台的专业护理报告撰写助手。请用中文输出一份简洁、可信、适合展示给宠物主的寄养总结报告。",
                buildBoardingPrompt(pet, keeper, order, orderId, careRecords));
        report.setContent_wsh(useAiOrFallback(aiContent, fallback));
        report.setCreated_at_wsh(LocalDateTime.now());
        storeInChroma(report);
        return report;
    }

    private String storeInChroma(AiReport report) {
        if (report.getId_wsh() == null || report.getId_wsh() <= 0) {
            report.setId_wsh(generateId());
        }
        String docId = String.valueOf(report.getId_wsh());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id_wsh", docId);
        metadata.put("order_id_wsh", String.valueOf(safeId(report.getOrder_id_wsh())));
        metadata.put("pet_id_wsh", String.valueOf(safeId(report.getPet_id_wsh())));
        metadata.put("keeper_id_wsh", String.valueOf(safeId(report.getKeeper_id_wsh())));
        metadata.put("type_wsh", report.getType_wsh());
        metadata.put("created_at_wsh", report.getCreated_at_wsh() != null
                ? report.getCreated_at_wsh().format(DTF) : LocalDateTime.now().format(DTF));

        float[] embedding = chromaService.generateDummyEmbedding(EMBEDDING_DIM);
        chromaService.add(getCollectionId(),
                List.of(docId),
                List.of(embedding),
                List.of(report.getContent_wsh()),
                List.of(metadata));
        // TODO 后期查看是否需要存储到chroma
        log.info("AI报告 {} 已存储到Chroma", docId);
        return docId;
    }

    private List<AiReport> toAiReports(ChromaGetResult result) {
        List<AiReport> reports = new ArrayList<>();
        if (result.ids == null || result.ids.isEmpty()) return reports;

        for (int i = 0; i < result.ids.size(); i++) {
            AiReport report = new AiReport();
            report.setId_wsh(parseLongSafely(result.ids.get(i)));
            if (result.documents != null && i < result.documents.size()) {
                report.setContent_wsh(result.documents.get(i));
            }
            if (result.metadatas != null && i < result.metadatas.size()) {
                Map<String, Object> meta = result.metadatas.get(i);
                report.setOrder_id_wsh(parseLongSafely((String) meta.get("order_id_wsh")));
                report.setPet_id_wsh(parseLongSafely((String) meta.get("pet_id_wsh")));
                report.setKeeper_id_wsh(parseLongSafely((String) meta.get("keeper_id_wsh")));
                report.setType_wsh((String) meta.get("type_wsh"));
                String createdAt = (String) meta.get("created_at_wsh");
                if (createdAt != null) {
                    try {
                        report.setCreated_at_wsh(LocalDateTime.parse(createdAt, DTF));
                    } catch (Exception e) {
                        report.setCreated_at_wsh(LocalDateTime.now());
                    }
                }
            }
            reports.add(report);
        }
        reports.sort((a, b) -> {
            if (a.getCreated_at_wsh() == null && b.getCreated_at_wsh() == null) return 0;
            if (a.getCreated_at_wsh() == null) return 1;
            if (b.getCreated_at_wsh() == null) return -1;
            return b.getCreated_at_wsh().compareTo(a.getCreated_at_wsh());
        });
        return reports;
    }

    private static Long generateId() {
        long id = UUID.randomUUID().getLeastSignificantBits();
        return id == Long.MIN_VALUE ? 1L : Math.abs(id);
    }

    private static Long parseLongSafely(String value) {
        if (value == null || value.isBlank() || "0".equals(value) || "null".equals(value)) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Pet findPet(Long petId) {
        return petId == null || petId <= 0 ? null : petMapper.selectById(petId);
    }

    private Keeper findKeeper(Long keeperId) {
        return keeperId == null || keeperId <= 0 ? null : keeperMapper.selectById(keeperId);
    }

    private PetOrder findOrder(Long orderId) {
        return orderId == null || orderId <= 0 ? null : orderMapper.selectById(orderId);
    }

    private List<CareRecord> findCareRecords(Long orderId) {
        if (orderId == null || orderId <= 0) return List.of();
        return careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByAsc(CareRecord::getRecord_time_wsh)
                        .last("LIMIT 30"));
    }

    private Long safeId(Long id) {
        return id == null ? 0L : id;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String useAiOrFallback(String aiContent, String fallback) {
        return aiContent == null || aiContent.isBlank() ? fallback : aiContent.trim();
    }

    private String petName(Pet pet, Long petId) {
        return pet != null ? pet.getName_wsh() : "宠物ID " + safeId(petId);
    }

    private String buildCarePrompt(Pet pet, Keeper keeper, PetOrder order, Long petId, List<CareRecord> careRecords) {
        return "请生成一份宠物护理建议，控制在800字以内。\n"
                + "宠物: " + petName(pet, petId) + "\n"
                + "类型: " + (pet != null ? valueOrDefault(pet.getType_wsh(), "未知") : "系统未找到该宠物，请按通用宠物护理场景生成") + "\n"
                + "品种: " + (pet != null ? valueOrDefault(pet.getBreed_wsh(), "未知") : "未知") + "\n"
                + "年龄: " + (pet != null && pet.getAge_wsh() != null ? pet.getAge_wsh() + "岁" : "未知") + "\n"
                + "体重: " + (pet != null && pet.getWeight_wsh() != null ? pet.getWeight_wsh() + "kg" : "未知") + "\n"
                + "过敏信息: " + (pet != null ? valueOrDefault(pet.getAllergies_wsh(), "无") : "未知") + "\n"
                + "生活习惯: " + (pet != null ? valueOrDefault(pet.getHabits_wsh(), "无") : "未知") + "\n"
                + "看护员: " + (keeper != null ? keeper.getName_wsh() : "未指定") + "\n"
                + "订单天数: " + (order != null && order.getDays_wsh() != null ? order.getDays_wsh() : "未知") + "\n"
                + "已上传护理动态:\n" + formatCareRecords(careRecords) + "\n"
                + "请包含：日常喂养、活动安排、卫生护理、风险提醒、异常处理。";
    }

    private String buildBoardingPrompt(Pet pet, Keeper keeper, PetOrder order, Long orderId, List<CareRecord> careRecords) {
        return "请生成一份宠物寄养总结报告，控制在800字以内。\n"
                + "订单ID: " + safeId(orderId) + (order == null ? "（系统未找到该订单，请按通用寄养完成场景生成）" : "") + "\n"
                + "订单状态: " + (order != null ? valueOrDefault(order.getStatus_wsh(), "未知") : "未知") + "\n"
                + "寄养天数: " + (order != null && order.getDays_wsh() != null ? order.getDays_wsh() : "未知") + "\n"
                + "开始日期: " + (order != null ? order.getStart_date_wsh() : "未知") + "\n"
                + "结束日期: " + (order != null ? order.getEnd_date_wsh() : "未知") + "\n"
                + "宠物: " + petName(pet, pet != null ? pet.getId_wsh() : null) + "\n"
                + "类型: " + (pet != null ? valueOrDefault(pet.getType_wsh(), "未知") : "未知") + "\n"
                + "看护员: " + (keeper != null ? keeper.getName_wsh() : "未指定") + "\n"
                + "护理动态:\n" + formatCareRecords(careRecords) + "\n"
                + "请包含：寄养概况、饮食状态、活动状态、健康观察、回家后建议。";
    }

    private String buildCareSuggestion(Pet pet, Keeper keeper, PetOrder order, Long petId, List<CareRecord> careRecords) {
        String petName = petName(pet, petId);
        String petType = pet != null ? valueOrDefault(pet.getType_wsh(), "宠物") : "宠物";
        String allergies = pet != null ? valueOrDefault(pet.getAllergies_wsh(), "无明确过敏信息") : "系统未找到宠物档案，请先按通用护理标准执行";
        String habits = pet != null ? valueOrDefault(pet.getHabits_wsh(), "无特殊习惯记录") : "未知";
        String keeperName = keeper != null ? keeper.getName_wsh() : "未指定看护员";
        String days = order != null && order.getDays_wsh() != null ? order.getDays_wsh() + "天" : "按实际服务周期";

        return "护理建议 - " + petName + "（" + petType + "）\n"
                + "看护员：" + keeperName + "\n"
                + "服务周期：" + days + "\n\n"
                + "1. 日常喂养：保持规律喂食，少量多次观察进食情况，全天提供干净饮水。\n"
                + "2. 活动安排：根据宠物体型和精神状态安排适度活动，避免突然增加运动强度。\n"
                + "3. 卫生护理：每日清洁生活区域，检查毛发、耳朵、爪垫和排泄情况。\n"
                + "4. 风险提醒：过敏/禁忌信息为：" + allergies + "；生活习惯为：" + habits + "。\n"
                + "5. 异常处理：如出现食欲下降、呕吐、腹泻、精神沉郁或异常叫声，应立即记录并联系宠物主，必要时就医。\n\n"
                + "已记录动态：\n" + formatCareRecords(careRecords);
    }

    private String buildBoardingReport(Pet pet, Keeper keeper, PetOrder order, Long orderId, List<CareRecord> careRecords) {
        String petName = petName(pet, pet != null ? pet.getId_wsh() : null);
        String petType = pet != null ? valueOrDefault(pet.getType_wsh(), "宠物") : "宠物";
        String keeperName = keeper != null ? keeper.getName_wsh() : "未指定看护员";
        String days = order != null && order.getDays_wsh() != null ? order.getDays_wsh() + "天" : "本次";
        String status = order != null ? valueOrDefault(order.getStatus_wsh(), "未知") : "未找到订单";

        return "寄养总结报告 - 订单 " + safeId(orderId) + "\n"
                + "宠物：" + petName + "（" + petType + "）\n"
                + "看护员：" + keeperName + "\n"
                + "订单状态：" + status + "\n"
                + "寄养周期：" + days + "\n\n"
                + "本次寄养期间，护理重点包括规律喂食、饮水管理、日常活动安排和健康状态观察。\n\n"
                + "护理动态摘要：\n" + formatCareRecords(careRecords) + "\n\n"
                + "回家后建议：持续观察食欲、饮水、排泄和精神状态；如发现呕吐、腹泻、持续沉郁或异常行为，请及时联系看护员并咨询兽医。";
    }

    private String formatCareRecords(List<CareRecord> records) {
        if (records == null || records.isEmpty()) {
            return "暂无看护动态记录。";
        }
        StringBuilder sb = new StringBuilder();
        for (CareRecord record : records) {
            sb.append("- ")
                    .append(record.getRecord_time_wsh() != null ? record.getRecord_time_wsh().format(DTF) : "未记录时间")
                    .append(" [").append(valueOrDefault(record.getType_wsh(), "note")).append("] ")
                    .append(valueOrDefault(record.getContent_wsh(), "仅上传了图片/附件"));
            if (record.getImages_wsh() != null && !record.getImages_wsh().isBlank()) {
                sb.append("（含图片）");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
