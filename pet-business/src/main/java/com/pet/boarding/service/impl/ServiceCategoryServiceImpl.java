package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.boarding.dto.ServiceCategoryCreateRequestDTO;
import com.pet.boarding.dto.ServiceCategoryDTO;
import com.pet.boarding.dto.ServiceCategoryUpdateRequestDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.vo.ServiceCategoryTreeVO;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.ServiceCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper serviceItemMapper;

    public ServiceCategoryServiceImpl(ServiceCategoryMapper categoryMapper, ServiceItemMapper serviceItemMapper) {
        this.categoryMapper = categoryMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    @Override
    public List<ServiceCategoryTreeVO> getTree() {
        List<ServiceCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getStatus_wsh, 1)
                        .orderByAsc(ServiceCategory::getSort_order_wsh)
                        .orderByAsc(ServiceCategory::getId_wsh));
        List<ServiceCategory> roots = all.stream().filter(c -> c.getParent_id_wsh() == null || c.getParent_id_wsh() == 0).collect(Collectors.toList());
        List<ServiceCategoryTreeVO> tree = new ArrayList<>();
        for (ServiceCategory root : roots) {
            tree.add(buildTreeNode(root, all));
        }
        return tree;
    }

    private ServiceCategoryTreeVO buildTreeNode(ServiceCategory node, List<ServiceCategory> all) {
        ServiceCategoryTreeVO vo = new ServiceCategoryTreeVO();
        vo.setId_wsh(node.getId_wsh());
        vo.setParent_id_wsh(node.getParent_id_wsh());
        vo.setName_wsh(node.getName_wsh());
        vo.setCode_wsh(node.getCode_wsh());
        vo.setSort_order_wsh(node.getSort_order_wsh());
        vo.setStatus_wsh(node.getStatus_wsh());
        for (ServiceCategory child : all) {
            if (child.getParent_id_wsh() != null && child.getParent_id_wsh().equals(node.getId_wsh())) {
                vo.getChildren().add(buildTreeNode(child, all));
            }
        }
        return vo;
    }

    @Override
    public List<ServiceCategory> listByParent(Long parentId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getParent_id_wsh, parentId)
                        .eq(ServiceCategory::getStatus_wsh, 1)
                        .orderByAsc(ServiceCategory::getSort_order_wsh));
    }

    @Override
    public ServiceCategory getById(Long id) {
        ServiceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    @Override
    @Transactional
    public ServiceCategory create(ServiceCategoryCreateRequestDTO request) {
        ServiceCategory category = new ServiceCategory();
        category.setParent_id_wsh(request.getParent_id_wsh() != null ? request.getParent_id_wsh() : 0L);
        category.setName_wsh(request.getName_wsh());
        category.setCode_wsh(request.getCode_wsh());
        category.setSort_order_wsh(request.getSort_order_wsh() != null ? request.getSort_order_wsh() : 0);
        category.setStatus_wsh(request.getStatus_wsh() != null ? request.getStatus_wsh() : 1);
        categoryMapper.insert(category);
        return category;
    }

    @Override
    @Transactional
    public ServiceCategory update(Long id, ServiceCategoryUpdateRequestDTO request) {
        ServiceCategory existing = getById(id);
        if (request.getParent_id_wsh() != null) existing.setParent_id_wsh(request.getParent_id_wsh());
        if (request.getName_wsh() != null) existing.setName_wsh(request.getName_wsh());
        if (request.getCode_wsh() != null) existing.setCode_wsh(request.getCode_wsh());
        if (request.getSort_order_wsh() != null) existing.setSort_order_wsh(request.getSort_order_wsh());
        if (request.getStatus_wsh() != null) existing.setStatus_wsh(request.getStatus_wsh());
        categoryMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ServiceCategory category = getById(id);
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<ServiceCategory>().eq(ServiceCategory::getParent_id_wsh, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        Long refCount = serviceItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.pet.boarding.entity.ServiceItem>()
                        .eq(com.pet.boarding.entity.ServiceItem::getCategory_id_wsh, id));
        if (refCount > 0) {
            throw new BusinessException("该分类被服务项目引用，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public ServiceCategoryDTO toDTO(ServiceCategory entity) {
        if (entity == null) return null;
        ServiceCategoryDTO dto = new ServiceCategoryDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setParent_id_wsh(entity.getParent_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setCode_wsh(entity.getCode_wsh());
        dto.setSort_order_wsh(entity.getSort_order_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        return dto;
    }

    @Override
    public List<ServiceCategory> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .orderByAsc(ServiceCategory::getSort_order_wsh)
                        .orderByAsc(ServiceCategory::getId_wsh));
    }

    @Override
    public List<ServiceCategory> listAllEnabled() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getStatus_wsh, 1)
                        .orderByAsc(ServiceCategory::getSort_order_wsh)
                        .orderByAsc(ServiceCategory::getId_wsh));
    }
}
