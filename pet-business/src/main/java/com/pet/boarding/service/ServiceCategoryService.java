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
     * 业务作用：获取所有已启用服务分类的树形结构，用于前端分类选择器展示。
     * 业务规则：仅返回已启用分类（status=1）；按 sort 和 id 升序排列。
     *
     * @return 分类树VO列表，每个节点包含子节点 children 列表
     */
    List<ServiceCategoryTreeVO> getTree();

    /**
     * 业务作用：根据父分类ID查询直接子分类列表。
     * 业务规则：仅返回已启用分类。
     *
     * @param parentId 父分类ID
     * @return 已启用的子分类列表，按 sort 升序排列
     */
    List<ServiceCategory> listByParent(Long parentId);

    /**
     * 业务作用：根据主键ID查询服务分类信息。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     *
     * @param id 分类ID
     * @return 分类实体
     * @throws BusinessException 如果分类不存在
     */
    ServiceCategory getById(Long id);

    /**
     * 业务作用：新增一个服务分类，支持树形层级。
     * 业务规则：parent_id 为空则设为根节点。
     *
     * @param request 创建请求DTO
     * @return 创建后的分类实体
     */
    ServiceCategory create(ServiceCategoryCreateRequestDTO request);

    /**
     * 业务作用：修改服务分类信息（名称、编码、排序、状态等）。
     * 业务规则：仅更新非 null 字段。
     *
     * @param id      分类ID
     * @param request 更新请求DTO
     * @return 更新后的分类实体
     * @throws BusinessException 如果分类不存在
     */
    ServiceCategory update(Long id, ServiceCategoryUpdateRequestDTO request);

    /**
     * 业务作用：删除服务分类，删除前检查子分类和服务项目引用。
     * 业务规则：有子分类或已被服务项目引用时禁止删除。
     * 异常情况：有子分类时抛 BusinessException；被引用时抛 BusinessException。
     *
     * @param id 分类ID
     * @throws BusinessException 如果存在子分类或服务项目引用
     */
    void delete(Long id);

    /**
     * 业务作用：查询所有服务分类（含已禁用的），按 sort 和 id 升序排列。
     *
     * @return 全部分类列表
     */
    List<ServiceCategory> listAll();

    /**
     * 业务作用：查询所有已启用的服务分类。
     *
     * @return 已启用的分类列表
     */
    List<ServiceCategory> listAllEnabled();

    /**
     * 业务作用：将服务分类实体转换为前端展示所需的 DTO。
     *
     * @param entity 分类实体
     * @return 分类DTO，入参为 null 时返回 null
     */
    ServiceCategoryDTO toDTO(ServiceCategory entity);
}
