package com.pet.module.complaint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.complaint.entity.Complaint;
import com.pet.module.complaint.mapper.ComplaintMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintMapper complaintMapper;

    public ComplaintService(ComplaintMapper complaintMapper) {
        this.complaintMapper = complaintMapper;
    }

    public List<Complaint> listByOwner(Long ownerId) {
        return complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getOwnerId, ownerId)
                        .orderByDesc(Complaint::getCreatedAt));
    }

    public List<Complaint> listAll() {
        return complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreatedAt));
    }

    @Transactional
    public Complaint create(Complaint complaint) {
        complaint.setStatus("pending");
        complaintMapper.insert(complaint);
        return complaint;
    }

    @Transactional
    public Complaint process(Long id, String result, String status) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException("投诉不存在");
        }
        complaint.setStatus(status);
        complaint.setResult(result);
        complaintMapper.updateById(complaint);
        return complaint;
    }
}
