package com.pet.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.ai.dto.AiChatResponseDTO;
import com.pet.ai.dto.RagAskRequestDTO;
import com.pet.ai.dto.RagDocumentDTO;
import com.pet.ai.dto.RagDocumentUpsertRequestDTO;
import com.pet.ai.service.AiChatService;
import com.pet.ai.service.RagService;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "【知识库】RAG 管理", description = "知识库文档增删改查、检索与 AI 问答")
public class RagController {

    private final RagService ragService;
    private final AiChatService aiChatService;

    /**
     * 【构造方法】注入知识库与 AI 问答服务
     *
     * 业务作用：由 Spring 注入 RagService 与 AiChatService，供知识库管理与问答接口使用。
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段。
     */
    public RagController(RagService ragService, AiChatService aiChatService) {
        this.ragService = ragService;
        this.aiChatService = aiChatService;
    }

    /**
     * 【分页查询知识库文档】按关键字与分类筛选文档
     *
     * 业务作用：对外暴露 GET /api/rag/documents，返回分页的知识库文档列表。
     * 业务规则：仅 ADMIN、MERCHANT、CUSTOMER_SERVICE 可访问（@PreAuthorize hasAnyRole）。
     * 异常情况：未登录或无角色时由 Spring Security 抛出 403；查询异常向上传播。
     * 注意事项：page/size 为 long 型且未做上限校验，传超大 size 可能导致大结果集。
     */
    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "分页查询知识库文档", description = "支持关键字与分类筛选")
    public Result<PageResult<RagDocumentDTO>> listDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        IPage<RagDocumentDTO> result = ragService.listDocuments(query, category, page, size);
        return Result.success(new PageResult<>(result));
    }

    /**
     * 【查询文档详情】按ID返回单篇知识库文档
     *
     * 业务作用：对外暴露 GET /api/rag/documents/{id}。
     * 业务规则：仅 ADMIN、MERCHANT、CUSTOMER_SERVICE 可访问。
     * 异常情况：文档不存在时由服务层抛业务异常；无角色 403。
     * 注意事项：本方法未做归属校验，任何具备上述角色的用户都能读取任意文档。
     */
    @GetMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "查询知识库文档详情")
    public Result<RagDocumentDTO> getDocument(@PathVariable @Parameter(description = "文档ID") Long id) {
        return Result.success(ragService.getDocument(id));
    }

    /**
     * 【新增知识库文档】以 JSON 请求体创建文档并向量化
     *
     * 业务作用：对外暴露 POST /api/rag/documents。
     * 业务规则：仅 ADMIN 可调用。
     * 异常情况：参数校验失败 400；无权限 403。
     * 注意事项：只有管理员可写，商家与客服仅可读。
     */
    @PostMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "新增知识库文档")
    public Result<RagDocumentDTO> createDocument(@Valid @RequestBody RagDocumentUpsertRequestDTO request) {
        return Result.success(ragService.createDocument(request));
    }

    /**
     * 【上传文档文件】接收文件并提取文本入库
     *
     * 业务作用：对外暴露 POST /api/rag/documents/upload，把上传的 txt/docx 转成知识文档。
     * 业务规则：仅 ADMIN；consumes 限定 multipart/form-data。
     * 异常情况：文件读取失败抛 IOException（方法签名声明 throws IOException，非业务异常）；缺文件 400；无权限 403。
     * 注意事项：文件内容通过 getBytes 一次性读入内存，大文件会占用较多堆内存；IOException 未被包装为 BusinessException，返回给前端的状态码取决于全局异常处理器。
     */
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "上传文档文件", description = "上传 txt/docx 文件并自动提取文本创建知识文档")
    public Result<RagDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) throws IOException {
        return Result.success(ragService.createFromFile(file.getOriginalFilename(), file.getBytes(), title, category));
    }

    /**
     * 【编辑知识库文档】更新文档内容并重建向量
     *
     * 业务作用：对外暴露 PUT /api/rag/documents/{id}。
     * 业务规则：仅 ADMIN；请求体经 @Valid 校验。
     * 异常情况：文档不存在抛业务异常；参数非法 400；无权限 403。
     */
    @PutMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "编辑知识库文档")
    public Result<RagDocumentDTO> updateDocument(
            @PathVariable @Parameter(description = "文档ID") Long id,
            @Valid @RequestBody RagDocumentUpsertRequestDTO request) {
        return Result.success(ragService.updateDocument(id, request));
    }

    /**
     * 【删除知识库文档】按ID删除文档
     *
     * 业务作用：对外暴露 DELETE /api/rag/documents/{id}。
     * 业务规则：仅 ADMIN。
     * 异常情况：文档不存在抛业务异常；无权限 403。
     * 注意事项：返回 Result<Void>，无业务数据体。
     */
    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除知识库文档")
    public Result<Void> deleteDocument(@PathVariable @Parameter(description = "文档ID") Long id) {
        ragService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 【检索知识库】按相关性返回匹配文档
     *
     * 业务作用：对外暴露 GET /api/rag/search，供知识库页面做关键词/语义检索。
     * 业务规则：仅需登录（isAuthenticated），不限角色。
     * 异常情况：未登录 401；query 缺失时 Spring 抛 400；检索异常向上传播。
     * 注意事项：limit 为 int 且无上限校验，与 listDocuments 的 long 型 size 不一致。
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检索知识库文档", description = "按相关性检索，供知识库页面展示")
    public Result<List<RagDocumentDTO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(ragService.search(query, limit));
    }

    /**
     * 【知识库问答】检索知识库并由 AI 生成回答
     *
     * 业务作用：对外暴露 POST /api/rag/ask，兼容 AI 助手页的问答入口。
     * 业务规则：仅需登录；请求体经 @Valid 校验。
     * 异常情况：未登录 401；问题为空 400；AI 调用失败由服务层抛出。
     * 注意事项：userId 会传入服务层，可能用于按用户维度记录或限流，具体行为在 AiChatService 内。
     */
    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "知识库问答", description = "检索知识库并由 AI 回答（兼容 AI 助手页）")
    public Result<AiChatResponseDTO> ask(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody RagAskRequestDTO request) {
        return Result.success(aiChatService.ask(request.getQuestion_wsh(), token.getUserId()));
    }
}
