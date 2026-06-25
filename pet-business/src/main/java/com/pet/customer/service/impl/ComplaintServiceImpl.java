package com.pet.customer.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageParam;
import com.pet.customer.entity.Complaint;
import com.pet.customer.mapper.ComplaintMapper;
import com.pet.customer.service.ComplaintService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintMapper complaintMapper;
    private final UserMapper userMapper;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper, UserMapper userMapper) {
        this.complaintMapper = complaintMapper;
        this.userMapper = userMapper;
    }

    public List<Complaint> listByOwner(Long ownerId) {
        log.info("调用 listByOwner()");
        return complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>()
                        .eq(Complaint::getOwner_id_wsh, ownerId)
                        .orderByDesc(Complaint::getCreated_at_wsh));
    }

    public List<Complaint> listAll() {
        log.info("调用 listAll()");
        List<Complaint> list = complaintMapper.selectList(
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh));
        for (Complaint c : list) enrichOwnerName(c);
        return list;
    }

    public IPage<Complaint> listPage(PageParam pageParam) {
        log.info("调用 listPage()");
        Page<Complaint> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        IPage<Complaint> result = complaintMapper.selectPage(page,
                new LambdaQueryWrapper<Complaint>().orderByDesc(Complaint::getCreated_at_wsh));
        for (Complaint c : result.getRecords()) enrichOwnerName(c);
        return result;
    }

    private void enrichOwnerName(Complaint c) {
        if (c.getOwner_id_wsh() != null) {
            User user = userMapper.selectById(c.getOwner_id_wsh());
            if (user != null) c.setOwner_name_wsh(user.getNickname_wsh() != null ? user.getNickname_wsh() : user.getUsername_wsh());
        }
    }

    @Transactional
    public Complaint create(Complaint complaint) {
        log.info("调用 create()");
        complaint.setStatus_wsh("pending");
        complaintMapper.insert(complaint);
        return complaint;
    }

    @Transactional
    public Complaint process(Long id, String result, String status) {
        log.info("调用 process()");
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException("投诉记录不存在");
        }
        complaint.setStatus_wsh(status);
        complaint.setResult_wsh(result);
        complaintMapper.updateById(complaint);
        return complaint;
    }
}
