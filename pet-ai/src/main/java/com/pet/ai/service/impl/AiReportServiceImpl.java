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
    private static final int EMBEDDING_DIM = 4;
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
     * 【AI 报告服务构造器】
     *
     * 业务作用：注入向量库、宠物、看护者、商家、订单、护理动态 Mapper 以及对话服务与报告 Mapper。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 构造器 → 逐字段赋值。
     *
     * 数据处理：仅字段赋值，collectionId 保持 null 延迟初始化。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
     * 【获取向量集合 ID】
     *
     * 业务作用：懒加载 Chroma 集合 ID，避免每次请求都远程创建或查询集合。
     *
     * 调用场景：读写 Chroma 前统一调用。
     *
     * 调用链：getReportsByOrder / storeInChroma → getCollectionId → chromaService.getOrCreateCollection。
     *
     * 数据处理：首次调用时把返回值缓存到实例字段 collectionId。
     *
     * 业务规则：集合名固定为 ai_reports。
     *
     * 状态影响：首次调用会写实例字段。
     *
     * 异常情况：Chroma 不可用时异常向上抛出。
     *
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
     * 【业务名称】按订单查询AI报告实现
     * <p>业务作用：根据订单ID查询AI报告列表，先查MySQL（AiReportMapper），未命中时降级到Chroma向量库按order_id_wsh元数据查询。</p>
     * <p>调用场景：用户在前端查看某订单的AI报告列表。</p>
     * <p>调用链：Controller → getReportsByOrder() → requireOrderAccess()鉴权 → MySQL查询 → 有数据直接返回 | 无数据 → ChromaService.get() → toAiReports()转换</p>
     * <p>数据处理：safeId()处理null→0L；LambdaQueryWrapper按order_id_wsh匹配+created_at_wsh倒序；Chroma查询设置where条件为order_id_wsh。</p>
     * <p>业务规则：MySQL数据优先；Chroma查询结果通过toAiReports()解析并按创建时间倒序。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：requireOrderAccess内部可能抛出400/404/403异常。</p>
     * <p>注意事项：Chroma中order_id_wsh以字符串形式存储于metadata。</p>
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
     * 【业务名称】按宠物查询AI报告实现
     * <p>业务作用：根据宠物ID查询AI报告列表，优先MySQL，未命中时从Chroma按pet_id_wsh查询。</p>
     * <p>调用场景：用户在宠物详情页查看所有历史AI报告。</p>
     * <p>调用链：Controller → getReportsByPet() → requirePetAccess()鉴权 → MySQL查询 → 有数据直接返回 | 无数据 → ChromaService.get() → toAiReports()</p>
     * <p>数据处理：LambdaQueryWrapper按pet_id_wsh过滤+创建时间倒序；Chroma查询设置where条件pet_id_wsh。</p>
     * <p>业务规则：仅宠物主可查看；MySQL > Chroma 降级。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：requirePetAccess抛出400/404/403。</p>
     * <p>注意事项：Chroma中pet_id_wsh以字符串形式存储。</p>
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
     * 【业务名称】手动创建AI报告实现
     * <p>业务作用：看护人/管理员直接提交报告内容，绕过AI模型，手动创建一条AI报告记录。</p>
     * <p>调用场景：看护人完成线下服务后手动录入总结报告。</p>
     * <p>调用链：Controller → createReport() → requireReportWriteAccess()鉴权 → 构造AiReport → saveReport() → MySQL插入 + Chroma存储</p>
     * <p>数据处理：从request提取order_id_wsh/pet_id_wsh/keeper_id_wsh/content_wsh/type_wsh设入新AiReport实体。</p>
     * <p>业务规则：ADMIN或关联订单/宠物有写权限；不调用AI。</p>
     * <p>状态影响：新增一条AI报告（MySQL+Chroma）。</p>
     * <p>异常情况：request为null或内容为空抛出400；无权限抛出403。</p>
     * <p>注意事项：Chroma存储失败不影响MySQL主记录，仅warn日志。</p>
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
     * 【业务名称】AI生成护理建议报告实现
     * <p>业务作用：查询宠物/看护人/订单/护理动态数据，构建AI提示词，调用AiChatService生成护理建议，AI不可用时降级为模板填充。</p>
     * <p>调用场景：用户点击"生成护理建议"按钮。</p>
     * <p>调用链：Controller → generateCareSuggestion() → 参数校验 → requireAccess()鉴权 → 查询Pet/Keeper/Order/CareRecord → 构建Prompt → LlmChatService.chat() → useAiOrFallback() → saveReport()</p>
     * <p>数据处理：order推导petId/keeperId；findPet/findKeeper/findOrder/findCareRecords查询关联数据；buildCarePrompt构建AI提示词；buildCareSuggestion构建降级模板；AI内容优先于模板。</p>
     * <p>业务规则：petId或orderId至少提供一个；type固定为"care"；AI返回null/blank时使用模板。</p>
     * <p>状态影响：新增一条type=care的AI报告。</p>
     * <p>异常情况：petId和orderId均为null抛出400；orderId指定但order不存在抛出404；鉴权失败抛出403。</p>
     * <p>注意事项：护理动态最多取最近30条（LIMIT 30）；AI提示词限定800字以内。</p>
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
     * 【业务名称】AI生成寄养总结报告（用户调用）
     * <p>业务作用：用户触发生成寄养总结报告，校验订单存在性和用户权限，委托generateBoardingReportUnchecked执行核心逻辑。</p>
     * <p>调用场景：用户在完成订单后点击"查看寄养总结"。</p>
     * <p>调用链：Controller → generateBoardingReport() → 校验orderId → findOrder() → requireOrderAccess()鉴权 → generateBoardingReportUnchecked()</p>
     * <p>数据处理：校验orderId不为null；查询PetOrder；校验用户对该订单的访问权限；委托无校验方法执行。</p>
     * <p>业务规则：orderId必填；仅宠物主、看护人、商家和ADMIN可访问。</p>
     * <p>状态影响：新增一条type=final的AI报告。</p>
     * <p>异常情况：orderId为null抛出400；订单不存在抛出404；无权限抛出403。</p>
     * <p>注意事项：校验通过后实际处理在generateBoardingReportUnchecked中。</p>
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
     * 【业务名称】内部AI生成寄养总结报告（事件驱动）
     * <p>业务作用：订单完成事件触发后内部调用，不校验用户权限，直接生成寄养总结报告。</p>
     * <p>调用场景：OrderCompletedReportListener在事务提交后异步调用。</p>
     * <p>调用链：OrderCompletedReportListener → generateBoardingReportInternal() → 校验orderId → generateBoardingReportUnchecked()</p>
     * <p>数据处理：校验orderId；不鉴权直接委托。</p>
     * <p>业务规则：orderId必填；无鉴权。</p>
     * <p>状态影响：新增一条type=final的AI报告。</p>
     * <p>异常情况：orderId为null抛出400；订单不存在抛出404。</p>
     * <p>注意事项：内部调用信任所有请求；监听器层面捕获异常不会传播到主事务。</p>
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
     * 【生成寄养总结（无鉴权）】
     *
     * 业务作用：真正执行寄养总结生成的内部方法，不做用户权限校验。
     *
     * 调用场景：generateBoardingReport 鉴权通过后调用；generateBoardingReportInternal 事件驱动时直接调用。
     *
     * 调用链：generateBoardingReport / generateBoardingReportInternal → generateBoardingReportUnchecked → 取数 → 构建提示词 → LlmChatService.chat → saveReport。
     *
     * 数据处理：petId、keeperId 为空时从订单补齐；护理动态取最近 30 条；AI 返回为空时使用模板。
     *
     * 业务规则：type 固定为 final；AI 内容优先，模板兜底。
     *
     * 状态影响：新增一条 final 类型报告，并尝试写入 Chroma。
     *
     * 异常情况：数据库异常向上抛出；Chroma 失败在 saveReport 内部被吞掉。
     *
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
     * 【保存 AI 报告】
     *
     * 业务作用：补齐创建时间后写入 MySQL，并尽力同步到 Chroma。
     *
     * 调用场景：所有生成或手工创建报告的路径最终都会调用。
     *
     * 调用链：createReport / generateCareSuggestion / generateBoardingReportUnchecked → saveReport → aiReportMapper.insert → storeInChroma。
     *
     * 数据处理：created_at_wsh 为空时补当前时间；插入后回填主键。
     *
     * 业务规则：Chroma 只是检索副本，写失败不影响主记录。
     *
     * 状态影响：新增一条 ai_report 记录；成功时额外写入向量库。
     *
     * 异常情况：MySQL 异常向上抛出；Chroma 异常被捕获并记 warn。
     *
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
     * 【写入向量库】
     *
     * 业务作用：把报告内容与元数据写入 Chroma，供后续向量检索。
     *
     * 调用场景：saveReport 中 MySQL 插入成功后调用。
     *
     * 调用链：saveReport → storeInChroma → 组装 metadata → generateDummyEmbedding → chromaService.add。
     *
     * 数据处理：主键缺失时用 UUID 派生 ID；元数据包含 id、order_id、pet_id、keeper_id、type 与创建时间。
     *
     * 业务规则：ID 统一以字符串存入 metadata。
     *
     * 状态影响：Chroma 中新增一条文档。
     *
     * 异常情况：调用失败异常向上抛，由 saveReport 捕获。
     *
     * 注意事项：使用随机向量而非真实语义向量，检索质量有限。
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

        // 报告不做语义检索，用占位向量满足接口
        float[] embedding = chromaService.generateDummyEmbedding(EMBEDDING_DIM);
        chromaService.add(getCollectionId(),
                List.of(docId),
                List.of(embedding),
                List.of(report.getContent_wsh()),
                List.of(metadata));
        log.info("AI报告 {} 已存储到Chroma", docId);
        return docId;
    }

    /**
     * 【向量结果转报告实体】
     *
     * 业务作用：把 Chroma 查询结果转换为 AiReport 列表并按创建时间倒序。
     *
     * 调用场景：MySQL 未命中时，查询向量库后转换。
     *
     * 调用链：getReportsByOrder / getReportsByPet → toAiReports → 逐条解析 metadata。
     *
     * 数据处理：documents 作为内容，metadatas 还原订单、宠物、看护者、类型与时间；时间解析失败用当前时间。
     *
     * 业务规则：ids 为空直接返回空列表；时间缺失的排最后。
     *
     * 状态影响：无（只读转换）。
     *
     * 异常情况：解析异常被逐条捕获，不中断整体转换。
     *
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
     * 【生成报告 ID】
     *
     * 业务作用：为无法从数据库拿到主键的报告生成一个正数 ID。
     *
     * 调用场景：storeInChroma 中报告主键缺失时调用。
     *
     * 调用链：storeInChroma → generateId → UUID 最低有效位取绝对值。
     *
     * 数据处理：Long.MIN_VALUE 取绝对值会溢出，单独映射为 1。
     *
     * 业务规则：结果恒为正数。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：与数据库自增主键可能冲突，仅作为向量库文档标识使用。
     */
    private static Long generateId() {
        long id = UUID.randomUUID().getLeastSignificantBits();
        return id == Long.MIN_VALUE ? 1L : Math.abs(id); // 避免取绝对值溢出
    }

    /**
     * 【安全解析 Long】
     *
     * 业务作用：把向量库元数据中的任意值解析为 Long，异常一律归零。
     *
     * 调用场景：toAiReports 还原各 ID 字段时调用。
     *
     * 调用链：toAiReports → parseLongSafely → Long.parseLong。
     *
     * 数据处理：null、空白串、字符串 0、字符串 null 均返回 0；解析失败返回 0。
     *
     * 业务规则：不抛异常，保证转换过程不中断。
     *
     * 状态影响：无。
     *
     * 异常情况：NumberFormatException 被吞掉。
     *
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
     * 【查询宠物】
     *
     * 业务作用：按 ID 查询宠物档案，ID 非法时直接返回 null。
     *
     * 调用场景：生成护理建议与寄养总结时取宠物上下文。
     *
     * 调用链：generateCareSuggestion / requirePetAccess → findPet → petMapper.selectById。
     *
     * 数据处理：ID 为 null 或非正数时不查库。
     *
     * 业务规则：查不到返回 null，由调用方按无档案处理。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
     * 注意事项：返回 null 时提示词会退化为通用描述。
     */
    private Pet findPet(Long petId) {
        return petId == null || petId <= 0 ? null : petMapper.selectById(petId); // 非法 ID 不查库
    }

    /**
     * 【查询看护者】
     *
     * 业务作用：按 ID 查询看护者记录，ID 非法时直接返回 null。
     *
     * 调用场景：生成报告时补充看护者姓名。
     *
     * 调用链：generateCareSuggestion / generateBoardingReportUnchecked → findKeeper → keeperMapper.selectById。
     *
     * 数据处理：ID 为 null 或非正数时不查库。
     *
     * 业务规则：查不到返回 null，提示词中显示未指定。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private Keeper findKeeper(Long keeperId) {
        return keeperId == null || keeperId <= 0 ? null : keeperMapper.selectById(keeperId);
    }

    /**
     * 【查询订单】
     *
     * 业务作用：按 ID 查询订单，ID 非法时直接返回 null。
     *
     * 调用场景：鉴权、取订单上下文与校验订单存在性。
     *
     * 调用链：generateBoardingReport / requireOrderAccess → findOrder → orderMapper.selectById。
     *
     * 数据处理：ID 为 null 或非正数时不查库。
     *
     * 业务规则：查不到返回 null。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
     * 注意事项：调用方需自行区分不存在与无权限。
     */
    private PetOrder findOrder(Long orderId) {
        return orderId == null || orderId <= 0 ? null : orderMapper.selectById(orderId);
    }

    /**
     * 【校验报告写权限】
     *
     * 业务作用：判断当前用户是否有权为指定订单或宠物创建报告。
     *
     * 调用场景：createReport 入口处调用。
     *
     * 调用链：createReport → requireReportWriteAccess → isAdmin / requireOrderAccess / requirePetAccess。
     *
     * 数据处理：请求体为空直接 400；管理员放行；有订单按订单鉴权，否则按宠物鉴权。
     *
     * 业务规则：订单优先于宠物，两者都不存在时按宠物鉴权失败处理。
     *
     * 状态影响：无。
     *
     * 异常情况：请求体为空 → 400；无权限 → 403；订单或宠物不存在 → 404。
     *
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
     * 【按订单或宠物鉴权】
     *
     * 业务作用：有订单时按订单鉴权，否则退化为按宠物鉴权。
     *
     * 调用场景：generateCareSuggestion 取数前调用。
     *
     * 调用链：generateCareSuggestion → requireAccess → requireOrderAccess / requirePetAccess。
     *
     * 数据处理：order 非空即走订单分支。
     *
     * 业务规则：订单鉴权覆盖宠物主、看护者、商家与管理员。
     *
     * 状态影响：无。
     *
     * 异常情况：无权限 → 403；资源不存在 → 404。
     *
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
     * 【按订单 ID 鉴权】
     *
     * 业务作用：先校验订单 ID 合法与订单存在，再委托实体级鉴权。
     *
     * 调用场景：getReportsByOrder 与 generateBoardingReport 调用。
     *
     * 调用链：getReportsByOrder / generateBoardingReport → requireOrderAccess(orderId) → findOrder → requireOrderAccess(userId, order)。
     *
     * 数据处理：ID 为空或非正数直接拒绝。
     *
     * 业务规则：订单必须存在才继续鉴权。
     *
     * 状态影响：无。
     *
     * 异常情况：ID 非法 → 400；订单不存在 → 404；无权限 → 403。
     *
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
     * 【按订单实体鉴权】
     *
     * 业务作用：判断用户是否为该订单的宠物主、看护者、商家或管理员。
     *
     * 调用场景：所有需要订单级访问控制的报告接口。
     *
     * 调用链：requireOrderAccess(orderId) / requireAccess / requireReportWriteAccess → 本方法。
     *
     * 数据处理：先判管理员与宠物主，再按 user_id 反查看护者与商家比对。
     *
     * 业务规则：四种身份任一命中即放行，否则拒绝。
     *
     * 状态影响：无。
     *
     * 异常情况：用户或订单为空 → 403；全部不匹配 → 403。
     *
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
     * 【按宠物鉴权】
     *
     * 业务作用：判断用户是否为该宠物的主人或管理员。
     *
     * 调用场景：无订单场景下的报告访问控制。
     *
     * 调用链：requireAccess / requireReportWriteAccess → requirePetAccess → findPet → 比对 owner_id_wsh。
     *
     * 数据处理：ID 非法直接拒绝；管理员放行；否则比对宠物主。
     *
     * 业务规则：仅宠物主与管理员可访问。
     *
     * 状态影响：无。
     *
     * 异常情况：ID 非法 → 400；宠物不存在 → 404；非本人 → 403。
     *
     * 注意事项：无。
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
     * 【判断是否管理员】
     *
     * 业务作用：按用户 ID 查询角色编码并判断是否包含 ADMIN。
     *
     * 调用场景：各鉴权方法的第一道放行判断。
     *
     * 调用链：requireOrderAccess / requirePetAccess / requireReportWriteAccess → isAdmin → roleMapper.selectRoleCodesByUserId。
     *
     * 数据处理：角色编码忽略大小写比较。
     *
     * 业务规则：查询异常时按非管理员处理，保持拒绝优先。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：查询异常被捕获并记 warn，返回 false。
     *
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
     * 【查询护理动态】
     *
     * 业务作用：取订单下最近的护理记录，作为报告生成上下文。
     *
     * 调用场景：生成护理建议与寄养总结时调用。
     *
     * 调用链：generateCareSuggestion / generateBoardingReportUnchecked → findCareRecords → careRecordMapper.selectList。
     *
     * 数据处理：按记录时间升序，最多 30 条。
     *
     * 业务规则：订单 ID 非法时返回空列表，不查库。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
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
     * 【ID 空值归一】
     *
     * 业务作用：把可能为 null 的 ID 统一转成 0。
     *
     * 调用场景：写入报告实体与拼接提示词时使用。
     *
     * 调用链：generateCareSuggestion / storeInChroma / buildBoardingPrompt → safeId。
     *
     * 数据处理：null → 0L，其余原样返回。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：数据库中 0 表示未关联，与真实主键不会冲突。
     */
    private Long safeId(Long id) {
        return id == null ? 0L : id;
    }

    /**
     * 【文本空值兜底】
     *
     * 业务作用：文本为空或空白时返回兜底文案。
     *
     * 调用场景：拼接提示词与模板时对宠物字段做默认值处理。
     *
     * 调用链：buildCarePrompt / buildCareSuggestion 等 → valueOrDefault。
     *
     * 数据处理：null 或空白返回 fallback。
     *
     * 业务规则：只判断空白，不做格式处理。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：fallback 由调用方保证非空。
     */
    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 【AI 内容优先兜底】
     *
     * 业务作用：AI 返回有效内容时使用 AI 结果，否则使用模板内容。
     *
     * 调用场景：生成护理建议与寄养总结赋值内容前调用。
     *
     * 调用链：generateCareSuggestion / generateBoardingReportUnchecked → useAiOrFallback。
     *
     * 数据处理：AI 内容非空白时 trim 后返回，否则返回模板。
     *
     * 业务规则：AI 不可用不影响报告生成，保证功能可用。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无法区分 AI 返回空与调用失败，两者都走模板。
     */
    private String useAiOrFallback(String aiContent, String fallback) {
        // AI 返回空白时退回模板
        return aiContent == null || aiContent.isBlank() ? fallback : aiContent.trim();
    }

    /**
     * 【取宠物展示名】
     *
     * 业务作用：宠物档案存在时返回其名称，否则返回带 ID 的占位描述。
     *
     * 调用场景：拼接提示词与模板标题时调用。
     *
     * 调用链：buildCarePrompt / buildCareSuggestion / buildBoardingReport → petName。
     *
     * 数据处理：pet 为 null 时拼接宠物 ID。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：占位描述会出现在给用户看的报告标题中。
     */
    private String petName(Pet pet, Long petId) {
        return pet != null ? pet.getName_wsh() : "宠物ID " + safeId(petId);
    }

    /**
     * 【构建护理建议提示词】
     *
     * 业务作用：把宠物档案、看护者、订单天数与护理动态拼成结构化提示词。
     *
     * 调用场景：generateCareSuggestion 调用大模型前构造入参。
     *
     * 调用链：generateCareSuggestion → buildCarePrompt → aiChatService.chat。
     *
     * 数据处理：字段缺失时使用未知或无；宠物不存在时提示按通用场景生成。
     *
     * 业务规则：要求输出 800 字以内，覆盖喂养、活动、卫生、风险与异常处理。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
                + "订单天数: " + (order != null && order.getDays_wsh() != null ? order.getDays_wsh() : "未知") + "\n"
                + "已上传护理动态:\n" + formatCareRecords(careRecords) + "\n"
                + "请包含：日常喂养、活动安排、卫生护理、风险提醒、异常处理。";
    }

    /**
     * 【构建寄养总结提示词】
     *
     * 业务作用：把订单状态、周期、宠物与护理动态拼成寄养总结提示词。
     *
     * 调用场景：generateBoardingReportUnchecked 调用大模型前构造入参。
     *
     * 调用链：generateBoardingReportUnchecked → buildBoardingPrompt → aiChatService.chat。
     *
     * 数据处理：订单不存在时在提示词中注明按通用完成场景生成。
     *
     * 业务规则：要求输出 800 字以内，覆盖概况、饮食、活动、健康与回家建议。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：订单字段大量使用三元表达式兜底，需与订单实体字段保持一致。
     */
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

    /**
     * 【生成护理建议模板】
     *
     * 业务作用：AI 不可用时输出固定结构的护理建议文本。
     *
     * 调用场景：generateCareSuggestion 中作为兜底内容。
     *
     * 调用链：generateCareSuggestion → buildCareSuggestion → useAiOrFallback。
     *
     * 数据处理：填充宠物名、类型、看护者、周期、过敏与习惯信息。
     *
     * 业务规则：固定五段式结构：喂养、活动、卫生、风险、异常处理。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：模板文案不含 AI 生成内容，用户可感知降级。
     */
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

    /**
     * 【生成寄养总结模板】
     *
     * 业务作用：AI 不可用时输出固定结构的寄养总结文本。
     *
     * 调用场景：generateBoardingReportUnchecked 中作为兜底内容。
     *
     * 调用链：generateBoardingReportUnchecked → buildBoardingReport → useAiOrFallback。
     *
     * 数据处理：填充订单号、宠物、看护者、状态、周期与动态摘要。
     *
     * 业务规则：包含寄养概况、动态摘要与回家后建议。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：模板文案固定，不随护理动态内容变化。
     */
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

    /**
     * 【格式化护理动态】
     *
     * 业务作用：把护理记录列表渲染为逐行的文本摘要。
     *
     * 调用场景：拼接提示词与模板时调用。
     *
     * 调用链：buildCarePrompt / buildBoardingPrompt / buildCareSuggestion / buildBoardingReport → formatCareRecords。
     *
     * 数据处理：每条输出时间、类型与内容，含图片时追加标记。
     *
     * 业务规则：空列表返回暂无看护动态记录。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
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
