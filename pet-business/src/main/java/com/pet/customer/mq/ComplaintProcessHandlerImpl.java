package com.pet.customer.mq;

import com.pet.common.mq.ComplaintProcessHandler;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ComplaintProcessHandlerImpl implements ComplaintProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(ComplaintProcessHandlerImpl.class);

    private final ComplaintMapper complaintMapper;
    private final NotificationService notificationService;

    public ComplaintProcessHandlerImpl(ComplaintMapper complaintMapper,
                                       NotificationService notificationService) {
        this.complaintMapper = complaintMapper;
        this.notificationService = notificationService;
    }

    @Override
    public void handle(Long complaintId) {
        if (complaintId == null) {
            log.warn("ComplaintProcessHandler received null complaintId");
            return;
        }

        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            log.warn("Complaint not found: {}, message skipped", complaintId);
            return;
        }

        log.info("Processing complaint {} (status={}, result={})",
                complaintId, complaint.getStatus_wsh(), complaint.getResult_wsh());

        if (complaint.getOwner_id_wsh() == null) {
            log.warn("Complaint {} has no owner, skipping notification", complaintId);
            return;
        }

        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(complaint.getOwner_id_wsh());
            notification.setTitle_wsh("Complaint processed");
            notification.setContent_wsh("Complaint \"" + complaint.getTitle_wsh()
                    + "\" has been " + complaint.getStatus_wsh()
                    + (complaint.getResult_wsh() != null ? ": " + complaint.getResult_wsh() : ""));
            notification.setType_wsh("complaint");
            notification.setRelated_id_wsh(complaintId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
            log.info("Notification sent for complaint {}", complaintId);
        } catch (Exception e) {
            log.warn("Failed to send notification for complaint {}", complaintId, e);
        }
    }
}
