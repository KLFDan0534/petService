package com.pet.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.system.vo.RealNameReviewVO;

public interface RealNameReviewService {

    /**
     * 【分页查询实名认证审核列表】
     *
     * 业务作用：分页查询实名认证审核记录，支持按审核状态和关键词过滤
     *
     * 调用场景：管理后台实名认证审核页面的列表展示
     *
     * 调用链：AdminController ↓ listPage() → UserMapper.selectPage → 遍历转RealNameReviewVO
     *
     * 数据处理：pageParam(page,size) + status(审核状态筛选) + keyword(用户名/昵称/真实姓名模糊匹配) → LambdaQueryWrapper条件组合 → MyBatis-Plus分页 → 转换Page<User>为Page<RealNameReviewVO>
     *
     * 业务规则：status为空则不限制状态；keyword为空则不加模糊条件；按更新时间倒序排列
     *
     * 状态影响：无
     *
     * 异常情况：无
     */
    IPage<RealNameReviewVO> listPage(PageRequestDTO pageParam, Integer status, String keyword);

    /**
     * 【审核通过实名认证】
     *
     * 业务作用：将用户实名认证状态置为"已认证"
     *
     * 调用场景：审核人员在后台审核通过用户的实名认证申请
     *
     * 调用链：AdminController ↓ approve() → UserMapper.selectById → 判读状态 → 设置REAL_NAME_VERIFIED → UserMapper.updateById
     *
     * 数据处理：userId(被审核用户) + reviewerId(审核人,预留) + remark(备注,预留) → 查询用户 → 判读状态为待审核 → 设置real_name_status=已认证 + 清除驳回原因 → updateById
     *
     * 业务规则：仅"待审核"状态的实名认证可以批准；通过后自动清除驳回原因
     *
     * 状态影响：更新user_wsh表的real_name_status_wsh=2(已认证) + 清除reject_reason_wsh
     *
     * 异常情况：用户不存在 → BusinessException；非待审核状态 → BusinessException("仅待审核状态的实名认证可批准")
     *
     * 注意事项：reviewerId和remark当前为预留参数，后续可扩展审核日志
     */
    void approve(Long userId, Long reviewerId, String remark);

    /**
     * 【驳回实名认证】
     *
     * 业务作用：将用户实名认证状态置为"已驳回"，记录驳回原因
     *
     * 调用场景：审核人员在后台驳回用户的实名认证申请
     *
     * 调用链：AdminController ↓ reject() → UserMapper.selectById → 判读状态 → 设置REAL_NAME_REJECTED → UserMapper.updateById
     *
     * 数据处理：userId + reviewerId(预留) + remark(驳回原因) → 查询用户 → 判读状态为待审核 → 设置real_name_status=已驳回 + 设置reject_reason=remark → updateById
     *
     * 业务规则：仅"待审核"状态的实名认证可以驳回；驳回原因会存储到reject_reason字段供用户查看
     *
     * 状态影响：更新user_wsh表的real_name_status_wsh=3(已驳回) + 写入reject_reason_wsh
     *
     * 异常情况：用户不存在 → BusinessException；非待审核状态 → BusinessException("仅待审核状态的实名认证可驳回")
     *
     * 注意事项：用户可在修改资料后重新提交实名认证，重新提交后状态变回"待审核"
     */
    void reject(Long userId, Long reviewerId, String remark);
}
