package com.pet.admin;

import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.mapper.MqConsumeLogMapper;
import com.pet.customer.mq.ComplaintProcessHandlerImpl;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投诉处理 MQ 消费者的幂等契约。
 *
 * <p>消费者是「先写通知、再 basicAck」，进程在两步之间崩溃会让 Broker 重投，
 * 通知就会重复。这里锁定：同一条投诉的同一个终态只发一次通知，
 * 且业务失败时必须释放占位以便重投能重试。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("投诉处理 MQ 消费幂等")
class ComplaintProcessHandlerTest {

    private static final long COMPLAINT_ID = 12L;
    private static final long OWNER_ID = 10L;

    @Mock private ComplaintMapper complaintMapper;
    @Mock private NotificationService notificationService;
    @Mock private MqConsumeLogMapper consumeLogMapper;

    private ComplaintProcessHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new ComplaintProcessHandlerImpl(complaintMapper, notificationService, consumeLogMapper);
    }

    @Test
    @DisplayName("首次消费：抢占台账成功后创建一条通知")
    void firstConsumeCreatesNotification() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("resolved", "已处理"));
        when(consumeLogMapper.insertClaim(anyString(), anyString())).thenReturn(1);

        handler.handle(COMPLAINT_ID);

        verify(consumeLogMapper).insertClaim("complaint-process", COMPLAINT_ID + ":resolved");
        verify(notificationService, times(1)).create(any(Notification.class));
    }

    @Test
    @DisplayName("重复消费：台账冲突时不再创建第二条通知")
    void duplicateConsumeSkipsNotification() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("resolved", "已处理"));
        when(consumeLogMapper.insertClaim(anyString(), anyString()))
                .thenThrow(new DuplicateKeyException("uk_consumer_event"));

        handler.handle(COMPLAINT_ID);

        verify(notificationService, never()).create(any(Notification.class));
    }

    @Test
    @DisplayName("同一事件重复投递两次只产生一条通知")
    void twoDeliveriesOfSameEventProduceOneNotification() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("rejected", "不符合受理条件"));
        // 第一次插入成功，第二次触发唯一键冲突
        when(consumeLogMapper.insertClaim(anyString(), anyString()))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("uk_consumer_event"));

        handler.handle(COMPLAINT_ID);
        handler.handle(COMPLAINT_ID);

        verify(notificationService, times(1)).create(any(Notification.class));
    }

    @Test
    @DisplayName("事件键带终态：不同终态各自成事件，不会被误判为重复")
    void eventKeyIncludesStatus() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("rejected", "驳回"));
        when(consumeLogMapper.insertClaim(anyString(), anyString())).thenReturn(1);

        handler.handle(COMPLAINT_ID);

        verify(consumeLogMapper).insertClaim("complaint-process", COMPLAINT_ID + ":rejected");
    }

    @Test
    @DisplayName("通知创建失败时释放台账占位，允许重投后重试")
    void failureReleasesClaim() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("resolved", "已处理"));
        when(consumeLogMapper.insertClaim(anyString(), anyString())).thenReturn(1);
        doThrow(new RuntimeException("db down")).when(notificationService).create(any(Notification.class));

        handler.handle(COMPLAINT_ID);

        verify(consumeLogMapper).releaseClaim("complaint-process", COMPLAINT_ID + ":resolved");
        // 失败被吞掉（与既有语义一致：通知失败不影响消息消费）
        verify(consumeLogMapper, never()).insertClaim(eq("complaint-process"), eq("other"));
    }

    @Test
    @DisplayName("投诉不存在时跳过，不抢占台账也不发通知")
    void missingComplaintSkipsEverything() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(null);

        handler.handle(COMPLAINT_ID);

        verify(consumeLogMapper, never()).insertClaim(anyString(), anyString());
        verify(notificationService, never()).create(any(Notification.class));
    }

    @Test
    @DisplayName("投诉没有所有者时跳过通知，但仍不重复占用台账")
    void complaintWithoutOwnerSkips() {
        Complaint complaint = complaint("resolved", "已处理");
        complaint.setOwner_id_wsh(null);
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint);

        handler.handle(COMPLAINT_ID);

        verify(consumeLogMapper, never()).insertClaim(anyString(), anyString());
        verify(notificationService, never()).create(any(Notification.class));
    }

    @Test
    @DisplayName("通知内容包含投诉标题与处理结果，且类型为 complaint")
    void notificationCarriesBusinessFields() {
        when(complaintMapper.selectById(COMPLAINT_ID)).thenReturn(complaint("resolved", "已退款"));
        when(consumeLogMapper.insertClaim(anyString(), anyString())).thenReturn(1);

        handler.handle(COMPLAINT_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).create(captor.capture());
        Notification n = captor.getValue();
        assertEquals(OWNER_ID, n.getUser_id_wsh());
        assertEquals("complaint", n.getType_wsh());
        assertEquals(COMPLAINT_ID, n.getRelated_id_wsh());
        assertTrue(n.getContent_wsh().contains("已退款"), "处理结果必须出现在通知里");
        assertTrue(n.getContent_wsh().contains("投诉标题"), "投诉标题必须出现在通知里");
    }

    private Complaint complaint(String status, String result) {
        Complaint complaint = new Complaint();
        complaint.setId_wsh(COMPLAINT_ID);
        complaint.setOwner_id_wsh(OWNER_ID);
        complaint.setTitle_wsh("投诉标题");
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(result);
        return complaint;
    }
}
