package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.pet.common.BusinessException;
import com.pet.operation.mapper.RecycleBinMapper;
import com.pet.operation.service.RecycleBinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RecycleBinServiceImpl implements RecycleBinService {

    private static final List<String> SOFT_DELETABLE_TABLES = Arrays.asList(
            "pet_wsh", "merchant_wsh", "keeper_wsh", "pet_service_wsh",
            "pet_order_wsh", "payment_wsh", "refund_wsh", "complaint_wsh",
            "chat_message_wsh", "favorite_wsh", "rating_wsh", "ai_report_wsh",
            "knowledge_document_wsh", "document_embedding_wsh", "address_wsh",
            "notice_wsh", "care_record_wsh", "wallet_wsh", "wallet_transaction_wsh",
            "withdrawal_wsh", "ticket_wsh", "ticket_message_wsh", "content_review_wsh",
            "notification_wsh", "adoption_pet_wsh", "adoption_application_wsh"
    );

    private final RecycleBinMapper recycleBinMapper;

    public RecycleBinServiceImpl(RecycleBinMapper recycleBinMapper) {
        this.recycleBinMapper = recycleBinMapper;
    }

    @Override
    public List<Map<String, Object>> listDeleted(String tableName) {
        log.info("listDeleted() called");
        validateTable(tableName);
        return recycleBinMapper.selectDeleted(tableName);
    }

    @Override
    @Transactional
    public void restore(String tableName, Long id) {
        log.info("restore() called");
        validateTable(tableName);
        int rows = recycleBinMapper.restore(tableName, id);
        if (rows == 0) {
            throw new BusinessException("记录不存在或未删除");
        }
    }

    @Override
    @Transactional
    public void softDelete(String tableName, Long id) {
        log.info("softDelete() called");
        validateTable(tableName);
        int rows = recycleBinMapper.softDelete(tableName, id);
        if (rows == 0) {
            throw new BusinessException("记录不存在");
        }
    }

    @Override
    public List<String> listSoftDeletableTables() {
        log.info("listSoftDeletableTables() called");
        return SOFT_DELETABLE_TABLES;
    }

    private void validateTable(String tableName) {
        if (!SOFT_DELETABLE_TABLES.contains(tableName)) {
            throw new BusinessException("不支持该表的回收站操作");
        }
    }
}
