package com.pet.ai.service;

import com.pet.ai.dto.AiReportCreateRequestDTO;
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
    List<AiReport> getReportsByOrder(Long userId, Long orderId);

    /**
     * 根据宠物ID获取AI报告列表
     * @param petId 宠物ID
     * @return AI报告列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<AiReport> getReportsByPet(Long userId, Long petId);

    /**
     * 创建AI报告
     * @param request 创建请求DTO
     * @return 创建后的AI报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport createReport(Long userId, AiReportCreateRequestDTO request);

    /**
     * 生成护理建议报告
     * @param petId 宠物ID
     * @param keeperId 看护人ID
     * @param orderId 订单ID
     * @return 生成的护理建议报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport generateCareSuggestion(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * 生成寄养报告
     * @param petId 宠物ID
     * @param keeperId 看护人ID
     * @param orderId 订单ID
     * @return 生成的寄养报告
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    AiReport generateBoardingReport(Long userId, Long petId, Long keeperId, Long orderId);

    /**
     * 订单完成后的内部报告生成入口，由系统事件调用，不面向用户请求。
     */
    AiReport generateBoardingReportInternal(Long petId, Long keeperId, Long orderId);
}

