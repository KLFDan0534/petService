package com.pet.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageParam;
import com.pet.customer.entity.Complaint;

import java.util.List;

public interface ComplaintService {
    /**
     * 根据投诉人ID获取投诉列表
     * @param ownerId 投诉人ID
     * @return 投诉列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Complaint> listByOwner(Long ownerId);
    /**
     * 获取所有投诉列表
     * @return 投诉列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Complaint> listAll();
    /**
     * 分页查询投诉列表
     * @param pageParam 分页参数
     * @return 分页投诉数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<Complaint> listPage(PageParam pageParam);
    /**
     * 创建投诉
     * @param complaint 投诉实体
     * @return 创建后的投诉
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Complaint create(Complaint complaint);
    /**
     * 处理投诉
     * @param id 投诉ID
     * @param result 处理结果
     * @param status 处理状态
     * @return 更新后的投诉
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Complaint process(Long id, String result, String status);
}

