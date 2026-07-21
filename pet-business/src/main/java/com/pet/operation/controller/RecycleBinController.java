package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.annotation.LogOperation;
import com.pet.common.Result;
import com.pet.operation.service.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recycle-bin")
@Tag(name = "【后台管理】回收站", description = "软删除记录查询和恢复")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    public RecycleBinController(RecycleBinService recycleBinService) {
        this.recycleBinService = recycleBinService;
    }

    /**
     * 列出支持回收站操作的数据表
     * @return 可回收表名列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/tables")
    @Operation(summary = "列出可回收表", description = "列出支持回收站操作的数据表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<String>> listTables() {
        log.info("调用 listTables()");
        return Result.success(recycleBinService.listSoftDeletableTables());
    }

    /**
     * 查询指定表中已删除的记录
     * @param table 表名
     * @return 已删除记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @Operation(summary = "列出已删除记录", description = "查询指定表中已删除的记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<Map<String, Object>>> listDeleted(@RequestParam String table) {
        return Result.success(recycleBinService.listDeleted(table));
    }

    /**
     * 恢复已软删除的记录
     * @param table 表名
     * @param id 记录ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/restore")
    @LogOperation(module = "recycle", operation = "restore", description = "Restore deleted record")
    @Operation(summary = "恢复记录", description = "恢复一条软删除的记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> restore(@RequestParam String table, @RequestParam Long id) {
        log.info("调用 restore()");
        recycleBinService.restore(table, id);
        return Result.success();
    }

    /**
     * 永久删除已软删除的记录
     * @param table 表名
     * @param id 记录ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/delete")
    @LogOperation(module = "recycle", operation = "permanent-delete", description = "Permanently delete record")
    @Operation(summary = "永久删除", description = "永久删除一条软删除的记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> permanentDelete(@RequestParam String table, @RequestParam Long id) {
        log.info("调用 permanentDelete()");
        recycleBinService.softDelete(table, id);
        return Result.success();
    }
}
