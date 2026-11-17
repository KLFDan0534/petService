package com.pet.pet.service;

import com.pet.pet.dto.CategoryCreateRequestDTO;
import com.pet.pet.dto.CategoryDTO;
import com.pet.pet.dto.CategoryUpdateRequestDTO;
import java.util.List;

public interface CategoryService {
    /**
     * 分类全量列表查询
     *
     * 业务作用：返回全部未被逻辑删除的分类，按 sort_order 和 ID 升序排列。
     * 业务规则：MyBatis-Plus 逻辑删除自动过滤已删除记录。
     * 注意事项：全量查询，建议大数据量时改造为树形分页加载。
     *
     * @return 分类 DTO 列表
     */
    List<CategoryDTO> listAll();
    /**
     * 按父级查询子分类列表
     *
     * 业务作用：根据父级 ID 查询直接子分类列表，用于构建分类层级树。
     * 业务规则：顶级分类的 parent_id = 0。
     * 注意事项：仅返回直接子分类，不递归。
     *
     * @param parentId 父分类 ID（顶级分类传 0）
     * @return 子分类 DTO 列表，按 sort_order 升序排列
     */
    List<CategoryDTO> listByParent(Long parentId);
    /**
     * 分类详情查询
     *
     * 业务作用：根据 ID 查询单条分类详情。
     * 业务规则：不存在时抛出 BusinessException。
     * 异常情况：分类不存在时抛出 BusinessException("分类不存在")。
     *
     * @param id 分类 ID
     * @return 分类 DTO，不存在时抛出 BusinessException
     */
    CategoryDTO getById(Long id);
    /**
     * 创建分类
     *
     * 业务作用：创建新分类，父分类 ID 默认为 0（顶级），排序号默认为 0。
     * 业务规则：父分类 ID 为空时自动设为 0；排序号为空时自动设为 0。
     * 注意事项：插入后返回的 DTO 包含自增 ID。
     *
     * @param request 创建请求，包含分类名称、父 ID 和排序号
     * @return 创建完成后的分类 DTO
     */
    CategoryDTO create(CategoryCreateRequestDTO request);
    /**
     * 更新分类
     *
     * 业务作用：更新分类信息，仅更新请求中非 null 的字段。
     * 业务规则：分类必须存在。
     * 异常情况：分类不存在时抛出 BusinessException("分类不存在")。
     * 注意事项：父 ID 更新可能导致分类树结构变化。
     *
     * @param id      分类 ID
     * @param request 更新的字段
     * @return 更新后的分类 DTO
     */
    CategoryDTO update(Long id, CategoryUpdateRequestDTO request);
    /**
     * 删除分类
     *
     * 业务作用：删除分类，存在子分类时拒绝删除以防止产生孤立数据。
     * 业务规则：存在子分类时不允许删除。
     * 异常情况：存在子分类时抛出 BusinessException("存在子分类，无法删除")。
     * 注意事项：先检查子分类数量再删除。
     *
     * @param id 分类 ID
     */
    void delete(Long id);
}

