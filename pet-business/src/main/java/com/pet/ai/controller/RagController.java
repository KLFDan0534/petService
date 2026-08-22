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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "【知识库】RAG 管理", description = "知识库文档增删改查、检索与 AI 问答")
public class RagController {

    private final RagService ragService;
    private final AiChatService aiChatService;

    public RagController(RagService ragService, AiChatService aiChatService) {
        this.ragService = ragService;
        this.aiChatService = aiChatService;
    }

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

    @GetMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "查询知识库文档详情")
    public Result<RagDocumentDTO> getDocument(@PathVariable @Parameter(description = "文档ID") Long id) {
        return Result.success(ragService.getDocument(id));
    }

    @PostMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "新增知识库文档")
    public Result<RagDocumentDTO> createDocument(@Valid @RequestBody RagDocumentUpsertRequestDTO request) {
        return Result.success(ragService.createDocument(request));
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "编辑知识库文档")
    public Result<RagDocumentDTO> updateDocument(
            @PathVariable @Parameter(description = "文档ID") Long id,
            @Valid @RequestBody RagDocumentUpsertRequestDTO request) {
        return Result.success(ragService.updateDocument(id, request));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除知识库文档")
    public Result<Void> deleteDocument(@PathVariable @Parameter(description = "文档ID") Long id) {
        ragService.deleteDocument(id);
        return Result.success();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检索知识库文档", description = "按相关性检索，供知识库页面展示")
    public Result<List<RagDocumentDTO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(ragService.search(query, limit));
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "知识库问答", description = "检索知识库并由 AI 回答（兼容 AI 助手页）")
    public Result<AiChatResponseDTO> ask(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody RagAskRequestDTO request) {
        return Result.success(aiChatService.ask(request.getQuestion_wsh(), token.getUserId()));
    }
}
