package com.pet.pet.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.pet.dto.CategoryCreateRequestDTO;
import com.pet.pet.dto.CategoryDTO;
import com.pet.pet.dto.CategoryUpdateRequestDTO;
import com.pet.pet.entity.Category;
import com.pet.pet.mapper.CategoryMapper;
import com.pet.pet.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryDTO> listAll() {
        log.info("调用 listAll()");
        return toDTOList(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort_order_wsh)
                        .orderByAsc(Category::getId_wsh)));
    }

    public List<CategoryDTO> listByParent(Long parentId) {
        log.info("调用 listByParent()");
        return toDTOList(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParent_id_wsh, parentId)
                        .orderByAsc(Category::getSort_order_wsh)));
    }

    public CategoryDTO getById(Long id) {
        log.info("调用 getById()");
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return toDTO(c);
    }

    @Transactional
    public CategoryDTO create(CategoryCreateRequestDTO request) {
        log.info("调用 create()");
        Category category = new Category();
        category.setName_wsh(request.getName_wsh());
        category.setParent_id_wsh(request.getParent_id_wsh() != null ? request.getParent_id_wsh() : 0L);
        category.setSort_order_wsh(request.getSort_order_wsh() != null ? request.getSort_order_wsh() : 0);
        categoryMapper.insert(category);
        return toDTO(category);
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryUpdateRequestDTO request) {
        log.info("调用 update()");
        Category existing = getByIdRaw(id);
        if (request.getName_wsh() != null) existing.setName_wsh(request.getName_wsh());
        if (request.getParent_id_wsh() != null) existing.setParent_id_wsh(request.getParent_id_wsh());
        if (request.getSort_order_wsh() != null) existing.setSort_order_wsh(request.getSort_order_wsh());
        categoryMapper.updateById(existing);
        return toDTO(existing);
    }

    @Transactional
    public void delete(Long id) {
        log.info("调用 delete()");
        long subCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParent_id_wsh, id));
        if (subCount > 0) throw new BusinessException("存在子分类，无法删除");
        categoryMapper.deleteById(id);
    }

    private Category getByIdRaw(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return c;
    }

    private CategoryDTO toDTO(Category c) {
        if (c == null) return null;
        CategoryDTO dto = new CategoryDTO();
        dto.setId_wsh(c.getId_wsh());
        dto.setName_wsh(c.getName_wsh());
        dto.setParent_id_wsh(c.getParent_id_wsh());
        dto.setSort_order_wsh(c.getSort_order_wsh());
        return dto;
    }

    private List<CategoryDTO> toDTOList(List<Category> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
