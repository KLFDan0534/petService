package com.pet.module.minio.controller;

import com.pet.common.Result;
import com.pet.module.minio.service.MinIoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MinIoService minIoService;

    public FileController(MinIoService minIoService) {
        this.minIoService = minIoService;
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(defaultValue = "common") String directory) {
        String url = minIoService.uploadFile(file, directory);
        return Result.success(Map.of("url", url, "objectName", url));
    }
}
