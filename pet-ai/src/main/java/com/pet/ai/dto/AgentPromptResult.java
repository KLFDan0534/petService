package com.pet.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * LLM 结构化提取结果（智能下单 plan 阶段）。
 * <p>由大模型将用户自然语言下单需求解析为结构化字段。</p>
 */
@Data
public class AgentPromptResult {

    /** 宠物昵称，找不到为 null */
    @JsonProperty("pet_name")
    private String petName;

    /** 宠物类型/品种描述，找不到为 null */
    @JsonProperty("pet_type")
    private String petType;

    /** 服务类型：BOARDING / GROOMING / TRAINING / WALK / MEDICAL；无法判断为 null */
    @JsonProperty("service_type")
    private String serviceType;

    /** 开始日期 yyyy-MM-dd，推算不出为 null */
    @JsonProperty("start_date")
    private String startDate;

    /** 寄养夜数（正整数），未提及为 null */
    @JsonProperty("nights")
    private Integer nights;

    /** 旧字段：寄养天数。仅为兼容旧提示词保留，读取时优先使用 nights */
    @JsonProperty("days")
    private Integer days;

    /** 用户偏好的门店名/商圈关键词，没有为 null */
    @JsonProperty("merchant_keyword")
    private String merchantKeyword;

    /** 每夜价格上限（预算），未提及为 null */
    @JsonProperty("max_price_per_night")
    private BigDecimal maxPricePerNight;

    /** 旧字段：每日价格上限。仅为兼容旧提示词保留 */
    @JsonProperty("max_price_per_day")
    private BigDecimal maxPricePerDay;

    /** 计费单位：night / session / hour，未明确为 null（服务端默认按 night 处理） */
    @JsonProperty("unit")
    private String unit;
}
