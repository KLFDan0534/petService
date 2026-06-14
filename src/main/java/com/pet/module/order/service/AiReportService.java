package com.pet.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.module.order.entity.AiReport;
import com.pet.module.order.mapper.AiReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiReportService {

    private final AiReportMapper aiReportMapper;

    public AiReportService(AiReportMapper aiReportMapper) {
        this.aiReportMapper = aiReportMapper;
    }

    public List<AiReport> getReportsByOrder(Long orderId) {
        return aiReportMapper.selectList(
                new LambdaQueryWrapper<AiReport>()
                        .eq(AiReport::getOrderId, orderId)
                        .orderByDesc(AiReport::getCreatedAt));
    }

    public List<AiReport> getReportsByPet(Long petId) {
        return aiReportMapper.selectList(
                new LambdaQueryWrapper<AiReport>()
                        .eq(AiReport::getPetId, petId)
                        .orderByDesc(AiReport::getCreatedAt));
    }

    @Transactional
    public AiReport createReport(AiReport report) {
        aiReportMapper.insert(report);
        return report;
    }

    public AiReport generateCareSuggestion(Long petId, Long keeperId, Long orderId) {
        AiReport report = new AiReport();
        report.setOrderId(orderId);
        report.setPetId(petId);
        report.setKeeperId(keeperId);
        report.setType("care");
        report.setContent("AI照护建议：\n" +
                "1. 保持每日定时喂食2-3次，提供充足饮水\n" +
                "2. 每日至少遛狗2次，每次不少于30分钟\n" +
                "3. 保持居住环境温度在20-25℃\n" +
                "4. 每日检查宠物精神状态和排便情况\n" +
                "5. 避免与陌生宠物接触，防止交叉感染\n" +
                "6. 按时给予宠物梳毛和清洁\n" +
                "7. 如发现异常及时联系宠物主\n" +
                "8. 记录每日饮食和活动情况");
        aiReportMapper.insert(report);
        return report;
    }

    public AiReport generateBoardingReport(Long petId, Long keeperId, Long orderId) {
        AiReport report = new AiReport();
        report.setOrderId(orderId);
        report.setPetId(petId);
        report.setKeeperId(keeperId);
        report.setType("final");
        report.setContent("AI寄养报告：\n" +
                "寄养期间宠物状态良好，已完成每日照护任务。\n" +
                "饮食情况：正常\n" +
                "活动情况：良好\n" +
                "精神状态：活跃\n" +
                "排便情况：正常\n" +
                "综合评价：优秀\n" +
                "感谢您使用我们的宠物寄养服务！");
        aiReportMapper.insert(report);
        return report;
    }
}
