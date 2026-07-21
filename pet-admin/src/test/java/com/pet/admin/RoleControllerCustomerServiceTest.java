package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.system.controller.RoleController;
import com.pet.system.dto.RoleCreateRequestDTO;
import com.pet.system.dto.SetUserRolesRequestDTO;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerCustomerServiceTest {

    private static final long USER_ID = 20L;
    private static final long CUSTOMER_SERVICE_ROLE_ID = 5L;
    private static final long OWNER_ROLE_ID = 1L;

    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;

    @Test
    void adminCannotCreateCustomerServiceRole() {
        RoleCreateRequestDTO request = new RoleCreateRequestDTO();
        request.setName_wsh("Customer Service");
        request.setCode_wsh("customer_service");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller().createRole(request));

        assertEquals(400, exception.getCode());
        verify(roleMapper, never()).insert(any(Role.class));
    }

    @Test
    void adminCannotGrantCustomerServiceRoleThroughUserRoles() {
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(customerServiceRole());
        when(userRoleMapper.countActiveUserRole(USER_ID, CUSTOMER_SERVICE_ROLE_ID)).thenReturn(0L);

        SetUserRolesRequestDTO request = new SetUserRolesRequestDTO();
        request.setRole_ids_wsh(List.of(OWNER_ROLE_ID, CUSTOMER_SERVICE_ROLE_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller().setUserRoles(USER_ID, request));

        assertEquals(400, exception.getCode());
        verify(userRoleMapper, never()).delete(any());
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void adminRoleUpdateKeepsExistingCustomerServiceRoleManagedByMerchantApplication() {
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(customerServiceRole());
        when(userRoleMapper.countActiveUserRole(USER_ID, CUSTOMER_SERVICE_ROLE_ID)).thenReturn(1L);

        SetUserRolesRequestDTO request = new SetUserRolesRequestDTO();
        request.setRole_ids_wsh(List.of(OWNER_ROLE_ID));

        controller().setUserRoles(USER_ID, request);

        verify(userRoleMapper).deleteUserRolesExcept(USER_ID, CUSTOMER_SERVICE_ROLE_ID);
        verify(userRoleMapper).restoreUserRole(USER_ID, OWNER_ROLE_ID);
        verify(userRoleMapper, never()).restoreUserRole(eq(USER_ID), eq(CUSTOMER_SERVICE_ROLE_ID));
    }

    @Test
    void adminCannotDeleteCustomerServiceRole() {
        when(roleMapper.selectById(CUSTOMER_SERVICE_ROLE_ID)).thenReturn(customerServiceRole());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller().deleteRole(CUSTOMER_SERVICE_ROLE_ID));

        assertEquals(400, exception.getCode());
        verify(roleMapper, never()).deleteById(CUSTOMER_SERVICE_ROLE_ID);
    }

    private RoleController controller() {
        return new RoleController(roleMapper, userRoleMapper);
    }

    private Role customerServiceRole() {
        Role role = new Role();
        role.setId_wsh(CUSTOMER_SERVICE_ROLE_ID);
        role.setName_wsh("Customer Service");
        role.setCode_wsh("CUSTOMER_SERVICE");
        return role;
    }
}
