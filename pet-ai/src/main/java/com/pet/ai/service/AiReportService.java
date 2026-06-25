package com.pet.ai.service;

import com.pet.ai.entity.AiReport;

import java.util.List;

public interface AiReportService {

    /**
     * 根据订单ID获取AI报告列表
     * @param orderId 订单ID
     * @return AI报告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AiReport> getReportsByOrder(Long orderId);

    /**
     * 根据宠物ID获取AI报告列表
     * @param petId 宠物ID
     * @return AI报告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AiReport> getReportsByPet(Long petId);

    /**
     * 创建AI报告
     * @param report AI报告实体
     * @return 创建后的AI报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport createReport(AiReport report);

    /**
     * 生成护理建议报告
     * @param petId 宠物ID
     * @param keeperId 看护人ID
     * @param orderId 订单ID
     * @return 生成的护理建议报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport generateCareSuggestion(Long petId, Long keeperId, Long orderId);

    /**
     * 生成寄养报告
     * @param petId 宠物ID
     * @param keeperId 看护人ID
     * @param orderId 订单ID
     * @return 生成的寄养报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport generateBoardingReport(Long petId, Long keeperId, Long orderId);
}

