package com.pet.ai.controller;

import com.pet.ai.dto.AgentConfirmRequestDTO;
import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentMessageRequestDTO;
import com.pet.ai.dto.AgentSessionDetail;
import com.pet.ai.dto.AgentSessionSummaryVO;
import com.pet.ai.dto.AgentTurnResult;
import com.pet.ai.service.AgentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能下单（会话式）。
 * <p>
 * 接口分工：
 * <ul>
 *   <li>{@code POST /message}：一轮对话。session_id 为空 = 首轮，成功后服务端才建会话（空会话不入库）；</li>
 *   <li>{@code GET /sessions}、{@code GET /sessions/{sid}}、{@code DELETE /sessions/{sid}}：历史会话；</li>
 *   <li>{@code POST /confirm}：用户点「确认下单」后才创建订单（可选自动支付）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "【AI】智能下单", description = "会话式智能下单：多轮修改方案，确认后才建单")
@Slf4j
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 发送一条消息（一轮对话）
     *
     * 业务作用：首轮不带 session_id，服务端处理成功后建会话并返回 id；续轮带上 session_id 即可在原方案上继续修改。
     * 业务规则：需登录；会话已下单（CONFIRMED）拒绝继续；LLM 两次都解析失败时首轮不入库。
     */
    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发送消息", description = "一轮对话：多轮修改寄养方案，确认前不会建单")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或会话状态不允许"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentTurnResult> message(@Valid @RequestBody AgentMessageRequestDTO request,
                                           @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("用户 {} 发送智能下单消息（session={}）", token.getUserId(), request.getSession_id_wsh());
        return Result.success(agentService.message(token.getUserId(), request));
    }

    /**
     * 历史会话列表
     * <p>业务作用：返回当前用户「至少说过一句话」的会话，按最后活动时间倒序。</p>
     * <p>业务规则：只查 agent_session_wsh，因此空会话天然不会出现。</p>
     */
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "历史会话列表", description = "按最后活动时间倒序，只含有消息的会话")
    public Result<List<AgentSessionSummaryVO>> listSessions(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(agentService.listSessions(token.getUserId()));
    }

    /**
     * 会话详情
     * <p>业务作用：恢复消息气泡与当前方案卡片（刷新页面 / 切换历史会话）。</p>
     */
    @GetMapping("/sessions/{sid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "会话详情", description = "消息列表 + 当前方案卡片")
    public Result<AgentSessionDetail> getSession(@PathVariable("sid") String sessionId,
                                                 @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(agentService.getSession(token.getUserId(), sessionId));
    }

    /**
     * 删除会话
     * <p>业务作用：删除会话状态与聊天记录；已创建的订单不受影响。</p>
     */
    @DeleteMapping("/sessions/{sid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除会话", description = "删除会话状态与聊天记录，不影响已创建订单")
    public Result<Void> deleteSession(@PathVariable("sid") String sessionId,
                                      @AuthenticationPrincipal JwtAuthenticationToken token) {
        agentService.deleteSession(token.getUserId(), sessionId);
        return Result.success();
    }

    /**
     * 确认下单
     *
     * 业务作用：凭会话ID创建订单，autoPay=true 时校验支付密码并直接支付。
     * 业务规则：status 的 CAS 保证一个会话只能下一单；重复点击 / 已下单都会被拒绝；事务失败整体回滚可重试。
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "确认下单", description = "凭会话ID创建订单（可选自动支付）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "会话不存在、无方案或已下单"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AgentExecuteResult> confirm(@Valid @RequestBody AgentConfirmRequestDTO request,
                                              @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("用户 {} 确认智能下单（session={}）", token.getUserId(), request.getSession_id_wsh());
        return Result.success(agentService.confirm(
                token.getUserId(),
                request.getSession_id_wsh(),
                request.getAuto_pay_wsh(),
                request.getPayment_password_wsh()));
    }
}
