package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.common.Result;
import com.pet.operation.entity.OperationLog;
import com.pet.operation.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-logs")
@Tag(name = "操作日志", description = "操作审计日志管理")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志
     * @param param 分页参数
     * @param module 模块名称（可选）
     * @param operation 操作类型（可选）
     * @param status 状态（可选）
     * @return 分页操作日志
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "分页查询操作日志")
    public Result<IPage<OperationLog>> page(PageParam param,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) String operation,
                                            @RequestParam(required = false) Integer status) {
        log.info("page() called");
        return Result.success(operationLogService.page(param, module, operation, status));
    }

    /**
     * 获取操作日志详情
     * @param id 日志ID
     * @return 操作日志详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "获取操作日志详情")
    public Result<OperationLog> getById(@PathVariable Long id) {
        log.info("getById() called");
        return Result.success(operationLogService.getById(id));
    }
}
