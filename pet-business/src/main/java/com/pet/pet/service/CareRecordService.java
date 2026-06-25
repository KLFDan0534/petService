package com.pet.pet.service;

import com.pet.pet.entity.CareRecord;

import java.util.List;

public interface CareRecordService {
    /**
     * 根据订单ID获取护理记录列表
     * @param orderId 订单ID
     * @return 护理记录列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<CareRecord> listByOrder(Long orderId);
    /**
     * 根据ID获取护理记录
     * @param id 记录ID
     * @return 护理记录实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecord getById(Long id);
    /**
     * 创建护理记录
     * @param record 护理记录实体
     * @return 创建后的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecord create(CareRecord record);
    /**
     * 更新护理记录
     * @param id 记录ID
     * @param record 护理记录实体
     * @return 更新后的护理记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecord update(Long id, CareRecord record);
    /**
     * 删除护理记录
     * @param id 记录ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
}

