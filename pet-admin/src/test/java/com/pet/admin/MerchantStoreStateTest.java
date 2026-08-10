package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.constant.MerchantStoreConstants;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.BusinessHoursMapper;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.boarding.service.KeeperService;
import com.pet.boarding.service.impl.MerchantServiceImpl;
import com.pet.common.StatusCode;
import com.pet.qualification.service.QualificationService;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantStoreStateTest {

    @Mock private MerchantMapper merchantMapper;
    @Mock private QualificationService qualificationService;
    @Mock private BusinessHoursMapper businessHoursMapper;
    @Mock private KeeperService keeperService;
    @Mock private UserMapper userMapper;

    @Test
    void approvedMerchantWithoutBusinessHoursStaysClosedByDefault() {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(10L);
        merchant.setStatus_wsh(StatusCode.MERCHANT_PENDING.getValue());
        merchant.setStore_status_wsh(MerchantStoreConstants.STATUS_CLOSED);

        when(merchantMapper.selectById(10L)).thenReturn(merchant);
        when(businessHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service().approve(10L);

        assertEquals(StatusCode.MERCHANT_APPROVED.getValue(), merchant.getStatus_wsh());
        assertEquals(MerchantStoreConstants.MODE_AUTO, merchant.getStore_mode_wsh());
        assertEquals(MerchantStoreConstants.STATUS_CLOSED, merchant.getStore_status_wsh());
        verify(keeperService, never()).syncMerchantStoreStatus(10L, true);
    }

    private MerchantServiceImpl service() {
        return new MerchantServiceImpl(merchantMapper, qualificationService, businessHoursMapper, keeperService, userMapper);
    }
}
