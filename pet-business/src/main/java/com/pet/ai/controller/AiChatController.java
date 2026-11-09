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
     * 【构造函数：注入 AI 对话服务】
     *
     * 业务作用：注入 AiChatService 供对话接口调用。
     *
     * 调用场景：容器启动创建 AiChatController Bean 时自动调用。
     *
     * 调用链：Spring IoC → AiChatController(构造) → aiChatService 字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本控制器只有一个对话接口。
     */
    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * 【AI 客服对话】
     *
     * 业务作用：接收用户问题与历史对话，调用 AI 服务生成回复，必要时提示转人工。
     *
     * 调用场景：智能客服聊天窗口发送消息。
     *
     * 调用链：POST /api/ai/chat → AiChatController.chat → AiChatService.chat。
     *
     * 数据处理：请求体经 @Valid 校验后连同当前用户 ID 一起下传，返回含回复内容与 need_human 标记的响应。
     *
     * 业务规则：需登录；用户 ID 取自令牌，不接受前端传入。
     *
     * 状态影响：具体是否落库聊天记录由 AiChatService 决定。
     *
     * 异常情况：请求体校验失败返回 400；未登录返回 401；AI 服务异常由服务层转换后抛出。
     *
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
