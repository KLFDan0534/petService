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
     * 列出可回收表
     *
     * <p>API: GET /api/recycle-bin/tables</p>
     * <p>请求来源：后台管理回收站页，管理员查看支持回收站操作的数据表</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：无</p>
     * <p>返回数据：List&lt;String&gt; - 支持软删除可回收的表名列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
     * 列出已删除记录
     *
     * <p>API: GET /api/recycle-bin</p>
     * <p>请求来源：后台管理回收站页，管理员查看指定表中已软删除的记录</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：@query table - 表名</p>
     * <p>返回数据：List&lt;Map&lt;String, Object&gt;&gt; - 已删除的记录列表，包含各字段的原始值</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 表名不合法或不支持</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
     * 恢复已删除记录
     *
     * <p>API: POST /api/recycle-bin/restore</p>
     * <p>请求来源：后台管理回收站页，管理员点击"恢复"按钮恢复一条软删除的记录</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query table - 表名</li>
     *   <li>@query id - 记录ID</li>
     * </ul>
     * </p>
     * <p>返回数据：无（Result.success()），该记录的deleted_at置空恢复可见</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 表名或ID不合法</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>404 - 记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
     * 永久删除记录
     *
     * <p>API: POST /api/recycle-bin/delete</p>
     * <p>请求来源：后台管理回收站页，管理员点击"永久删除"物理删除一条已软删除的记录</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：
     * <ul>
     *   <li>@query table - 表名</li>
     *   <li>@query id - 记录ID</li>
     * </ul>
     * </p>
     * <p>返回数据：无（Result.success()），该记录从数据库中物理删除</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 表名或ID不合法</li>
     *   <li>403 - 非管理员无权限</li>
     *   <li>404 - 记录不存在</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
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
