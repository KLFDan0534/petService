package com.pet.membership.service;

import com.pet.membership.dto.MemberPlanCreateRequestDTO;
import com.pet.membership.dto.MemberPlanDTO;
import com.pet.membership.dto.MemberPlanUpdateRequestDTO;
import com.pet.membership.entity.MemberPlan;

import java.util.List;

public interface MemberPlanService {
    List<MemberPlanDTO> listPlans(Integer status, boolean activeOnly);

    MemberPlanDTO getPlan(Long id);

    MemberPlanDTO createPlan(MemberPlanCreateRequestDTO request);

    MemberPlanDTO updatePlan(Long id, MemberPlanUpdateRequestDTO request);

    MemberPlanDTO updateStatus(Long id, Integer status);

    void deletePlan(Long id);

    MemberPlanDTO toDTO(MemberPlan plan);
}
