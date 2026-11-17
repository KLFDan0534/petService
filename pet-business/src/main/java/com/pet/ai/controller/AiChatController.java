package com.pet.ai.controller;

import com.pet.ai.dto.AiChatRequestDTO;
import com.pet.ai.dto.AiChatResponseDTO;
import com.pet.ai.service.AiChatService;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "【智能客服】AI 对话", description = "AI 智能客服对话，回复前检索知识库，支持转人工")
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 业务作用：注入 AiChatService 供对话接口调用。
     * 注意事项：本控制器只有一个对话接口。
     */
    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 业务作用：接收用户问题与历史对话，调用 AI 服务生成回复，必要时提示转人工。
     * 业务规则：需登录；用户 ID 取自令牌，不接受前端传入。
     * 异常情况：请求体校验失败返回 400；未登录返回 401；AI 服务异常由服务层转换后抛出。
     * 注意事项：控制器只做透传，知识库检索与转人工判定全部在服务层。
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "AI 客服对话", description = "携带历史对话，回复基于知识库；需要人工时返回 need_human=true")
    public Result<AiChatResponseDTO> chat(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody AiChatRequestDTO request) {
        return Result.success(aiChatService.chat(request, token.getUserId()));
    }
}
