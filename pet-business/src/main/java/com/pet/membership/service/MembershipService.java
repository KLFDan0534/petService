package com.pet.membership.service;

import com.pet.membership.dto.UserMembershipDTO;
import com.pet.membership.entity.UserMembership;

import java.util.List;

public interface MembershipService {
    UserMembershipDTO getCurrentMembership(Long userId);

    List<UserMembershipDTO> listMembershipsForAdmin(String status);

    int expireMemberships();

    UserMembershipDTO toDTO(UserMembership membership);
}
