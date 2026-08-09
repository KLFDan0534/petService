package com.pet.ai.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.ai.dto.AskResult;
import com.pet.ai.dto.KnowledgeDocumentDTO;
import com.pet.ai.dto.RagDocumentCreateRequestDTO;
import com.pet.ai.entity.KnowledgeDocument;
import com.pet.ai.service.RagService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.pet.ai.dto.AskRequestDTO;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "【AI】知识库", description = "RAG知识文档管理和AI问答（普通用户/管理员使用）")
@Slf4j
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 【业务名称】知识文档列表（接口）
     * <p>业务作用：获取Chroma向量库中所有知识文档列表。</p>
     * <p>调用场景：前端知识库管理页面展示全部文档。</p>
     * <p>调用链：前端GET /api/rag/documents → listDocuments() → RagService.listAll() → ChromaService.get() → toDTO()</p>
     * <p>数据处理：委托service查询全量文档；实体转DTO。</p>
     * <p>业务规则：无需登录（公开接口），文档按创建时间倒序排列。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：Chroma不可用时返回空列表。</p>
     * <p>注意事项：接口权限为公开，无需鉴权。</p>
     */
    @GetMapping("/documents")
    @Operation(summary = "文档列表", description = "获取所有知识文档")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KnowledgeDocumentDTO>> listDocuments() {
        log.info("调用 listDocuments()");
        List<KnowledgeDocument> list = ragService.listAll();
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【业务名称】搜索知识文档（接口）
     * <p>业务作用：根据关键词和分类搜索知识文档，双阶段检索：向量搜索优先，Term搜索降级。</p>
     * <p>调用场景：用户在知识库页面搜索框输入关键词搜索。</p>
     * <p>调用链：前端GET /api/rag/search → search() → RagService.search() → 向量搜索/Term搜索 → toDTO()</p>
     * <p>数据处理：query和category参数从Query String获取；委托service双阶段检索；实体转DTO。</p>
     * <p>业务规则：公开接口无需登录；query必填；category可选。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：query为空返回400错误。</p>
     * <p>注意事项：category过滤在检索前执行。</p>
     */
    @GetMapping("/search")
    @Operation(summary = "搜索知识", description = "通过分类过滤查找文档")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KnowledgeDocumentDTO>> search(@RequestParam String query,
            @RequestParam(required = false) String category) {
        log.info("调用 search()");
        List<KnowledgeDocument> list = ragService.search(query, category);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【业务名称】AI知识库问答（接口）
     * <p>业务作用：基于知识库内容进行AI问答，支持携带宠物档案信息以获取个性化回答。</p>
     * <p>调用场景：用户在知识库页面提问，可关联宠物档案。</p>
     * <p>调用链：前端POST /api/rag/ask → ask() → RagService.answer(question, petProfile) → 检索+AI问答 → 返回AskResult</p>
     * <p>数据处理：从请求DTO解析question和pet_profile_wsh；委托service检索+AI生成；封装AskResult返回。</p>
     * <p>业务规则：公开接口无需登录；请求体需通过@Valid校验。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：参数校验失败返回400。</p>
     * <p>注意事项：AI不可用时自动降级为知识库模板或宠物档案模板。</p>
     */
    @PostMapping("/ask")
    @Operation(summary = "AI问答", description = "基于知识文档进行问答")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AskResult> ask(@Valid @RequestBody AskRequestDTO request) {
        log.info("调用 ask()");
        String question = request.getQuestion_wsh();
        String petProfile = request.getPet_profile_wsh();
        String answer = ragService.answer(question, petProfile);
        return Result.success(new AskResult(question, answer));
    }

    /**
     * 【业务名称】创建知识文档（接口）
     * <p>业务作用：管理员新增知识文档，标题和内容保存到Chroma向量库并生成向量嵌入。</p>
     * <p>调用场景：管理员在后台知识库管理页面手动添加文档。</p>
     * <p>调用链：前端POST /api/rag/documents → create() → RagService.create() → HTML转义 → Chroma存储 → toDTO()</p>
     * <p>数据处理：接收请求DTO；委托service创建（含HTML转义）；实体转DTO。</p>
     * <p>业务规则：需要ADMIN角色权限；请求体需通过@Valid校验。</p>
     * <p>状态影响：新增一条知识文档记录。</p>
     * <p>异常情况：参数错误返回400；未登录返回401；非ADMIN返回403。</p>
     * <p>注意事项：标题和内容会自动HTML转义防止XSS。</p>
     */
    @PostMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建文档", description = "新增知识文档")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KnowledgeDocumentDTO> create(@Valid @RequestBody RagDocumentCreateRequestDTO request) {
        log.info("调用 create()");
        return Result.success(toDTO(ragService.create(request)));
    }

    /**
     * 【业务名称】删除知识文档（接口）
     * <p>业务作用：管理员根据ID从Chroma向量库中删除知识文档。</p>
     * <p>调用场景：管理员在后台知识库管理页面删除文档。</p>
     * <p>调用链：前端DELETE /api/rag/documents/{id} → delete() → RagService.delete(id) → ChromaService.delete()</p>
     * <p>数据处理：ID通过路径变量传入；委托service执行删除。</p>
     * <p>业务规则：需要ADMIN角色权限。</p>
     * <p>状态影响：从Chroma中删除一条文档记录。</p>
     * <p>异常情况：未登录返回401；非ADMIN返回403；ID不存在时返回200（幂等）。</p>
     * <p>注意事项：删除后不可恢复。</p>
     */
    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除文档", description = "删除知识文档")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "文档ID") @PathVariable Long id) {
        log.info("调用 delete()");
        ragService.delete(id);
        return Result.success();
    }

    /**
     * 【业务名称】上传文件导入知识文档（接口）
     * <p>业务作用：上传.txt或.docx文件，自动提取文本内容创建知识文档，文件名作为默认标题。</p>
     * <p>调用场景：管理员在后台批量导入知识文档文件。</p>
     * <p>调用链：前端POST /api/rag/documents/upload → uploadDocument() → 校验文件格式 → RagService.createFromFile() → Chroma存储 → toDTO()</p>
     * <p>数据处理：从MultipartFile获取文件名和字节数据；前置校验文件格式和文件名；委托service创建。</p>
     * <p>业务规则：需要ADMIN角色权限；仅支持.txt和.docx格式；文件名不能为空。</p>
     * <p>状态影响：新增一条知识文档记录。</p>
     * <p>异常情况：文件格式不支持返回400；文件名空返回400；未登录返回401；非ADMIN返回403。</p>
     * <p>注意事项：文件内容不做HTML转义（信任上传来源）；大文件上传注意服务器内存限制。</p>
     */
    @PostMapping("/documents/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "上传文档文件", description = "上传 txt/docx 文件并自动提取文本创建知识文档")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KnowledgeDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) throws IOException {
        log.info("调用 uploadDocument(): fileName={}, title={}, category={}", file.getOriginalFilename(), title, category);
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return Result.error(400, "文件名不能为空");
        }
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".txt") && !lower.endsWith(".docx")) {
            return Result.error(400, "不支持的文件格式，仅支持 .txt 和 .docx 文件");
        }
        KnowledgeDocument doc = ragService.createFromFile(fileName, file.getBytes(), title, category);
        return Result.success(toDTO(doc));
    }

    /**
     * 【业务名称】知识文档实体转DTO
     * <p>业务作用：将KnowledgeDocument实体转换为前端展示的KnowledgeDocumentDTO。</p>
     * <p>注意事项：字段一一映射，不涉及数据转换逻辑。</p>
     */
    private KnowledgeDocumentDTO toDTO(KnowledgeDocument entity) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setTitle_wsh(entity.getTitle_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setCategory_wsh(entity.getCategory_wsh());
        dto.setSource_type_wsh(entity.getSource_type_wsh());
        dto.setSource_path_wsh(entity.getSource_path_wsh());
        dto.setWord_count_wsh(entity.getWord_count_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
