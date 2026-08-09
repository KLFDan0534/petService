package com.pet.ai.listener;

import com.pet.ai.service.AiReportService;
import com.pet.order.entity.PetOrder;
import com.pet.order.event.OrderCompletedEvent;
import com.pet.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 【订单完成事件监听器 - AI 报告生成】
 *
 * 业务作用：
 * 在订单完成事务提交后，自动触发 AI 寄养总结报告的生成。
 * 使用 Spring 的 TransactionalEventListener(AFTER_COMMIT) 确保
 * 在订单状态变更事务成功提交后才执行，避免事务未提交时读取不到最新的订单数据。
 *
 * 触发时机：
 * OrderFulfillmentService.completeOrder() 执行成功并提交事务后。
 *
 * 调用链：
 * OrderFulfillmentService.completeOrder()
 *   ↓ 发布 OrderCompletedEvent
 * OrderCompletedReportListener.handleOrderCompleted()
 *   ↓ (事务提交后，AFTER_COMMIT)
 * AiReportService.generateBoardingReportInternal()
 *   ↓
 * AI 模型生成报告 → MySQL + Chroma 存储
 *   ↓
 * 标记 PetOrder.final_report_generated = 1
 *
 * 幂等性：
 * 通过 PetOrder.final_report_generated 字段防止重复生成。
 * 如果该字段已为 1，则直接跳过。
 *
 * 异常处理：
 * 生成失败仅记录 warn 日志，不影响原订单事务。
 *
 * 注意事项：
 * - fallbackExecution=true：即使没有事务，也执行监听器（支持非事务环境）
 * - 此监听器在 pet-ai 模块中，与 pet-business 解耦
 */
@Component
@Slf4j
public class OrderCompletedReportListener {

    private final AiReportService aiReportService;
    private final OrderMapper orderMapper;

    public OrderCompletedReportListener(AiReportService aiReportService, OrderMapper orderMapper) {
        this.aiReportService = aiReportService;
        this.orderMapper = orderMapper;
    }

    /**
     * 【业务名称】订单完成事件处理 - AI报告生成
     * <p>业务作用：在订单完成事务提交后（AFTER_COMMIT），自动调用AI生成寄养总结报告，并更新订单的final_report_generated标志位。</p>
     * <p>调用场景：订单状态变更为"已完成"时由Spring事件机制自动触发。</p>
     * <p>调用链：订单完成 → OrderCompletedEvent发布 → TransactionalEventListener(AFTER_COMMIT) → handleOrderCompleted() → AiReportService.generateBoardingReportInternal() → AI生成报告 → 更新订单final_report_generated=1</p>
     * <p>数据处理：校验event和orderId非空；查询PetOrder；校验final_report_generated不为1（幂等控制）；调用AI生成报告；更新订单标志位。</p>
     * <p>业务规则：通过final_report_generated字段保证幂等性（每个订单只生成一次）；fallbackExecution=true支持非事务环境。</p>
     * <p>状态影响：新增一条type=final的AI报告；更新PetOrder.final_report_generated=1。</p>
     * <p>异常情况：event为null或orderId为null时静默返回；订单不存在或已生成报告时跳过；AI生成异常捕获warn日志不阻断主流程。</p>
     * <p>注意事项：此方法在事务提交后异步执行，不阻塞订单完成主流程；异常不会传播到发布方事务。</p>
     *
     * @param event 订单完成事件，包含orderId/petId/keeperId
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        PetOrder order = orderMapper.selectById(event.orderId());
        if (order == null || Integer.valueOf(1).equals(order.getFinal_report_generated_wsh())) {
            return;
        }
        try {
            aiReportService.generateBoardingReportInternal(event.petId(), event.keeperId(), event.orderId());
            order.setFinal_report_generated_wsh(1);
            orderMapper.updateById(order);
        } catch (Exception e) {
            log.warn("为订单 {} 生成最终报告失败", event.orderId(), e);
        }
    }
}
