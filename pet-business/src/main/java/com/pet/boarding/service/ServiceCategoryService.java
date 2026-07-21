package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceCategoryCreateRequestDTO;
import com.pet.boarding.dto.ServiceCategoryDTO;
import com.pet.boarding.dto.ServiceCategoryUpdateRequestDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.vo.ServiceCategoryTreeVO;

import java.util.List;

public interface ServiceCategoryService {
    List<ServiceCategoryTreeVO> getTree();
    List<ServiceCategory> listByParent(Long parentId);
    ServiceCategory getById(Long id);
    ServiceCategory create(ServiceCategoryCreateRequestDTO request);
    ServiceCategory update(Long id, ServiceCategoryUpdateRequestDTO request);
    void delete(Long id);
    List<ServiceCategory> listAll();
    List<ServiceCategory> listAllEnabled();

    ServiceCategoryDTO toDTO(ServiceCategory entity);
}
