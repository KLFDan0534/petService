package com.pet.customer.controller;

import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.customer.dto.TicketAddMessageRequestDTO;
import com.pet.customer.dto.TicketAssignRequestDTO;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.dto.TicketListRequestDTO;
import com.pet.customer.dto.TicketMessageDTO;
import com.pet.customer.dto.TicketResolveRequestDTO;
import com.pet.customer.service.TicketService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "【用户端】工单管理", description = "客服工单管理（用户/管理员/商户/客服使用）")
public class TicketController {

    private final TicketService ticketService;

    /**
     * 【构造函数：注入工单服务】
     *
     * 业务作用：由 Spring 注入 TicketService，供各接口调用。
     *
     * 调用场景：容器启动创建 TicketController Bean 时自动调用。
     *
     * 调用链：Spring IoC → TicketController(构造) → ticketService 字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：本控制器不直接访问 Mapper，全部业务逻辑在 TicketService 中。
     */
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 【获取我的工单列表】
     *
     * 业务作用：返回当前登录用户自己提交的工单列表。
     *
     * 调用场景：用户端「我的工单」页面加载。
     *
     * 调用链：GET /api/tickets/me → TicketController.listMyTickets → TicketService.listByUser → TicketMapper.selectList。
     *
     * 数据处理：从 JWT 取 userId，直接作为查询条件，按创建时间倒序返回全部状态的工单。
     *
     * 业务规则：仅需登录（isAuthenticated），只能看到自己发起的工单。
     *
     * 状态影响：只读。
     *
     * 异常情况：未登录由安全框架拦截返回 401。
     *
     * 注意事项：不返回他人的工单，用户 ID 完全取自令牌，不接受前端传入。
     */
    @GetMapping("/me")
    @Operation(summary = "获取我的工单列表")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TicketDTO>> listMyTickets(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(ticketService.listByUser(token.getUserId()));
    }

    /**
     * 【管理员、商户、客服分页获取工单列表】
     *
     * 业务作用：按当前用户角色计算可见范围后分页查询工单。
     *
     * 调用场景：后台工单管理列表页（管理员/商家/客服三种角色共用）。
     *
     * 调用链：GET /api/tickets → TicketController.listAll → hasRole → TicketService.listPageForStaff → TicketMapper.selectPage。
     *
     * 数据处理：把 token 中的角色转换为 admin/merchant/customerService 三个布尔值，连同分页与筛选条件一起下传，由服务层做数据范围隔离。
     *
     * 业务规则：要求 ADMIN、MERCHANT 或 CUSTOMER_SERVICE 角色之一；可见范围由服务层按角色收敛。
     *
     * 状态影响：只读。
     *
     * 异常情况：无对应角色时由 @PreAuthorize 拦截返回 403。
     *
     * 注意事项：角色判断依赖令牌中的 authorities，多角色用户会同时带上多个布尔值。
     */
    @GetMapping
    @Operation(summary = "管理员、商户、客服获取工单列表")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<TicketDTO>> listAll(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "分页及筛选请求") TicketListRequestDTO pageParam) {
        return Result.success(new PageResult<>(ticketService.listPageForStaff(
                pageParam,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"))));
    }

    /**
     * 【获取工单详情】
     *
     * 业务作用：按 ID 返回工单详情，并按角色做可见性校验。
     *
     * 调用场景：工单详情页、消息列表页初始化。
     *
     * 调用链：GET /api/tickets/{id} → TicketController.getById → hasRole → TicketService.getByIdForUser。
     *
     * 数据处理：把工单 ID、当前用户 ID 与三个角色布尔值传给服务层，由服务层判定是否可见。
     *
     * 业务规则：需登录；非工单归属用户且非管理员/商家/客服时无权查看。
     *
     * 状态影响：只读。
     *
     * 异常情况：工单不存在抛 404；无权查看抛 403（均由服务层抛出）。
     *
     * 注意事项：权限判断不在控制器内完成，控制器只负责传递角色标识。
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取工单详情")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> getById(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.getByIdForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 【创建工单】
     *
     * 业务作用：以当前登录用户身份提交一条售后或客服工单。
     *
     * 调用场景：用户在订单或服务页面发起售后申请。
     *
     * 调用链：POST /api/tickets → TicketController.create → TicketService.create → TicketMapper.insert。
     *
     * 数据处理：请求体经 @Valid 校验后连同 userId 一起下传，创建后返回工单 DTO。
     *
     * 业务规则：需登录；创建人固定为当前令牌用户，不接受前端指定。
     *
     * 状态影响：新增一条工单记录，初始状态由服务层设定。
     *
     * 异常情况：请求体字段校验失败返回 400。
     *
     * 注意事项：工单初始状态与优先级规则在服务层实现，控制器不做补充。
     */
    @PostMapping
    @Operation(summary = "创建工单")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> create(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Valid @RequestBody TicketCreateRequestDTO request) {
        return Result.success(ticketService.create(token.getUserId(), request));
    }

    /**
     * 【指派工单】
     *
     * 业务作用：把工单指派给指定处理人，未指定时指派给当前操作人。
     *
     * 调用场景：后台工单列表点击「指派/认领」。
     *
     * 调用链：POST /api/tickets/{id}/assign → TicketController.assign → TicketService.assignForStaff。
     *
     * 数据处理：assigneeId 优先取请求体 assignee_id_wsh，为空则回退为当前用户 ID；连同操作人 ID 与角色布尔值下传。
     *
     * 业务规则：要求 ADMIN、MERCHANT 或 CUSTOMER_SERVICE 角色；请求体可省略（required=false）。
     *
     * 状态影响：更新工单的处理人字段，具体状态流转由服务层决定。
     *
     * 异常情况：工单不存在抛 404；越权指派由服务层抛 403。
     *
     * 注意事项：请求体为空时等价于「自己认领」，这是一个隐式行为。
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "指派工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> assign(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody(required = false) TicketAssignRequestDTO request) {
        // 未指定处理人时默认指派给操作人
        Long assigneeId = request != null && request.getAssignee_id_wsh() != null
                ? request.getAssignee_id_wsh()
                : token.getUserId();
        return Result.success(ticketService.assignForStaff(
                id,
                assigneeId,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 【解决工单】
     *
     * 业务作用：把工单标记为已解决，并记录处理结果。
     *
     * 调用场景：客服处理完成后在详情页点击「解决」。
     *
     * 调用链：POST /api/tickets/{id}/resolve → TicketController.resolve → TicketService.resolveForStaff。
     *
     * 数据处理：处理结果取请求体 result_wsh，请求体可省略（此时传 null）。
     *
     * 业务规则：要求 ADMIN、MERCHANT 或 CUSTOMER_SERVICE 角色；只能处理可见范围内的工单。
     *
     * 状态影响：工单状态流转为已解决，由服务层写库。
     *
     * 异常情况：工单不存在抛 404；状态不允许解决时由服务层抛 400。
     *
     * 注意事项：不传处理结果也能解决，此时结果字段为空。
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "解决工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> resolve(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @RequestBody(required = false) TicketResolveRequestDTO request) {
        return Result.success(ticketService.resolveForStaff(
                id,
                request != null ? request.getResult_wsh() : null,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 【关闭工单】
     *
     * 业务作用：把工单直接关闭，结束处理流程。
     *
     * 调用场景：后台对无效或重复工单做关闭处理。
     *
     * 调用链：POST /api/tickets/{id}/close → TicketController.close → TicketService.closeForStaff。
     *
     * 数据处理：仅传工单 ID、操作人 ID 与角色布尔值，无请求体。
     *
     * 业务规则：要求 ADMIN、MERCHANT 或 CUSTOMER_SERVICE 角色。
     *
     * 状态影响：工单状态流转为已关闭，由服务层写库。
     *
     * 异常情况：工单不存在抛 404；状态不允许关闭时由服务层抛 400。
     *
     * 注意事项：关闭后通常不可再追加消息，具体限制在服务层。
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT','CUSTOMER_SERVICE')")
    @Operation(summary = "关闭工单")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketDTO> close(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.closeForStaff(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 【获取工单消息列表】
     *
     * 业务作用：返回某条工单下的全部往来消息。
     *
     * 调用场景：工单详情页的消息时间线。
     *
     * 调用链：GET /api/tickets/{id}/messages → TicketController.listMessages → TicketService.listMessagesForUser。
     *
     * 数据处理：传工单 ID、当前用户 ID 与角色布尔值，由服务层校验可见性后返回消息列表。
     *
     * 业务规则：需登录；仅工单参与方或内部角色可查看。
     *
     * 状态影响：只读。
     *
     * 异常情况：工单不存在抛 404；无权查看抛 403。
     *
     * 注意事项：无。
     */
    @GetMapping("/{id}/messages")
    @Operation(summary = "获取工单消息列表")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TicketMessageDTO>> listMessages(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id) {
        return Result.success(ticketService.listMessagesForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE")));
    }

    /**
     * 【添加工单消息】
     *
     * 业务作用：在工单下追加一条文字消息，可附带文件地址。
     *
     * 调用场景：用户在工单详情页追问，或客服回复。
     *
     * 调用链：POST /api/tickets/{id}/messages → TicketController.addMessage → TicketService.addMessageForUser → TicketMessageMapper.insert。
     *
     * 数据处理：取请求体 content_wsh 与 file_url_wsh，连同工单 ID、发送人 ID 与角色布尔值下传。
     *
     * 业务规则：需登录；请求体经 @Valid 校验（内容必填规则由 DTO 注解决定）。
     *
     * 状态影响：新增一条工单消息记录；是否联动工单状态由服务层决定。
     *
     * 异常情况：请求体校验失败返回 400；工单不存在或无权访问由服务层抛 404/403。
     *
     * 注意事项：file_url_wsh 可为空，表示纯文本消息。
     */
    @PostMapping("/{id}/messages")
    @Operation(summary = "添加工单消息")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<TicketMessageDTO> addMessage(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @Parameter(description = "工单ID") @PathVariable Long id,
            @Valid @RequestBody TicketAddMessageRequestDTO request) {
        return Result.success(ticketService.addMessageForUser(
                id,
                token.getUserId(),
                hasRole(token, "ADMIN"),
                hasRole(token, "MERCHANT"),
                hasRole(token, "CUSTOMER_SERVICE"),
                request.getContent_wsh(),
                request.getFile_url_wsh()));
    }

    /**
     * 【判断令牌是否具备指定角色】
     *
     * 业务作用：检查当前登录令牌的权限集合中是否包含 ROLE_ 加指定角色的权限。
     *
     * 调用场景：listAll、getById、assign、resolve、close、listMessages、addMessage 计算角色布尔值时调用。
     *
     * 调用链：TicketController 各接口 → hasRole → JwtAuthenticationToken.getAuthorities。
     *
     * 数据处理：拼出 "ROLE_" + role，遍历权限集合做字符串相等比较。
     *
     * 业务规则：token 为 null 时直接返回 false；权限名需与 Spring Security 的 ROLE_ 前缀约定一致。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只做精确匹配，不支持层级或继承关系。
     */
    private boolean hasRole(JwtAuthenticationToken token, String role) {
        String authority = "ROLE_" + role;
        return token != null
                && token.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
