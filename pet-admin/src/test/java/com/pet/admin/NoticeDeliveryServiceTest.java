package com.pet.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NoticeReadMapper;
import com.pet.operation.service.NotificationService;
import com.pet.operation.service.impl.NoticeServiceImpl;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    void noticeCreateWithNotificationCreatesUserNotifications() {
        stubInsertId(NOTICE_ID);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L), user(2L)));
        NoticeCreateRequestDTO request = noticeCreateRequest(" Notification , popup , notification ");

        Notice result = service().create(request);

        assertEquals("notification,popup", result.getDelivery_type_wsh());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(2)).create(captor.capture());
        assertEquals(List.of(1L, 2L), captor.getAllValues().stream().map(Notification::getUser_id_wsh).toList());
        captor.getAllValues().forEach(notification -> {
            assertEquals("notice", notification.getType_wsh());
            assertEquals(NOTICE_ID, notification.getRelated_id_wsh());
            assertEquals("系统公告", notification.getTitle_wsh());
            assertEquals("公告内容", notification.getContent_wsh());
        });
        verify(notificationService, never()).deleteByRelatedId(any());
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
    }

    @Test
    void updateFromPopupToNotificationRecreatesUserNotifications() {
        Notice existing = activeNotice("popup");
        when(noticeMapper.selectById(NOTICE_ID)).thenReturn(existing);
        when(userMapper.selectList(any())).thenReturn(List.of(user(1L), user(2L)));
        NoticeUpdateRequestDTO request = new NoticeUpdateRequestDTO();
        request.setDelivery_type_wsh("notification");

        service().update(NOTICE_ID, request);

        verify(notificationService).deleteByRelatedId(NOTICE_ID);
        verify(notificationService, times(2)).create(any(Notification.class));
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
    }

    private NoticeServiceImpl service() {
        return new NoticeServiceImpl(noticeMapper, noticeReadMapper, notificationService, userMapper);
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

    private User user(Long id) {
        User user = new User();
        user.setId_wsh(id);
        return user;
    }
}