package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.mq.MessageSender;
import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NoticeReadMapper;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.NoticeServiceImpl;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeDeliveryServiceTest {

    private static final long NOTICE_ID = 100L;

    @Mock private NoticeMapper noticeMapper;
    @Mock private NoticeReadMapper noticeReadMapper;
    @Mock private NotificationService notificationService;
    @Mock private UserMapper userMapper;
    @Mock private MessageSender messageSender;

    @Test
    void bannerCreateAllowsBlankContentAndDelivery() {
        stubInsertId(NOTICE_ID);
        NoticeCreateRequestDTO request = new NoticeCreateRequestDTO();
        request.setTitle_wsh("首页横幅");
        request.setContent_wsh("");
        request.setType_wsh("banner");

        Notice result = service().create(request);

        assertEquals("banner", result.getType_wsh());
        assertEquals("", result.getContent_wsh());
        assertEquals("", result.getDelivery_type_wsh());
        verify(notificationService, never()).create(any(Notification.class));
        verify(notificationService, never()).deleteByRelatedId(any());
        // banner 类型不投递通知，不应产生 MQ 消息
        verify(messageSender, never()).sendNoticeNotification(any());
    }

    @Test
    void noticeCreateRequiresContentAndDelivery() {
        NoticeCreateRequestDTO missingContent = noticeCreateRequest("popup");
        missingContent.setContent_wsh(" ");
        assertThrows(BusinessException.class, () -> service().create(missingContent));

        NoticeCreateRequestDTO missingDelivery = noticeCreateRequest(null);
        assertThrows(BusinessException.class, () -> service().create(missingDelivery));

        verify(noticeMapper, never()).insert(any(Notice.class));
    }

    @Test
    void noticeCreateWithNotificationSendsMqMessage() {
        stubInsertId(NOTICE_ID);
        NoticeCreateRequestDTO request = noticeCreateRequest(" Notification , popup , notification ");

        Notice result = service().create(request);

        assertEquals("notification,popup", result.getDelivery_type_wsh());
        // 通知生成已改为异步：create() 只投递公告ID，不再同步插入通知
        verify(messageSender, times(1)).sendNoticeNotification(NOTICE_ID);
        verify(notificationService, never()).create(any(Notification.class));
        verify(notificationService, never()).deleteByRelatedId(any());
    }

    @Test
    void noticeCreateWithPopupOnlyDoesNotSendMqMessage() {
        stubInsertId(NOTICE_ID);
        NoticeCreateRequestDTO request = noticeCreateRequest("popup");

        service().create(request);

        // 纯弹窗公告不需要生成站内通知
        verify(messageSender, never()).sendNoticeNotification(any());
    }

    @Test
    void updateFromNotificationToPopupDeletesNotificationsWithoutRecreate() {
        Notice existing = activeNotice("notification");
        when(noticeMapper.selectById(NOTICE_ID)).thenReturn(existing);
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO();
        request.setDelivery_type_wsh("popup");

        Notice result = service().update(NOTICE_ID, request);

        assertEquals("popup", result.getDelivery_type_wsh());
        verify(notificationService).deleteByRelatedId(NOTICE_ID);
        verify(notificationService, never()).create(any(Notification.class));
        verify(noticeReadMapper).delete(any(LambdaQueryWrapper.class));
        verify(noticeMapper).updateById(existing);
        // 改为纯弹窗后不再需要生成通知
        verify(messageSender, never()).sendNoticeNotification(any());
    }

    @Test
    void updateFromPopupToNotificationSendsMqMessage() {
        Notice existing = activeNotice("popup");
        when(noticeMapper.selectById(NOTICE_ID)).thenReturn(existing);
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO();
        request.setDelivery_type_wsh("notification");

        service().update(NOTICE_ID, request);

        verify(notificationService).deleteByRelatedId(NOTICE_ID);
        // 重新同步同样走 MQ 异步
        verify(messageSender, times(1)).sendNoticeNotification(NOTICE_ID);
        verify(notificationService, never()).create(any(Notification.class));
    }

    @Test
    void inactiveNoticeDeletesNotificationsWithoutRecreate() {
        Notice existing = activeNotice("notification");
        when(noticeMapper.selectById(NOTICE_ID)).thenReturn(existing);
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO();
        request.setStatus_wsh(StatusCode.NOTICE_DRAFT.getValue());

        Notice result = service().update(NOTICE_ID, request);

        assertEquals(StatusCode.NOTICE_DRAFT.getValue(), result.getStatus_wsh());
        verify(notificationService).deleteByRelatedId(NOTICE_ID);
        verify(notificationService, never()).create(any(Notification.class));
        // 草稿状态不生成通知
        verify(messageSender, never()).sendNoticeNotification(any());
    }

    private NoticeServiceImpl service() {
        return new NoticeServiceImpl(noticeMapper, noticeReadMapper, notificationService,
                userMapper, messageSender);
    }

    private void stubInsertId(Long id) {
        doAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            notice.setId_wsh(id);
            return 1;
        }).when(noticeMapper).insert(any(Notice.class));
    }

    private NoticeCreateRequestDTO noticeCreateRequest(String deliveryType) {
        NoticeCreateRequestDTO request = new NoticeCreateRequestDTO();
        request.setTitle_wsh("系统公告");
        request.setContent_wsh("公告内容");
        request.setType_wsh("notice");
        request.setDelivery_type_wsh(deliveryType);
        return request;
    }

    private Notice activeNotice(String deliveryType) {
        Notice notice = new Notice();
        notice.setId_wsh(NOTICE_ID);
        notice.setTitle_wsh("系统公告");
        notice.setContent_wsh("公告内容");
        notice.setType_wsh("notice");
        notice.setDelivery_type_wsh(deliveryType);
        notice.setStatus_wsh(StatusCode.NOTICE_ACTIVE.getValue());
        return notice;
    }
}
