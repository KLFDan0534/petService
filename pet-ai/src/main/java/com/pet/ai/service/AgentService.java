package com.pet.ai.service;

import com.pet.ai.dto.AgentExecuteResult;

/**
 * AI 代理服务接口，用户通过自然语言描述需求，Agent 自主完成宠物寄养下单全流程。
 * <p>
 * Agent 执行流程包含 9 个步骤：
 * 1. 意图识别（提取宠物类型和寄养天数）
 * 2. 查询宠物档案（匹配用户宠物）
 * 3. 搜索附近商家
 * 4. 搜索附近看护人
 * 5. 多维度评分排序（评分、距离、价格、投诉率、完成率）
 * 6. 生成推荐
 * 7. 创建待支付订单
 * 8. 创建待支付记录
 * 9. 发送 MQ 通知
 * <p>
 * 支持自动支付模式（需支付密码授权）。
 */
public interface AgentService {

    /**
     * 【业务名称】AI Agent自动下单执行入口
     * <p>业务作用：根据用户的自然语言描述（如"金毛寄养3天"），Agent自动完成宠物寄养订单的全流程：意图识别→查询宠物档案→搜索附近商家→搜索附近看护人→评分排序→生成推荐→创建订单→创建支付→发送通知。支持自动支付模式。</p>
     * <p>调用场景：用户在AI助手中输入"帮我下单，金毛寄养3天"等自然语言指令。</p>
     * <p>调用链：AgentController.execute() → AgentService.execute() → step1~step9流水线 → OrderService.createOrder() → PaymentService.createPayment() → MessageSender.sendOrderCreate()</p>
     * <p>数据处理：正则解析用户输入提取宠物类型和天数；查询用户宠物档案匹配；根据用户坐标搜索5km内商家和看护人；多维度评分（评分40%+距离20%+价格20%+投诉率10%+完成率10%）；构造OrderCreateRequestDTO创建订单；创建余额支付记录；发送MQ通知。</p>
     * <p>业务规则：任何步骤缺少必要信息时抛出NeedUserInputException返回needs_user_input状态提示用户补充；autoPay=true时需要验证支付密码；自动支付时先验证密码再创建订单和支付再执行支付；所有异常事务回滚；MQ通知失败不影响主流程。</p>
     * <p>状态影响：新增一条待支付订单和一条待支付记录；autoPay=true时订单直接变为已支付。</p>
     * <p>异常情况：用户输入为空/宠物类型无法识别/天数缺失/无宠物档案/无位置信息/附近无商家或无看护人/用户无手机号 → NeedUserInputException返回前端提示；支付密码错误 → BusinessException(400/403)；数据库异常 → 事务回滚并返回安全错误信息。</p>
     * <p>注意事项：此方法带@Transactional事务注解，任何步骤失败都会回滚已创建的订单和支付；返回结果包含status字段区分success/pending_payment/needs_user_input/failed。</p>
     *
     * @param userId           当前用户ID
     * @param userInput        用户自然语言输入（如"金毛寄养 3 天"）
     * @param latitude         用户纬度（用于附近搜索），传null则使用用户档案中的坐标
     * @param longitude        用户经度
     * @param autoPay          是否启用自动支付，启用时需要提供支付密码
     * @param paymentPassword  支付密码，autoPay为true时必须提供
     * @return Agent执行结果，包含状态、订单号、支付号、推荐看护人等信息
     */
    AgentExecuteResult execute(Long userId, String userInput, Double latitude, Double longitude,
                                Boolean autoPay, String paymentPassword);
}

