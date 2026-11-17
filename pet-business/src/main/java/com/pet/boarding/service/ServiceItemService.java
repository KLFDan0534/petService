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
 * 服务项目是商家提供的具体宠物服务（如：狗狗洗澡 80元/次、猫咪寄养 100元/夜等）。
 * 每个服务项目归属于一个商家和一个服务分类，支持启用/禁用切换。
 * 客户可通过此接口浏览商家上架的服务项目并下单。
 */
public interface ServiceItemService {

    /**
     * 业务作用：查询某个商家下所有服务项目（含已禁用的），商家管理后台用。
     *
     * @param merchantId 商家ID
     * @return 所有服务项目列表（按创建时间倒序）
     */
    List<ServiceItem> listByMerchantForManage(Long merchantId);

    /**
     * 业务作用：根据主键ID查询服务项目信息。
     * 业务规则：查询结果为 null 时抛出 BusinessException。
     *
     * @param id 服务项目ID
     * @return 服务项目实体
     * @throws BusinessException 如果服务项目不存在
     */
    ServiceItem getById(Long id);

    /**
     * 业务作用：根据多个服务项目ID批量查询。
     *
     * @param ids 服务项目ID集合
     * @return 服务项目列表
     */
    List<ServiceItem> listByIds(Collection<Long> ids);

    /**
     * 业务作用：按商家ID查询状态为「已上架」的服务项目，供需要从可售服务中挑选的场景使用。
     * 业务规则：只返回已上架服务，已下架/未审核的不参与下单。
     *
     * @param merchantId 商家ID
     * @return 已上架服务项目列表，无匹配时返回空列表
     */
    List<ServiceItem> listEnabledByMerchant(Long merchantId);

    /**
     * 业务作用：商家新增一个服务项目，自动关联分类编码。
     * 业务规则：分类编码自动同步；默认启用。
     *
     * @param merchantId 服务端派生的归属商家ID（MERCHANT 自动派生，ADMIN 显式指定）
     * @param dto 服务项目创建请求DTO
     * @return 创建后的服务项目实体
     */
    ServiceItem create(Long merchantId, ServiceItemCreateRequestDTO dto);

    /**
     * 业务作用：修改服务项目信息，更新分类时同步更新 type 编码。
     * 业务规则：仅更新非 null 字段；更新分类时同步更新 type。
     *
     * @param id  服务项目ID
     * @param dto 服务项目更新请求DTO
     * @return 更新后的服务项目实体
     * @throws BusinessException 如果服务项目不存在
     */
    ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto);

    /**
     * 业务作用：物理删除服务项目。
     *
     * @param id 服务项目ID
     * @throws BusinessException 如果服务项目不存在
     */
    void delete(Long id);

    /**
     * 业务作用：切换服务项目的上架/下架状态。
     *
     * @param id 服务项目ID
     * @throws BusinessException 如果服务项目不存在
     */
    void toggleStatus(Long id);

    /**
     * 业务作用：将服务项目实体转换为前端展示所需的 DTO（含分类名称）。
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
     * 服务版本 + 可预约性标记（仅 night 单位且商家开放未来预约）。
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
