package com.pet.ai.service;

import com.pet.ai.dto.AgentExecuteResult;
import com.pet.ai.dto.AgentPlanResult;

/**
 * AI 代理服务接口，用户通过自然语言描述需求，Agent 自动完成宠物寄养下单。
 * <p>
 * 两阶段流程：
 * 1. plan：LLM 结构化提取需求 → 匹配宠物/商家/看护人 → 生成推荐方案（不落库）→ 返回方案 token
 * 2. confirm：凭方案 token 创建订单/支付（可选自动支付）
 * <p>
 * 当前仅支持日间寄养（day 计费、date_range 预约）。
 */
public interface AgentService {

    /**
     * 【业务名称】智能下单 plan 阶段
     * <p>业务作用：用 LLM 将用户自然语言需求解析为结构化字段（宠物、服务类型、日期、天数、门店偏好、预算），匹配宠物档案与附近看护人，生成推荐方案但不落单，返回方案 token 供用户确认。</p>
     * <p>业务规则：LLM 不可用/解析失败返回 failed 状态，不做规则兜底；非寄养服务类型（洗护/遛宠/训练/医疗）明确提示不支持；日期需晚于今天且天数 1..365；无坐标时回退用户档案坐标。</p>
     * <p>状态影响：无（纯只读 + 内存缓存）。</p>
     *
     * @param userId     当前用户ID
     * @param userInput  用户自然语言需求
     * @param latitude   纬度（可空）
     * @param longitude  经度（可空）
     * @param address    定位地址（可空）
     * @return 推荐方案（plan_generated / needs_user_input / failed）
     */
    AgentPlanResult plan(Long userId, String userInput, Double latitude, Double longitude, String address);

    /**
     * 【业务名称】智能下单 confirm 阶段
     * <p>业务作用：凭 plan 阶段返回的方案 token 创建订单与支付记录，autoPay=true 时校验支付密码并直接支付。事务内完成，失败回滚并释放方案 token 供重试。</p>
     * <p>业务规则：token 原子认领防重复下单；支付密码校验失败不创建订单；成功后才消费 token（afterCommit）。</p>
     * <p>状态影响：新增订单与支付记录。</p>
     *
     * @param userId          当前用户ID
     * @param planToken       plan 阶段返回的方案 token
     * @param autoPay         是否自动支付
     * @param paymentPassword 支付密码（autoPay=true 时必填）
     * @return 执行结果（success / pending_payment / needs_user_input / failed）
     */
    AgentExecuteResult confirm(Long userId, String planToken, Boolean autoPay, String paymentPassword);
}