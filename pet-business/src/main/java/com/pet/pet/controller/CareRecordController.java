package com.pet.pet.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.operation.service.impl.MinIoService;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.service.CareRecordService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/care-records")
@Tag(name = "护理记录", description = "寄养期间的日常护理记录")
@Slf4j
public class CareRecordController {

    private final CareRecordService careRecordService;
    private final MinIoService minIoService;

    public CareRecordController(CareRecordService careRecordService, MinIoService minIoService) {
        this.careRecordService = careRecordService;
        this.minIoService = minIoService;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private List<MultipartFile> mergeFiles(MultipartFile[] files, MultipartFile file) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            merged.addAll(Arrays.asList(files));
        }
        if (file != null) {
            merged.add(file);
        }
        return merged;
    }

    /**
     * 根据订单ID获取护理记录列表
     * @param orderId 订单ID
     * @return 护理记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单获取护理记录", description = "获取指定订单的护理记录")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CareRecord>> listByOrder(@PathVariable Long orderId) {
        log.info("调用 listByOrder()");
        return Result.success(careRecordService.listByOrder(orderId));
    }

    /**
     * 根据ID获取护理记录详情
     * @param id 护理记录ID
     * @return 护理记录详情
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取护理记录")
    @PreAuthorize("isAuthenticated()")
    public Result<CareRecord> getById(@PathVariable Long id) {
        log.info("调用 getById()");
        return Result.success(careRecordService.getById(id));
    }

    /**
     * 创建护理记录并上传照片文件
     * @param token 当前用户认证信息
     * @param orderIdWsh 订单ID
     * @param orderId 订单ID（备用字段名）
     * @param petIdWsh 宠物ID
     * @param petId 宠物ID（备用字段名）
     * @param typeWsh 护理类型
     * @param type 护理类型（备用字段名）
     * @param contentWsh 护理内容
     * @param content 护理内容（备用字段名）
     * @param recordTimeWsh 记录时间
     * @param recordTime 记录时间（备用字段名）
     * @param files 上传照片文件数组
     * @param file 单个上传照片文件
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "创建护理记录并上传照片")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecord> upload(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @RequestParam("order_id_wsh") Long orderIdWsh,
                                     @RequestParam(value = "orderId", required = false) Long orderId,
                                     @RequestParam(value = "pet_id_wsh", required = false) Long petIdWsh,
                                     @RequestParam(value = "petId", required = false) Long petId,
                                     @RequestParam(value = "type_wsh", required = false) String typeWsh,
                                     @RequestParam(value = "type", required = false) String type,
                                     @RequestParam(value = "content_wsh", required = false) String contentWsh,
                                     @RequestParam(value = "content", required = false) String content,
                                     @RequestParam(value = "record_time_wsh", required = false) String recordTimeWsh,
                                     @RequestParam(value = "recordTime", required = false) String recordTime,
                                     @RequestParam(value = "files", required = false) MultipartFile[] files,
                                     @RequestParam(value = "file", required = false) MultipartFile file) {
        log.info("调用 upload()");
        Long resolvedOrderId = orderIdWsh != null ? orderIdWsh : orderId;
        Long resolvedPetId = petIdWsh != null ? petIdWsh : petId;
        String resolvedType = firstNonBlank(typeWsh, type);
        String resolvedContent = firstNonBlank(contentWsh, content);
        String resolvedRecordTime = firstNonBlank(recordTimeWsh, recordTime);

        List<MultipartFile> mergedFiles = mergeFiles(files, file);
        List<String> imageUrls = new ArrayList<>();
        if (!mergedFiles.isEmpty()) {
            String dir = "care-records/" + resolvedOrderId;
            for (MultipartFile f : mergedFiles) {
                String objectName = minIoService.uploadFile(f, dir);
                imageUrls.add(minIoService.getFileUrl(objectName));
            }
        }

        CareRecord record = new CareRecord();
        record.setOrder_id_wsh(resolvedOrderId);
        record.setPet_id_wsh(resolvedPetId);
        record.setKeeper_id_wsh(token.getUserId());
        record.setType_wsh(resolvedType != null ? resolvedType : "photo");
        record.setContent_wsh(resolvedContent);
        record.setImages_wsh(imageUrls.isEmpty() ? null : String.join(",", imageUrls));
        record.setRecord_time_wsh(resolvedRecordTime != null ? LocalDateTime.parse(resolvedRecordTime) : LocalDateTime.now());

        return Result.success(careRecordService.create(record));
    }

    /**
     * 创建护理记录
     * @param record 护理记录信息
     * @return 创建的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping
    @Operation(summary = "创建护理记录")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecord> create(@Valid @RequestBody CareRecord record) {
        log.info("调用 create()");
        return Result.success(careRecordService.create(record));
    }

    /**
     * 更新护理记录
     * @param id 护理记录ID
     * @param record 护理记录信息
     * @return 更新后的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/{id}")
    @Operation(summary = "更新护理记录")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecord> update(@PathVariable Long id, @Valid @RequestBody CareRecord record) {
        log.info("调用 update()");
        return Result.success(careRecordService.update(id, record));
    }

    /**
     * 删除护理记录
     * @param id 护理记录ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/{id}")
    @Operation(summary = "删除护理记录")
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("调用 delete()");
        careRecordService.delete(id);
        return Result.success();
    }
}
