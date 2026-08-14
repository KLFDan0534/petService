package com.pet.operation.controller;

import com.pet.boarding.entity.Merchant;
import com.pet.boarding.service.MerchantScopeResolver;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.ServiceMediaService;
import com.pet.common.BusinessException;
import com.pet.operation.dto.FileRecordDTO;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.security.JwtAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U1: 产品图片上传端点的作用域解析 —— MERCHANT 自动派生自己商家；
 * ADMIN 必须显式指定存在的 merchantId；无商家身份/未知商家一律拒绝。
 */
@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private MinIoService minIoService;
    @Mock private FileRecordService fileRecordService;
    @Mock private ServiceMediaService serviceMediaService;
    @Mock private MerchantService merchantService;

    private FileController controller;

    @BeforeEach
    void setUp() {
        MerchantScopeResolver scopeResolver = new MerchantScopeResolver(merchantService);
        controller = new FileController(minIoService, fileRecordService, serviceMediaService, scopeResolver);
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
        merchant.setUser_id_wsh(userId);
        return merchant;
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
    }

    @Test
    void merchantAutoDerivesOwnMerchant() {
        when(merchantService.findByUserId(7L)).thenReturn(merchantOf(7L, 99L));
        FileRecord record = new FileRecord();
        record.setId_wsh(5L);
        record.setObject_name_wsh("service/uuid.png");
        when(serviceMediaService.uploadProductImage(eq(7L), eq(99L), any())).thenReturn(record);
        when(minIoService.getFileUrl("service/uuid.png")).thenReturn("/minio/pet-service/service/uuid.png");

        FileRecordDTO dto = controller.uploadProductImage(png(), null, merchantToken(7L)).getData();

        assertNotNull(dto);
        assertEquals(5L, dto.getId_wsh());
        assertEquals("/minio/pet-service/service/uuid.png", dto.getUrl_wsh());
        verify(serviceMediaService).uploadProductImage(eq(7L), eq(99L), any());
    }

    @Test
    void adminMustSupplyExistingMerchantId() {
        when(merchantService.getById(42L)).thenReturn(merchantOf(2L, 42L));
        FileRecord record = new FileRecord();
        record.setObject_name_wsh("service/uuid.png");
        when(serviceMediaService.uploadProductImage(eq(1L), eq(42L), any())).thenReturn(record);

        controller.uploadProductImage(png(), 42L, adminToken());

        verify(serviceMediaService).uploadProductImage(eq(1L), eq(42L), any());
    }

    @Test
    void adminWithoutMerchantIdRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> controller.uploadProductImage(png(), null, adminToken()));
        assertEquals(400, e.getCode());
    }

    @Test
    void adminWithUnknownMerchantRejected() {
        when(merchantService.getById(404L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> controller.uploadProductImage(png(), 404L, adminToken()));
        assertEquals(404, e.getCode());
    }

    @Test
    void merchantWithoutMerchantIdentityRejected() {
        when(merchantService.findByUserId(7L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> controller.uploadProductImage(png(), null, merchantToken(7L)));
        assertEquals(403, e.getCode());
    }
}