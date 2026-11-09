package com.pet.marketing.controller;

import com.pet.common.Result;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.dto.CouponQuoteDTO;
import com.pet.marketing.dto.CouponQuoteRequestDTO;
import com.pet.marketing.dto.CouponTemplateCreateRequestDTO;
import com.pet.marketing.dto.CouponTemplateDTO;
import com.pet.marketing.dto.CouponTemplateStatusRequestDTO;
import com.pet.marketing.dto.UserCouponDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.UserCoupon;
import com.pet.marketing.service.CouponService;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "【用户端】优惠券管理", description = "优惠券模板和用户优惠券管理（用户领取/管理员发放）")
public class CouponController {
    private final CouponService couponService;

    /**
     * 【优惠券控制器构造注入】
     *
     * 业务作用：注入优惠券业务服务。
     *
     * 调用场景：Spring 容器创建 CouponController 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 couponService。
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
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 【管理员查询全部优惠券模板】
     *
     * 业务作用：返回所有优惠券模板，供后台管理。
     *
     * 调用场景：后台优惠券模板管理页。
     *
     * 调用链：GET /api/coupons/templates → listTemplates() → couponService.listTemplates(false)。
     *
     * 数据处理：以 activeOnly=false 调用服务层查询全部模板并包装为 Result。
     *
     * 业务规则：需 ADMIN 角色。
     *
     * 状态影响：无。
     *
     * 异常情况：无权限由 @PreAuthorize 拦截返回 403。
     *
     * 注意事项：无。
     */
    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取优惠券模板列表", description = "管理员获取所有优惠券模板列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回模板列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CouponTemplateDTO>> listTemplates() {
        return Result.success(couponService.listTemplates(false));
    }

    /**
     * 【查询可领取的优惠券模板】
     *
     * 业务作用：返回当前生效可领取的模板列表，供用户领取页展示。
     *
     * 调用场景：用户进入领券中心。
     *
     * 调用链：GET /api/coupons/templates/active → listActiveTemplates() → couponService.listTemplates(true)。
     *
     * 数据处理：以 activeOnly=true 调用服务层过滤后包装为 Result。
     *
     * 业务规则：需登录。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401，无权限返回 403。
     *
     * 注意事项：生效判定在服务层实现，控制器不做额外过滤。
     */
    @GetMapping("/templates/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取活跃优惠券模板", description = "获取当前可领取的优惠券模板列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回活跃模板列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CouponTemplateDTO>> listActiveTemplates() {
        return Result.success(couponService.listTemplates(true));
    }

    /**
     * 【管理员创建优惠券模板】
     *
     * 业务作用：创建新的优惠券模板并返回模板详情。
     *
     * 调用场景：后台新增优惠券活动。
     *
     * 调用链：POST /api/coupons/templates → createTemplate() → couponService.createTemplate() → toTemplateDTO()。
     *
     * 数据处理：取当前登录用户 ID 作为创建人，交给服务层落库后把实体转换为 DTO。
     *
     * 业务规则：需 ADMIN 角色；请求体经 @Valid 校验。
     *
     * 状态影响：新增优惠券模板记录（由服务层写入）。
     *
     * 异常情况：参数校验失败返回 400；无权限返回 403。
     *
     * 注意事项：控制器只做参数透传与 DTO 转换，业务校验在服务层。
     */
    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建优惠券模板", description = "管理员创建新的优惠券模板")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功返回模板信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponTemplateDTO> createTemplate(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                    @Valid @RequestBody CouponTemplateCreateRequestDTO request) {
        return Result.success(toTemplateDTO(couponService.createTemplate(token.getUserId(), request)));
    }

    /**
     * 【管理员更新模板状态】
     *
     * 业务作用：启用或禁用指定优惠券模板，并回查最新模板信息返回。
     *
     * 调用场景：后台上下线优惠券活动。
     *
     * 调用链：POST /api/coupons/templates/{id}/status → updateStatus() → couponService.updateTemplateStatus() → couponService.listTemplates(false)。
     *
     * 数据处理：从请求体取 status_wsh 交给服务层更新，然后拉取全部模板并筛出该 ID 返回。
     *
     * 业务规则：需 ADMIN 角色；请求体为空时 status 传 null 交由服务层判定。
     *
     * 状态影响：更新模板状态（由服务层写入）。
     *
     * 异常情况：状态值非法由服务层抛 400；无权限返回 403。
     *
     * 注意事项：更新后为拿到最新数据重新全量查询模板再过滤，属低效写法，且找不到时返回 null。
     */
    @PostMapping("/templates/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新模板状态", description = "管理员更新优惠券模板的启用/禁用状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "状态更新成功返回模板信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponTemplateDTO> updateStatus(@Parameter(description = "模板ID") @PathVariable Long id,
                                                  @RequestBody CouponTemplateStatusRequestDTO request) {
        couponService.updateTemplateStatus(id, request != null ? request.getStatus_wsh() : null);
        // 更新后全量回查再筛出目标模板
        return Result.success(couponService.listTemplates(false).stream()
                .filter(item -> item.getId_wsh().equals(id))
                .findFirst()
                .orElse(null));
    }

    /**
     * 【管理员向指定用户发券】
     *
     * 业务作用：给指定用户发放某模板的优惠券，返回发放数量。
     *
     * 调用场景：后台定向发券。
     *
     * 调用链：POST /api/coupons/templates/{id}/grant → grantToUser() → couponService.grantToUser()。
     *
     * 数据处理：把操作人 ID、模板 ID 与请求体透传给服务层，返回发放条数。
     *
     * 业务规则：需 ADMIN 角色。
     *
     * 状态影响：新增用户优惠券记录（由服务层写入）。
     *
     * 异常情况：参数错误由服务层抛 400；无权限返回 403。
     *
     * 注意事项：无。
     */
    @PostMapping("/templates/{id}/grant")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发放优惠券", description = "管理员向指定用户发放优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发放成功返回发放数量"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> grantToUser(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @Parameter(description = "模板ID") @PathVariable Long id,
                                       @RequestBody CouponGrantRequestDTO request) {
        return Result.success(couponService.grantToUser(token.getUserId(), id, request));
    }

    /**
     * 【管理员向全部用户发券】
     *
     * 业务作用：向所有用户发放指定模板的优惠券，返回发放数量。
     *
     * 调用场景：后台全量营销发券。
     *
     * 调用链：POST /api/coupons/templates/{id}/grant-all → grantToAll() → couponService.grantToAllUsers()。
     *
     * 数据处理：请求体可选，直接透传给服务层。
     *
     * 业务规则：需 ADMIN 角色；请求体允许为空。
     *
     * 状态影响：批量新增用户优惠券记录（由服务层写入）。
     *
     * 异常情况：无权限返回 403；服务层校验失败抛 400。
     *
     * 注意事项：全量发放数据量大，耗时可能较长，注意接口超时设置。
     */
    @PostMapping("/templates/{id}/grant-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发放给所有用户", description = "管理员向所有用户发放优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发放成功返回发放数量"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Integer> grantToAll(@AuthenticationPrincipal JwtAuthenticationToken token,
                                      @Parameter(description = "模板ID") @PathVariable Long id,
                                      @RequestBody(required = false) CouponGrantRequestDTO request) {
        return Result.success(couponService.grantToAllUsers(token.getUserId(), id, request));
    }

    /**
     * 【管理员按条件发券】
     *
     * 业务作用：按注册时间、消费、订单、宠物数等条件筛选用户后定向发券。
     *
     * 调用场景：后台精准营销。
     *
     * 调用链：POST /api/coupons/templates/{id}/grant-condition → grantByCondition() → couponService.grantByCondition()。
     *
     * 数据处理：请求体可选，原样透传给服务层做条件筛选与发放。
     *
     * 业务规则：需 ADMIN 角色；具体条件字段与组合规则在服务层实现。
     *
     * 状态影响：批量新增用户优惠券记录（由服务层写入）。
     *
     * 异常情况：无权限返回 403；条件非法由服务层抛 400。
     *
     * 注意事项：本方法未加 @ApiResponses 声明，接口文档中错误码不完整。
     */
    @PostMapping("/templates/{id}/grant-condition")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "条件发放优惠券", description = "管理员按注册时间、消费、订单、宠物数等条件定向发放优惠券")
    public Result<Integer> grantByCondition(@AuthenticationPrincipal JwtAuthenticationToken token,
                                            @Parameter(description = "模板ID") @PathVariable Long id,
                                            @RequestBody(required = false) CouponGrantRequestDTO request) {
        return Result.success(couponService.grantByCondition(token.getUserId(), id, request));
    }

    /**
     * 【用户领取优惠券】
     *
     * 业务作用：当前登录用户领取指定模板的优惠券并返回券详情。
     *
     * 调用场景：用户在领券中心点击领取。
     *
     * 调用链：POST /api/coupons/templates/{id}/claim → claim() → couponService.claim() → couponService.listMyCoupons()。
     *
     * 数据处理：服务层创建用户券后，重新查询我的优惠券列表并筛出刚领取的那条返回。
     *
     * 业务规则：需登录；每人限领等规则由服务层校验。
     *
     * 状态影响：新增用户优惠券记录（由服务层写入）。
     *
     * 异常情况：已领完/重复领取等由服务层抛 400。
     *
     * 注意事项：领取后重新全量查询我的券再过滤，效率不高，找不到时返回 null。
     */
    @PostMapping("/templates/{id}/claim")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "领取优惠券", description = "用户领取指定的优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "领取成功返回优惠券信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<UserCouponDTO> claim(@AuthenticationPrincipal JwtAuthenticationToken token,
                                       @Parameter(description = "模板ID") @PathVariable Long id) {
        UserCoupon coupon = couponService.claim(token.getUserId(), id);
        // 领取后回查我的券列表取出新券
        return Result.success(couponService.listMyCoupons(token.getUserId()).stream()
                .filter(item -> item.getId_wsh().equals(coupon.getId_wsh()))
                .findFirst()
                .orElse(null));
    }

    /**
     * 【查询我的优惠券】
     *
     * 业务作用：返回当前用户拥有的全部优惠券。
     *
     * 调用场景：用户「我的优惠券」页面。
     *
     * 调用链：GET /api/coupons/my → myCoupons() → couponService.listMyCoupons()。
     *
     * 数据处理：以登录用户 ID 查询后包装为 Result。
     *
     * 业务规则：需登录。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401。
     *
     * 注意事项：无。
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的优惠券", description = "获取当前用户拥有的所有优惠券")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回优惠券列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<UserCouponDTO>> myCoupons(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(couponService.listMyCoupons(token.getUserId()));
    }

    /**
     * 【查询可用优惠券】
     *
     * 业务作用：按商家与订单金额筛选当前用户可用的优惠券。
     *
     * 调用场景：下单页计算可用券。
     *
     * 调用链：GET /api/coupons/available → availableCoupons() → couponService.listAvailableCoupons()。
     *
     * 数据处理：把用户 ID、商家 ID 与订单金额透传给服务层做适用性过滤。
     *
     * 业务规则：需登录；merchant_id_wsh 与 order_amount_wsh 为可选参数。
     *
     * 状态影响：无。
     *
     * 异常情况：未登录返回 401。
     *
     * 注意事项：金额为空时服务层无法做门槛判断，具体降级策略在服务层。
     */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取可用优惠券", description = "获取当前用户可用的优惠券列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回可用优惠券列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<UserCouponDTO>> availableCoupons(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                        @Parameter(description = "商家ID") @RequestParam(required = false) Long merchant_id_wsh,
                                                        @Parameter(description = "订单金额") @RequestParam(required = false) BigDecimal order_amount_wsh) {
        return Result.success(couponService.listAvailableCoupons(token.getUserId(), merchant_id_wsh, order_amount_wsh));
    }

    /**
     * 【优惠券报价】
     *
     * 业务作用：根据订单信息计算最优优惠券方案并返回报价。
     *
     * 调用场景：下单页选择优惠券后试算价格。
     *
     * 调用链：POST /api/coupons/quote → quote() → couponService.quote()。
     *
     * 数据处理：把用户 ID 与报价请求体交给服务层计算最优券与折扣。
     *
     * 业务规则：需登录；最优券选择规则在服务层实现。
     *
     * 状态影响：无（纯计算，不核销券）。
     *
     * 异常情况：未登录返回 401；参数非法由服务层抛 400。
     *
     * 注意事项：仅试算不占用优惠券，真正核销发生在下单流程。
     */
    @PostMapping("/quote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "优惠券报价", description = "根据订单金额计算最优优惠券折扣")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回报价信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CouponQuoteDTO> quote(@AuthenticationPrincipal JwtAuthenticationToken token,
                                        @RequestBody CouponQuoteRequestDTO request) {
        return Result.success(couponService.quote(token.getUserId(), request));
    }

    /**
     * 【模板实体转 DTO】
     *
     * 业务作用：把 CouponTemplate 实体按同名属性拷贝为 CouponTemplateDTO。
     *
     * 调用场景：createTemplate() 返回创建结果时。
     *
     * 调用链：createTemplate() → toTemplateDTO() → BeanUtils.copyProperties()。
     *
     * 数据处理：按属性名自动拷贝，不做字段裁剪或加工。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：依赖实体与 DTO 字段同名，字段改名会导致拷贝静默失效。
     */
    private CouponTemplateDTO toTemplateDTO(CouponTemplate template) {
        CouponTemplateDTO dto = new CouponTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
}
