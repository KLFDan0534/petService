package com.pet.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.pet.ai.dto.AiReportCreateRequestDTO;
import com.pet.ai.entity.AiReport;
import com.pet.ai.mapper.AiReportMapper;
import com.pet.ai.service.LlmChatService;
import com.pet.ai.service.AiReportService;
import com.pet.ai.service.ChromaService;
import com.pet.ai.service.ChromaService.ChromaGetResult;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.entity.Pet;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.PetMapper;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.common.BusinessException;
import com.pet.system.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 报告服务实现类，集成 DeepSeek AI 模型和 Chroma 向量数据库。
 * <p>
 * 核心流程：
 * 1. 查询宠物、看护人、订单和护理动态数据
 * 2. 构建 AI 提示词（含宠物档案和护理记录上下文）
 * 3. 调用 LlmChatService 获取 AI 生成内容
 * 4. AI 不可用时降级为模板填充
 * 5. 保存报告到 MySQL 和 Chroma 向量数据库
 * <p>
 * 关键业务规则：
 * - 报告访问鉴权：仅宠物主、关联看护人、关联商家和 ADMIN 可查看
 * - 报告类型：care（护理建议）、final（寄养总结）
 * - Chroma 存储失败不影响 MySQL 主记录
 */
@Service
@Slf4j
public class AiReportServiceImpl implements AiReportService {

    private static final String COLLECTION_NAME = "ai_reports";
    /**
     * Chroma v2 的 add 接口强制要求 embeddings 字段（实测缺字段直接 400 missing field `embeddings`），
     * 而报告只按 metadata（order_id_wsh / pet_id_wsh）精确取回，从不做向量检索。
     * 因此这里写入一个固定的 4 维零向量满足接口格式，全类共用同一实例，不参与任何相似度计算。
     */
    private static final float[] STORAGE_VECTOR = new float[4];
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChromaService chromaService;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final CareRecordMapper careRecordMapper;
    private final LlmChatService aiChatService;
    private final AiReportMapper aiReportMapper;
    private final RoleMapper roleMapper;

    private String collectionId;

    /**
     * 业务作用：注入向量库、宠物、看护者、商家、订单、护理动态 Mapper 以及对话服务与报告 Mapper。
     * 注意事项：直接注入多个 Mapper 用于跨模块取数，未走各模块 Service。
     */
    public AiReportServiceImpl(ChromaService chromaService, PetMapper petMapper,
                                KeeperMapper keeperMapper, MerchantMapper merchantMapper, OrderMapper orderMapper,
                                CareRecordMapper careRecordMapper,
                                LlmChatService aiChatService,
                                AiReportMapper aiReportMapper,
                                RoleMapper roleMapper) {
        this.chromaService = chromaService;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
        this.merchantMapper = merchantMapper;
        this.orderMapper = orderMapper;
        this.careRecordMapper = careRecordMapper;
        this.aiChatService = aiChatService;
        this.aiReportMapper = aiReportMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 业务作用：懒加载 Chroma 集合 ID，避免每次请求都远程创建或查询集合。
     * 业务规则：集合名固定为 ai_reports。
     * 异常情况：Chroma 不可用时异常向上抛出。
     * 注意事项：字段非 volatile，并发首次调用可能重复创建，结果幂等可接受。
     */
    private String getCollectionId() {
        // 首次调用才创建集合，之后复用缓存
        if (collectionId == null) {
            collectionId = chromaService.getOrCreateCollection(COLLECTION_NAME);
        }
        return collectionId;
    }

    /**
     * 按订单查询AI报告实现
     *
     * 业务作用：根据订单ID查询AI报告列表，先查MySQL（AiReportMapper），未命中时降级到Chroma向量库按order_id_wsh元数据查询。
     * 业务规则：MySQL数据优先；Chroma查询结果通过toAiReports()解析并按创建时间倒序。
     * 异常情况：requireOrderAccess内部可能抛出400/404/403异常。
     * 注意事项：Chroma中order_id_wsh以字符串形式存储于metadata。
     */
    @Override
    public List<AiReport> getReportsByOrder(Long userId, Long orderId) {
        log.info("调用 getReportsByOrder()");
        // 先鉴权，再查数据
        requireOrderAccess(userId, orderId);
        List<AiReport> reports = aiReportMapper.selectList(
                new LambdaQueryWrapper<AiReport>()
                        .eq(AiReport::getOrder_id_wsh, safeId(orderId))
                        .orderByDesc(AiReport::getCreated_at_wsh));
        // MySQL 命中就直接返回
        if (!reports.isEmpty()) {
            return reports;
        }
        // 没命中再降级查向量库
        Map<String, Object> where = Map.of("order_id_wsh", String.valueOf(orderId));
        ChromaGetResult result = chromaService.get(getCollectionId(), null, where,
                List.of("documents", "metadatas"));
        return toAiReports(result);
    }

    /**
     * 按宠物查询AI报告实现
     *
     * 业务作用：根据宠物ID查询AI报告列表，优先MySQL，未命中时从Chroma按pet_id_wsh查询。
     * 业务规则：仅宠物主可查看；MySQL > Chroma 降级。
     * 异常情况：requirePetAccess抛出400/404/403。
     * 注意事项：Chroma中pet_id_wsh以字符串形式存储。
     */
    @Override
    public List<AiReport> getReportsByPet(Long userId, Long petId) {
        log.info("调用 getReportsByPet()");
        requirePetAccess(userId, petId);
        List<AiReport> reports = aiReportMapper.selectList(
                new LambdaQueryWrapper<AiReport>()
                        .eq(AiReport::getPet_id_wsh, safeId(petId))
                        .orderByDesc(AiReport::getCreated_at_wsh));
        // MySQL 命中就直接返回
        if (!reports.isEmpty()) {
            return reports;
        }
        // 没命中再降级查向量库
        Map<String, Object> where = Map.of("pet_id_wsh", String.valueOf(petId));
        ChromaGetResult result = chromaService.get(getCollectionId(), null, where,
                List.of("documents", "metadatas"));
        return toAiReports(result);
    }

    /**
     * 手动创建AI报告实现
     *
     * 业务作用：看护人/管理员直接提交报告内容，绕过AI模型，手动创建一条AI报告记录。
     * 业务规则：ADMIN或关联订单/宠物有写权限；不调用AI。
     * 异常情况：request为null或内容为空抛出400；无权限抛出403。
     * 注意事项：Chroma存储失败不影响MySQL主记录，仅warn日志。
     */
    @Override
    public AiReport createReport(Long userId, AiReportCreateRequestDTO request) {
        log.info("调用 createReport()");
        // 校验写权限
        requireReportWriteAccess(userId, request);
        AiReport report = new AiReport();
        report.setOrder_id_wsh(request.getOrder_id_wsh());
        report.setPet_id_wsh(request.getPet_id_wsh());
        report.setKeeper_id_wsh(request.getKeeper_id_wsh());
        report.setContent_wsh(request.getContent_wsh());
        report.setType_wsh(request.getType_wsh());
        saveReport(report);
        return report;
    }

    /**
     * AI生成护理建议报告实现
     *
     * 业务作用：查询宠物/看护人/订单/护理动态数据，构建AI提示词，调用AiChatService生成护理建议，AI不可用时降级为模板填充。
     * 业务规则：petId或orderId至少提供一个；type固定为"care"；AI返回null/blank时使用模板。
     * 异常情况：petId和orderId均为null抛出400；orderId指定但order不存在抛出404；鉴权失败抛出403。
     * 注意事项：护理动态最多取最近30条（LIMIT 30）；AI提示词限定800字以内。
     */
    @Override
    public AiReport generateCareSuggestion(Long userId, Long petId, Long keeperId, Long orderId) {
        log.info("调用 generateCareSuggestion()");
        // 宠物和订单至少要有一个
        if (petId == null && orderId == null) {
            throw new BusinessException(400, "请提供宠物ID或订单ID");
        }

        PetOrder order = findOrder(orderId);
        // 指定了订单就必须存在
        if (orderId != null && order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        requireAccess(userId, order, petId);
        if (order != null) {
            // 从订单补齐宠物和看护者
            if (petId == null) petId = order.getPet_id_wsh();
            if (keeperId == null) keeperId = order.getKeeper_id_wsh();
        } else {
            keeperId = null;
        }

        Pet pet = findPet(petId);
        Keeper keeper = findKeeper(keeperId);
        List<CareRecord> careRecords = findCareRecords(orderId);

        AiReport report = new AiReport();
        report.setOrder_id_wsh(safeId(orderId));
        report.setPet_id_wsh(safeId(petId));
        report.setKeeper_id_wsh(safeId(keeperId));
        report.setType_wsh("care");

        // 先备好模板，AI 不可用时兜底
        String fallback = buildCareSuggestion(pet, keeper, order, petId, careRecords);
        // 调大模型生成正文
        String aiContent = aiChatService.chat(
                "你是宠物寄养平台的专业宠物护理顾问。请用中文输出结构清晰、可直接给用户阅读的护理建议。",
                buildCarePrompt(pet, keeper, order, petId, careRecords));
        // AI 内容优先，空则用模板
        report.setContent_wsh(useAiOrFallback(aiContent, fallback));
        report.setCreated_at_wsh(LocalDateTime.now());
        saveReport(report);
        return report;
    }

    /**
     * AI生成寄养总结报告（用户调用）
     *
     * 业务作用：用户触发生成寄养总结报告，校验订单存在性和用户权限，委托generateBoardingReportUnchecked执行核心逻辑。
     * 业务规则：orderId必填；仅宠物主、看护人、商家和ADMIN可访问。
     * 异常情况：orderId为null抛出400；订单不存在抛出404；无权限抛出403。
     * 注意事项：校验通过后实际处理在generateBoardingReportUnchecked中。
     */
    @Override
    public AiReport generateBoardingReport(Long userId, Long petId, Long keeperId, Long orderId) {
        log.info("调用 generateBoardingReport()");
        // 订单 ID 必填
        if (orderId == null) {
            throw new BusinessException(400, "请提供订单ID");
        }
        PetOrder order = findOrder(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 校验订单访问权限
        requireOrderAccess(userId, order);
        return generateBoardingReportUnchecked(petId, keeperId, orderId, order);
    }

    /**
     * 内部AI生成寄养总结报告（事件驱动）
     *
     * 业务作用：订单完成事件触发后内部调用，不校验用户权限，直接生成寄养总结报告。
     * 业务规则：orderId必填；无鉴权。
     * 异常情况：orderId为null抛出400；订单不存在抛出404。
     * 注意事项：内部调用信任所有请求；监听器层面捕获异常不会传播到主事务。
     */
    @Override
    public AiReport generateBoardingReportInternal(Long petId, Long keeperId, Long orderId) {
        log.info("调用 generateBoardingReportInternal()");
        // 订单 ID 必填
        if (orderId == null) {
            throw new BusinessException(400, "请提供订单ID");
        }

        PetOrder order = findOrder(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return generateBoardingReportUnchecked(petId, keeperId, orderId, order);
    }

    /**
     * 业务作用：真正执行寄养总结生成的内部方法，不做用户权限校验。
     * 业务规则：type 固定为 final；AI 内容优先，模板兜底。
     * 异常情况：数据库异常向上抛出；Chroma 失败在 saveReport 内部被吞掉。
     * 注意事项：方法名带 Unchecked 表示调用方必须已完成鉴权，内部不得再对外暴露。
     */
    private AiReport generateBoardingReportUnchecked(Long petId, Long keeperId, Long orderId, PetOrder order) {
        if (order != null) {
            // 从订单补齐宠物和看护者
            if (petId == null) petId = order.getPet_id_wsh();
            if (keeperId == null) keeperId = order.getKeeper_id_wsh();
        } else {
            keeperId = null;
        }

        Pet pet = findPet(petId);
        Keeper keeper = findKeeper(keeperId);
        List<CareRecord> careRecords = findCareRecords(orderId);

        AiReport report = new AiReport();
        report.setOrder_id_wsh(safeId(orderId));
        report.setPet_id_wsh(safeId(petId));
        report.setKeeper_id_wsh(safeId(keeperId));
        // 寄养总结类型
        report.setType_wsh("final");

        // 先备好模板，AI 不可用时兜底
        String fallback = buildBoardingReport(pet, keeper, order, orderId, careRecords);
        // 调大模型生成正文
        String aiContent = aiChatService.chat(
                "你是宠物寄养平台的专业护理报告撰写助手。请用中文输出一份简洁、可信、适合展示给宠物主的寄养总结报告。",
                buildBoardingPrompt(pet, keeper, order, orderId, careRecords));
        // AI 内容优先，空则用模板
        report.setContent_wsh(useAiOrFallback(aiContent, fallback));
        report.setCreated_at_wsh(LocalDateTime.now());
        saveReport(report);
        return report;
    }

    /**
     * 业务作用：补齐创建时间后写入 MySQL，并尽力同步到 Chroma。
     * 业务规则：Chroma 只是检索副本，写失败不影响主记录。
     * 异常情况：MySQL 异常向上抛出；Chroma 异常被捕获并记 warn。
     * 注意事项：因此 Chroma 与 MySQL 可能出现数据不一致，属预期降级。
     */
    private void saveReport(AiReport report) {
        // 没给时间就补当前时间
        if (report.getCreated_at_wsh() == null) {
            report.setCreated_at_wsh(LocalDateTime.now());
        }
        aiReportMapper.insert(report);
        try {
            // 向量库失败不影响主记录
            storeInChroma(report);
        } catch (Exception e) {
            log.warn("AI报告 {} 写入Chroma失败，已保留数据库记录: {}", report.getId_wsh(), e.getMessage());
        }
    }

    /**
     * 业务作用：把报告内容与元数据写入 Chroma，供后续向量检索。
     * 业务规则：ID 统一以字符串存入 metadata。
     * 异常情况：调用失败异常向上抛，由 saveReport 捕获。
     * 注意事项：向量仅为满足 Chroma add 的必填字段，报告一律按 metadata 精确取回，不做语义检索。
     */
    private String storeInChroma(AiReport report) {
        // 没有主键时生成一个用于文档 ID
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

        chromaService.add(getCollectionId(),
                List.of(docId),
                List.of(STORAGE_VECTOR),
                List.of(report.getContent_wsh()),
                List.of(metadata));
        log.info("AI报告 {} 已存储到Chroma", docId);
        return docId;
    }

    /**
     * 业务作用：把 Chroma 查询结果转换为 AiReport 列表并按创建时间倒序。
     * 业务规则：ids 为空直接返回空列表；时间缺失的排最后。
     * 异常情况：解析异常被逐条捕获，不中断整体转换。
     * 注意事项：返回的实体不是数据库记录，没有持久化主键语义。
     */
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
                report.setOrder_id_wsh(parseLongSafely(meta.get("order_id_wsh")));
                report.setPet_id_wsh(parseLongSafely(meta.get("pet_id_wsh")));
                report.setKeeper_id_wsh(parseLongSafely(meta.get("keeper_id_wsh")));
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
        // 按创建时间倒序，时间缺失的排最后
        reports.sort((a, b) -> {
            if (a.getCreated_at_wsh() == null && b.getCreated_at_wsh() == null) return 0;
            if (a.getCreated_at_wsh() == null) return 1;
            if (b.getCreated_at_wsh() == null) return -1;
            return b.getCreated_at_wsh().compareTo(a.getCreated_at_wsh());
        });
        return reports;
    }

    /**
     * 业务作用：为无法从数据库拿到主键的报告生成一个正数 ID。
     * 业务规则：结果恒为正数。
     * 注意事项：与数据库自增主键可能冲突，仅作为向量库文档标识使用。
     */
    private static Long generateId() {
        long id = UUID.randomUUID().getLeastSignificantBits();
        return id == Long.MIN_VALUE ? 1L : Math.abs(id); // 避免取绝对值溢出
    }

    /**
     * 业务作用：把向量库元数据中的任意值解析为 Long，异常一律归零。
     * 业务规则：不抛异常，保证转换过程不中断。
     * 异常情况：NumberFormatException 被吞掉。
     * 注意事项：返回 0 表示无效 ID，与真实 ID 0 无法区分。
     */
    private static Long parseLongSafely(Object rawValue) {
        if (rawValue == null) return 0L;
        String value = String.valueOf(rawValue);
        // 无效值统一按 0 处理
        if (value.isBlank() || "0".equals(value) || "null".equals(value)) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 业务作用：按 ID 查询宠物档案，ID 非法时直接返回 null。
     * 业务规则：查不到返回 null，由调用方按无档案处理。
     * 注意事项：返回 null 时提示词会退化为通用描述。
     */
    private Pet findPet(Long petId) {
        return petId == null || petId <= 0 ? null : petMapper.selectById(petId); // 非法 ID 不查库
    }

    /**
     * 业务作用：按 ID 查询看护者记录，ID 非法时直接返回 null。
     * 业务规则：查不到返回 null，提示词中显示未指定。
     */
    private Keeper findKeeper(Long keeperId) {
        return keeperId == null || keeperId <= 0 ? null : keeperMapper.selectById(keeperId);
    }

    /**
     * 业务作用：按 ID 查询订单，ID 非法时直接返回 null。
     * 业务规则：查不到返回 null。
     * 注意事项：调用方需自行区分不存在与无权限。
     */
    private PetOrder findOrder(Long orderId) {
        return orderId == null || orderId <= 0 ? null : orderMapper.selectById(orderId);
    }

    /**
     * 业务作用：判断当前用户是否有权为指定订单或宠物创建报告。
     * 数据处理：请求体为空直接 400；管理员放行；有订单按订单鉴权，否则按宠物鉴权。
     * 业务规则：订单优先于宠物，两者都不存在时按宠物鉴权失败处理。
     * 异常情况：请求体为空 → 400；无权限 → 403；订单或宠物不存在 → 404。
     * 注意事项：未校验内容字段是否为空，空内容也会被写入。
     */
    private void requireReportWriteAccess(Long userId, AiReportCreateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(400, "报告内容不能为空");
        }
        // 管理员直接放行
        if (isAdmin(userId)) {
            return;
        }
        PetOrder order = findOrder(request.getOrder_id_wsh());
        if (order != null) {
            requireOrderAccess(userId, order);
            return;
        }
        requirePetAccess(userId, request.getPet_id_wsh());
    }

    /**
     * 业务作用：有订单时按订单鉴权，否则退化为按宠物鉴权。
     * 业务规则：订单鉴权覆盖宠物主、看护者、商家与管理员。
     * 异常情况：无权限 → 403；资源不存在 → 404。
     * 注意事项：宠物维度的鉴权只允许宠物主与管理员。
     */
    private void requireAccess(Long userId, PetOrder order, Long petId) {
        if (order != null) {
            requireOrderAccess(userId, order);
            return;
        }
        requirePetAccess(userId, petId);
    }

    /**
     * 业务作用：先校验订单 ID 合法与订单存在，再委托实体级鉴权。
     * 业务规则：订单必须存在才继续鉴权。
     * 异常情况：ID 非法 → 400；订单不存在 → 404；无权限 → 403。
     * 注意事项：多一次数据库查询，与实体级重载配合使用。
     */
    private void requireOrderAccess(Long userId, Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(400, "订单ID不能为空");
        }
        PetOrder order = findOrder(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        requireOrderAccess(userId, order);
    }

    /**
     * 业务作用：判断用户是否为该订单的宠物主、看护者、商家或管理员。
     * 业务规则：四种身份任一命中即放行，否则拒绝。
     * 异常情况：用户或订单为空 → 403；全部不匹配 → 403。
     * 注意事项：看护者与商家都需要额外查询，单次鉴权最多三次查库。
     */
    private void requireOrderAccess(Long userId, PetOrder order) {
        if (userId == null || order == null) {
            throw new BusinessException(403, "无权访问此报告");
        }
        // 管理员或宠物主放行
        if (isAdmin(userId) || userId.equals(order.getOwner_id_wsh())) {
            return;
        }
        // 接单看护者放行
        if (order.getKeeper_id_wsh() != null) {
            List<Keeper> keepers = keeperMapper.selectList(
                    new LambdaQueryWrapper<Keeper>().eq(Keeper::getUser_id_wsh, userId));
            if (keepers.stream().anyMatch(k -> order.getKeeper_id_wsh().equals(k.getId_wsh()))) {
                return;
            }
        }
        // 归属商家放行
        if (order.getMerchant_id_wsh() != null) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUser_id_wsh, userId).last("LIMIT 1"));
            if (merchant != null && order.getMerchant_id_wsh().equals(merchant.getId_wsh())) {
                return;
            }
        }
        throw new BusinessException(403, "无权访问此报告");
    }

    /**
     * 业务作用：判断用户是否为该宠物的主人或管理员。
     * 数据处理：ID 非法直接拒绝；管理员放行；否则比对宠物主。
     * 业务规则：仅宠物主与管理员可访问。
     * 异常情况：ID 非法 → 400；宠物不存在 → 404；非本人 → 403。
     */
    private void requirePetAccess(Long userId, Long petId) {
        if (petId == null || petId <= 0) {
            throw new BusinessException(400, "宠物ID不能为空");
        }
        if (isAdmin(userId)) {
            return;
        }
        Pet pet = findPet(petId);
        if (pet == null) {
            throw new BusinessException(404, "宠物不存在");
        }
        // 非宠物主一律拒绝
        if (!userId.equals(pet.getOwner_id_wsh())) {
            throw new BusinessException(403, "无权访问此报告");
        }
    }

    /**
     * 业务作用：按用户 ID 查询角色编码并判断是否包含 ADMIN。
     * 业务规则：查询异常时按非管理员处理，保持拒绝优先。
     * 异常情况：查询异常被捕获并记 warn，返回 false。
     * 注意事项：每次鉴权都会查一次角色表，高频接口可考虑缓存。
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            // 查角色失败按非管理员处理
            List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
            return roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
        } catch (Exception e) {
            log.warn("查询用户 {} 角色失败: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 业务作用：取订单下最近的护理记录，作为报告生成上下文。
     * 业务规则：订单 ID 非法时返回空列表，不查库。
     * 注意事项：限制 30 条是为了控制提示词长度与成本。
     */
    private List<CareRecord> findCareRecords(Long orderId) {
        if (orderId == null || orderId <= 0) return List.of();
        return careRecordMapper.selectList(
                new LambdaQueryWrapper<CareRecord>()
                        .eq(CareRecord::getOrder_id_wsh, orderId)
                        .orderByAsc(CareRecord::getRecord_time_wsh)
                        .last("LIMIT 30")); // 最多 30 条，控制提示词长度
    }

    /**
     * 业务作用：把可能为 null 的 ID 统一转成 0。
     * 注意事项：数据库中 0 表示未关联，与真实主键不会冲突。
     */
    private Long safeId(Long id) {
        return id == null ? 0L : id;
    }

    /**
     * 业务作用：文本为空或空白时返回兜底文案。
     * 业务规则：只判断空白，不做格式处理。
     * 注意事项：fallback 由调用方保证非空。
     */
    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 业务作用：AI 返回有效内容时使用 AI 结果，否则使用模板内容。
     * 数据处理：AI 内容非空白时 trim 后返回，否则返回模板。
     * 业务规则：AI 不可用不影响报告生成，保证功能可用。
     * 注意事项：无法区分 AI 返回空与调用失败，两者都走模板。
     */
    private String useAiOrFallback(String aiContent, String fallback) {
        // AI 返回空白时退回模板
        return aiContent == null || aiContent.isBlank() ? fallback : aiContent.trim();
    }

    /**
     * 业务作用：宠物档案存在时返回其名称，否则返回带 ID 的占位描述。
     * 注意事项：占位描述会出现在给用户看的报告标题中。
     */
    private String petName(Pet pet, Long petId) {
        return pet != null ? pet.getName_wsh() : "宠物ID " + safeId(petId);
    }

    /**
     * 业务作用：把宠物档案、看护者、计费数量与护理动态拼成结构化提示词。
     * 业务规则：要求输出 800 字以内，覆盖喂养、活动、卫生、风险与异常处理。
     * 注意事项：护理动态最多 30 条，过长会被模型截断。
     */
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
                + "计费数量: " + (order != null && order.getDays_wsh() != null ? order.getDays_wsh() : "未知") + "\n"
                + "已上传护理动态:\n" + formatCareRecords(careRecords) + "\n"
                + "请包含：日常喂养、活动安排、卫生护理、风险提醒、异常处理。";
    }

    /**
     * 业务作用：把订单状态、周期、宠物与护理动态拼成寄养总结提示词。
     * 业务规则：要求输出 800 字以内，覆盖概况、饮食、活动、健康与回家建议。
     * 注意事项：订单字段大量使用三元表达式兜底，需与订单实体字段保持一致。
     */
    private String buildBoardingPrompt(Pet pet, Keeper keeper, PetOrder order, Long orderId, List<CareRecord> careRecords) {
        return "请生成一份宠物寄养总结报告，控制在800字以内。\n"
                + "订单ID: " + safeId(orderId) + (order == null ? "（系统未找到该订单，请按通用寄养完成场景生成）" : "") + "\n"
                + "订单状态: " + (order != null ? valueOrDefault(order.getStatus_wsh(), "未知") : "未知") + "\n"
                + "计费数量: " + (order != null && order.getDays_wsh() != null ? order.getDays_wsh() : "未知") + "\n"
                + "开始日期: " + (order != null ? order.getStart_date_wsh() : "未知") + "\n"
                + "结束日期: " + (order != null ? order.getEnd_date_wsh() : "未知") + "\n"
                + "宠物: " + petName(pet, pet != null ? pet.getId_wsh() : null) + "\n"
                + "类型: " + (pet != null ? valueOrDefault(pet.getType_wsh(), "未知") : "未知") + "\n"
                + "看护员: " + (keeper != null ? keeper.getName_wsh() : "未指定") + "\n"
                + "护理动态:\n" + formatCareRecords(careRecords) + "\n"
                + "请包含：寄养概况、饮食状态、活动状态、健康观察、回家后建议。";
    }

    /**
     * 业务作用：AI 不可用时输出固定结构的护理建议文本。
     * 业务规则：固定五段式结构：喂养、活动、卫生、风险、异常处理。
     * 注意事项：模板文案不含 AI 生成内容，用户可感知降级。
     */
    private String buildCareSuggestion(Pet pet, Keeper keeper, PetOrder order, Long petId, List<CareRecord> careRecords) {
        String petName = petName(pet, petId);
        String petType = pet != null ? valueOrDefault(pet.getType_wsh(), "宠物") : "宠物";
        String allergies = pet != null ? valueOrDefault(pet.getAllergies_wsh(), "无明确过敏信息") : "系统未找到宠物档案，请先按通用护理标准执行";
        String habits = pet != null ? valueOrDefault(pet.getHabits_wsh(), "无特殊习惯记录") : "未知";
        String keeperName = keeper != null ? keeper.getName_wsh() : "未指定看护员";
        String days = order != null && order.getDays_wsh() != null ? "计费数量 " + order.getDays_wsh() : "按实际服务周期";

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

    /**
     * 业务作用：AI 不可用时输出固定结构的寄养总结文本。
     * 业务规则：包含寄养概况、动态摘要与回家后建议。
     * 注意事项：模板文案固定，不随护理动态内容变化。
     */
    private String buildBoardingReport(Pet pet, Keeper keeper, PetOrder order, Long orderId, List<CareRecord> careRecords) {
        String petName = petName(pet, pet != null ? pet.getId_wsh() : null);
        String petType = pet != null ? valueOrDefault(pet.getType_wsh(), "宠物") : "宠物";
        String keeperName = keeper != null ? keeper.getName_wsh() : "未指定看护员";
        String days = order != null && order.getDays_wsh() != null ? "计费数量 " + order.getDays_wsh() : "本次";
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

    /**
     * 业务作用：把护理记录列表渲染为逐行的文本摘要。
     * 业务规则：空列表返回暂无看护动态记录。
     * 注意事项：时间缺失时输出未记录时间。
     */
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
