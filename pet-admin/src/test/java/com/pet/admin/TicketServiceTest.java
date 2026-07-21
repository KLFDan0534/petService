package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.TicketCreateRequestDTO;
import com.pet.customer.dto.TicketDTO;
import com.pet.customer.entity.Ticket;
import com.pet.customer.entity.TicketMessage;
import com.pet.customer.mapper.TicketMapper;
import com.pet.customer.mapper.TicketMessageMapper;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.TicketServiceImpl;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final long OWNER_ID = 10L;
    private static final long ORDER_ID = 100L;
    private static final long MERCHANT_ID = 30L;
    private static final long CUSTOMER_SERVICE_USER_ID = 40L;

    @Mock private TicketMapper ticketMapper;
    @Mock private TicketMessageMapper ticketMessageMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private OrderMapper orderMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantCustomerServiceService merchantCustomerServiceService;

    @Test
    void orderTicketStoresOrderMerchant() {
        PetOrder order = order(ORDER_ID, OWNER_ID, MERCHANT_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));

        TicketDTO result = service().create(OWNER_ID, requestForOrder());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getUser_id_wsh());
        assertEquals(ORDER_ID, captor.getValue().getOrder_id_wsh());
        assertEquals(MERCHANT_ID, captor.getValue().getMerchant_id_wsh());
        assertEquals("pending", captor.getValue().getStatus_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
    }

    @Test
    void ticketWithUnknownMerchantIsRejected() {
        TicketCreateRequestDTO request = new TicketCreateRequestDTO();
        request.setMerchant_id_wsh(MERCHANT_ID);
        request.setTitle_wsh("Need help");
        request.setContent_wsh("Question for merchant");
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(OWNER_ID, request));

        assertEquals(404, exception.getCode());
        verify(ticketMapper, never()).insert(any(Ticket.class));
    }

    @Test
    void approvedCustomerServiceCanResolveOwnMerchantTicket() {
        Ticket ticket = merchantTicket(MERCHANT_ID);
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(MERCHANT_ID));

        TicketDTO result = service().resolveForStaff(
                1L,
                "handled by merchant support",
                CUSTOMER_SERVICE_USER_ID,
                false,
                false,
                true);

        assertEquals("resolved", ticket.getStatus_wsh());
        assertEquals("handled by merchant support", ticket.getResult_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
        verify(ticketMapper).updateById(ticket);
        verify(ticketMessageMapper).insert(any(TicketMessage.class));
    }

    @Test
    void customerServiceCannotResolveAnotherMerchantTicket() {
        when(ticketMapper.selectById(1L)).thenReturn(merchantTicket(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(999L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().resolveForStaff(
                        1L,
                        "outside scope",
                        CUSTOMER_SERVICE_USER_ID,
                        false,
                        false,
                        true));

        assertEquals(403, exception.getCode());
        verify(ticketMapper, never()).updateById(any(Ticket.class));
        verify(ticketMessageMapper, never()).insert(any(TicketMessage.class));
    }

    private TicketServiceImpl service() {
        return new TicketServiceImpl(
                ticketMapper,
                ticketMessageMapper,
                userMapper,
                notificationService,
                orderMapper,
                merchantMapper,
                merchantCustomerServiceService);
    }

    private TicketCreateRequestDTO requestForOrder() {
        TicketCreateRequestDTO request = new TicketCreateRequestDTO();
        request.setOrder_id_wsh(ORDER_ID);
        request.setTitle_wsh("Need help");
        request.setContent_wsh("Question about my order");
        return request;
    }

    private PetOrder order(Long orderId, Long ownerId, Long merchantId) {
        PetOrder order = new PetOrder();
        order.setId_wsh(orderId);
        order.setOwner_id_wsh(ownerId);
        order.setMerchant_id_wsh(merchantId);
        return order;
    }

    private Ticket merchantTicket(Long merchantId) {
        Ticket ticket = new Ticket();
        ticket.setId_wsh(1L);
        ticket.setUser_id_wsh(OWNER_ID);
        ticket.setMerchant_id_wsh(merchantId);
        ticket.setTitle_wsh("Need help");
        ticket.setContent_wsh("Question about my order");
        ticket.setStatus_wsh("pending");
        return ticket;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        return merchant;
    }
}
