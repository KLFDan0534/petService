package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.KeeperCreateRequestDTO;
import com.pet.boarding.entity.Keeper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.KeeperMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.impl.KeeperServiceImpl;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.order.mapper.OrderMapper;
import com.pet.qualification.service.QualificationService;
import com.pet.system.entity.Role;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeeperEmploymentServiceTest {

    private static final long KEEPER_ID = 10L;
    private static final long KEEPER_USER_ID = 20L;
    private static final long MERCHANT_ID = 30L;
    private static final long MERCHANT_USER_ID = 40L;
    private static final long KEEPER_ROLE_ID = 50L;

    @Mock private KeeperMapper keeperMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private QualificationService qualificationService;
    @Mock private OrderMapper orderMapper;

    @Test
    void keeperCanResignWhenNoUnfinishedOrders() {
        Keeper keeper = activeKeeper();
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(keeperRole());

        service().resign(KEEPER_ID, KEEPER_USER_ID);

        assertEquals(StatusCode.KEEPER_RESIGNED.getValue(), keeper.getStatus_wsh());
        verify(keeperMapper).updateById(keeper);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void keeperCannotResignAnotherKeeperProfile() {
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(activeKeeper());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resign(KEEPER_ID, 999L));

        assertEquals(403, exception.getCode());
        verify(keeperMapper, never()).updateById(any(Keeper.class));
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 2, 5, 6})
    void keeperCannotResignWhenStatusIsNotEmploymentState(Integer status) {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(status);
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resign(KEEPER_ID, KEEPER_USER_ID));

        assertEquals(400, exception.getCode());
        verify(orderMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(keeperMapper, never()).updateById(any(Keeper.class));
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    void keeperCanResignWhenOfflineOrBusyAndNoUnfinishedOrders(int status) {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(status);
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(keeperRole());

        service().resign(KEEPER_ID, KEEPER_USER_ID);

        assertEquals(StatusCode.KEEPER_RESIGNED.getValue(), keeper.getStatus_wsh());
        verify(keeperMapper).updateById(keeper);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void resignationIsBlockedByUnfinishedOrders() {
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(activeKeeper());
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resign(KEEPER_ID, KEEPER_USER_ID));

        assertEquals(400, exception.getCode());
        verify(keeperMapper, never()).updateById(any(Keeper.class));
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void merchantCanTerminateOwnedKeeper() {
        Keeper keeper = activeKeeper();
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(keeperRole());

        service().terminateByMerchant(KEEPER_ID, MERCHANT_USER_ID);

        assertEquals(StatusCode.KEEPER_TERMINATED.getValue(), keeper.getStatus_wsh());
        verify(keeperMapper).updateById(keeper);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void merchantCannotTerminateOtherMerchantKeeper() {
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(activeKeeper());
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(999L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().terminateByMerchant(KEEPER_ID, MERCHANT_USER_ID));

        assertEquals(403, exception.getCode());
        verify(keeperMapper, never()).updateById(any(Keeper.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 2, 5, 6})
    void merchantCannotTerminateWhenKeeperStatusIsNotEmploymentState(Integer status) {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(status);
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(merchantMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchant(MERCHANT_ID));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().terminateByMerchant(KEEPER_ID, MERCHANT_USER_ID));

        assertEquals(400, exception.getCode());
        verify(orderMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(keeperMapper, never()).updateById(any(Keeper.class));
        verify(userRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void approvingAfterResignationRestoresSoftDeletedKeeperRoleBeforeInsert() {
        Keeper keeper = new Keeper();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setUser_id_wsh(KEEPER_USER_ID);
        keeper.setStatus_wsh(StatusCode.KEEPER_PENDING.getValue());

        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(keeperRole());
        when(userRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userRoleMapper.restoreUserRole(KEEPER_USER_ID, KEEPER_ROLE_ID)).thenReturn(1);

        service().approve(KEEPER_ID);

        assertEquals(StatusCode.KEEPER_ACTIVE.getValue(), keeper.getStatus_wsh());
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void resignationChecksBlockingOrdersBeforeUpdatingAndRevokingRole() {
        Keeper keeper = activeKeeper();
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(keeperRole());

        service().resign(KEEPER_ID, KEEPER_USER_ID);

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(orderMapper).selectCount(captor.capture());
        InOrder inOrder = inOrder(orderMapper, keeperMapper, userRoleMapper);
        inOrder.verify(orderMapper).selectCount(any(LambdaQueryWrapper.class));
        inOrder.verify(keeperMapper).updateById(keeper);
        inOrder.verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void createAfterResignedOrTerminatedKeeperReusesExistingProfileAsPending(int status) {
        Keeper existing = activeKeeper();
        existing.setStatus_wsh(status);

        KeeperCreateRequestDTO request = keeperCreateRequest(99L);
        when(merchantMapper.selectById(99L)).thenReturn(merchant(99L));
        when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        Keeper result = service().create(request, KEEPER_USER_ID);

        assertEquals(existing.getId_wsh(), result.getId_wsh());
        assertEquals(99L, existing.getMerchant_id_wsh());
        assertEquals("new keeper", existing.getName_wsh());
        assertEquals(StatusCode.KEEPER_PENDING.getValue(), existing.getStatus_wsh());
        assertEquals(BigDecimal.valueOf(5.0), existing.getRating_wsh());
        verify(keeperMapper).updateById(existing);
        verify(keeperMapper, never()).insert(any(Keeper.class));
        verify(qualificationService).createOrUpdatePending(any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4})
    void createBlocksWhenExistingKeeperIsPendingOrStillEmployed(int status) {
        Keeper existing = activeKeeper();
        existing.setStatus_wsh(status);

        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        when(keeperMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> service().create(keeperCreateRequest(MERCHANT_ID), KEEPER_USER_ID));

        verify(keeperMapper, never()).updateById(any(Keeper.class));
        verify(keeperMapper, never()).insert(any(Keeper.class));
        verify(qualificationService, never()).createOrUpdatePending(any(), any(), any(), any(), any(), any(), any());
    }

    private KeeperServiceImpl service() {
        return new KeeperServiceImpl(
                keeperMapper,
                merchantMapper,
                roleMapper,
                userRoleMapper,
                qualificationService,
                orderMapper);
    }

    @Test
    void setOnlineStatusToOfflineRecordsManualSource() {
        Keeper keeper = activeKeeper();
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);

        service().setOnlineStatus(KEEPER_ID, StatusCode.KEEPER_OFFLINE.getValue());

        assertEquals(KeeperServiceImpl.OFFLINE_SOURCE_MANUAL, keeper.getOffline_source_wsh());
        verify(keeperMapper).updateById(keeper);
    }

    @Test
    void setOnlineStatusToActiveClearsManualSource() {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
        keeper.setOffline_source_wsh(KeeperServiceImpl.OFFLINE_SOURCE_MANUAL);
        when(keeperMapper.selectById(KEEPER_ID)).thenReturn(keeper);

        service().setOnlineStatus(KEEPER_ID, StatusCode.KEEPER_ACTIVE.getValue());

        assertEquals(KeeperServiceImpl.OFFLINE_SOURCE_SYSTEM, keeper.getOffline_source_wsh());
        assertEquals(StatusCode.KEEPER_ACTIVE.getValue(), keeper.getStatus_wsh());
        verify(keeperMapper).updateById(keeper);
    }

    @Test
    void storeOpenSyncDoesNotResetManualOfflineKeeper() {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
        keeper.setOffline_source_wsh(KeeperServiceImpl.OFFLINE_SOURCE_MANUAL);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(keeper));

        service().syncMerchantStoreStatus(MERCHANT_ID, true);

        assertEquals(StatusCode.KEEPER_OFFLINE.getValue(), keeper.getStatus_wsh());
        assertEquals(KeeperServiceImpl.OFFLINE_SOURCE_MANUAL, keeper.getOffline_source_wsh());
        verify(keeperMapper, never()).updateById(any(Keeper.class));
    }

    @Test
    void storeOpenSyncResetsSystemOfflineKeeperToActive() {
        Keeper keeper = activeKeeper();
        keeper.setStatus_wsh(StatusCode.KEEPER_OFFLINE.getValue());
        keeper.setOffline_source_wsh(KeeperServiceImpl.OFFLINE_SOURCE_SYSTEM);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(keeper));

        service().syncMerchantStoreStatus(MERCHANT_ID, true);

        assertEquals(StatusCode.KEEPER_ACTIVE.getValue(), keeper.getStatus_wsh());
        assertEquals(KeeperServiceImpl.OFFLINE_SOURCE_SYSTEM, keeper.getOffline_source_wsh());
        verify(keeperMapper).updateById(keeper);
    }

    @Test
    void storeCloseSyncMarksActiveKeeperOfflineWithSystemSource() {
        Keeper keeper = activeKeeper();
        keeper.setOffline_source_wsh(KeeperServiceImpl.OFFLINE_SOURCE_MANUAL);
        when(keeperMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(keeper));

        service().syncMerchantStoreStatus(MERCHANT_ID, false);

        assertEquals(StatusCode.KEEPER_OFFLINE.getValue(), keeper.getStatus_wsh());
        assertEquals(KeeperServiceImpl.OFFLINE_SOURCE_SYSTEM, keeper.getOffline_source_wsh());
        verify(keeperMapper).updateById(keeper);
    }

    private Keeper activeKeeper() {
        Keeper keeper = new Keeper();
        keeper.setId_wsh(KEEPER_ID);
        keeper.setUser_id_wsh(KEEPER_USER_ID);
        keeper.setMerchant_id_wsh(MERCHANT_ID);
        keeper.setStatus_wsh(StatusCode.KEEPER_ACTIVE.getValue());
        return keeper;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        merchant.setUser_id_wsh(MERCHANT_USER_ID);
        return merchant;
    }

    private Role keeperRole() {
        Role role = new Role();
        role.setId_wsh(KEEPER_ROLE_ID);
        role.setCode_wsh("KEEPER");
        return role;
    }

    private KeeperCreateRequestDTO keeperCreateRequest(Long merchantId) {
        KeeperCreateRequestDTO request = new KeeperCreateRequestDTO();
        request.setMerchant_id_wsh(merchantId);
        request.setName_wsh("new keeper");
        request.setPhone_wsh("13800000000");
        request.setExperience_years_wsh(3);
        request.setPrice_per_day_wsh(new BigDecimal("88.00"));
        request.setMax_pets_wsh(2);
        request.setBio_wsh("careful");
        request.setQualification_image_wsh("cert.png");
        return request;
    }

}
