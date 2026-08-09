package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceCategoryCreateRequestDTO;
import com.pet.boarding.dto.ServiceCategoryDTO;
import com.pet.boarding.dto.ServiceCategoryUpdateRequestDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.vo.ServiceCategoryTreeVO;

import java.util.List;

/**
 * 服务分类管理接口。
 * <p>
 * 服务分类是服务项目的分类体系，采用树形结构（支持多级父子关系）。
 * 用于对商家提供的宠物服务（如洗澡、美容、寄养等）进行归类管理。
 * 分类有启用/禁用状态，启用的分类才能关联服务项目。
 */
public interface ServiceCategoryService {

    /**
     * 【获取服务分类树】
     *
     * 业务作用：获取所有已启用服务分类的树形结构，用于前端分类选择器展示。
     * 调用场景：服务项目管理页面加载分类树时调用。
     * 调用链：ServiceCategoryController → getTree → ServiceCategoryMapper.selectList → 递归构建树
     * 数据处理：查询所有已启用分类，以 parent_id 为空或 0 的为根节点，递归构建树形结构。
     * 业务规则：仅返回已启用分类（status=1）；按 sort 和 id 升序排列。
     * 状态影响：只读操作。
     *
     * @return 分类树VO列表，每个节点包含子节点 children 列表
     */
    List<ServiceCategoryTreeVO> getTree();

    /**
     * 【查询子分类列表】
     *
     * 业务作用：根据父分类ID查询直接子分类列表。
     * 调用场景：前端分类级联选择时调用。
     * 调用链：ServiceCategoryController → listByParent → ServiceCategoryMapper.selectList
     * 数据处理：按 parent_id 查询，过滤已启用分类，按 sort 升序排列。
     * 业务规则：仅返回已启用分类。
     * 状态影响：只读操作。
     *
     * @param parentId 父分类ID
     * @return 已启用的子分类列表，按 sort 升序排列
     */
    List<ServiceCategory> listByParent(Long parentId);

    /**
     * 【根据ID查询服务分类】
     *
     * 业务作用：根据主键ID查询服务分类信息。
     * 调用场景：被 update、delete 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → ServiceCategoryMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     *
     * @param id 分类ID
     * @return 分类实体
     * @throws BusinessException 如果分类不存在
     */
    ServiceCategory getById(Long id);

    /**
     * 【创建服务分类】
     *
     * 业务作用：新增一个服务分类，支持树形层级。
     * 调用场景：管理员在后台新增服务分类时调用。
     * 调用链：ServiceCategoryController → create @Transactional → ServiceCategoryMapper.insert
     * 数据处理：parent_id 默认为 0（根节点），sort_order 默认为 0，status 默认为 1（启用）。
     * 业务规则：parent_id 为空则设为根节点。
     * 状态影响：新增分类记录。
     *
     * @param request 创建请求DTO
     * @return 创建后的分类实体
     */
    ServiceCategory create(ServiceCategoryCreateRequestDTO request);

    /**
     * 【更新服务分类】
     *
     * 业务作用：修改服务分类信息（名称、编码、排序、状态等）。
     * 调用场景：管理员在后台编辑服务分类时调用。
     * 调用链：ServiceCategoryController → update @Transactional → ServiceCategoryMapper.updateById
     * 数据处理：仅更新 DTO 中非 null 字段。
     * 业务规则：仅更新非 null 字段。
     * 状态影响：更新分类字段。
     *
     * @param id      分类ID
     * @param request 更新请求DTO
     * @return 更新后的分类实体
     * @throws BusinessException 如果分类不存在
     */
    ServiceCategory update(Long id, ServiceCategoryUpdateRequestDTO request);

    /**
     * 【删除服务分类】
     *
     * 业务作用：删除服务分类，删除前检查子分类和服务项目引用。
     * 调用场景：管理员在后台删除服务分类时调用。
     * 调用链：ServiceCategoryController → delete @Transactional → 校验子分类 → 校验引用 → ServiceCategoryMapper.deleteById
     * 数据处理：先检查是否有子分类，再检查是否被服务项目引用，通过后物理删除。
     * 业务规则：有子分类或已被服务项目引用时禁止删除。
     * 状态影响：物理删除分类记录。
     * 异常情况：有子分类时抛 BusinessException；被引用时抛 BusinessException。
     *
     * @param id 分类ID
     * @throws BusinessException 如果存在子分类或服务项目引用
     */
    void delete(Long id);

    /**
     * 【查询全部分类】
     *
     * 业务作用：查询所有服务分类（含已禁用的），按 sort 和 id 升序排列。
     * 调用场景：管理后台分类管理列表展示时调用。
     * 调用链：ServiceCategoryController → listAll → ServiceCategoryMapper.selectList
     * 数据处理：查询全部记录，按 sort_order 和 id 升序排列。
     * 状态影响：只读操作。
     *
     * @return 全部分类列表
     */
    List<ServiceCategory> listAll();

    /**
     * 【查询已启用分类】
     *
     * 业务作用：查询所有已启用的服务分类。
     * 调用场景：创建服务项目时选择分类时调用。
     * 调用链：ServiceCategoryController → listAllEnabled → ServiceCategoryMapper.selectList
     * 数据处理：按 status=1 过滤，按 sort_order 和 id 升序排列。
     * 状态影响：只读操作。
     *
     * @return 已启用的分类列表
     */
    List<ServiceCategory> listAllEnabled();

    /**
     * 【分类实体转DTO】
     *
     * 业务作用：将服务分类实体转换为前端展示所需的 DTO。
     * 调用场景：Controller 层返回分类信息前调用。
     * 调用链：各查询 Controller → toDTO
     * 数据处理：字段拷贝。
     * 状态影响：只读操作。
     *
     * @param entity 分类实体
     * @return 分类DTO，入参为 null 时返回 null
     */
    ServiceCategoryDTO toDTO(ServiceCategory entity);
}
