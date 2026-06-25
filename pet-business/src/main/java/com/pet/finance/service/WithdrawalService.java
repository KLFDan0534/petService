package com.pet.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.finance.entity.Withdrawal;
import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {
    /**
     * 根据用户ID获取提现记录列表
     * @param userId 用户ID
     * @return 提现记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Withdrawal> listByUser(Long userId);
    /**
     * 获取所有提现记录列表
     * @return 提现记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Withdrawal> listAll();
    /**
     * 分页查询提现记录列表
     * @param pageParam 分页参数
     * @return 分页提现数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<Withdrawal> listPage(PageParam pageParam);
    /**
     * 申请提现
     * @param userId 用户ID
     * @param amount 提现金额
     * @param bankName 银行名称
     * @param bankCard 银行卡号
     * @param accountName 开户人姓名
     * @return 创建的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Withdrawal apply(Long userId, BigDecimal amount, String bankName, String bankCard, String accountName);
    /**
     * 审批通过提现
     * @param id 提现ID
     * @param remark 审批备注
     * @return 更新后的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Withdrawal approve(Long id, String remark);
    /**
     * 驳回提现申请
     * @param id 提现ID
     * @param remark 驳回原因
     * @return 更新后的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Withdrawal reject(Long id, String remark);
    /**
     * 完成提现（标记已打款）
     * @param id 提现ID
     * @return 更新后的提现记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Withdrawal complete(Long id);
}

