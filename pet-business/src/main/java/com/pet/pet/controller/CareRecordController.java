package com.pet.pet.controller;

import com.pet.common.BusinessException;
import com.pet.common.Result;
import com.pet.operation.service.impl.MinIoService;
import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordUpdateRequestDTO;
import com.pet.pet.service.CareRecordService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/care-records")
@Tag(name = "【用户端】护理记录管理", description = "宠物护理记录管理（看护者/商家/管理员使用）")
@Slf4j
public class CareRecordController {

    private final CareRecordService careRecordService;
    private final MinIoService minIoService;

    public CareRecordController(CareRecordService careRecordService, MinIoService minIoService) {
        this.careRecordService = careRecordService;
        this.minIoService = minIoService;
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单获取护理记录", description = "根据订单ID获取护理记录列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<List<CareRecordDTO>> listByOrder(@PathVariable @Parameter(description = "订单ID") Long orderId,
                                                   @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(careRecordService.listByOrder(token.getUserId(), isAdmin(token), orderId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取护理记录详情", description = "根据ID获取护理记录详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<CareRecordDTO> getById(@PathVariable @Parameter(description = "护理记录ID") Long id,
                                         @AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(careRecordService.getById(token.getUserId(), isAdmin(token), id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传护理记录图片并创建", description = "上传护理记录图片并创建护理记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecordDTO> upload(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestParam(value = "order_id_wsh", required = false) Long orderIdWsh,
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
        Long resolvedOrderId = orderIdWsh != null ? orderIdWsh : orderId;
        if (resolvedOrderId == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }

        List<String> imageUrls = uploadImages(resolvedOrderId, mergeFiles(files, file));

        CareRecordCreateRequestDTO request = new CareRecordCreateRequestDTO();
        request.setOrder_id_wsh(resolvedOrderId);
        request.setPet_id_wsh(petIdWsh != null ? petIdWsh : petId);
        request.setType_wsh(defaultText(firstNonBlank(typeWsh, type), "photo"));
        request.setContent_wsh(firstNonBlank(contentWsh, content));
        request.setImages_wsh(imageUrls.isEmpty() ? null : String.join(",", imageUrls));
        request.setRecord_time_wsh(parseRecordTime(firstNonBlank(recordTimeWsh, recordTime)));

        return Result.success(careRecordService.create(token.getUserId(), isAdmin(token), request));
    }

    @PostMapping
    @Operation(summary = "创建护理记录", description = "创建新的护理记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecordDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @Valid @RequestBody CareRecordCreateRequestDTO request) {
        return Result.success(careRecordService.create(token.getUserId(), isAdmin(token), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新护理记录", description = "更新护理记录信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<CareRecordDTO> update(@PathVariable @Parameter(description = "护理记录ID") Long id,
                                        @AuthenticationPrincipal JwtAuthenticationToken token,
                                        @Valid @RequestBody CareRecordUpdateRequestDTO request) {
        return Result.success(careRecordService.update(token.getUserId(), isAdmin(token), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除护理记录", description = "删除护理记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "404", description = "资源不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PreAuthorize("hasAnyRole('KEEPER','MERCHANT','ADMIN')")
    public Result<Void> delete(@PathVariable @Parameter(description = "护理记录ID") Long id,
                               @AuthenticationPrincipal JwtAuthenticationToken token) {
        careRecordService.delete(token.getUserId(), isAdmin(token), id);
        return Result.success();
    }

    private List<MultipartFile> mergeFiles(MultipartFile[] files, MultipartFile file) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            Arrays.stream(files)
                    .filter(item -> item != null && !item.isEmpty())
                    .forEach(merged::add);
        }
        if (file != null && !file.isEmpty()) {
            merged.add(file);
        }
        return merged;
    }

    private List<String> uploadImages(Long orderId, List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile item : files) {
            String contentType = item.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new BusinessException(400, "care record file must be an image");
            }
            String objectName = minIoService.uploadFile(item, "care-records/" + orderId);
            imageUrls.add(minIoService.getFileUrl(objectName));
        }
        return imageUrls;
    }

    private LocalDateTime parseRecordTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(value.trim().replace(' ', 'T'));
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isAdmin(JwtAuthenticationToken token) {
        return token != null && token.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
