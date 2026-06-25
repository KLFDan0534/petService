package com.pet.system.controller;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.Result;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pet.system.vo.RoleVO;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class RoleController {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleController(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 获取所有角色列表
     * @return 角色列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/roles")
    public Result<List<RoleVO>> listRoles() {
        log.info("调用 listRoles()");
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

    /**
     * 创建角色
     * @param role 角色信息
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PostMapping("/roles")
    public Result<Void> createRole(@RequestBody Role role) {
        log.info("调用 createRole()");
        roleMapper.insert(role);
        return Result.success();
    }

    /**
     * 更新角色信息
     * @param id 角色ID
     * @param role 角色信息
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        log.info("调用 updateRole()");
        role.setId_wsh(id);
        roleMapper.updateById(role);
        return Result.success();
    }

    /**
     * 删除角色
     * @param id 角色ID
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        log.info("调用 deleteRole()");
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRole_id_wsh, id));
        return Result.success();
    }

    /**
     * 获取用户的所有角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @GetMapping("/users/{userId}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long userId) {
        log.info("调用 getUserRoles()");
        List<UserRole> urs = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUser_id_wsh, userId));
        return Result.success(urs.stream().map(UserRole::getRole_id_wsh).collect(Collectors.toList()));
    }

    /**
     * 设置用户的角色
     * @param userId 用户ID
     * @param body 请求体，包含role_ids_wsh角色ID列表
     * @return 无返回值
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    @PutMapping("/users/{userId}/roles")
    public Result<Void> setUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        log.info("调用 setUserRoles()");
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUser_id_wsh, userId));
        List<Long> roleIds = body.get("role_ids_wsh");
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUser_id_wsh(userId);
                ur.setRole_id_wsh(roleId);
                userRoleMapper.insert(ur);
            }
        }
        return Result.success();
    }
}
