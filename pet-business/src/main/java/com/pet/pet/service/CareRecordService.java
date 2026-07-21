package com.pet.pet.service;

import com.pet.pet.dto.CareRecordCreateRequestDTO;
import com.pet.pet.dto.CareRecordDTO;
import com.pet.pet.dto.CareRecordUpdateRequestDTO;
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
    List<CareRecordDTO> listByOrder(Long orderId);

    List<CareRecordDTO> listByOrder(Long actorUserId, boolean admin, Long orderId);
    /**
     * 根据ID获取护理记录
     * @param id 记录ID
     * @return 护理记录数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecordDTO getById(Long id);

    CareRecordDTO getById(Long actorUserId, boolean admin, Long id);
    /**
     * 创建护理记录
     * @param request 创建请求
     * @return 创建后的护理记录数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CareRecordDTO create(CareRecordCreateRequestDTO request);

    CareRecordDTO create(Long actorUserId, boolean admin, CareRecordCreateRequestDTO request);
    /**
     * 更新护理记录
     * @param id 记录ID
     * @param request 更新请求
     * @return 更新后的护理记录数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
    **/
    CareRecordDTO update(Long id, CareRecordUpdateRequestDTO request);

    CareRecordDTO update(Long actorUserId, boolean admin, Long id, CareRecordUpdateRequestDTO request);

    /**
     * 删除护理记录
     * @param id 记录ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);

    void delete(Long actorUserId, boolean admin, Long id);
}

