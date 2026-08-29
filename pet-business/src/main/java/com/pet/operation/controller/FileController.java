package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.operation.dto.FileRecordDTO;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pet.boarding.service.MerchantScopeResolver;
import com.pet.boarding.service.ServiceMediaService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/files")
@Tag(name = "【用户端】文件管理", description = "文件上传管理（用户文件上传/下载）")
@Slf4j
public class FileController {

    private final MinIoService minIoService;
    private final FileRecordService fileRecordService;
    private final ServiceMediaService serviceMediaService;
    private final MerchantScopeResolver merchantScopeResolver;

    public FileController(MinIoService minIoService, FileRecordService fileRecordService,
                          ServiceMediaService serviceMediaService, MerchantScopeResolver merchantScopeResolver) {
        this.minIoService = minIoService;
        this.fileRecordService = fileRecordService;
        this.serviceMediaService = serviceMediaService;
        this.merchantScopeResolver = merchantScopeResolver;
    }

    /**
     * 上传产品图片到MinIO存储并返回文件记录
     * 权限：MERCHANT 或 ADMIN
     * 作用域：MERCHANT 自动派生自己所属商家；ADMIN 必须显式指定 merchantId；
     * 其他角色一律拒绝。存储目录、扩展名、MIME 均由服务端控制。
     *
     * @author: wsh
     * @date: 2026/8/12
     **/
    @PostMapping("/product-image")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANT')")
    @Operation(summary = "上传产品图片", description = "仅MERCHANT/ADMIN；服务端校验内容并记录product用途与商家归属")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "图片格式/大小/尺寸不合法"),
            @ApiResponse(responseCode = "403", description = "无权限访问或商家归属不符"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<FileRecordDTO> uploadProductImage(
            @Parameter(description = "上传的图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "ADMIN显式指定的目标商家ID（MERCHANT 可省略）") @RequestParam(required = false) Long merchantId,
            @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 uploadProductImage()");
        Long targetMerchantId = merchantScopeResolver.resolve(merchantId, token);
        FileRecord record = serviceMediaService.uploadProductImage(
                token.getUserId(), targetMerchantId, file);
        return Result.success(toDTO(record));
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null || token.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : token.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 上传文件到MinIO存储并返回URL
     * @param file 上传的文件
     * @param directory 存储目录
     * @param token 当前用户认证信息
     * @return 文件记录（包含URL）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传文件", description = "上传文件到MinIO存储并返回URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<FileRecordDTO> upload(@Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
                                        @Parameter(description = "存储目录") @RequestParam String directory,
                                        @AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 upload()");
        String objectName = minIoService.uploadFile(file, directory);
        FileRecord record = new FileRecord();
        record.setOriginal_name_wsh(file.getOriginalFilename());
        record.setObject_name_wsh(objectName);
        record.setSize_wsh(file.getSize());
        record.setContent_type_wsh(file.getContentType());
        record.setUser_id_wsh(token.getUserId());
        fileRecordService.create(record);
        return Result.success(toDTO(record));
    }

    /**
     * 获取当前用户的文件列表
     * @param token 当前用户认证信息
     * @return 文件记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "文件列表", description = "获取当前用户的文件列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<FileRecordDTO>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 list()");
        List<FileRecord> list = fileRecordService.listByUser(token.getUserId());
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 下载文件（重定向到MinIO文件URL）
     * @param id 文件记录ID
     * @param response HTTP响应
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "下载文件", description = "重定向到MinIO文件URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public void download(@Parameter(description = "文件记录ID") @PathVariable Long id, HttpServletResponse response) throws IOException {
        FileRecord record = fileRecordService.getById(id);
        if (record == null) {
            response.sendError(404, "文件不存在");
            return;
        }
        String url = minIoService.getFileUrl(record.getObject_name_wsh());
        response.sendRedirect(url);
    }

    /**
     * 管理员分页查看全部文件资源列表
     * @param pageParam 分页参数
     * @param keyword 按原始文件名模糊搜索（可选）
     * @return 分页文件记录DTO列表
     * @author: wsh
     * @date: 2026/8/22
     **/
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员文件列表", description = "管理员分页查看全部文件资源，支持按原始文件名模糊搜索")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<FileRecordDTO>> adminList(PageRequestDTO pageParam,
            @Parameter(description = "按原始文件名模糊搜索") @RequestParam(required = false) String keyword) {
        log.info("调用 adminList()");
        var page = fileRecordService.pageAll(pageParam, keyword);
        var dtoList = page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        PageResult<FileRecordDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }

    /**
     * 管理员删除文件资源记录
     * @param id 文件记录ID
     * @return 操作结果
     * @author: wsh
     * @date: 2026/8/22
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除文件记录", description = "管理员删除指定文件记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "文件不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> delete(@Parameter(description = "文件记录ID") @PathVariable Long id) {
        log.info("调用 delete()");
        if (fileRecordService.getById(id) == null) {
            throw new BusinessException(404, "文件不存在");
        }
        fileRecordService.deleteById(id);
        return Result.success();
    }

    private FileRecordDTO toDTO(FileRecord entity) {
        FileRecordDTO dto = new FileRecordDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setUrl_wsh(minIoService.getFileUrl(entity.getObject_name_wsh()));
        dto.setOriginal_name_wsh(entity.getOriginal_name_wsh());
        dto.setFile_type_wsh(entity.getContent_type_wsh());
        dto.setFile_size_wsh(entity.getSize_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }
}
