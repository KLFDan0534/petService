package com.pet.module.rag.controller;

import com.pet.common.Result;
import com.pet.module.rag.entity.KnowledgeDocument;
import com.pet.module.rag.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/documents")
    public Result<List<KnowledgeDocument>> listDocuments() {
        return Result.success(ragService.listAll());
    }

    @GetMapping("/search")
    public Result<List<KnowledgeDocument>> search(@RequestParam String query,
                                                   @RequestParam(required = false) String category) {
        return Result.success(ragService.search(query, category));
    }

    @PostMapping("/ask")
    public Result<Map<String, String>> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = ragService.answer(question);
        return Result.success(Map.of("question", question, "answer", answer));
    }

    @PostMapping("/documents")
    public Result<KnowledgeDocument> create(@RequestBody KnowledgeDocument doc) {
        return Result.success(ragService.create(doc));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ragService.delete(id);
        return Result.success();
    }
}
