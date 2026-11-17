package com.pet.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.system.vo.RealNameReviewVO;

public interface RealNameReviewService {

    /**
     * 业务作用：分页查询实名认证审核记录，支持按审核状态和关键词过滤
     * 业务规则：status为空则不限制状态；keyword为空则不加模糊条件；按更新时间倒序排列
     */
    IPage<RealNameReviewVO> listPage(PageRequestDTO pageParam, Integer status, String keyword);

    /**
     * 业务作用：将用户实名认证状态置为"已认证"
     * 业务规则：仅"待审核"状态的实名认证可以批准；通过后自动清除驳回原因
     * 异常情况：用户不存在 → BusinessException；非待审核状态 → BusinessException("仅待审核状态的实名认证可批准")
     * 注意事项：reviewerId和remark当前为预留参数，后续可扩展审核日志
     */
    void approve(Long userId, Long reviewerId, String remark);

    /**
     * 业务作用：将用户实名认证状态置为"已驳回"，记录驳回原因
     * 业务规则：仅"待审核"状态的实名认证可以驳回；驳回原因会存储到reject_reason字段供用户查看
     * 异常情况：用户不存在 → BusinessException；非待审核状态 → BusinessException("仅待审核状态的实名认证可驳回")
     * 注意事项：用户可在修改资料后重新提交实名认证，重新提交后状态变回"待审核"
     */
    void reject(Long userId, Long reviewerId, String remark);
}
