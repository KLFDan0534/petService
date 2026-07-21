package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.entity.Merchant;
import com.pet.boarding.mapper.MerchantMapper;
import com.pet.common.BusinessException;
import com.pet.customer.dto.ComplaintCreateRequestDTO;
import com.pet.customer.dto.ComplaintDTO;
import com.pet.customer.dto.ComplaintEvidenceDTO;
import com.pet.customer.entity.ChatMessage;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ChatMessageMapper;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.service.MerchantCustomerServiceService;
import com.pet.customer.service.impl.ComplaintServiceImpl;
import com.pet.mq.MessageSender;
import com.pet.operation.service.NotificationService;
import com.pet.order.entity.PetOrder;
import com.pet.order.mapper.OrderMapper;
import com.pet.pet.entity.CareRecord;
import com.pet.pet.mapper.CareRecordMapper;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    private static final long OWNER_ID = 10L;
    private static final long ORDER_ID = 100L;
    private static final long MERCHANT_ID = 30L;
    private static final long CUSTOMER_SERVICE_USER_ID = 40L;

    @Mock private ComplaintMapper complaintMapper;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private OrderMapper orderMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private CareRecordMapper careRecordMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantCustomerServiceService merchantCustomerServiceService;
    @Mock private MessageSender messageSender;

    @Test
    void orderComplaintRequiresOrderOwner() {
        PetOrder order = order(ORDER_ID, 99L);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().create(requestForOrder(), OWNER_ID));

        assertEquals(403, exception.getCode());
        verify(complaintMapper, never()).insert(any(Complaint.class));
    }

    @Test
    void orderComplaintDefaultsTargetAndReturnsEvidenceSummary() {
        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setKeeper_id_wsh(20L);
        order.setMerchant_id_wsh(MERCHANT_ID);
        order.setOrder_no_wsh("ORD100");
        order.setStatus_wsh("in_progress");
        ChatMessage latestMessage = new ChatMessage();
        latestMessage.setCreated_at_wsh(LocalDateTime.of(2026, 7, 4, 10, 0));
        CareRecord latestCare = new CareRecord();
        latestCare.setRecord_time_wsh(LocalDateTime.of(2026, 7, 4, 11, 0));

        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(MERCHANT_ID));
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(careRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(chatMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestMessage);
        when(careRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestCare);

        ComplaintDTO result = service().create(requestForOrder(), OWNER_ID);

        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintMapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getTarget_id_wsh());
        assertEquals("keeper", captor.getValue().getTarget_type_wsh());
        assertEquals(MERCHANT_ID, captor.getValue().getMerchant_id_wsh());
        assertNotNull(result.getEvidence_summary_wsh());
        assertEquals("ORD100", result.getEvidence_summary_wsh().getOrder_no_wsh());
        assertEquals("in_progress", result.getEvidence_summary_wsh().getOrder_status_wsh());
        assertEquals(3L, result.getEvidence_summary_wsh().getChat_message_count_wsh());
        assertEquals(2L, result.getEvidence_summary_wsh().getCare_record_count_wsh());
    }

    @Test
    void approvedCustomerServiceCanProcessOwnMerchantComplaint() {
        Complaint complaint = merchantComplaint(MERCHANT_ID);
        when(complaintMapper.selectById(1L)).thenReturn(complaint);
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(MERCHANT_ID));

        ComplaintDTO result = service().processForStaff(
                1L,
                "handled by merchant support",
                "resolved",
                CUSTOMER_SERVICE_USER_ID,
                false,
                false,
                true);

        assertEquals("resolved", complaint.getStatus_wsh());
        assertEquals("handled by merchant support", complaint.getResult_wsh());
        assertEquals(MERCHANT_ID, result.getMerchant_id_wsh());
        verify(complaintMapper).updateById(complaint);
    }

    @Test
    void customerServiceCannotProcessAnotherMerchantComplaint() {
        when(complaintMapper.selectById(1L)).thenReturn(merchantComplaint(MERCHANT_ID));
        when(merchantCustomerServiceService.getApprovedMerchantIds(CUSTOMER_SERVICE_USER_ID))
                .thenReturn(Set.of(999L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().processForStaff(
                        1L,
                        "outside scope",
                        "resolved",
                        CUSTOMER_SERVICE_USER_ID,
                        false,
                        false,
                        true));

        assertEquals(403, exception.getCode());
        verify(complaintMapper, never()).updateById(any(Complaint.class));
    }

    @Test
    void evidenceIncludesRecentChatAndCareRecords() {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(1L);
        complaint.setOrder_id_wsh(ORDER_ID);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("Service issue");
        complaint.setContent_wsh("Need customer service review");

        PetOrder order = order(ORDER_ID, OWNER_ID);
        order.setOrder_no_wsh("ORD100");
        order.setStatus_wsh("in_progress");

        ChatMessage message = new ChatMessage();
        message.setId_wsh(11L);
        message.setOrder_id_wsh(ORDER_ID);
        message.setContent_wsh("Please check this");
        CareRecord record = new CareRecord();
        record.setId_wsh(21L);
        record.setOrder_id_wsh(ORDER_ID);
        record.setContent_wsh("Lunch photo");

        when(complaintMapper.selectById(1L)).thenReturn(complaint);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(chatMessageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(careRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));
        when(careRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        ComplaintEvidenceDTO evidence = service().getEvidence(1L);

        assertEquals("ORD100", evidence.getSummary_wsh().getOrder_no_wsh());
        assertEquals(1, evidence.getChat_messages_wsh().size());
        assertEquals("Please check this", evidence.getChat_messages_wsh().get(0).getContent_wsh());
        assertEquals(1, evidence.getCare_records_wsh().size());
        assertEquals("Lunch photo", evidence.getCare_records_wsh().get(0).getContent_wsh());
    }

    private ComplaintServiceImpl service() {
        return new ComplaintServiceImpl(
                complaintMapper,
                userMapper,
                notificationService,
                orderMapper,
                chatMessageMapper,
                careRecordMapper,
                merchantMapper,
                merchantCustomerServiceService,
                messageSender);
    }

    private ComplaintCreateRequestDTO requestForOrder() {
        ComplaintCreateRequestDTO request = new ComplaintCreateRequestDTO();
        request.setOrder_id_wsh(ORDER_ID);
        request.setTitle_wsh("Service issue");
        request.setContent_wsh("Need customer service review");
        return request;
    }

    private PetOrder order(Long orderId, Long ownerId) {
        PetOrder order = new PetOrder();
        order.setId_wsh(orderId);
        order.setOwner_id_wsh(ownerId);
        return order;
    }

    private Complaint merchantComplaint(Long merchantId) {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(1L);
        complaint.setMerchant_id_wsh(merchantId);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("Service issue");
        complaint.setContent_wsh("Need customer service review");
        complaint.setStatus_wsh("pending");
        return complaint;
    }

    private Merchant merchant(Long id) {
        Merchant merchant = new Merchant();
        merchant.setId_wsh(id);
        return merchant;
    }
}
