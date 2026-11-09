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
            "notification_wsh"
    );

    private final RecycleBinMapper recycleBinMapper;

    /**
     * 【构造函数：注入回收站 Mapper】
     *
     * 业务作用：注入 RecycleBinMapper 供软删除记录的查询、恢复与删除使用。
     *
     * 调用场景：容器启动创建 RecycleBinServiceImpl Bean 时自动调用。
     *
     * 调用链：Spring IoC → RecycleBinServiceImpl(构造) → recycleBinMapper 字段赋值。
     *
     * 数据处理：仅做字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：允许操作的表由静态常量白名单控制，不在白名单内的一律拒绝。
     */
    public RecycleBinServiceImpl(RecycleBinMapper recycleBinMapper) {
        this.recycleBinMapper = recycleBinMapper;
    }

    /**
     * 查询指定表中已软删除的记录
     */
    @Override
    public List<Map<String, Object>> listDeleted(String tableName) {
        log.info("listDeleted() 被调用");
        validateTable(tableName);
        return recycleBinMapper.selectDeleted(tableName);
    }

    /**
     * 恢复一条软删除记录
     */
    @Override
    @Transactional
    public void restore(String tableName, Long id) {
        log.info("restore() 被调用");
        validateTable(tableName);
        int rows = recycleBinMapper.restore(tableName, id);
        // 影响行数为 0 说明记录不存在或已恢复
        if (rows == 0) {
            throw new BusinessException("记录不存在或未删除");
        }
    }

    /**
     * 软删除一条记录
     */
    @Override
    @Transactional
    public void softDelete(String tableName, Long id) {
        log.info("softDelete() 被调用");
        validateTable(tableName);
        int rows = recycleBinMapper.softDelete(tableName, id);
        // 影响行数为 0 说明记录不存在
        if (rows == 0) {
            throw new BusinessException("记录不存在");
        }
    }

    /**
     * 获取所有支持软删除的表名列表
     */
    @Override
    public List<String> listSoftDeletableTables() {
        log.info("listSoftDeletableTables() 被调用");
        return SOFT_DELETABLE_TABLES;
    }

    /**
     * 【校验表名是否支持回收站操作】
     *
     * 业务作用：防止把任意表名拼进 SQL，只允许白名单内的表。
     *
     * 调用场景：listDeleted、restore、softDelete 的入口校验。
     *
     * 调用链：listDeleted / restore / softDelete → validateTable → SOFT_DELETABLE_TABLES.contains。
     *
     * 数据处理：精确匹配白名单列表。
     *
     * 业务规则：只允许 SOFT_DELETABLE_TABLES 中列出的表。
     *
     * 状态影响：无。
     *
     * 异常情况：表名不在白名单时抛 BusinessException("不支持该表的回收站操作")。
     *
     * 注意事项：这是防 SQL 注入的关键校验；新增可回收表时必须同步维护白名单常量。
     */
    private void validateTable(String tableName) {
        // 白名单校验，防止表名拼接造成注入
        if (!SOFT_DELETABLE_TABLES.contains(tableName)) {
            throw new BusinessException("不支持该表的回收站操作");
        }
    }
}
