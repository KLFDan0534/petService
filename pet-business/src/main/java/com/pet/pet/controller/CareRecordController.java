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

    /**
     * 【护理记录控制器构造注入】
     *
     * 业务作用：注入护理记录业务服务与对象存储服务。
     *
     * 调用场景：Spring 容器创建 CareRecordController 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 careRecordService 与 minIoService。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public CareRecordController(CareRecordService careRecordService, MinIoService minIoService) {
        this.careRecordService = careRecordService;
        this.minIoService = minIoService;
    }

    /**
     * 【按订单查询护理记录】
     *
     * 业务作用：返回某订单下的护理记录列表。
     *
     * 调用场景：订单详情页展示护理记录。
     *
     * 调用链：GET /api/care-records/order/{orderId} → listByOrder() → careRecordService.listByOrder()。
     *
     * 数据处理：把登录用户 ID、是否管理员与订单 ID 交给服务层做权限判断与查询。
     *
     * 业务规则：需登录；可见范围（寄养师/商家/管理员/宠物主人）由服务层控制。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401；无权限由服务层抛 403。
     *
     * 注意事项：管理员判定由 isAdmin() 解析 token 权限，非管理员走归属校验。
     */
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

    /**
     * 【查询护理记录详情】
     *
     * 业务作用：按 ID 获取单条护理记录。
     *
     * 调用场景：护理记录详情弹窗。
     *
     * 调用链：GET /api/care-records/{id} → getById() → careRecordService.getById()。
     *
     * 数据处理：透传用户 ID、是否管理员与记录 ID。
     *
     * 业务规则：需登录；可读权限由服务层校验。
     *
     * 状态影响：无。
     *
     * 异常情况：记录不存在抛 404；无权限抛 403。
     *
     * 注意事项：无。
     */
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
        // 订单号为空无法归档图片，直接拒绝
        if (resolvedOrderId == null) {
            throw new BusinessException(400, "order id cannot be empty");
        }

        List<String> imageUrls = uploadImages(resolvedOrderId, mergeFiles(files, file));

        CareRecordCreateRequestDTO request = new CareRecordCreateRequestDTO();
        request.setOrder_id_wsh(resolvedOrderId);
        request.setPet_id_wsh(petIdWsh != null ? petIdWsh : petId);
        request.setType_wsh(defaultText(firstNonBlank(typeWsh, type), "photo")); // 类型缺省按 photo 归档
        request.setContent_wsh(firstNonBlank(contentWsh, content));
        request.setImages_wsh(imageUrls.isEmpty() ? null : String.join(",", imageUrls));
        request.setRecord_time_wsh(parseRecordTime(firstNonBlank(recordTimeWsh, recordTime)));

        return Result.success(careRecordService.create(token.getUserId(), isAdmin(token), request));
    }

    /**
     * 【创建护理记录】
     *
     * 业务作用：以 JSON 方式创建一条护理记录。
     *
     * 调用场景：寄养师/商家在订单过程中记录喂食、遛狗等。
     *
     * 调用链：POST /api/care-records → create() → careRecordService.create()。
     *
     * 数据处理：请求体经 @Valid 校验后透传给服务层，返回创建的记录。
     *
     * 业务规则：需 KEEPER/MERCHANT/ADMIN 角色；管理员标记由 isAdmin() 决定。
     *
     * 状态影响：新增护理记录（由服务层写入）。
     *
     * 异常情况：参数校验失败返回 400；无权限返回 403。
     *
     * 注意事项：无。
     */
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

    /**
     * 【更新护理记录】
     *
     * 业务作用：修改已有护理记录的内容。
     *
     * 调用场景：寄养师/商家更正护理记录。
     *
     * 调用链：PUT /api/care-records/{id} → update() → careRecordService.update()。
     *
     * 数据处理：透传记录 ID、操作人 ID、是否管理员与更新请求体。
     *
     * 业务规则：需 KEEPER/MERCHANT/ADMIN 角色；可写权限由服务层校验。
     *
     * 状态影响：更新护理记录（由服务层写入）。
     *
     * 异常情况：记录不存在抛 404；无权限抛 403；参数非法抛 400。
     *
     * 注意事项：无。
     */
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

    /**
     * 【删除护理记录】
     *
     * 业务作用：删除指定护理记录。
     *
     * 调用场景：寄养师/商家删除误记的记录。
     *
     * 调用链：DELETE /api/care-records/{id} → delete() → careRecordService.delete()。
     *
     * 数据处理：透传记录 ID、操作人 ID 与是否管理员。
     *
     * 业务规则：需 KEEPER/MERCHANT/ADMIN 角色；可写权限由服务层校验。
     *
     * 状态影响：删除护理记录（由服务层写入）。
     *
     * 异常情况：记录不存在抛 404；无权限抛 403。
     *
     * 注意事项：无。
     */
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

    /**
     * 【合并多文件参数】
     *
     * 业务作用：把 files 数组参数与单个 file 参数合并成一个非空文件列表。
     *
     * 调用场景：upload() 兼容前端两种上传字段名时。
     *
     * 调用链：upload() → mergeFiles() → uploadImages()。
     *
     * 数据处理：先收集 files 中非空项，再追加非空的 file。
     *
     * 业务规则：null 或空文件会被过滤掉。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：兼容 files 与 file 两种字段名，是为了兼容不同前端的上传写法。
     */
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

    /**
     * 【上传护理记录图片】
     *
     * 业务作用：逐个校验图片类型并上传到对象存储，返回可访问 URL 列表。
     *
     * 调用场景：upload() 处理 multipart 上传。
     *
     * 调用链：upload() → uploadImages() → minIoService.uploadFile() → minIoService.getFileUrl()。
     *
     * 数据处理：校验 contentType 以 image/ 开头 → 上传到 care-records/{orderId} 目录 → 收集访问 URL。
     *
     * 业务规则：非图片文件直接拒绝。
     *
     * 状态影响：向对象存储写入文件（无数据库写入）。
     *
     * 异常情况：contentType 为空或非图片抛 BusinessException(400, "care record file must be an image")；上传异常由 MinIoService 抛出。
     *
     * 注意事项：逐个文件串行上传，文件多时耗时较长；异常提示为英文，与其他中文提示不一致。
     */
    private List<String> uploadImages(Long orderId, List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile item : files) {
            String contentType = item.getContentType();
            // 只接受图片类型，其余一律拒绝
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new BusinessException(400, "care record file must be an image");
            }
            String objectName = minIoService.uploadFile(item, "care-records/" + orderId);
            imageUrls.add(minIoService.getFileUrl(objectName));
        }
        return imageUrls;
    }

    /**
     * 【解析记录时间】
     *
     * 业务作用：把前端传入的时间字符串解析为 LocalDateTime。
     *
     * 调用场景：upload() 处理 record_time 参数时。
     *
     * 调用链：upload() → parseRecordTime()。
     *
     * 数据处理：空值返回当前时间；否则把空格替换为 T 后按 ISO 格式解析。
     *
     * 业务规则：仅支持 yyyy-MM-ddTHH:mm:ss 形式（空格分隔也可）。
     *
     * 状态影响：无。
     *
     * 异常情况：格式不匹配抛 DateTimeParseException（未包装为业务异常）。
     *
     * 注意事项：不支持自定义格式，前端需保证时间格式。
     */
    private LocalDateTime parseRecordTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        // 兼容空格分隔的时间格式
        return LocalDateTime.parse(value.trim().replace(' ', 'T'));
    }

    /**
     * 【取第一个非空白字符串】
     *
     * 业务作用：在带 _wsh 后缀与不带后缀的两个参数中取有效值，兼容新旧字段命名。
     *
     * 调用场景：upload() 解析 orderId、petId、type、content、recordTime 时。
     *
     * 调用链：upload() → firstNonBlank()。
     *
     * 数据处理：first 非空白则返回 first，否则返回 second（second 空白时返回 null）。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：不做 trim，返回原值。
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    /**
     * 【空值兜底】
     *
     * 业务作用：值为空时返回默认值。
     *
     * 调用场景：upload() 给 type 字段兜底为 photo。
     *
     * 调用链：upload() → defaultText()。
     *
     * 数据处理：null 或空白返回 fallback，否则返回原值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 【判断是否管理员】
     *
     * 业务作用：从登录凭证中判断当前用户是否具备 ROLE_ADMIN 权限。
     *
     * 调用场景：所有需要区分管理员与普通内部人员的接口。
     *
     * 调用链：listByOrder()/getById()/upload()/create()/update()/delete() → isAdmin()。
     *
     * 数据处理：遍历 token 的权限列表，比对 "ROLE_ADMIN" 字符串。
     *
     * 业务规则：token 为空时视为非管理员。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：硬编码角色名，与 @PreAuthorize 中的 hasRole('ADMIN') 需保持一致。
     */
    private boolean isAdmin(JwtAuthenticationToken token) {
        return token != null && token.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
