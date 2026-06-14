package com.pet.module.minio.controller;

import com.pet.common.Result;
import com.pet.module.minio.service.MinIoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传管理")
public class FileController {

    private final MinIoService minIoService;

    public FileController(MinIoService minIoService) {
        this.minIoService = minIoService;
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到MinIO存储并返回访问URL")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(defaultValue = "common") String directory) {
        String url = minIoService.uploadFile(file, directory);
        return Result.success(Map.of("url", url, "objectName", url));
    }
}
