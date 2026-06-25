package com.pet.pet.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.pet.entity.Category;
import com.pet.pet.mapper.CategoryMapper;
import com.pet.pet.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类服务实现类
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 获取所有分类列表
     * @return 分类列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public List<Category> listAll() {
        log.info("调用 listAll()");
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort_order_wsh)
                        .orderByAsc(Category::getId_wsh));
    }

    /**
     * 根据父级ID获取子分类列表
     * @param parentId 父级ID
     * @return 子分类列表
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public List<Category> listByParent(Long parentId) {
        log.info("调用 listByParent()");
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParent_id_wsh, parentId)
                        .orderByAsc(Category::getSort_order_wsh));
    }

    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类实体
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    public Category getById(Long id) {
        log.info("调用 getById()");
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return c;
    }

    /**
     * 创建分类
     * @param category 分类实体
     * @return 创建后的分类
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    public Category create(Category category) {
        log.info("调用 create()");
        if (category.getSort_order_wsh() == null) category.setSort_order_wsh(0);
        if (category.getParent_id_wsh() == null) category.setParent_id_wsh(0L);
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 更新分类
     * @param id 分类ID
     * @param category 分类实体
     * @return 更新后的分类
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    public Category update(Long id, Category category) {
        log.info("调用 update()");
        Category existing = getById(id);
        if (category.getName_wsh() != null) existing.setName_wsh(category.getName_wsh());
        if (category.getParent_id_wsh() != null) existing.setParent_id_wsh(category.getParent_id_wsh());
        if (category.getSort_order_wsh() != null) existing.setSort_order_wsh(category.getSort_order_wsh());
        categoryMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Transactional
    public void delete(Long id) {
        log.info("调用 delete()");
        long subCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParent_id_wsh, id));
        if (subCount > 0) throw new BusinessException("存在子分类，无法删除");
        categoryMapper.deleteById(id);
    }
}
