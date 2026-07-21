package com.pet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.Result;
import com.pet.system.dto.RoleCreateRequestDTO;
import com.pet.system.dto.RoleUpdateRequestDTO;
import com.pet.system.dto.SetUserRolesRequestDTO;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import com.pet.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@Tag(name = "【后台管理】角色管理", description = "管理员管理角色及用户角色分配")
public class RoleController {
    private static final String CUSTOMER_SERVICE_ROLE_CODE = "CUSTOMER_SERVICE";
    private static final String CUSTOMER_SERVICE_MANAGED_MESSAGE =
            "CUSTOMER_SERVICE role is managed by merchant customer service applications";

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleController(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @GetMapping("/roles")
    @Operation(summary = "获取角色列表", description = "管理员获取所有角色及其用户数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功，返回角色列表及用户数量"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<RoleVO>> listRoles() {
        log.info("listRoles() called");
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .orderByAsc(Role::getId_wsh)
                        .last("LIMIT 1000"));
        List<UserRole> allLinks = userRoleMapper.selectList(null);
        Map<Long, Integer> countMap = new HashMap<>();
        for (UserRole ur : allLinks) {
            if (ur.getRole_id_wsh() != null) {
                countMap.merge(ur.getRole_id_wsh(), 1, Integer::sum);
            }
        }
        List<RoleVO> result = new ArrayList<>(roles.size());
        for (Role r : roles) {
            RoleVO vo = new RoleVO();
            vo.setId_wsh(r.getId_wsh());
            vo.setName_wsh(r.getName_wsh());
            vo.setCode_wsh(r.getCode_wsh());
            vo.setDescription_wsh(r.getDescription_wsh());
            vo.setCreated_at_wsh(r.getCreated_at_wsh());
            vo.setUser_count_wsh(countMap.getOrDefault(r.getId_wsh(), 0));
            result.add(vo);
        }
        return Result.success(result);
    }

    @PostMapping("/roles")
    @Operation(summary = "创建角色", description = "管理员创建新角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> createRole(@RequestBody RoleCreateRequestDTO request) {
        log.info("createRole() called");
        if (request != null && isCustomerServiceCode(request.getCode_wsh())) {
            throw new BusinessException(400, CUSTOMER_SERVICE_MANAGED_MESSAGE);
        }
        Role role = new Role();
        role.setName_wsh(request.getName_wsh());
        role.setCode_wsh(request.getCode_wsh());
        role.setDescription_wsh(request.getDescription_wsh());
        roleMapper.insert(role);
        return Result.success();
    }

    @PutMapping("/roles/{id}")
    @Operation(summary = "更新角色信息", description = "管理员更新角色信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> updateRole(@Parameter(description = "角色ID") @PathVariable Long id,
                                   @RequestBody RoleUpdateRequestDTO request) {
        log.info("updateRole() called");
        Role existing = roleMapper.selectById(id);
        if (isCustomerServiceRole(existing)
                || (request != null && isCustomerServiceCode(request.getCode_wsh()))) {
            throw new BusinessException(400, CUSTOMER_SERVICE_MANAGED_MESSAGE);
        }
        Role role = new Role();
        role.setId_wsh(id);
        role.setName_wsh(request.getName_wsh());
        role.setCode_wsh(request.getCode_wsh());
        role.setDescription_wsh(request.getDescription_wsh());
        roleMapper.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "删除角色", description = "管理员删除角色（客服角色由商家管理）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("deleteRole() called");
        Role existing = roleMapper.selectById(id);
        if (isCustomerServiceRole(existing)) {
            throw new BusinessException(400, CUSTOMER_SERVICE_MANAGED_MESSAGE);
        }
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRole_id_wsh, id));
        return Result.success();
    }

    @GetMapping("/users/{userId}/roles")
    @Operation(summary = "获取用户角色", description = "管理员获取用户分配的所有角色ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功，返回角色ID列表"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<Long>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("getUserRoles() called");
        List<UserRole> urs = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUser_id_wsh, userId));
        return Result.success(urs.stream().map(UserRole::getRole_id_wsh).collect(Collectors.toList()));
    }

    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "设置用户角色", description = "管理员设置用户的非客服角色")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Void> setUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId,
                                     @Valid @RequestBody SetUserRolesRequestDTO request) {
        log.info("setUserRoles() called");
        Role customerServiceRole = findCustomerServiceRole();
        Long customerServiceRoleId = customerServiceRole != null ? customerServiceRole.getId_wsh() : null;
        boolean alreadyCustomerService = isExistingUserCustomerService(userId, customerServiceRoleId);

        Set<Long> requestedRoleIds = new LinkedHashSet<>();
        if (request.getRole_ids_wsh() != null) {
            for (Long roleId : request.getRole_ids_wsh()) {
                if (roleId != null) {
                    requestedRoleIds.add(roleId);
                }
            }
        }

        if (customerServiceRoleId != null && requestedRoleIds.contains(customerServiceRoleId) && !alreadyCustomerService) {
            throw new BusinessException(400, "CUSTOMER_SERVICE role must be granted by merchant approval");
        }
        requestedRoleIds.remove(customerServiceRoleId);

        userRoleMapper.deleteUserRolesExcept(userId, customerServiceRoleId);

        for (Long roleId : requestedRoleIds) {
            if (userRoleMapper.restoreUserRole(userId, roleId) > 0) {
                continue;
            }
            UserRole ur = new UserRole();
            ur.setUser_id_wsh(userId);
            ur.setRole_id_wsh(roleId);
            userRoleMapper.insert(ur);
        }
        return Result.success();
    }

    private boolean isExistingUserCustomerService(Long userId, Long customerServiceRoleId) {
        if (customerServiceRoleId == null) {
            return false;
        }
        Long exists = userRoleMapper.countActiveUserRole(userId, customerServiceRoleId);
        return exists != null && exists > 0;
    }

    private Role findCustomerServiceRole() {
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode_wsh, CUSTOMER_SERVICE_ROLE_CODE)
                .last("LIMIT 1"));
    }

    private boolean isCustomerServiceRole(Role role) {
        return role != null && isCustomerServiceCode(role.getCode_wsh());
    }

    private boolean isCustomerServiceCode(String code) {
        return code != null && CUSTOMER_SERVICE_ROLE_CODE.equalsIgnoreCase(code.trim());
    }
}
