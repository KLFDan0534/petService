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

    /**
     * 【业务名称】分类全量列表查询（实现）
     * 业务作用：查询全部分类，按 sort_order 和 ID 升序排列，返回未被逻辑删除的分类。
     * 调用场景：分类管理树状展示、分类下拉选择。
     * 调用链：listAll() → CategoryMapper.selectList(LambdaQueryWrapper) → toDTOList()。
     * 数据处理：无条件查询全量，按 sort_order 升序、ID 升序排列。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤。
     * 状态影响：无。
     * 异常情况：无。
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
     * 【业务名称】按父级查询子分类列表（实现）
     * 业务作用：根据父级 ID 查询直接子分类，用于递归构建分类层级树。
     * 调用场景：分类多级选择器、树形展开。
     * 调用链：listByParent() → CategoryMapper.selectList() → toDTOList()。
     * 数据处理：按 parent_id 精确匹配，按 sort_order 升序排列。
     * 业务规则：顶级分类 parentId = 0。
     * 状态影响：无。
     * 异常情况：无。
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
     * 【业务名称】分类详情查询（实现）
     * 业务作用：根据 ID 查询单条分类，不存在则抛异常。
     * 调用场景：分类编辑页、详情展示。
     * 调用链：getById() → CategoryMapper.selectById() → toDTO()。
     * 数据处理：主键查询单条。
     * 业务规则：不存在时抛 BusinessException。
     * 状态影响：无。
     * 异常情况：不存在时抛出 BusinessException("分类不存在")。
     * 注意事项：无。
     */
    public CategoryDTO getById(Long id) {
        log.info("调用 getById()");
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        return toDTO(c);
    }

    /**
     * 【业务名称】创建分类（实现）
     * 业务作用：创建新分类，父 ID 默认 0（顶级），排序号默认 0。
     * 调用场景：后台管理新建分类。
     * 调用链：create() → CategoryMapper.insert() → toDTO()。
     * 数据处理：从请求复制名称、父 ID、排序号到新实体。
     * 业务规则：父 ID 为空时设为 0；排序号为空时设为 0。
     * 状态影响：新增一条分类记录。
     * 异常情况：无。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】更新分类（实现）
     * 业务作用：更新分类信息，仅更新请求中非 null 字段。
     * 调用场景：后台管理编辑分类。
     * 调用链：update() → getByIdRaw() → CategoryMapper.updateById() → toDTO()。
     * 数据处理：仅更新名称、父 ID、排序号中非 null 的字段。
     * 业务规则：分类必须存在。
     * 状态影响：更新分类表对应记录。
     * 异常情况：分类不存在时抛出 BusinessException("分类不存在")。
     * 注意事项：@Transactional 保证事务一致性。
     */
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

    /**
     * 【业务名称】删除分类（实现）
     * 业务作用：删除分类，存在子分类时拒绝删除。
     * 调用场景：后台管理删除废弃分类。
     * 调用链：delete() → 查询子分类数量 → CategoryMapper.deleteById()。
     * 数据处理：先查询子分类数量，无子分类时执行删除。
     * 业务规则：存在子分类时不允许删除。
     * 状态影响：删除分类记录。
     * 异常情况：存在子分类时抛出 BusinessException("存在子分类，无法删除")。
     * 注意事项：@Transactional 保证事务一致性。
     */
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
