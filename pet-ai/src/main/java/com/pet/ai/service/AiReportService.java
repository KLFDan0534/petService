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
     * 【业务名称】按订单查询AI报告
     * <p>业务作用：根据订单ID查询关联的AI报告列表（护理建议或寄养总结），先查MySQL数据库，未命中时降级到Chroma向量库查询。</p>
     * <p>调用场景：用户在前端查看某个订单的AI报告列表（如护理建议、寄养总结）。</p>
     * <p>调用链：AiReportController.getByOrder() → AiReportService.getReportsByOrder() → 鉴权 → AiReportMapper.selectList() 或 ChromaService.get()</p>
     * <p>数据处理：校验orderId合法性；校验用户访问权限；先查MySQL（LambdaQueryWrapper按order_id_wsh过滤+创建时间倒序），有数据直接返回；无数据时查Chroma并转换为AiReport列表。</p>
     * <p>业务规则：仅宠物主、关联看护人、关联商家和ADMIN角色可查看；MySQL数据优先，Chroma为降级补充。</p>
     * <p>状态影响：无状态变更，只读操作。</p>
     * <p>异常情况：orderId为null或不合法时抛出BusinessException(400)；订单不存在抛出404；无权限抛出403。</p>
     * <p>注意事项：Chroma查询结果按创建时间倒序排序；parseLongSafely处理可能的格式异常。</p>
     *
     * @param userId  用户ID，用于鉴权
     * @param orderId 订单ID
     * @return AI报告列表，按创建时间倒序
     */
    List<AiReport> getReportsByOrder(Long userId, Long orderId);

    /**
     * 【业务名称】按宠物查询AI报告
     * <p>业务作用：根据宠物ID查询关联的AI报告列表，先查MySQL数据库，未命中时降级到Chroma向量库查询。</p>
     * <p>调用场景：用户在宠物详情页查看该宠物所有的AI报告（历史护理建议、历史寄养总结）。</p>
     * <p>调用链：AiReportController.getByPet() → AiReportService.getReportsByPet() → 鉴权 → AiReportMapper.selectList() 或 ChromaService.get()</p>
     * <p>数据处理：校验petId合法性；校验用户是否为宠物主；先MySQL查询，未命中时从Chroma按pet_id_wsh元数据查询并转换。</p>
     * <p>业务规则：仅宠物主本人和ADMIN可查看；MySQL > Chroma 降级策略。</p>
     * <p>状态影响：无状态变更，只读操作。</p>
     * <p>异常情况：petId不合法抛出BusinessException(400)；宠物不存在抛出404；非宠物主抛出403。</p>
     * <p>注意事项：需要预先在petMapper中确认宠物主人。</p>
     *
     * @param userId 用户ID，用于鉴权
     * @param petId  宠物ID
     * @return AI报告列表，按创建时间倒序
     */
    List<AiReport> getReportsByPet(Long userId, Long petId);

    /**
     * 【业务名称】手动创建AI报告
     * <p>业务作用：看护人或管理员直接提交报告内容创建AI报告，不经过AI模型生成流程，内容为用户/看护人手动编写。</p>
     * <p>调用场景：看护人完成服务后手动编写护理总结提交；管理员补充报告。</p>
     * <p>调用链：AiReportController.create() → AiReportService.createReport() → 写权限鉴权 → 构造AiReport → saveReport() → MySQL插入 + Chroma存储</p>
     * <p>数据处理：从requestDTO中提取order_id/pet_id/keeper_id/content/type字段设置到AiReport实体；调用saveReport持久化。</p>
     * <p>业务规则：需要写权限——ADMIN或关联订单/宠物有权限；不调用AI模型。</p>
     * <p>状态影响：新增一条AI报告记录（MySQL + Chroma）。</p>
     * <p>异常情况：request为null或内容为空抛出BusinessException(400)；无写权限抛出403。</p>
     * <p>注意事项：Chroma存储失败不影响MySQL主记录；type字段由调用方传入。</p>
     *
     * @param userId  用户ID，用于鉴权
     * @param request 创建请求DTO，包含报告内容和关联ID
     * @return 创建完成后的AI报告
     */
    AiReport createReport(Long userId, AiReportCreateRequestDTO request);

    /**
     * 【业务名称】AI生成护理建议报告
     * <p>业务作用：根据宠物档案、看护人信息、订单信息和护理动态，调用AI模型生成结构化的宠物护理建议报告。AI不可用时降级为模板填充。</p>
     * <p>调用场景：用户在订单详情页或宠物详情页点击"生成护理建议"按钮。</p>
     * <p>调用链：AiReportController.generateCareSuggestion() → AiReportService.generateCareSuggestion() → 鉴权 → 查询数据(pet/keeper/order/careRecords) → 构建AI Prompt → AiChatService.chat() → AI返回/模板降级 → saveReport() → MySQL + Chroma</p>
     * <p>数据处理：查询PetOrder/Pet/Keeper/CareRecord关联数据；构建带宠物档案上下文的AI提示词；AI返回后trim()；AI为null时使用buildCareSuggestion()模板；保存到MySQL和Chroma。</p>
     * <p>业务规则：petId和orderId至少提供一个；从order可推导petId和keeperId；AI生成内容优先级高于模板；报告类型为"care"。</p>
     * <p>状态影响：新增一条type="care"的AI报告。</p>
     * <p>异常情况：petId和orderId均为null抛出400；orderId指定但order不存在抛出404；鉴权失败抛出403。</p>
     * <p>注意事项：AI不可用时模板建议包含日常喂养、活动安排、卫生护理、风险提醒和异常处理五个方面。</p>
     *
     * @param userId   用户ID，用于鉴权
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return AI生成的护理建议报告
     */
    AiReport generateCareSuggestion(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * 【业务名称】AI生成寄养总结报告（用户调用）
     * <p>业务作用：根据宠物档案、看护人信息、订单信息和护理动态，调用AI模型生成寄养总结报告。包含寄养概况、饮食状态、活动状态、健康观察和回家后建议。</p>
     * <p>调用场景：用户在订单完成后查看"寄养总结"时触发。</p>
     * <p>调用链：AiReportController.generateBoardingReport() → AiReportService.generateBoardingReport() → 鉴权 → generateBoardingReportUnchecked() → 查询数据 → AI生成/模板降级 → saveReport()</p>
     * <p>数据处理：校验orderId；校验用户对该订单的访问权限；委托generateBoardingReportUnchecked执行数据查询和AI生成核心逻辑。</p>
     * <p>业务规则：orderId必填；必须对订单有访问权限；报告类型为"final"。</p>
     * <p>状态影响：新增一条type="final"的AI报告。</p>
     * <p>异常情况：orderId为null抛出400；订单不存在抛出404；无权限抛出403。</p>
     * <p>注意事项：最终内容为AI生成（优先）或模板降级（AI不可用时）。</p>
     *
     * @param userId   用户ID，用于鉴权
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return AI生成的寄养总结报告
     */
    AiReport generateBoardingReport(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * 【业务名称】内部AI生成寄养总结报告（事件驱动）
     * <p>业务作用：订单完成事件触发后内部调用生成寄养总结报告，不校验用户权限，直接根据订单信息生成。由OrderCompletedReportListener在事务提交后调用。</p>
     * <p>调用场景：订单状态变更为"已完成"时，@TransactionalEventListener(phase = AFTER_COMMIT) 自动触发。</p>
     * <p>调用链：OrderCompletedReportListener.handleOrderCompleted() → AiReportService.generateBoardingReportInternal() → generateBoardingReportUnchecked() → 查询数据 → AI生成 → saveReport()</p>
     * <p>数据处理：校验orderId；不校验用户权限直接委托generateBoardingReportUnchecked执行。</p>
     * <p>业务规则：无鉴权校验（内部调用信任）；orderId必填；生成后更新订单的final_report_generated标志位为1。</p>
     * <p>状态影响：新增一条type="final"的AI报告；更新PetOrder.final_report_generated=1。</p>
     * <p>异常情况：orderId为null抛出400；订单不存在抛出404。</p>
     * <p>注意事项：此方法异步执行（事件监听），不阻塞主流程；生成失败由监听器捕获并记录warn日志。</p>
     *
     * @param petId    宠物ID
     * @param keeperId 看护人ID
     * @param orderId  订单ID
     * @return 生成的寄养总结报告
     */
    AiReport generateBoardingReportInternal(Long petId, Long keeperId, Long orderId);
}

