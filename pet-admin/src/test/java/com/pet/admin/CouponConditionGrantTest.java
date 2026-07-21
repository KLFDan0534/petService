package com.pet.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.marketing.dto.CouponGrantRequestDTO;
import com.pet.marketing.entity.CouponTemplate;
import com.pet.marketing.entity.UserCoupon;
import com.pet.marketing.mapper.CouponTemplateMapper;
import com.pet.marketing.mapper.CouponUsageMapper;
import com.pet.marketing.mapper.UserCouponMapper;
import com.pet.marketing.service.impl.CouponServiceImpl;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.mapper.PetMapper;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponConditionGrantTest {

    @Mock private CouponTemplateMapper templateMapper;
    @Mock private UserCouponMapper userCouponMapper;
    @Mock private CouponUsageMapper usageMapper;
    @Mock private UserMapper userMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private PetMapper petMapper;

    private CouponServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CouponServiceImpl(
                templateMapper,
                userCouponMapper,
                usageMapper,
                userMapper,
                orderMapper,
                petMapper,
                new ObjectMapper());
    }

    @Test
    void grantByConditionIssuesToUsersMatchingAnyEnabledCondition() {
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L), user(2L), user(3L)));
        when(orderMapper.selectCouponGrantOrderStats()).thenReturn(List.of(
                stat(1L, "total_spend_wsh", new BigDecimal("600.00"), "order_count_wsh", 1L),
                stat(2L, "total_spend_wsh", new BigDecimal("20.00"), "order_count_wsh", 1L)
        ));
        when(petMapper.selectCouponGrantPetCounts()).thenReturn(List.of(
                stat(2L, "pet_count_wsh", 1L),
                stat(3L, "pet_count_wsh", 4L)
        ));
        mockCouponStock();

        CouponGrantRequestDTO request = new CouponGrantRequestDTO();
        request.setQuantity_wsh(1);
        request.setMin_total_spend_wsh(new BigDecimal("500.00"));
        request.setMin_pet_count_wsh(3);

        int issued = service.grantByCondition(99L, 10L, request);

        assertEquals(2, issued);
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of(1L, 3L), captor.getAllValues().stream().map(UserCoupon::getUser_id_wsh).toList());
    }

    @Test
    void grantByConditionReturnsZeroWhenAnyNumericConditionIsNegative() {
        CouponGrantRequestDTO request = new CouponGrantRequestDTO();
        request.setQuantity_wsh(1);
        request.setMin_pet_count_wsh(-1);

        int issued = service.grantByCondition(99L, 10L, request);

        assertEquals(0, issued);
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
        verify(templateMapper, never()).increaseIssuedQuantity(any(), any(Integer.class));
    }

    @Test
    void grantByConditionDefaultsToAllNormalUsersWhenNoConditionIsEnabled() {
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L), user(2L)));
        when(orderMapper.selectCouponGrantOrderStats()).thenReturn(List.of());
        when(petMapper.selectCouponGrantPetCounts()).thenReturn(List.of());
        mockCouponStock();

        CouponGrantRequestDTO request = new CouponGrantRequestDTO();
        request.setQuantity_wsh(2);

        int issued = service.grantByCondition(99L, 10L, request);

        assertEquals(4, issued);
    }

    private void mockCouponStock() {
        CouponTemplate template = new CouponTemplate();
        template.setId_wsh(10L);
        template.setName_wsh("test");
        template.setType_wsh("amount");
        template.setDiscount_amount_wsh(new BigDecimal("10.00"));
        template.setPer_user_limit_wsh(10);
        template.setStatus_wsh(1);
        template.setValid_from_wsh(LocalDateTime.now().minusDays(1));
        template.setValid_to_wsh(LocalDateTime.now().plusDays(1));
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(userCouponMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.increaseIssuedQuantity(10L, 1)).thenReturn(1);
    }

    private User user(Long id) {
        User user = new User();
        user.setId_wsh(id);
        user.setStatus_wsh(1);
        return user;
    }

    private Map<String, Object> stat(Long userId, Object... pairs) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("user_id_wsh", userId);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
