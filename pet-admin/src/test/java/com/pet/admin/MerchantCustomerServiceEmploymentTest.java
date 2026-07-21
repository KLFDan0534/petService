package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.customer.dto.MerchantCustomerServiceApplyRequestDTO;
import com.pet.customer.dto.MerchantCustomerServiceDTO;
import com.pet.customer.dto.MerchantCustomerServiceReviewRequestDTO;
import com.pet.customer.entity.MerchantCustomerService;
import com.pet.customer.mapper.MerchantCustomerServiceMapper;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.MerchantCustomerServiceServiceImpl;
import com.pet.system.mapper.UserMapper;
import com.pet.system.service.RoleGrantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantCustomerServiceEmploymentTest {

    private static final long APPLICATION_ID = 10L;
    private static final long CUSTOMER_SERVICE_USER_ID = 20L;
    private static final long MERCHANT_ID = 30L;
    private static final long MERCHANT_USER_ID = 40L;

    @Mock private MerchantCustomerServiceMapper mapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private UserMapper userMapper;
    @Mock private RoleGrantService roleGrantService;

    @Test
    void userCanApplyToApprovedMerchantCustomerService() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(approvedMerchant(MERCHANT_ID));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        MerchantCustomerServiceApplyRequestDTO request = new MerchantCustomerServiceApplyRequestDTO();
        request.setMerchant_id_wsh(MERCHANT_ID);
        request.setApplicant_note_wsh("I can cover after-sale tickets");

        MerchantCustomerServiceDTO result = service().apply(CUSTOMER_SERVICE_USER_ID, request);

        ArgumentCaptor<MerchantCustomerService> captor = ArgumentCaptor.forClass(MerchantCustomerService.class);
        verify(mapper).insert(captor.capture());
        assertEquals(MERCHANT_ID, captor.getValue().getMerchant_id_wsh());
        assertEquals(CUSTOMER_SERVICE_USER_ID, captor.getValue().getUser_id_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_PENDING, captor.getValue().getStatus_wsh());
        assertEquals("I can cover after-sale tickets", captor.getValue().getApplicant_note_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_PENDING, result.getStatus_wsh());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rejected", "resigned", "terminated"})
    void userCanReapplyAfterTerminalCustomerServiceApplicationStatus(String status) {
        MerchantCustomerService existing = reviewedApplication(status);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(approvedMerchant(MERCHANT_ID));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        MerchantCustomerServiceApplyRequestDTO request = new MerchantCustomerServiceApplyRequestDTO();
        request.setMerchant_id_wsh(MERCHANT_ID);
        request.setApplicant_note_wsh(" ready again ");

        MerchantCustomerServiceDTO result = service().apply(CUSTOMER_SERVICE_USER_ID, request);

        assertEquals(MerchantCustomerServiceService.STATUS_PENDING, existing.getStatus_wsh());
        assertEquals("ready again", existing.getApplicant_note_wsh());
        assertEquals(null, existing.getReview_note_wsh());
        assertEquals(null, existing.getReviewer_id_wsh());
        assertEquals(null, existing.getReviewed_at_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_PENDING, result.getStatus_wsh());
        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(MerchantCustomerService.class));
    }

    @Test
    void userCannotApplyToOwnMerchantAsCustomerService() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(approvedMerchantOwnedBy(CUSTOMER_SERVICE_USER_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().apply(CUSTOMER_SERVICE_USER_ID, applyRequest(MERCHANT_ID)));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).insert(any(MerchantCustomerService.class));
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
    }

    @Test
    void userCannotApplyToUnapprovedMerchant() {
        Merchant merchant = approvedMerchant(MERCHANT_ID);
        merchant.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().apply(CUSTOMER_SERVICE_USER_ID, applyRequest(MERCHANT_ID)));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).insert(any(MerchantCustomerService.class));
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
    }

    @Test
    void userCannotApplyWhenAlreadyApprovedForSameMerchant() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(approvedMerchant(MERCHANT_ID));
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approvedApplication());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().apply(CUSTOMER_SERVICE_USER_ID, applyRequest(MERCHANT_ID)));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).insert(any(MerchantCustomerService.class));
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
    }

    @Test
    void merchantApproveApplicationGrantsCustomerServiceRole() {
        MerchantCustomerService application = pendingApplication();
        MerchantCustomerServiceReviewRequestDTO request = new MerchantCustomerServiceReviewRequestDTO();
        request.setReview_note_wsh("welcome aboard");

        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);

        MerchantCustomerServiceDTO result = service().approve(APPLICATION_ID, MERCHANT_USER_ID, request);

        assertEquals(MerchantCustomerServiceService.STATUS_APPROVED, application.getStatus_wsh());
        assertEquals(MERCHANT_USER_ID, application.getReviewer_id_wsh());
        assertEquals("welcome aboard", application.getReview_note_wsh());
        assertNotNull(application.getReviewed_at_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_APPROVED, result.getStatus_wsh());
        verify(mapper).updateById(application);
        verify(roleGrantService).grantRoleToUser(CUSTOMER_SERVICE_USER_ID, "CUSTOMER_SERVICE");
    }

    @Test
    void anotherMerchantCannotApproveApplication() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(999L));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(pendingApplication());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().approve(APPLICATION_ID, MERCHANT_USER_ID, null));

        assertEquals(403, exception.getCode());
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
        verify(roleGrantService, never()).grantRoleToUser(any(), any());
    }

    @Test
    void customerServiceCanResignOwnedApprovedApplication() {
        MerchantCustomerService application = approvedApplication();
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        MerchantCustomerServiceDTO result = service().resign(APPLICATION_ID, CUSTOMER_SERVICE_USER_ID);

        assertEquals(MerchantCustomerServiceService.STATUS_RESIGNED, application.getStatus_wsh());
        assertEquals(CUSTOMER_SERVICE_USER_ID, application.getReviewer_id_wsh());
        assertNotNull(application.getReviewed_at_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_RESIGNED, result.getStatus_wsh());
        verify(mapper).updateById(application);
        verify(roleGrantService).revokeRoleFromUser(CUSTOMER_SERVICE_USER_ID, "CUSTOMER_SERVICE");
    }

    @Test
    void customerServiceCannotResignAnotherUsersApplication() {
        when(mapper.selectById(APPLICATION_ID)).thenReturn(approvedApplication());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resign(APPLICATION_ID, 999L));

        assertEquals(403, exception.getCode());
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
        verify(roleGrantService, never()).revokeRoleFromUser(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected", "resigned", "terminated"})
    void customerServiceCannotResignUnlessApplicationIsApproved(String status) {
        MerchantCustomerService application = approvedApplication();
        application.setStatus_wsh(status);
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resign(APPLICATION_ID, CUSTOMER_SERVICE_USER_ID));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
        verify(roleGrantService, never()).revokeRoleFromUser(any(), any());
    }

    @Test
    void resignDoesNotRevokeRoleWhenUserStillServesAnotherMerchant() {
        MerchantCustomerService application = approvedApplication();
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service().resign(APPLICATION_ID, CUSTOMER_SERVICE_USER_ID);

        assertEquals(MerchantCustomerServiceService.STATUS_RESIGNED, application.getStatus_wsh());
        verify(roleGrantService, never()).revokeRoleFromUser(any(), any());
    }

    @Test
    void merchantCanTerminateOwnedCustomerService() {
        MerchantCustomerService application = approvedApplication();
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        MerchantCustomerServiceDTO result = service().terminateByMerchant(APPLICATION_ID, MERCHANT_USER_ID);

        assertEquals(MerchantCustomerServiceService.STATUS_TERMINATED, application.getStatus_wsh());
        assertEquals(MERCHANT_USER_ID, application.getReviewer_id_wsh());
        assertNotNull(application.getReviewed_at_wsh());
        assertEquals(MerchantCustomerServiceService.STATUS_TERMINATED, result.getStatus_wsh());
        verify(mapper).updateById(application);
        verify(roleGrantService).revokeRoleFromUser(CUSTOMER_SERVICE_USER_ID, "CUSTOMER_SERVICE");
    }

    @Test
    void terminateDoesNotRevokeRoleWhenUserStillServesAnotherMerchant() {
        MerchantCustomerService application = approvedApplication();
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service().terminateByMerchant(APPLICATION_ID, MERCHANT_USER_ID);

        assertEquals(MerchantCustomerServiceService.STATUS_TERMINATED, application.getStatus_wsh());
        verify(roleGrantService, never()).revokeRoleFromUser(any(), any());
    }

    @Test
    void merchantCannotTerminateAnotherMerchantsCustomerService() {
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(999L));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(approvedApplication());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().terminateByMerchant(APPLICATION_ID, MERCHANT_USER_ID));

        assertEquals(403, exception.getCode());
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
        verify(roleGrantService, never()).revokeRoleFromUser(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected", "resigned", "terminated"})
    void merchantCannotTerminateNonApprovedApplicationAsEmployment(String status) {
        MerchantCustomerService application = approvedApplication();
        application.setStatus_wsh(status);

        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));
        when(mapper.selectById(APPLICATION_ID)).thenReturn(application);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().terminateByMerchant(APPLICATION_ID, MERCHANT_USER_ID));

        assertEquals(400, exception.getCode());
        verify(mapper, never()).updateById(any(MerchantCustomerService.class));
    }

    private MerchantCustomerServiceServiceImpl service() {
        return new MerchantCustomerServiceServiceImpl(mapper, merchantMapper, userMapper, roleGrantService);
    }

    private MerchantCustomerService approvedApplication() {
        MerchantCustomerService application = new MerchantCustomerService();
        application.setId_wsh(APPLICATION_ID);
        application.setMerchant_id_wsh(MERCHANT_ID);
        application.setUser_id_wsh(CUSTOMER_SERVICE_USER_ID);
        application.setStatus_wsh(MerchantCustomerServiceService.STATUS_APPROVED);
        return application;
    }

    private MerchantCustomerService pendingApplication() {
        MerchantCustomerService application = new MerchantCustomerService();
        application.setId_wsh(APPLICATION_ID);
        application.setMerchant_id_wsh(MERCHANT_ID);
        application.setUser_id_wsh(CUSTOMER_SERVICE_USER_ID);
        application.setStatus_wsh(MerchantCustomerServiceService.STATUS_PENDING);
        return application;
    }

    private MerchantCustomerService reviewedApplication(String status) {
        MerchantCustomerService application = approvedApplication();
        application.setStatus_wsh(status);
        application.setApplicant_note_wsh("old note");
        application.setReview_note_wsh("old review");
        application.setReviewer_id_wsh(999L);
        application.setReviewed_at_wsh(LocalDateTime.now().minusDays(1));
        return application;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        merchant.setUser_id_wsh(MERCHANT_USER_ID);
        return merchant;
    }

    private Merchant approvedMerchant(Long id) {
        Merchant merchant = merchant(id);
        merchant.setStatus_wsh(StatusCode.MERCHANT_APPROVED.getValue());
        return merchant;
    }

    private Merchant approvedMerchantOwnedBy(Long userId) {
        Merchant merchant = approvedMerchant(MERCHANT_ID);
        merchant.setUser_id_wsh(userId);
        return merchant;
    }

    private MerchantCustomerServiceApplyRequestDTO applyRequest(Long merchantId) {
        MerchantCustomerServiceApplyRequestDTO request = new MerchantCustomerServiceApplyRequestDTO();
        request.setMerchant_id_wsh(merchantId);
        request.setApplicant_note_wsh("apply");
        return request;
    }
}
