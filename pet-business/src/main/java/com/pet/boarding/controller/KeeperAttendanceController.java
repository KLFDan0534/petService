package com.pet.boarding.controller;

import com.pet.boarding.dto.AttendanceCheckRequestDTO;
import com.pet.boarding.dto.KeeperAttendanceDTO;
import com.pet.boarding.service.KeeperAttendanceService;
import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/keeper-attendance")
@Tag(name = "【用户端】考勤管理", description = "看护者打卡考勤管理（看护者签到/商家查看）")
public class KeeperAttendanceController {

    private final KeeperAttendanceService attendanceService;

    public KeeperAttendanceController(KeeperAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

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
}
