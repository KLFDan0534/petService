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

    /**
     * 业务作用：注入 CategoryMapper 供各查询与增删改方法使用。
     * 注意事项：本类未显式实现接口方法上的 @Override 之外逻辑，全部直接操作 Mapper。
     */
    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 分类全量列表查询（实现）
     *
     * 业务作用：查询全部分类，按 sort_order 和 ID 升序排列，返回未被逻辑删除的分类。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤。
     * 注意事项：全量查询，数据量大时建议分页。
     */
    public List<CategoryDTO> listAll() {
        log.info("调用 listAll()");
        return toDTOList(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort_order_wsh)
                        .orderByAsc(Category::getId_wsh)));
    }

    /**
     * 按父级查询子分类列表（实现）
     *
     * 业务作用：根据父级 ID 查询直接子分类，用于递归构建分类层级树。
     * 业务规则：顶级分类 parentId = 0。
     * 注意事项：仅返回直接子分类，不递归查询。
     */
    public List<CategoryDTO> listByParent(Long parentId) {
        log.info("调用 listByParent()");
        return toDTOList(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParent_id_wsh, parentId)
                        .orderByAsc(Category::getSort_order_wsh)));
    }

    /**
     * 分类详情查询（实现）
     *
     * 业务作用：根据 ID 查询单条分类，不存在则抛异常。
     * 业务规则：不存在时抛 BusinessException。
     * 异常情况：不存在时抛出 BusinessException("分类不存在")。
     */
    public CategoryDTO getById(Long id) {
        log.info("调用 getById()");
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return toDTO(c);
    }

    /**
     * 创建分类（实现）
     *
     * 业务作用：创建新分类，父 ID 默认 0（顶级），排序号默认 0。
     * 业务规则：父 ID 为空时设为 0；排序号为空时设为 0。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    public CategoryDTO create(CategoryCreateRequestDTO request) {
        log.info("调用 create()");
        Category category = new Category();
        category.setName_wsh(request.getName_wsh());
        // 父级与排序号为空时按顶级、默认序处理
        category.setParent_id_wsh(request.getParent_id_wsh() != null ? request.getParent_id_wsh() : 0L);
        category.setSort_order_wsh(request.getSort_order_wsh() != null ? request.getSort_order_wsh() : 0);
        categoryMapper.insert(category);
        return toDTO(category);
    }

    /**
     * 更新分类（实现）
     *
     * 业务作用：更新分类信息，仅更新请求中非 null 字段。
     * 业务规则：分类必须存在。
     * 异常情况：分类不存在时抛出 BusinessException("分类不存在")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    public CategoryDTO update(Long id, CategoryUpdateRequestDTO request) {
        log.info("调用 update()");
        Category existing = getByIdRaw(id);
        // 仅覆盖请求中非空字段，避免清空其他属性
        if (request.getName_wsh() != null) existing.setName_wsh(request.getName_wsh());
        if (request.getParent_id_wsh() != null) existing.setParent_id_wsh(request.getParent_id_wsh());
        if (request.getSort_order_wsh() != null) existing.setSort_order_wsh(request.getSort_order_wsh());
        categoryMapper.updateById(existing);
        return toDTO(existing);
    }

    /**
     * 删除分类（实现）
     *
     * 业务作用：删除分类，存在子分类时拒绝删除。
     * 业务规则：存在子分类时不允许删除。
     * 异常情况：存在子分类时抛出 BusinessException("存在子分类，无法删除")。
     * 注意事项：@Transactional 保证事务一致性。
     */
    @Transactional
    public void delete(Long id) {
        log.info("调用 delete()");
        long subCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParent_id_wsh, id));
        // 存在子分类时拒绝删除，避免层级悬空
        if (subCount > 0) throw new BusinessException("存在子分类，无法删除");
        categoryMapper.deleteById(id);
    }

    /**
     * 业务作用：主键查询分类实体，不存在时抛业务异常。
     * 业务规则：分类必须存在。
     * 异常情况：查不到时抛 BusinessException("分类不存在")。
     * 注意事项：与 getById 的区别是返回实体而非 DTO，供内部更新使用。
     */
    private Category getByIdRaw(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return c;
    }

    /**
     * 业务作用：把 Category 实体字段拷贝到 CategoryDTO。
     * 业务规则：null 入参不抛异常，直接返回 null。
     * 注意事项：返回值可能为 null，调用方需自行判空。
     */
    private CategoryDTO toDTO(Category c) {
        if (c == null) return null;
        CategoryDTO dto = new CategoryDTO();
        dto.setId_wsh(c.getId_wsh());
        dto.setName_wsh(c.getName_wsh());
        dto.setParent_id_wsh(c.getParent_id_wsh());
        dto.setSort_order_wsh(c.getSort_order_wsh());
        return dto;
    }

    /**
     * 业务作用：批量把分类实体转换为 DTO。
     * 数据处理：入参为 null 时返回空列表，否则逐条映射。
     * 业务规则：空输入返回 List.of() 而不是 null。
     * 注意事项：列表中的 null 元素会被 toDTO 映射为 null 并保留在结果中。
     */
    private List<CategoryDTO> toDTOList(List<Category> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
