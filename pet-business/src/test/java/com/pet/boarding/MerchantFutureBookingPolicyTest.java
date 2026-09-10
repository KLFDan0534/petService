package com.pet.boarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.boarding.constant.MerchantStoreConstants;
import com.pet.boarding.controller.MerchantController;
import com.pet.boarding.dto.MerchantCreateRequestDTO;
import com.pet.boarding.dto.MerchantDTO;
import com.pet.boarding.dto.MerchantUpdateRequestDTO;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.MerchantService;
import com.pet.boarding.service.impl.MerchantServiceImpl;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.qualification.service.QualificationService;
import com.pet.security.JwtAuthenticationToken;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantFutureBookingPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private MerchantMapper merchantMapper;
    @Mock private QualificationService qualificationService;
    @Mock private BusinessHoursMapper businessHoursMapper;
    @Mock private KeeperService keeperService;
    @Mock private MerchantService merchantService;
    @Mock private UserMapper userMapper;

    @Test
    void createDefaultsFutureBookingEnabledToOnWhenOmitted() {
        MerchantCreateRequestDTO dto = new MerchantCreateRequestDTO();
        dto.setName_wsh("未来预约店");

        Merchant merchant = service().create(dto, 10L);

        assertEquals(1, merchant.getFuture_booking_enabled_wsh());
        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getFuture_booking_enabled_wsh());
    }

    @Test
    void createHonorsExplicitFutureBookingDisabled() {
        MerchantCreateRequestDTO dto = new MerchantCreateRequestDTO();
        dto.setName_wsh("关闭预约店");
        dto.setFuture_booking_enabled_wsh(0);

        Merchant merchant = service().create(dto, 10L);

        assertEquals(0, merchant.getFuture_booking_enabled_wsh());
    }

    @Test
    void updateChangesFutureBookingPolicy() {
        Merchant existing = new Merchant();
        existing.setId_wsh(10L);
        existing.setFuture_booking_enabled_wsh(1);
        when(merchantMapper.selectById(10L)).thenReturn(existing);

        MerchantUpdateRequestDTO dto = new MerchantUpdateRequestDTO();
        dto.setFuture_booking_enabled_wsh(0);

        Merchant updated = service().update(10L, dto);

        assertEquals(0, updated.getFuture_booking_enabled_wsh());
    }

    @Test
    void updateRejectsInvalidFutureBookingValue() {
        Merchant existing = new Merchant();
        existing.setId_wsh(10L);
        existing.setFuture_booking_enabled_wsh(1);
        when(merchantMapper.selectById(10L)).thenReturn(existing);

        MerchantUpdateRequestDTO dto = new MerchantUpdateRequestDTO();
        dto.setFuture_booking_enabled_wsh(2);

        assertThrows(BusinessException.class, () -> service().update(10L, dto));
    }

    @Test
    void updateKeepsPolicyWhenFieldAbsent() {
        Merchant existing = new Merchant();
        existing.setId_wsh(10L);
        existing.setFuture_booking_enabled_wsh(1);
        when(merchantMapper.selectById(10L)).thenReturn(existing);

        MerchantUpdateRequestDTO dto = new MerchantUpdateRequestDTO();

        Merchant updated = service().update(10L, dto);

        assertEquals(1, updated.getFuture_booking_enabled_wsh());
        verify(merchantMapper).updateById(existing);
    }

    @Test
    void toDTODefaultsFutureBookingEnabledToOnForLegacyNull() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchant.setFuture_booking_enabled_wsh(null);
        when(qualificationService.listByOwner(any(), anyLong(), anyBoolean())).thenReturn(List.of());

        MerchantDTO dto = service().toDTO(merchant);

        assertEquals(1, dto.getFuture_booking_enabled_wsh());
    }

    @Test
    void toDTOExposesStoredPolicy() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchant.setFuture_booking_enabled_wsh(0);
        when(qualificationService.listByOwner(any(), anyLong(), anyBoolean())).thenReturn(List.of());

        MerchantDTO dto = service().toDTO(merchant);

        assertEquals(0, dto.getFuture_booking_enabled_wsh());
    }

    @Test
    void createRequestDeserializesSnakeCaseField() throws Exception {
        MerchantCreateRequestDTO snake = objectMapper.readValue(
                "{\"future_booking_enabled_wsh\":0}", MerchantCreateRequestDTO.class);
        assertEquals(0, snake.getFuture_booking_enabled_wsh());

        MerchantCreateRequestDTO empty = objectMapper.readValue(
                "{}", MerchantCreateRequestDTO.class);
        assertNull(empty.getFuture_booking_enabled_wsh());
    }

    @Test
    void updateRequestDeserializesSnakeCaseField() throws Exception {
        MerchantUpdateRequestDTO snake = objectMapper.readValue(
                "{\"future_booking_enabled_wsh\":1}", MerchantUpdateRequestDTO.class);
        assertEquals(1, snake.getFuture_booking_enabled_wsh());
    }

    @Test
    void storeDTOCombinesFutureBookingAndRealtimeStoreStatus() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        merchant.setUser_id_wsh(1L);
        merchant.setName_wsh("独立语义店");
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        merchant.setStore_mode_wsh(MerchantStoreConstants.MODE_AUTO);
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);
        merchant.setFuture_booking_enabled_wsh(1);
        when(qualificationService.listByOwner(any(), anyLong(), anyBoolean())).thenReturn(List.of());

        MerchantDTO dto = service().toDTO(merchant);

        assertEquals(MerchantStoreConstants.STATUS_CLOSED, dto.getStore_status_wsh());
        assertEquals(1, dto.getFuture_booking_enabled_wsh());
        assertNotNull(dto.getStore_status_wsh());
        assertNotNull(dto.getFuture_booking_enabled_wsh());
    }

    @Test
    void nonOwnerCannotUpdateFutureBookingPolicy() {
        JwtAuthenticationToken owner = new JwtAuthenticationToken(1L, "user1", List.of());
        MerchantUpdateRequestDTO dto = new MerchantUpdateRequestDTO();
        dto.setFuture_booking_enabled_wsh(0);
        when(merchantService.isOwner(10L, 1L)).thenReturn(false);

        MerchantController controller = new MerchantController(merchantService);

        var result = controller.update(owner, 10L, dto);

        assertEquals(403, result.getCode());
        verify(merchantService, never()).update(any(), any());
    }

    @Test
    void ownerCanUpdateFutureBookingPolicy() {
        JwtAuthenticationToken owner = new JwtAuthenticationToken(1L, "user1", List.of());
        MerchantUpdateRequestDTO dto = new MerchantUpdateRequestDTO();
        dto.setFuture_booking_enabled_wsh(0);
        Merchant existing = new Merchant();
        existing.setId_wsh(10L);
        existing.setFuture_booking_enabled_wsh(0);
        when(merchantService.isOwner(10L, 1L)).thenReturn(true);
        when(merchantService.update(10L, dto)).thenReturn(existing);
        when(merchantService.toDTO(existing)).thenReturn(new MerchantDTO());

        MerchantController controller = new MerchantController(merchantService);

        var result = controller.update(owner, 10L, dto);

        assertEquals(200, result.getCode());
        verify(merchantService).update(10L, dto);
    }

    private MerchantServiceImpl service() {
        return new MerchantServiceImpl(merchantMapper, qualificationService, businessHoursMapper, keeperService, userMapper);
    }
}