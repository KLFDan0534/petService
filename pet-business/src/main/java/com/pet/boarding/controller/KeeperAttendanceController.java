package com.pet.boarding.controller;

import com.pet.boarding.dto.AttendanceCheckRequestDTO;
import com.pet.boarding.dto.KeeperAttendanceDTO;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.PageRequestDTO;
import com.pet.common.PageResult;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/keeper-attendance")
@Tag(name = "【用户端】考勤管理", description = "看护者打卡考勤管理（看护者签到/商家查看）")
@Slf4j
public class KeeperAttendanceController {

    private final KeeperAttendanceService attendanceService;

    public KeeperAttendanceController(KeeperAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 看护者签到
     *
     * <p>API: POST /api/keeper-attendance/check-in</p>
     * <p>请求来源：看护者端上下班打卡页，点击"签到"按钮</p>
     * <p>权限要求：KEEPER角色（@PreAuthorize("hasRole('KEEPER')")）</p>
     * <p>输入参数：@body AttendanceCheckRequestDTO - 包含签到位置经纬度（用于地理围栏校验）</p>
     * <p>返回数据：KeeperAttendanceDTO - 签到记录，包含签到时间、位置和状态</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 签到参数错误或不在允许的签到范围内</li>
     *   <li>403 - 非看护者无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "看护者签到", description = "看护者进行上班签到打卡")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "签到成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperAttendanceDTO> checkIn(@AuthenticationPrincipal JwtAuthenticationToken token,
                                               @Valid @RequestBody AttendanceCheckRequestDTO request) {
        return Result.success(attendanceService.checkIn(token.getUserId(), request));
    }

    /**
     * 看护者签退
     *
     * <p>API: POST /api/keeper-attendance/check-out</p>
     * <p>请求来源：看护者端上下班打卡页，点击"签退"按钮</p>
     * <p>权限要求：KEEPER角色（@PreAuthorize("hasRole('KEEPER')")）</p>
     * <p>输入参数：@body AttendanceCheckRequestDTO - 包含签退位置经纬度</p>
     * <p>返回数据：KeeperAttendanceDTO - 签退记录，包含签到和签退时间</p>
     * <p>异常情况：
     * <ul>
     *   <li>400 - 尚未签到或不在允许的签退范围内</li>
     *   <li>403 - 非看护者无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @PostMapping("/check-out")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "看护者签退", description = "看护者进行下班签退打卡")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "签退成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperAttendanceDTO> checkOut(@AuthenticationPrincipal JwtAuthenticationToken token,
                                                @Valid @RequestBody AttendanceCheckRequestDTO request) {
        return Result.success(attendanceService.checkOut(token.getUserId(), request));
    }

    /**
     * 获取当前签到状态
     *
     * <p>API: GET /api/keeper-attendance/me/current</p>
     * <p>请求来源：看护者端首页状态栏，显示当前是否已签到</p>
     * <p>权限要求：KEEPER角色（@PreAuthorize("hasRole('KEEPER')")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：KeeperAttendanceDTO - 当前签到状态，包含最近签到记录（若已签退则返回null或已签退状态）</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非看护者无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me/current")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "获取当前签到状态", description = "获取看护者当前的签到/签退状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回当前状态"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<KeeperAttendanceDTO> current(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(attendanceService.current(token.getUserId()));
    }

    /**
     * 获取今日打卡记录
     *
     * <p>API: GET /api/keeper-attendance/me/today</p>
     * <p>请求来源：看护者端考勤统计页，查看今日签到/签退记录</p>
     * <p>权限要求：KEEPER角色（@PreAuthorize("hasRole('KEEPER')")）</p>
     * <p>输入参数：无（从token中提取用户ID）</p>
     * <p>返回数据：List&lt;KeeperAttendanceDTO&gt; - 今日的签到/签退记录列表</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非看护者无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/me/today")
    @PreAuthorize("hasRole('KEEPER')")
    @Operation(summary = "获取今日打卡记录", description = "获取看护者今日的打卡记录列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回今日打卡记录"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperAttendanceDTO>> today(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(attendanceService.today(token.getUserId()));
    }

    /**
     * 商家获取今日考勤
     *
     * <p>API: GET /api/keeper-attendance/merchant/today</p>
     * <p>请求来源：商家后台考勤管理页，查看今日所有看护者的考勤统计</p>
     * <p>权限要求：MERCHANT角色（@PreAuthorize("hasRole('MERCHANT')")）</p>
     * <p>输入参数：无（从token中提取用户ID，关联到商家）</p>
     * <p>返回数据：List&lt;KeeperAttendanceDTO&gt; - 今日该商家下所有看护者的考勤记录</p>
     * <p>异常情况：
     * <ul>
     *   <li>403 - 非商家无权限</li>
     *   <li>500 - 服务器内部错误</li>
     * </ul>
     * </p>
     */
    @GetMapping("/merchant/today")
    @PreAuthorize("hasRole('MERCHANT')")
    @Operation(summary = "获取商家今日考勤", description = "商家查看今日所有看护者的考勤记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回考勤记录列表"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<KeeperAttendanceDTO>> merchantToday(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(attendanceService.listMerchantToday(token.getUserId()));
    }

    /**
     * 管理员分页查询全部考勤记录
     *
     * <p>API: GET /api/keeper-attendance/admin-list</p>
     * <p>权限要求：ADMIN角色（@PreAuthorize("hasRole('ADMIN')")）</p>
     * <p>输入参数：分页参数（page/size），可选按商家精确过滤</p>
     * <p>返回数据：PageResult&lt;KeeperAttendanceDTO&gt; - 考勤记录分页（含看护者、商家名称、上岗状态）</p>
     */
    @GetMapping("/admin-list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询全部考勤记录", description = "管理员分页查看所有看护者的考勤记录，可按商家过滤")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回考勤记录分页"),
            @ApiResponse(responseCode = "403", description = "无权限访问"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<KeeperAttendanceDTO>> adminList(PageRequestDTO pageParam,
                                                             @RequestParam(required = false) Long merchant_id_wsh) {
        log.info("Calling adminList(merchant_id_wsh={})", merchant_id_wsh);
        var page = attendanceService.pageAll(pageParam, merchant_id_wsh);
        var dtoList = page.getRecords()
                .stream()
                .map(attendanceService::toDTO)
                .collect(Collectors.toList());
        PageResult<KeeperAttendanceDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.copyPageInfo(page);
        return Result.success(result);
    }
}
