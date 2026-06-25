package com.pet.customer.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.PageParam;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.service.TicketService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "工单管理", description = "客服工单系统")
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 获取我的工单列表
     * @param token 当前用户认证信息
     * @return 工单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/me")
    @Operation(summary = "获取我的工单")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Ticket>> listMyTickets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 listMyTickets()");
        return Result.success(ticketService.listByUser(token.getUserId()));
    }

    /**
     * 分页获取所有工单
     * @param pageParam 分页参数
     * @return 分页工单列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "分页获取所有工单")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    public Result<PageResult<Ticket>> listAll(PageParam pageParam) {
        log.info("调用 listAll()");
        return Result.success(new PageResult<>(ticketService.listPage(pageParam)));
    }

    /**
     * 获取工单详情
     * @param id 工单ID
     * @return 工单详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "获取工单详情")
    @PreAuthorize("isAuthenticated()")
    public Result<Ticket> getById(@PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(ticketService.getById(id));
    }

    /**
     * 创建工单
     * @param token 当前用户认证信息
     * @param ticket 工单信息
     * @return 创建的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @Operation(summary = "创建工单")
    @PreAuthorize("isAuthenticated()")
    public Result<Ticket> create(@AuthenticationPrincipal JwtAuthenticationToken token, @Valid @RequestBody Ticket ticket) {
        log.info("调用 create()");
        return Result.success(ticketService.create(token.getUserId(), ticket));
    }

    /**
     * 分配工单给客服人员
     * @param id 工单ID
     * @param body 请求体，包含assignee_id_wsh
     * @return 更新后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    @Operation(summary = "分配工单")
    public Result<Ticket> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        log.info("调用 assign()");
        return Result.success(ticketService.assign(id, body.get("assignee_id_wsh")));
    }

    /**
     * 处理完成工单
     * @param id 工单ID
     * @param body 请求体，包含result_wsh处理结果（可选）
     * @return 更新后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    @Operation(summary = "处理完成工单")
    public Result<Ticket> resolve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("调用 resolve()");
        return Result.success(ticketService.resolve(id, body != null ? body.get("result_wsh") : null));
    }

    /**
     * 关闭工单
     * @param id 工单ID
     * @return 更新后的工单
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER_SERVICE')")
    @Operation(summary = "关闭工单")
    public Result<Ticket> close(@PathVariable Long id) {
        log.info("调用 close()");
        return Result.success(ticketService.close(id));
    }

    /**
     * 获取工单消息列表
     * @param id 工单ID
     * @return 消息列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}/messages")
    @Operation(summary = "获取工单消息")
    @PreAuthorize("isAuthenticated()")
    public Result<List<TicketMessage>> listMessages(@PathVariable Long id) {
        log.info("调用 listMessages()");
        return Result.success(ticketService.listMessages(id));
    }

    /**
     * 添加工单回复消息
     * @param token 当前用户认证信息
     * @param id 工单ID
     * @param body 请求体，包含content_wsh消息内容
     * @return 创建的消息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/{id}/messages")
    @Operation(summary = "添加工单回复")
    @PreAuthorize("isAuthenticated()")
    public Result<TicketMessage> addMessage(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        log.info("调用 addMessage()");
        return Result.success(ticketService.addMessage(id, token.getUserId(), body.get("content_wsh")));
    }
}