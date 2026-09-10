package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.common.Result;
import com.pet.operation.dto.OperationLogDTO;
import com.pet.operation.entity.OperationLog;
import com.pet.operation.service.OperationLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation-logs")
@Tag(name = "【后台管理】操作日志", description = "操作审计日志管理")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志
     *
     * <p>API: GET /api/operation-logs</p>
     * <p>请求来源：后台管理审计日志页，管理员查看系统操作日志，支持按模块、操作类型和状态筛选</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query PageRequestDTO - 分页参数（页码、每页大小）</li>
     *   <li>@query module - 模块名称（可选，如user/order/merchant）</li>
     *   <li>@query operation - 操作类型（可选，如create/update/delete）</li>
     *   <li>@query status - 执行状态（可选，成功/失败）</li>
     * </ul>
     * </p>
     * <p>返回数据：IPage&lt;OperationLogDTO&gt; - 分页的操作日志，包含操作人、IP、请求URL、执行时长等</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 分页参数错误</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping
    @Operation(summary = "分页查询操作日志")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<IPage<OperationLogDTO>> page(@Parameter(description = "分页参数") PageRequestDTO param,
                                                @Parameter(description = "模块名称（可选）") @RequestParam(required = false) String module,
                                                @Parameter(description = "操作类型（可选）") @RequestParam(required = false) String operation,
                                                @Parameter(description = "状态（可选）") @RequestParam(required = false) Integer status) {
        log.info("page() 被调用");
        return Result.success(toDTOPage(operationLogService.page(param, module, operation, status)));
    }

    /**
     * 获取操作日志详情
     *
     * <p>API: GET /api/operation-logs/{id}</p>
     * <p>请求来源：后台管理审计日志详情页，管理员查看单条日志的详细信息</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@path id - 日志ID</p>
     * <p>返回数据：OperationLogDTO - 操作日志详情，含请求参数、响应结果等</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>404 - 日志不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取操作日志详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<OperationLogDTO> getById(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("getById() 被调用");
        return Result.success(toDTO(operationLogService.getById(id)));
    }

    private OperationLogDTO toDTO(OperationLog entity) {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUser_id_wsh(entity.getUser_id_wsh());
        dto.setUsername_wsh(entity.getUsername_wsh());
        dto.setModule_wsh(entity.getModule_wsh());
        dto.setOperation_wsh(entity.getOperation_wsh());
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setMethod_wsh(entity.getMethod_wsh());
        dto.setRequest_url_wsh(entity.getRequest_url_wsh());
        dto.setIp_address_wsh(entity.getIp_address_wsh());
        dto.setDuration_wsh(entity.getDuration_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    private IPage<OperationLogDTO> toDTOPage(IPage<OperationLog> page) {
        Page<OperationLogDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }
}
