package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.common.OrderStatus;
import com.pet.common.RefundStatus;
import com.pet.finance.service.AccountingService;
import com.pet.order.dto.RefundDTO;
import com.pet.order.entity.PetOrder;
import com.pet.order.entity.Refund;
import com.pet.order.mapper.OrderMapper;
import com.pet.order.mapper.RefundMapper;
import com.pet.order.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderRefundStatusTest {

    @Mock private RefundMapper refundMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private AccountingService accountingService;

    @Test
    void createRefundAcceptsConfirmedOrderAndStoresPreviousStatus() {
        PetOrder order = order(OrderStatus.CONFIRMED);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Refund refund = service().createRefund(7L, "ORD_REFUND", "change plan");

        assertEquals(OrderStatus.REFUNDING, order.getStatus_wsh());
        assertEquals(OrderStatus.CONFIRMED, refund.getOrder_status_before_refund_wsh());
        assertEquals(RefundStatus.PENDING, refund.getStatus_wsh());
        verify(orderMapper).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
        verify(refundMapper).insert(refund);
    }

    @Test
    void rejectRefundRestoresStoredPreviousOrderStatus() {
        Refund refund = new Refund();
        refund.setId_wsh(1L);
        refund.setOrder_id_wsh(100L);
        refund.setStatus_wsh(RefundStatus.PENDING);
        refund.setOrder_status_before_refund_wsh(OrderStatus.CONFIRMED);

        PetOrder order = order(OrderStatus.REFUNDING);
        when(refundMapper.selectById(1L)).thenReturn(refund);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(refundMapper.update(any(Refund.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(orderMapper.update(any(PetOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service().rejectRefund(1L);

        assertEquals(RefundStatus.REJECTED, refund.getStatus_wsh());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus_wsh());
        verify(refundMapper).update(any(Refund.class), any(LambdaUpdateWrapper.class));
        verify(orderMapper).update(any(PetOrder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void refundDtoIncludesPreviousOrderStatus() {
        Refund refund = new Refund();
        refund.setId_wsh(1L);
        refund.setOrder_status_before_refund_wsh(OrderStatus.CONFIRMED);

        RefundDTO dto = service().toDTO(refund);

        assertEquals(OrderStatus.CONFIRMED, dto.getOrder_status_before_refund_wsh());
    }

    private RefundServiceImpl service() {
        return new RefundServiceImpl(refundMapper, orderMapper, accountingService);
    }

    private PetOrder order(String status) {
        PetOrder order = new PetOrder();
        order.setId_wsh(100L);
        order.setOwner_id_wsh(7L);
        order.setOrder_no_wsh("ORD_REFUND");
        order.setStatus_wsh(status);
        order.setFinal_amount_wsh(new BigDecimal("12.30"));
        return order;
    }
}
