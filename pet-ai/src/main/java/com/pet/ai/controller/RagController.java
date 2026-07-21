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
     * 获取所有知识文档列表
     * @return 知识文档列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 搜索知识文档
     * @param query 查询关键词
     * @param category 分类过滤（可选）
     * @return 匹配的知识文档列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 基于知识文档进行AI问答
     * @param body 请求体，包含question和可选的petProfile/pet_profile_wsh
     * @return AI问答结果
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 创建知识文档
     * @param doc 知识文档信息
     * @return 创建的知识文档
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 删除知识文档
     * @param id 文档ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
     * 上传文件并自动提取文本创建知识文档
     * @param file 上传的文件（仅支持.txt和.docx格式）
     * @param title 文档标题（可选）
     * @param category 文档分类（可选）
     * @return 创建的知识文档
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
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
