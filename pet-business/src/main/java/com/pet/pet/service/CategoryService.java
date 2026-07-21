package com.pet.pet.service;

import com.pet.pet.dto.CategoryCreateRequestDTO;
import com.pet.pet.dto.CategoryDTO;
import com.pet.pet.dto.CategoryUpdateRequestDTO;
import com.pet.pet.entity.Category;
import java.util.List;

public interface CategoryService {
    /**
     * 获取所有分类列表
     * @return 分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<CategoryDTO> listAll();
    /**
     * 根据父级ID获取子分类列表
     * @param parentId 父级ID
     * @return 子分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<CategoryDTO> listByParent(Long parentId);
    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CategoryDTO getById(Long id);
    /**
     * 创建分类
     * @param request 创建请求
     * @return 创建后的分类数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CategoryDTO create(CategoryCreateRequestDTO request);
    /**
     * 更新分类
     * @param id 分类ID
     * @param request 更新请求
     * @return 更新后的分类数据传输对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    CategoryDTO update(Long id, CategoryUpdateRequestDTO request);
    /**
     * 删除分类
     * @param id 分类ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
}

