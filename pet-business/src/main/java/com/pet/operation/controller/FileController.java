package com.pet.operation.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传管理")
@Slf4j
public class FileController {

    private final MinIoService minIoService;
    private final FileRecordService fileRecordService;

    public FileController(MinIoService minIoService, FileRecordService fileRecordService) {
        this.minIoService = minIoService;
        this.fileRecordService = fileRecordService;
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
    public Result<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam String directory,
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
        record.setUrl_wsh(minIoService.getFileUrl(objectName));
        return Result.success(record);
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
    public Result<List<FileRecord>> list(@AuthenticationPrincipal JwtAuthenticationToken token) {
        log.info("调用 list()");
        return Result.success(fileRecordService.listByUser(token.getUserId()));
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
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileRecord record = fileRecordService.getById(id);
        if (record == null) {
            response.sendError(404, "文件不存在");
            return;
        }
        String url = minIoService.getFileUrl(record.getObject_name_wsh());
        response.sendRedirect(url);
    }
}
