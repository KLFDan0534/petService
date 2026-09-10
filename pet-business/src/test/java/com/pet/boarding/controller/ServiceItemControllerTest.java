package com.pet.boarding.controller;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.service.MerchantScopeResolver;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceAvailabilityService;
import com.pet.boarding.service.ServiceItemService;
import com.pet.common.BusinessException;
import com.pet.security.JwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U2-2: 服务管理入口作用域契约 —— MERCHANT 归属自动派生，ADMIN 显式指定；
 * 管理操作仅限归属商家或管理员；公开详情/代理管理列表同样受控。
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemControllerTest {

    @Mock private ServiceItemService serviceItemService;
    @Mock private MerchantService merchantService;
    @Mock private ServiceAvailabilityService serviceAvailabilityService;

    private ServiceItemController controller;
    private MerchantScopeResolver scopeResolver;

    @BeforeEach
    void setUp() {
        scopeResolver = new MerchantScopeResolver(merchantService);
        controller = new ServiceItemController(serviceItemService, merchantService,
                serviceAvailabilityService, scopeResolver);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void as(JwtAuthenticationToken token) {
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private JwtAuthenticationToken merchantToken(long userId) {
        return new JwtAuthenticationToken(userId, "merchant-user",
                List.of(() -> "ROLE_MERCHANT"));
    }

    private JwtAuthenticationToken adminToken() {
        return new JwtAuthenticationToken(1L, "admin",
                List.of(() -> "ROLE_ADMIN"));
    }

    private Merchant merchantOf(long userId, long merchantId) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(merchantId);
        merchant.setName_wsh("爱宠之家");
        merchant.setUser_id_wsh(userId);
        return merchant;
    }

    private ServiceItem item(Long id, long merchantId) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        return item;
    }

    /**
     * CT-01: MERCHANT 创建时归属自动派生为本人商家，客户端不参与归属选择。
     */
    @Test
    void merchantCreateDerivesOwnMerchant() {
        when(merchantService.findByUserId(7L)).thenReturn(merchantOf(7L, 99L));
        ServiceItem saved = item(301L, 99L);
        when(serviceItemService.create(eq(99L), any(ServiceItemCreateRequestDTO.class))).thenReturn(saved);
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(301L);
        when(serviceItemService.toDTO(saved)).thenReturn(dto);

        as(merchantToken(7L));
        ServiceItemDTO result = controller.create(null, new ServiceItemCreateRequestDTO()).getData();

        assertNotNull(result);
        verify(serviceItemService).create(eq(99L), any(ServiceItemCreateRequestDTO.class));
    }

    /**
     * CT-02: ADMIN 创建必须显式指定存在的目标商家。
     */
    @Test
    void adminCreateRequiresExistingMerchantId() {
        when(merchantService.getById(42L)).thenReturn(merchantOf(2L, 42L));
        when(serviceItemService.create(eq(42L), any(ServiceItemCreateRequestDTO.class)))
                .thenReturn(item(302L, 42L));
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(302L);
        when(serviceItemService.toDTO(any(ServiceItem.class))).thenReturn(dto);

        as(adminToken());
        controller.create(42L, new ServiceItemCreateRequestDTO());

        verify(serviceItemService).create(eq(42L), any(ServiceItemCreateRequestDTO.class));
    }

    /**
     * CT-03: ADMIN 未指定或指定不存在的商家一律拒绝。
     */
    @Test
    void adminCreateWithoutOrWithUnknownMerchantRejected() {
        as(adminToken());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> controller.create(null, new ServiceItemCreateRequestDTO())).getCode());

        when(merchantService.getById(404L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> controller.create(404L, new ServiceItemCreateRequestDTO())).getCode());
    }

    /**
     * CT-04: 无商家身份的普通 MERCHANT 拒绝创建。
     */
    @Test
    void merchantWithoutIdentityRejected() {
        when(merchantService.findByUserId(7L)).thenReturn(null);
        as(merchantToken(7L));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.create(null, new ServiceItemCreateRequestDTO())).getCode());
    }

    /**
     * CT-05: 更新/删除/管理详情仅限归属商家或管理员。
     */
    @Test
    void manageOpsRestrictedToOwnerOrAdmin() {
        when(serviceItemService.getById(301L)).thenReturn(item(301L, 99L));
        when(merchantService.isOwner(99L, 7L)).thenReturn(true);
        as(merchantToken(7L));
        controller.update(301L, new ServiceItemUpdateRequestDTO());
        verify(serviceItemService).update(eq(301L), any(ServiceItemUpdateRequestDTO.class));

        when(serviceItemService.getById(302L)).thenReturn(item(302L, 99L));
        when(merchantService.isOwner(99L, 7L)).thenReturn(false);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.update(302L, new ServiceItemUpdateRequestDTO())).getCode());

        when(serviceItemService.getById(303L)).thenReturn(item(303L, 99L));
        as(adminToken());
        controller.update(303L, new ServiceItemUpdateRequestDTO());
        verify(serviceItemService).update(eq(303L), any(ServiceItemUpdateRequestDTO.class));

        when(serviceItemService.getById(304L)).thenReturn(item(304L, 99L));
        when(merchantService.isOwner(99L, 7L)).thenReturn(true);
        as(merchantToken(7L));
        controller.manageDetail(304L);
        verify(serviceItemService).getManageDetail(304L);

        when(serviceItemService.getById(305L)).thenReturn(item(305L, 99L));
        when(merchantService.isOwner(99L, 7L)).thenReturn(false);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.manageDetail(305L)).getCode());
    }

    /**
     * CT-06: 商家管理列表归属校验；公开列表不受影响。
     */
    @Test
    void merchantManageListScopedToOwnMerchant() {
        when(merchantService.isOwner(99L, 7L)).thenReturn(true);
        when(serviceItemService.listByMerchantForManage(99L)).thenReturn(List.of(item(301L, 99L)));
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(301L);
        when(serviceItemService.toDTO(any(ServiceItem.class))).thenReturn(dto);

        as(merchantToken(7L));
        controller.listByMerchantForManage(99L);

        verify(merchantService).isOwner(99L, 7L);
        verify(serviceItemService).listByMerchantForManage(99L);

        when(merchantService.isOwner(99L, 7L)).thenReturn(false);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> controller.listByMerchantForManage(99L)).getCode());
    }

    /**
     * CT-07: 公开接口（列表/详情/可用性）不做作用域校验且不查询商家归属。
     */
    @Test
    void publicEndpointsDoNotCheckScope() {
        when(serviceItemService.queryPublic(any(ServiceItemQueryDTO.class)))
                .thenReturn(null);
        ServiceItemQueryDTO q = new ServiceItemQueryDTO();
        q.setMerchant_id_wsh(99L);
        q.setPage_wsh(1);
        q.setSize_wsh(100);

        controller.pagePublic(new ServiceItemQueryDTO(null, 99L, null, null, null, null, 1, 20));

        verify(merchantService, never()).isOwner(any(), any());
        verify(merchantService, never()).findByUserId(any());
        verify(merchantService, never()).getById(any());
    }
}