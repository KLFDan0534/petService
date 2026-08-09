package com.pet.ai.controller;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.Result;
import com.pet.ai.dto.AiReportCreateRequestDTO;
import com.pet.ai.dto.AiReportDTO;
import com.pet.ai.entity.AiReport;
import com.pet.ai.service.AiReportService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "【AI】AI报告", description = "AI报告生成和管理（用户/看护者/管理员使用）")
@Slf4j
public class AiReportController {

    private final AiReportService aiReportService;

    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    /**
     * 【业务名称】按订单查询AI报告（接口）
     * <p>业务作用：根据订单ID查询关联的AI报告列表，返回DTO格式的数据给前端。</p>
     * <p>调用场景：前端订单详情页查看AI报告列表。</p>
     * <p>调用链：前端GET /api/ai/reports/order/{orderId} → getByOrder() → AiReportService.getReportsByOrder() → 鉴权 → MySQL/Chroma查询 → 转换为DTO列表</p>
     * <p>数据处理：从token获取userId；委托service查询；将AiReport实体通过toDTO()转换为AiReportDTO。</p>
     * <p>业务规则：需用户登录；鉴权逻辑在Service层。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：鉴权失败返回403；资源不存在返回404。</p>
     * <p>注意事项：参数orderId通过路径变量传入。</p>
     */
    @GetMapping("/reports/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据订单获取报告", description = "根据订单ID获取AI报告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<AiReportDTO>> getByOrder(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @Parameter(description = "订单ID") @PathVariable Long orderId) {
        log.info("调用 getByOrder()");
        List<AiReport> list = aiReportService.getReportsByOrder(token.getUserId(), orderId);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【业务名称】按宠物查询AI报告（接口）
     * <p>业务作用：根据宠物ID查询所有关联的AI报告列表。</p>
     * <p>调用场景：前端宠物详情页查看该宠物的所有历史AI报告。</p>
     * <p>调用链：前端GET /api/ai/reports/pet/{petId} → getByPet() → AiReportService.getReportsByPet()</p>
     * <p>数据处理：从token获取userId；委托service查询；实体转DTO。</p>
     * <p>业务规则：需用户登录；仅宠物主可查看。</p>
     * <p>状态影响：只读操作。</p>
     * <p>异常情况：无权限返回403。</p>
     * <p>注意事项：petId通过路径变量传入。</p>
     */
    @GetMapping("/reports/pet/{petId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据宠物获取报告", description = "根据宠物ID获取AI报告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "404", description = "资源不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<AiReportDTO>> getByPet(@AuthenticationPrincipal JwtAuthenticationToken token,
                                              @Parameter(description = "宠物ID") @PathVariable Long petId) {
        log.info("调用 getByPet()");
        List<AiReport> list = aiReportService.getReportsByPet(token.getUserId(), petId);
        return Result.success(list.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * 【业务名称】手动创建AI报告（接口）
     * <p>业务作用：看护人或管理员手动提交报告内容创建AI报告，不经过AI模型生成。</p>
     * <p>调用场景：看护人完成服务后手动录入护理总结。</p>
     * <p>调用链：前端POST /api/ai/reports → create() → AiReportService.createReport() → MySQL插入 + Chroma存储</p>
     * <p>数据处理：从token获取userId；从request body获取报告信息；委托service创建；实体转DTO。</p>
     * <p>业务规则：需要ADMIN或KEEPER角色；请求体包含order_id/pet_id/keeper_id/content/type。</p>
     * <p>状态影响：新增一条AI报告记录。</p>
     * <p>异常情况：参数错误返回400；无权限返回403。</p>
     * <p>注意事项：Content由用户直接提供，不会经过AI模型。</p>
     */
    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
    @Operation(summary = "创建报告", description = "手动创建AI报告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AiReportDTO> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                      @RequestBody AiReportCreateRequestDTO request) {
        log.info("调用 create()");
        return Result.success(toDTO(aiReportService.createReport(token.getUserId(), request)));
    }

    /**
     * 【业务名称】AI生成护理建议报告（接口）
     * <p>业务作用：根据宠物/看护人/订单信息调用AI生成护理建议报告。参数可通过请求体或请求参数两种方式传入。</p>
     * <p>调用场景：用户在前端点击"生成护理建议"按钮。</p>
     * <p>调用链：前端POST /api/ai/care-suggestion → generateCareSuggestion() → resolveLong解析参数 → AiReportService.generateCareSuggestion() → AI生成/降级 → 返回DTO</p>
     * <p>数据处理：resolveLong()统一解析参数（支持JSON body驼峰/下划线键名和Query Param两种方式）；委托service生成；实体转DTO。</p>
     * <p>业务规则：需用户登录；petId和orderId至少提供一个。</p>
     * <p>状态影响：新增一条type=care的AI报告。</p>
     * <p>异常情况：参数错误返回400；无权限返回403。</p>
     * <p>注意事项：body和RequestParam同时传参时Query Param优先。</p>
     */
    @PostMapping("/care-suggestion")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成护理建议", description = "AI生成护理建议报告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AiReportDTO> generateCareSuggestion(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long keeperId,
            @RequestParam(required = false) Long orderId) {
        log.info("调用 generateCareSuggestion()");
        petId = resolveLong(petId, body, "petId", "pet_id_wsh");
        keeperId = resolveLong(keeperId, body, "keeperId", "keeper_id_wsh");
        orderId = resolveLong(orderId, body, "orderId", "order_id_wsh");
        return Result.success(toDTO(aiReportService.generateCareSuggestion(token.getUserId(), petId, keeperId, orderId)));
    }

    /**
     * 【业务名称】AI生成寄养总结报告（接口）
     * <p>业务作用：根据宠物/看护人/订单信息调用AI生成寄养总结报告。参数可通过请求体或请求参数传入。</p>
     * <p>调用场景：用户在订单完成后点击"查看寄养总结"。</p>
     * <p>调用链：前端POST /api/ai/boarding-report → generateBoardingReport() → resolveLong解析参数 → AiReportService.generateBoardingReport() → AI生成 → 返回DTO</p>
     * <p>数据处理：resolveLong()统一解析参数；委托service生成（含鉴权）；实体转DTO。</p>
     * <p>业务规则：需用户登录；orderId必填。</p>
     * <p>状态影响：新增一条type=final的AI报告。</p>
     * <p>异常情况：参数错误返回400；无权限返回403。</p>
     * <p>注意事项：body和RequestParam同时传参时Query Param优先。</p>
     */
    @PostMapping("/boarding-report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成寄养报告", description = "AI生成寄养总结报告")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限访问"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<AiReportDTO> generateBoardingReport(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long keeperId,
            @RequestParam(required = false) Long orderId) {
        log.info("调用 generateBoardingReport()");
        petId = resolveLong(petId, body, "petId", "pet_id_wsh");
        keeperId = resolveLong(keeperId, body, "keeperId", "keeper_id_wsh");
        orderId = resolveLong(orderId, body, "orderId", "order_id_wsh");
        return Result.success(toDTO(aiReportService.generateBoardingReport(token.getUserId(), petId, keeperId, orderId)));
    }

    /**
     * 【业务名称】AiReport实体转DTO
     * <p>业务作用：将AiReport实体对象转换为前端展示的AiReportDTO对象，剥离内部字段只暴露必要数据。</p>
     * <p>注意事项：字段一一映射，不涉及数据转换逻辑。</p>
     */
    private AiReportDTO toDTO(AiReport entity) {
        AiReportDTO dto = new AiReportDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setOrder_id_wsh(entity.getOrder_id_wsh());
        dto.setPet_id_wsh(entity.getPet_id_wsh());
        dto.setKeeper_id_wsh(entity.getKeeper_id_wsh());
        dto.setContent_wsh(entity.getContent_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setCreated_at_wsh(entity.getCreated_at_wsh());
        return dto;
    }

    /**
     * 【业务名称】参数解析辅助方法
     * <p>业务作用：统一解析接口请求参数，支持Query Param优先，其次从JSON body中查找（支持驼峰和下划线两种键名）。</p>
     * <p>调用场景：generateCareSuggestion和generateBoardingReport两个接口的参数解析。</p>
     * <p>业务规则：queryValue非null直接返回；从body中先查camelKey再查snakeKey；支持Number类型直接转换和字符串解析。</p>
     * <p>注意事项：String类型解析时调用Long.parseLong，格式异常会抛出NumberFormatException。</p>
     */
    private Long resolveLong(Long queryValue, Map<String, Object> body, String camelKey, String snakeKey) {
        if (queryValue != null) {
            return queryValue;
        }
        if (body == null) {
            return null;
        }
        Object value = body.containsKey(camelKey) ? body.get(camelKey) : body.get(snakeKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.parseLong(text);
    }
}
