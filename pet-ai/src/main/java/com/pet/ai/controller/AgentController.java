package com.pet.ai.controller;

import com.pet.ai.dto.AgentChatRequestDTO;
import com.pet.ai.dto.AgentChatResponseDTO;
import com.pet.ai.dto.AgentExecuteRequestDTO;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.service.AgentService;
import com.pet.ai.service.AiChatService;
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
    private final AiChatService aiChatService;

    public AgentController(AgentService agentService, AiChatService aiChatService) {
        this.agentService = agentService;
        this.aiChatService = aiChatService;
    }

    /**
     * 【业务名称】AI客服聊天
     * <p>业务作用：接收用户问题和历史消息，携带系统提示词（"小宠"AI助手角色设定），调用AI模型返回回复。支持多轮对话历史上下文。</p>
     * <p>调用场景：用户在AI助手聊天界面发送消息。</p>
     * <p>调用链：前端POST /api/agent/chat → chat() → 组装systemPrompt+history+当前消息 → AiChatService.chat(fullMessages) → AI回复 → 返回AgentChatResponseDTO</p>
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
        log.info("User {} sent agent chat message", token.getUserId());
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
     * 【业务名称】AI Agent自动下单
     * <p>业务作用：通过自然语言指令让AI Agent自动完成宠物寄养下单全流程。用户输入如"金毛寄养3天"，Agent自动识别意图、匹配宠物、搜索附近商家和看护人、评分排序推荐、创建订单和支付。</p>
     * <p>调用场景：用户在AI助手中输入"帮我下单"或类似的下单指令。</p>
     * <p>调用链：前端POST /api/agent/execute → execute() → AgentService.execute() → 9步流水线 → 返回AgentExecuteResult</p>
     * <p>数据处理：解析请求DTO中的input/location/autoPay/paymentPassword参数，传递给AgentService。</p>
     * <p>业务规则：需用户登录；请求数据需通过@Valid校验；autoPay=true时需提供支付密码。</p>
     * <p>状态影响：成功时创建新订单和支付记录。</p>
     * <p>异常情况：Service层异常被捕获并返回友好的错误信息。</p>
     * <p>注意事项：前端需根据返回的status字段切换UI状态（success/pending_payment/needs_user_input/failed）。</p>
     */
    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "执行Agent", description = "通过自然语言创建宠物服务订单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentExecuteResult> execute(
            @Valid @RequestBody AgentExecuteRequestDTO request,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("User {} called agent execute", token.getUserId());
        AgentExecuteResult result = agentService.execute(
                token.getUserId(),
                request.getInput_wsh(),
                request.getLatitude_wsh(),
                request.getLongitude_wsh(),
                request.getAuto_pay_wsh(),
                request.getPayment_password_wsh());
        return Result.success(result);
    }
}
