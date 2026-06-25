package com.pet.pet.service;

import com.pet.pet.entity.Category;
import java.util.List;

public interface CategoryService {
    /**
     * 获取所有分类列表
     * @return 分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Category> listAll();
    /**
     * 根据父级ID获取子分类列表
     * @param parentId 父级ID
     * @return 子分类列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Category> listByParent(Long parentId);
    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类实体
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Category getById(Long id);
    /**
     * 创建分类
     * @param category 分类实体
     * @return 创建后的分类
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Category create(Category category);
    /**
     * 更新分类
     * @param id 分类ID
     * @param category 分类实体
     * @return 更新后的分类
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Category update(Long id, Category category);
    /**
     * 删除分类
     * @param id 分类ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
}

