package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemQueryDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.dto.ServiceManageDetailVO;
import com.pet.boarding.dto.ServiceProductDetailVO;
import com.pet.boarding.dto.ServiceQueryResultVO;
import com.pet.boarding.entity.ServiceItem;

import java.util.Collection;
import java.util.List;

/**
 * 服务项目管理接口。
 * <p>
 * 服务项目是商家提供的具体宠物服务（如：狗狗洗澡 80元/次、猫咪寄养 100元/天等）。
 * 每个服务项目归属于一个商家和一个服务分类，支持启用/禁用切换。
 * 客户可通过此接口浏览商家上架的服务项目并下单。
 */
public interface ServiceItemService {

    /**
     * 【查询所有已启用服务项目】
     *
     * 业务作用：获取系统中所有已启用的服务项目列表。
     * 调用场景：用户端浏览全部可预约服务时调用。
     * 调用链：ServiceItemController → listAll → ServiceItemMapper.selectList（按 status=ENABLED 过滤）
     * 数据处理：查询 service_item 表，仅返回已启用记录。
     * 状态影响：只读操作。
     *
     * @return 已启用的服务项目列表
     */
    List<ServiceItem> listAll();

    /**
     * 【查询商家下已启用服务项目】
     *
     * 业务作用：查询某个商家下所有已启用的服务项目（对外展示）。
     * 调用场景：用户端查看商家详情页的服务列表时调用。
     * 调用链：ServiceItemController → listByMerchant → ServiceItemMapper.selectList（按 merchant_id + ENABLED）
     * 数据处理：按 merchant_id 和 status=ENABLED 查询。
     * 状态影响：只读操作。
     *
     * @param merchantId 商家ID
     * @return 已启用的服务项目列表
     */
    List<ServiceItem> listByMerchant(Long merchantId);

    /**
     * 【查询商家下所有服务项目（含禁用）】
     *
     * 业务作用：查询某个商家下所有服务项目（含已禁用的），商家管理后台用。
     * 调用场景：商家在后台管理服务项目列表时调用。
     * 调用链：ServiceItemController → listByMerchantForManage → ServiceItemMapper.selectList（按 merchant_id）
     * 数据处理：按 merchant_id 查询全部记录，按创建时间倒序排列。
     * 状态影响：只读操作。
     *
     * @param merchantId 商家ID
     * @return 所有服务项目列表（按创建时间倒序）
     */
    List<ServiceItem> listByMerchantForManage(Long merchantId);

    /**
     * 【根据ID查询服务项目】
     *
     * 业务作用：根据主键ID查询服务项目信息。
     * 调用场景：被 update、delete、toggleStatus 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → ServiceItemMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     * 状态影响：只读操作。
     *
     * @param id 服务项目ID
     * @return 服务项目实体
     * @throws BusinessException 如果服务项目不存在
     */
    ServiceItem getById(Long id);

    /**
     * 【批量查询服务项目】
     *
     * 业务作用：根据多个服务项目ID批量查询。
     * 调用场景：需要一次展示多个服务项目详情的场景。
     * 调用链：上层业务方法 → listByIds → ServiceItemMapper.selectBatchIds
     * 数据处理：按ID集合批量查询，入参为空时返回空列表。
     * 状态影响：只读操作。
     *
     * @param ids 服务项目ID集合
     * @return 服务项目列表
     */
    List<ServiceItem> listByIds(Collection<Long> ids);

    /**
     * 【创建服务项目】
     *
     * 业务作用：商家新增一个服务项目，自动关联分类编码。
     * 调用场景：商家在后台新增服务项目时调用。
     * 调用链：ServiceItemController → create @Transactional → ServiceItemMapper.insert
     * 数据处理：根据 DTO 构建实体；如果指定分类ID则自动获取分类编码填入 type 字段；默认状态为已启用。
     * 业务规则：分类编码自动同步；默认启用。
     * 状态影响：新增服务项目记录。
     *
     * @param merchantId 服务端派生的归属商家ID（MERCHANT 自动派生，ADMIN 显式指定）
     * @param dto 服务项目创建请求DTO
     * @return 创建后的服务项目实体
     */
    ServiceItem create(Long merchantId, ServiceItemCreateRequestDTO dto);

    /**
     * 【更新服务项目】
     *
     * 业务作用：修改服务项目信息，更新分类时同步更新 type 编码。
     * 调用场景：商家在后台编辑服务项目时调用。
     * 调用链：ServiceItemController → update @Transactional → ServiceItemMapper.updateById
     * 数据处理：仅更新非 null 字段；如果更新 category_id 则同步从分类表读取 code 更新 type 字段。
     * 业务规则：仅更新非 null 字段；更新分类时同步更新 type。
     * 状态影响：更新服务项目字段。
     *
     * @param id  服务项目ID
     * @param dto 服务项目更新请求DTO
     * @return 更新后的服务项目实体
     * @throws BusinessException 如果服务项目不存在
     */
    ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto);

    /**
     * 【删除服务项目】
     *
     * 业务作用：物理删除服务项目。
     * 调用场景：商家在后台删除服务项目时调用。
     * 调用链：ServiceItemController → delete @Transactional → ServiceItemMapper.deleteById
     * 数据处理：先查询存在性，再物理删除。
     * 状态影响：物理删除服务项目记录。
     *
     * @param id 服务项目ID
     * @throws BusinessException 如果服务项目不存在
     */
    void delete(Long id);

    /**
     * 【切换服务项目启用/禁用状态】
     *
     * 业务作用：切换服务项目的上架/下架状态。
     * 调用场景：商家在后台启用或禁用服务项目时调用。
     * 调用链：ServiceItemController → toggleStatus @Transactional → ServiceItemMapper.updateById
     * 数据处理：ENABLED → DISABLED 或 DISABLED → ENABLED 相互切换。
     * 状态影响：更新服务项目的 status 字段。
     *
     * @param id 服务项目ID
     * @throws BusinessException 如果服务项目不存在
     */
    void toggleStatus(Long id);

    /**
     * 【更新服务项目图片】
     *
     * 业务作用：更新服务项目的展示图片列表。
     * 调用场景：商家在后台编辑服务项目图片时调用。
     * 调用链：ServiceItemController → updateImages @Transactional → ServiceItemMapper.updateById
     * 数据处理：直接替换 images 字段值（逗号分隔多个URL）。
     * 状态影响：更新服务项目的 images 字段。
     *
     * @param id     服务项目ID
     * @param images 图片URL（逗号分隔多个URL）
     * @return 更新后的服务项目实体
     * @throws BusinessException 如果服务项目不存在
     */
    ServiceItem updateImages(Long id, String images);

    /**
     * 【根据分类查询服务项目】
     *
     * 业务作用：查询指定分类下所有已启用的服务项目。
     * 调用场景：用户端按分类筛选服务时调用。
     * 调用链：ServiceItemController → listByCategory → ServiceItemMapper.selectList
     * 数据处理：按 category_id 和 status=ENABLED 查询。
     * 状态影响：只读操作。
     *
     * @param categoryId 服务分类ID
     * @return 已启用的服务项目列表
     */
    List<ServiceItem> listByCategory(Long categoryId);

    /**
     * 【服务项目实体转DTO】
     *
     * 业务作用：将服务项目实体转换为前端展示所需的 DTO（含分类名称）。
     * 调用场景：Controller 层返回服务项目信息前调用。
     * 调用链：各查询 Controller → toDTO → ServiceCategoryMapper.selectById（查询分类名称）
     * 数据处理：字段拷贝 + 根据 category_id 查询分类名称填入 category_name。
     * 状态影响：只读操作。
     *
     * @param entity 服务项目实体
     * @return 服务项目DTO（含分类名称），入参为 null 时返回 null
     */
    ServiceItemDTO toDTO(ServiceItem entity);

    /**
     * 【公开服务列表分页查询】
     *
     * 业务作用：用户端服务浏览的统一查询入口，支持分类、关键字、排序、分页与距离。
     * 业务规则：只公开上架服务 + 已审核商家 + 启用分类；评分聚合与商家/分类均为批量查询，禁止 N+1；
     * 排序走白名单；无定位时距离为 null 且不能使用距离排序。
     *
     * @param query 查询参数（可为 null 表示默认行为）
     * @return 分页结果（含总数）
     */
    ServiceQueryResultVO queryPublic(ServiceItemQueryDTO query);

    /**
     * 【公开服务列表（非分页语义）】
     *
     * 业务作用：兼容旧客户端与商品级列表，返回 {@link #queryPublic} 的分页切片结果。
     *
     * @param query 查询参数
     * @return 服务 DTO 列表
     */
    List<ServiceItemDTO> listPublic(ServiceItemQueryDTO query);

    /**
     * 【公共可见性受控的旧详情读取】
     *
     * 业务作用：匿名用户的兼容详情路径，只返回通过公共可见性不变量
     * （上架服务 + 已审核商家 + 启用分类）的服务，且 images_wsh 只保留
     * 服务端可信值，外部/data/协议相对地址一律丢弃。
     *
     * @param id 服务产品ID
     * @return 服务 DTO（图册已被可信化）
     * @throws BusinessException 不存在 404 / 不可见 400
     */
    ServiceItemDTO getByIdPublic(Long id);

    /**
     * 【服务产品公开详情投影】
     *
     * 业务作用：详情页权威数据源。白名单字段 + 有序可信图册 + 评分聚合 +
     * 服务版本 + 可预约性标记（仅 day/天 单位且商家开放未来预约）。
     *
     * @param id 服务产品ID
     * @return 公开详情投影
     * @throws BusinessException 不存在 404 / 不可见 400
     */
    ServiceProductDetailVO getPublicDetail(Long id);

    /**
     * 【服务产品管理详情】
     *
     * 业务作用：商家/管理员查看单个服务的完整管理投影（含原图册条目）。
     * 调用方必须完成归属/权限校验，本方法不校验权限。
     *
     * @param id 服务产品ID
     * @return 管理详情（标量 DTO + 有序图册）
     * @throws BusinessException 不存在 404
     */
    ServiceManageDetailVO getManageDetail(Long id);
}
