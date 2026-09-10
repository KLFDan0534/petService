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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务分类管理服务实现。
 * <p>
 * 负责服务分类的树形结构维护，包括分类的 CRUD、树构建、删除前引用校验。
 */
@Service
public class ServiceCategoryServiceImpl implements ServiceCategoryService {

    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper serviceItemMapper;

    public ServiceCategoryServiceImpl(ServiceCategoryMapper categoryMapper, ServiceItemMapper serviceItemMapper) {
        this.categoryMapper = categoryMapper;
        this.serviceItemMapper = serviceItemMapper;
    }

    /**
     * 【获取服务分类树】
     *
     * 业务作用：获取所有已启用服务分类的树形结构。
     * 调用场景：服务项目管理页面加载分类树时调用。
     * 调用链：ServiceCategoryController → getTree → ServiceCategoryMapper.selectList → 递归构建树
     * 数据处理：查询所有已启用分类，以 parent_id=0 或 null 的为根节点，递归构建树形结构。
     * 业务规则：仅返回已启用分类；按 sort 和 id 升序排列。
     * 状态影响：只读操作。
     */
    @Override
    @Cacheable(value = "service-category", key = "'tree:ALL'")
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

    /**
     * 递归构建分类树节点。
     *
     * @param node 当前分类节点
     * @param all  全部分类列表（用于快速查找子节点）
     * @return 树节点VO
     */
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

    /**
     * 【查询子分类列表】
     *
     * 业务作用：根据父分类ID查询直接子分类列表。
     * 调用场景：前端分类级联选择时调用。
     * 调用链：ServiceCategoryController → listByParent → ServiceCategoryMapper.selectList
     * 数据处理：按 parent_id 查询已启用分类，按 sort 升序排列。
     * 业务规则：仅返回已启用的子分类。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceCategory> listByParent(Long parentId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getParent_id_wsh, parentId)
                        .eq(ServiceCategory::getStatus_wsh, 1)
                        .orderByAsc(ServiceCategory::getSort_order_wsh));
    }

    /**
     * 【根据ID查询服务分类】
     *
     * 业务作用：根据主键ID查询服务分类信息。
     * 调用场景：被 update、delete 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → ServiceCategoryMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     * 异常情况：分类不存在抛 BusinessException("分类不存在")。
     */
    @Override
    public ServiceCategory getById(Long id) {
        ServiceCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    /**
     * 【创建服务分类】
     *
     * 业务作用：新增一个服务分类。
     * 调用场景：管理员在后台新增服务分类时调用。
     * 调用链：ServiceCategoryController → create @Transactional → ServiceCategoryMapper.insert
     * 数据处理：parent_id 默认为 0（根节点），sort_order 默认为 0，status 默认为 1（启用）。
     * 业务规则：parent_id 为空则设为根节点。
     * 状态影响：新增分类记录。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-category", "service-item-page"}, allEntries = true)
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

    /**
     * 【更新服务分类】
     *
     * 业务作用：修改服务分类信息。
     * 调用场景：管理员在后台编辑服务分类时调用。
     * 调用链：ServiceCategoryController → update @Transactional → ServiceCategoryMapper.updateById
     * 数据处理：仅更新 DTO 中非 null 字段。
     * 业务规则：仅更新非 null 字段。
     * 状态影响：更新分类字段。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-category", "service-item-page"}, allEntries = true)
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

    /**
     * 【删除服务分类】
     *
     * 业务作用：删除服务分类，删除前检查子分类和服务项目引用。
     * 调用场景：管理员在后台删除服务分类时调用。
     * 调用链：ServiceCategoryController → delete @Transactional → 校验子分类 → 校验引用 → ServiceCategoryMapper.deleteById
     * 数据处理：先检查子分类，再检查服务项目引用，通过后方可删除。
     * 业务规则：有子分类或已被服务项目引用时禁止删除。
     * 状态影响：物理删除分类记录。
     * 事务边界：校验 + 删除在同一事务中。
     * 异常情况：有子分类或引用时抛 BusinessException。
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"service-category", "service-item-page"}, allEntries = true)
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

    /**
     * 【分类实体转DTO】
     *
     * 业务作用：将服务分类实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回分类信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
     */
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

    /**
     * 【查询全部分类】
     *
     * 业务作用：查询所有服务分类（含已禁用的）。
     * 调用场景：管理后台分类管理列表展示时调用。
     * 调用链：ServiceCategoryController → listAll → ServiceCategoryMapper.selectList（按 sort 和 id 升序）
     * 数据处理：查询全部记录，按 sort_order 和 id 升序排列。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceCategory> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .orderByAsc(ServiceCategory::getSort_order_wsh)
                        .orderByAsc(ServiceCategory::getId_wsh));
    }

    /**
     * 【查询已启用分类】
     *
     * 业务作用：查询所有已启用的服务分类。
     * 调用场景：创建服务项目时选择分类时调用。
     * 调用链：ServiceCategoryController → listAllEnabled → ServiceCategoryMapper.selectList（按 status=1 过滤）
     * 数据处理：按 status=1 过滤，按 sort_order 和 id 升序排列。
     * 状态影响：只读操作。
     */
    @Override
    @Cacheable(value = "service-category", key = "'list:ALL'")
    public List<ServiceCategory> listAllEnabled() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getStatus_wsh, 1)
                        .orderByAsc(ServiceCategory::getSort_order_wsh)
                        .orderByAsc(ServiceCategory::getId_wsh));
    }
}
