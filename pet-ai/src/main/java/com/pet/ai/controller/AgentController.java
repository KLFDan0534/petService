package com.pet.ai.controller;

import com.pet.ai.dto.AgentChatRequestDTO;
import com.pet.ai.dto.AgentChatResponseDTO;
import com.pet.ai.dto.AgentConfirmRequestDTO;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentPlanRequestDTO;
import com.pet.ai.dto.AgentPlanResult;
import com.pet.ai.service.AgentService;
import com.pet.ai.service.LlmChatService;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "【AI】AI助手", description = "AI客服聊天助手（用户/管理员使用）")
@Slf4j
public class AgentController {

    private static final String SYSTEM_PROMPT = """
            你是“宠物之家”服务平台的 AI 客服助手，名字叫“小宠”。
            请用中文回复，保持友好、专业、耐心。
            如果用户的问题超出宠物护理和平台服务范围，请礼貌引导回宠物相关话题。
            回答应简洁清晰，不要过长。
            """;

    private final AgentService agentService;
    private final LlmChatService aiChatService;

    public AgentController(AgentService agentService, LlmChatService aiChatService) {
        this.agentService = agentService;
        this.aiChatService = aiChatService;
    }

    /**
     * 【业务名称】AI客服聊天
     * <p>业务作用：接收用户问题和历史消息，携带系统提示词（"小宠"AI助手角色设定），调用AI模型返回回复。支持多轮对话历史上下文。</p>
     * <p>调用场景：用户在AI助手聊天界面发送消息。</p>
     * <p>调用链：前端POST /api/agent/chat → chat() → 组装systemPrompt+history+当前消息 → LlmChatService.chat(fullMessages) → AI回复 → 返回AgentChatResponseDTO</p>
     * <p>数据处理：校验消息非空；组装消息列表（system提示词 + 历史消息 + 当前用户消息）；AI返回null时使用降级文案；设置finishReason="stop"。</p>
     * <p>业务规则：需用户登录鉴权；消息为空返回400错误。</p>
     * <p>状态影响：无状态变更。</p>
     * <p>异常情况：AI不可用时返回降级文案"我暂时无法连接 AI 模型，请稍后再试。"。</p>
     * <p>注意事项：SYSTEM_PROMPT固定为平台AI助手"小宠"设定；历史消息由前端维护和传递。</p>
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "AI聊天助手", description = "AI客服助手聊天")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentChatResponseDTO> chat(
            @RequestBody AgentChatRequestDTO request,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        String message = request.getQuestion_wsh();
        log.info("用户 {} 发送了 Agent 聊天消息", token.getUserId());
        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息不能为空");
        }

        List<Map<String, String>> history = request.getHistory_wsh();
        if (history == null) {
            history = new ArrayList<>();
        }

        List<Map<String, String>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        fullMessages.addAll(history);
        fullMessages.add(Map.of("role", "user", "content", message));

        AgentChatResponseDTO dto = new AgentChatResponseDTO();
        String reply = aiChatService.chat(fullMessages);
        dto.setReply(reply == null || reply.isBlank()
                ? "我暂时无法连接 AI 模型，请稍后再试。"
                : reply);
        dto.setFinishReason("stop");
        return Result.success(dto);
    }

    /**
     * 【业务名称】智能下单 plan 阶段
     * <p>业务作用：接收用户自然语言下单需求，由 LLM 解析并生成推荐寄养方案（不落单），返回方案 token 供前端展示确认卡片。</p>
     * <p>调用链：前端POST /api/agent/plan → plan() → AgentService.plan() → LLM 提取 + 商家/看护人匹配 → 返回 AgentPlanResult</p>
     * <p>业务规则：需用户登录；仅支持日间寄养(day)；LLM 不可用/解析失败返回 failed 状态。</p>
     * <p>状态影响：无（不写库）。</p>
     */
    @PostMapping("/plan")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "智能下单-生成方案", description = "LLM解析用户需求并生成寄养推荐方案")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentPlanResult> plan(
            @Valid @RequestBody AgentPlanRequestDTO request,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("用户 {} 调用了智能下单 plan", token.getUserId());
        AgentPlanResult result = agentService.plan(
                token.getUserId(),
                request.getInput_wsh(),
                request.getLatitude_wsh(),
                request.getLongitude_wsh(),
                request.getAddress_wsh());
        return Result.success(result);
    }

    /**
     * 【业务名称】智能下单 confirm 阶段
     * <p>业务作用：凭 plan 阶段返回的方案 token 创建订单，autoPay=true 时校验支付密码并直接支付。</p>
     * <p>调用链：前端POST /api/agent/confirm → confirm() → AgentService.confirm() → 创建订单+支付</p>
     * <p>业务规则：需用户登录；token 原子认领防重复下单；失败回滚并释放 token 供重试。</p>
     * <p>状态影响：成功时创建新订单和支付记录。</p>
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "智能下单-确认下单", description = "凭方案token创建订单（可选自动支付）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或方案已过期"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "方案不属于当前用户"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentExecuteResult> confirm(
            @Valid @RequestBody AgentConfirmRequestDTO request,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("用户 {} 调用了智能下单 confirm", token.getUserId());
        AgentExecuteResult result = agentService.confirm(
                token.getUserId(),
                request.getPlan_token_wsh(),
                request.getAuto_pay_wsh(),
                request.getPayment_password_wsh());
        return Result.success(result);
    }
}
