package com.pet.ai.service;

import com.pet.ai.dto.AiReportCreateRequestDTO;
import com.pet.ai.entity.AiReport;

import java.util.List;

/**
 * AI 报告服务接口，提供宠物护理建议报告和寄养总结报告的生成、查询功能。
 * <p>
 * 报告通过 AI 模型（DeepSeek）根据宠物档案、护理动态和订单信息自动生成，
 * 同时保存到数据库和 Chroma 向量数据库中用于后续检索。
 * 外部用户调用需鉴权，内部事件调用（订单完成）无需鉴权。
 */
public interface AiReportService {

    /**
     * 按订单查询AI报告
     *
     * 业务作用：根据订单ID查询关联的AI报告列表（护理建议或寄养总结），先查MySQL数据库，未命中时降级到Chroma向量库查询。
     * 业务规则：仅宠物主、关联看护人、关联商家和ADMIN角色可查看；MySQL数据优先，Chroma为降级补充。
     * 异常情况：orderId为null或不合法时抛出BusinessException(400)；订单不存在抛出404；无权限抛出403。
     * 注意事项：Chroma查询结果按创建时间倒序排序；parseLongSafely处理可能的格式异常。
     *
     * @param userId  用户ID，用于鉴权
     * @param orderId 订单ID
     * @return AI报告列表，按创建时间倒序
     */
    List<AiReport> getReportsByOrder(Long userId, Long orderId);

    /**
     * 按宠物查询AI报告
     *
     * 业务作用：根据宠物ID查询关联的AI报告列表，先查MySQL数据库，未命中时降级到Chroma向量库查询。
     * 业务规则：仅宠物主本人和ADMIN可查看；MySQL > Chroma 降级策略。
     * 异常情况：petId不合法抛出BusinessException(400)；宠物不存在抛出404；非宠物主抛出403。
     * 注意事项：需要预先在petMapper中确认宠物主人。
     *
     * @param userId 用户ID，用于鉴权
     * @param petId  宠物ID
     * @return AI报告列表，按创建时间倒序
     */
    List<AiReport> getReportsByPet(Long userId, Long petId);

    /**
     * 手动创建AI报告
     *
     * 业务作用：看护人或管理员直接提交报告内容创建AI报告，不经过AI模型生成流程，内容为用户/看护人手动编写。
     * 业务规则：需要写权限——ADMIN或关联订单/宠物有权限；不调用AI模型。
     * 异常情况：request为null或内容为空抛出BusinessException(400)；无写权限抛出403。
     * 注意事项：Chroma存储失败不影响MySQL主记录；type字段由调用方传入。
     *
     * @param userId  用户ID，用于鉴权
     * @param request 创建请求DTO，包含报告内容和关联ID
     * @return 创建完成后的AI报告
     */
    AiReport createReport(Long userId, AiReportCreateRequestDTO request);

    /**
     * AI生成护理建议报告
     *
     * 业务作用：根据宠物档案、看护人信息、订单信息和护理动态，调用AI模型生成结构化的宠物护理建议报告。AI不可用时降级为模板填充。
     * 业务规则：petId和orderId至少提供一个；从order可推导petId和keeperId；AI生成内容优先级高于模板；报告类型为"care"。
     * 异常情况：petId和orderId均为null抛出400；orderId指定但order不存在抛出404；鉴权失败抛出403。
     * 注意事项：AI不可用时模板建议包含日常喂养、活动安排、卫生护理、风险提醒和异常处理五个方面。
     *
     * @param userId   用户ID，用于鉴权
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return AI生成的护理建议报告
     */
    AiReport generateCareSuggestion(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * AI生成寄养总结报告（用户调用）
     *
     * 业务作用：根据宠物档案、看护人信息、订单信息和护理动态，调用AI模型生成寄养总结报告。包含寄养概况、饮食状态、活动状态、健康观察和回家后建议。
     * 业务规则：orderId必填；必须对订单有访问权限；报告类型为"final"。
     * 异常情况：orderId为null抛出400；订单不存在抛出404；无权限抛出403。
     * 注意事项：最终内容为AI生成（优先）或模板降级（AI不可用时）。
     *
     * @param userId   用户ID，用于鉴权
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return AI生成的寄养总结报告
     */
    AiReport generateBoardingReport(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * 内部AI生成寄养总结报告（事件驱动）
     *
     * 业务作用：订单完成事件触发后内部调用生成寄养总结报告，不校验用户权限，直接根据订单信息生成。由OrderCompletedReportListener在事务提交后调用。
     * 业务规则：无鉴权校验（内部调用信任）；orderId必填；生成后更新订单的final_report_generated标志位为1。
     * 异常情况：orderId为null抛出400；订单不存在抛出404。
     * 注意事项：此方法异步执行（事件监听），不阻塞主流程；生成失败由监听器捕获并记录warn日志。
     *
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return 生成的寄养总结报告
     */
    AiReport generateBoardingReportInternal(Long petId, Long keeperId, Long orderId);
}

