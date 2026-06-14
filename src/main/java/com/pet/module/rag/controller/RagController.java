package com.pet.module.rag.controller;

import com.pet.common.Result;
import com.pet.module.rag.entity.KnowledgeDocument;
import com.pet.module.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "知识库管理", description = "RAG知识库的文档管理和智能问答")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/documents")
    @Operation(summary = "获取知识库文档列表", description = "获取知识库中的所有文档")
    public Result<List<KnowledgeDocument>> listDocuments() {
        return Result.success(ragService.listAll());
    }

    @GetMapping("/search")
    @Operation(summary = "搜索知识库", description = "根据关键词搜索知识库文档，可按分类筛选")
    public Result<List<KnowledgeDocument>> search(@RequestParam String query,
                                                   @RequestParam(required = false) String category) {
        return Result.success(ragService.search(query, category));
    }

    @PostMapping("/ask")
    @Operation(summary = "智能问答", description = "向知识库提问并获取AI回答")
    public Result<Map<String, String>> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.answer(question);
        return Result.success(Map.of("question", question, "answer", answer));
    }

    @PostMapping("/documents")
    @Operation(summary = "新增知识文档", description = "向知识库中添加新的知识文档")
    public Result<KnowledgeDocument> create(@RequestBody KnowledgeDocument doc) {
        return Result.success(ragService.create(doc));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除知识文档", description = "根据ID删除知识库中的文档")
    public Result<Void> delete(@PathVariable Long id) {
        ragService.delete(id);
        return Result.success();
    }
}
