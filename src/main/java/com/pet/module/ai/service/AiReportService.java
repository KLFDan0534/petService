package com.pet.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.ai.entity.AiReport;
import com.pet.module.ai.mapper.AiReportMapper;
import com.pet.module.keeper.entity.Keeper;
import com.pet.module.keeper.mapper.KeeperMapper;
import com.pet.module.order.entity.PetOrder;
import com.pet.module.order.mapper.OrderMapper;
import com.pet.module.pet.entity.Pet;
import com.pet.module.pet.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiReportService {

    private final AiReportMapper aiReportMapper;
    private final PetMapper petMapper;
    private final KeeperMapper keeperMapper;
    private final OrderMapper orderMapper;

    public AiReportService(AiReportMapper aiReportMapper, PetMapper petMapper,
                           KeeperMapper keeperMapper, OrderMapper orderMapper) {
        this.aiReportMapper = aiReportMapper;
        this.petMapper = petMapper;
        this.keeperMapper = keeperMapper;
        this.orderMapper = orderMapper;
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
        Pet pet = petMapper.selectById(petId);
        Keeper keeper = keeperMapper.selectById(keeperId);
        PetOrder order = orderMapper.selectById(orderId);

        String petName = pet != null ? pet.getName() : "宠物";
        String keeperName = keeper != null ? keeper.getName() : "寄养员";
        String petType = pet != null && pet.getType() != null ? pet.getType() : "宠物";
        String allergies = pet != null && pet.getAllergies() != null ? pet.getAllergies() : "无已知过敏";
        String habits = pet != null && pet.getHabits() != null ? pet.getHabits() : "无特殊习惯";
        int days = order != null && order.getDays() != null ? order.getDays() : 1;

        AiReport report = new AiReport();
        report.setOrderId(orderId);
        report.setPetId(petId);
        report.setKeeperId(keeperId);
        report.setType("care");

        String content = "AI照护建议 - " + petName + "（" + petType + "）\n" +
                "寄养照护员：" + keeperName + "\n" +
                "寄养天数：" + days + "天\n\n" +
                "【日常照护】\n" +
                "1. 保持每日定时喂食2-3次，提供充足饮水\n" +
                "2. 根据宠物习惯，每日合理安排活动时间\n" +
                "3. 保持居住环境温度在20-25℃\n" +
                "4. 每日检查宠物精神状态和排便情况\n\n" +
                "【特殊注意】\n" +
                "- 过敏信息：" + allergies + "\n" +
                "- 特殊习惯：" + habits + "\n" +
                "- 避免与陌生宠物接触，防止交叉感染\n\n" +
                "【卫生管理】\n" +
                "1. 按时给予宠物梳毛和清洁\n" +
                "2. 保持居住环境清洁通风\n" +
                "3. 定期消毒宠物用具\n\n" +
                "如发现异常及时联系宠物主。";
        report.setContent(content);
        aiReportMapper.insert(report);
        return report;
    }

    public AiReport generateBoardingReport(Long petId, Long keeperId, Long orderId) {
        Pet pet = petMapper.selectById(petId);
        Keeper keeper = keeperMapper.selectById(keeperId);
        PetOrder order = orderMapper.selectById(orderId);

        String petName = pet != null ? pet.getName() : "宠物";
        String keeperName = keeper != null ? keeper.getName() : "寄养员";
        String petType = pet != null && pet.getType() != null ? pet.getType() : "宠物";
        int days = order != null && order.getDays() != null ? order.getDays() : 1;

        AiReport report = new AiReport();
        report.setOrderId(orderId);
        report.setPetId(petId);
        report.setKeeperId(keeperId);
        report.setType("final");

        String content = "AI寄养报告 - " + petName + "（" + petType + "）\n" +
                "寄养照护员：" + keeperName + "\n" +
                "寄养天数：" + days + "天\n" +
                "报告生成时间：系统自动生成\n\n" +
                "【寄养总结】\n" +
                petName + "在本次寄养期间接受了以下照护服务：\n" +
                "- 每日定时喂食和饮水管理\n" +
                "- 日常活动与运动安排\n" +
                "- 健康状况监测\n\n" +
                "【状态评估】\n" +
                "饮食情况：正常（按时进食，食欲良好）\n" +
                "活动情况：良好（每日保持适量运动）\n" +
                "精神状态：活跃（与照护员互动良好）\n" +
                "排便情况：正常\n" +
                "睡眠情况：良好\n" +
                "综合评价：优秀\n\n" +
                "【寄养建议】\n" +
                "1. 建议回家后继续保持当前饮食和作息习惯\n" +
                "2. 建议定期进行健康检查\n" +
                "3. 如发现任何异常请及时就医\n\n" +
                "感谢您使用我们的宠物寄养服务，期待下次为您服务！";
        report.setContent(content);
        aiReportMapper.insert(report);
        return report;
    }
}
