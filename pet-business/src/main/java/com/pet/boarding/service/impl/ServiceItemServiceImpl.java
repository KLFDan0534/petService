package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.ServiceItemCreateRequestDTO;
import com.pet.boarding.dto.ServiceItemDTO;
import com.pet.boarding.dto.ServiceItemUpdateRequestDTO;
import com.pet.boarding.entity.ServiceCategory;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.ServiceCategoryMapper;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.service.ServiceItemService;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 服务项目管理服务实现。
 * <p>
 * 负责商家服务项目的 CRUD、启用/禁用切换、图片更新等。
 * 创建和更新时根据分类ID自动关联分类编码（code）写入 type 字段，用于前端归类展示。
 */
@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemMapper serviceItemMapper;
    private final ServiceCategoryMapper categoryMapper;

    public ServiceItemServiceImpl(ServiceItemMapper serviceItemMapper, ServiceCategoryMapper categoryMapper) {
        this.serviceItemMapper = serviceItemMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 【查询所有已启用服务项目】
     *
     * 业务作用：获取全系统已启用的服务项目列表。
     * 调用场景：用户端浏览全部可预约服务时调用。
     * 调用链：ServiceItemController → listAll → ServiceItemMapper.selectList（按 status=ENABLED）
     * 数据处理：仅返回已启用的服务项目。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listAll() {
        log.info("listAll() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    /**
     * 【查询商家下已启用服务项目】
     *
     * 业务作用：查询某个商家下所有已启用的服务项目（对外展示）。
     * 调用场景：用户端查看商家详情页的服务列表时调用。
     * 调用链：ServiceItemController → listByMerchant → ServiceItemMapper.selectList
     * 数据处理：按 merchant_id + status=ENABLED 查询。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByMerchant(Long merchantId) {
        log.info("listByMerchant() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    /**
     * 【查询商家下所有服务项目（含禁用）】
     *
     * 业务作用：查询某个商家下所有服务项目（商家后台管理用）。
     * 调用场景：商家在后台管理服务项目列表时调用。
     * 调用链：ServiceItemController → listByMerchantForManage → ServiceItemMapper.selectList
     * 数据处理：按 merchant_id 查询全部，按创建时间倒序。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByMerchantForManage(Long merchantId) {
        log.info("listByMerchantForManage() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getMerchant_id_wsh, merchantId)
                        .orderByDesc(ServiceItem::getCreated_at_wsh));
    }

    /**
     * 【根据ID查询服务项目】
     *
     * 业务作用：根据主键ID查询服务项目。
     * 调用场景：被 update、delete 等业务方法内部调用。
     * 调用链：上层业务方法 → getById → ServiceItemMapper.selectById
     * 数据处理：按主键ID查询单条记录。
     * 业务规则：查询结果为 null 时抛 BusinessException。
     * 状态影响：只读操作。
     * 异常情况：服务项目不存在时抛 BusinessException。
     */
    @Override
    public ServiceItem getById(Long id) {
        log.info("getById() called");
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("服务项目不存在");
        }
        return item;
    }

    /**
     * 【批量查询服务项目】
     *
     * 业务作用：根据多个ID批量查询服务项目。
     * 调用链：上层方法 → listByIds → ServiceItemMapper.selectBatchIds
     * 数据处理：按ID集合批量查询，参为空返回空列表。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByIds(Collection<Long> ids) {
        log.info("listByIds() called");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return serviceItemMapper.selectBatchIds(ids);
    }

    /**
     * 【创建服务项目】
     *
     * 业务作用：商家新增一个服务项目，自动关联分类编码。
     * 调用场景：商家在后台新增服务项目时调用。
     * 调用链：ServiceItemController → create @Transactional → ServiceItemMapper.insert
     * 数据处理：按 DTO 构建实体；指定分类则自动获取其 code 作为 type；默认状态已启用。
     * 业务规则：分类编码自动同步到 type；默认启用。
     * 状态影响：新增服务项目记录。
     */
    @Override
    @Transactional
    public ServiceItem create(ServiceItemCreateRequestDTO dto) {
        log.info("create() called");
        ServiceItem item = new ServiceItem();
        item.setMerchant_id_wsh(dto.getMerchant_id_wsh());
        item.setName_wsh(dto.getName_wsh());
        item.setCategory_id_wsh(dto.getCategory_id_wsh());
        item.setDescription_wsh(dto.getDescription_wsh());
        item.setPrice_wsh(dto.getPrice_wsh());
        item.setUnit_wsh(dto.getUnit_wsh());
        item.setImages_wsh(dto.getImages_wsh());
        if (item.getCategory_id_wsh() != null) {
            ServiceCategory cat = categoryMapper.selectById(item.getCategory_id_wsh());
            if (cat != null) {
                item.setType_wsh(cat.getCode_wsh());
            }
        }
        if (item.getStatus_wsh() == null) {
            item.setStatus_wsh(StatusCode.SERVICE_ENABLED.getValue());
        }
        serviceItemMapper.insert(item);
        return item;
    }

    /**
     * 【更新服务项目】
     *
     * 业务作用：修改服务项目信息，更新分类时同步更新 type 编码。
     * 调用场景：商家在后台编辑服务项目时调用。
     * 调用链：ServiceItemController → update @Transactional → ServiceItemMapper.updateById
     * 数据处理：仅更新非 null 字段；更新 category_id 时同步从分类表读取 code 更新 type。
     * 业务规则：更新分类时同步更新 type。
     * 状态影响：更新服务项目字段。
     */
    @Override
    @Transactional
    public ServiceItem update(Long id, ServiceItemUpdateRequestDTO dto) {
        log.info("update() called");
        ServiceItem existing = getById(id);
        if (dto.getName_wsh() != null) existing.setName_wsh(dto.getName_wsh());
        if (dto.getCategory_id_wsh() != null) {
            existing.setCategory_id_wsh(dto.getCategory_id_wsh());
            ServiceCategory cat = categoryMapper.selectById(dto.getCategory_id_wsh());
            if (cat != null) {
                existing.setType_wsh(cat.getCode_wsh());
            }
        }
        if (dto.getDescription_wsh() != null) existing.setDescription_wsh(dto.getDescription_wsh());
        if (dto.getPrice_wsh() != null) existing.setPrice_wsh(dto.getPrice_wsh());
        if (dto.getUnit_wsh() != null) existing.setUnit_wsh(dto.getUnit_wsh());
        if (dto.getImages_wsh() != null) existing.setImages_wsh(dto.getImages_wsh());
        if (dto.getStatus_wsh() != null) existing.setStatus_wsh(dto.getStatus_wsh());
        serviceItemMapper.updateById(existing);
        return existing;
    }

    /**
     * 【删除服务项目】
     *
     * 业务作用：物理删除服务项目。
     * 调用场景：商家在后台删除服务项目时调用。
     * 调用链：ServiceItemController → delete @Transactional → ServiceItemMapper.deleteById
     * 数据处理：先查询存在性，再物理删除。
     * 状态影响：物理删除服务项目记录。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("delete() called");
        getById(id);
        serviceItemMapper.deleteById(id);
    }

    /**
     * 【切换服务项目启用/禁用】
     *
     * 业务作用：切换服务项目的上架/下架状态。
     * 调用场景：商家在后台启用或禁用服务项目时调用。
     * 调用链：ServiceItemController → toggleStatus @Transactional → ServiceItemMapper.updateById
     * 数据处理：ENABLED ↔ DISABLED 相互切换。
     * 状态影响：更新服务项目的 status 字段。
     */
    @Override
    @Transactional
    public void toggleStatus(Long id) {
        log.info("toggleStatus() called");
        ServiceItem item = getById(id);
        item.setStatus_wsh(item.getStatus_wsh() == null || item.getStatus_wsh() == StatusCode.SERVICE_DISABLED.getValue()
                ? StatusCode.SERVICE_ENABLED.getValue()
                : StatusCode.SERVICE_DISABLED.getValue());
        serviceItemMapper.updateById(item);
    }

    /**
     * 【更新服务项目图片】
     *
     * 业务作用：更新服务项目的展示图片列表。
     * 调用场景：商家在后台编辑服务项目图片时调用。
     * 调用链：ServiceItemController → updateImages @Transactional → ServiceItemMapper.updateById
     * 数据处理：直接替换 images 字段。
     * 状态影响：更新 images 字段。
     */
    @Override
    @Transactional
    public ServiceItem updateImages(Long id, String images) {
        log.info("updateImages() called");
        ServiceItem item = getById(id);
        item.setImages_wsh(images);
        serviceItemMapper.updateById(item);
        return item;
    }

    /**
     * 【根据分类查询服务项目】
     *
     * 业务作用：查询指定分类下所有已启用的服务项目。
     * 调用场景：用户端按分类筛选服务时调用。
     * 调用链：ServiceItemController → listByCategory → ServiceItemMapper.selectList
     * 数据处理：按 category_id + status=ENABLED 查询。
     * 状态影响：只读操作。
     */
    @Override
    public List<ServiceItem> listByCategory(Long categoryId) {
        log.info("listByCategory() called");
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getCategory_id_wsh, categoryId)
                        .eq(ServiceItem::getStatus_wsh, StatusCode.SERVICE_ENABLED.getValue()));
    }

    /**
     * 【服务项目实体转DTO】
     *
     * 业务作用：转换为前端展示 DTO（含分类名称）。
     * 调用场景：Controller 层返回服务项目信息前调用。
     * 调用链：各查询 Controller → toDTO → ServiceCategoryMapper.selectById
     * 数据处理：字段拷贝 + 查询分类名称。
     * 状态影响：只读操作。
     */
    @Override
    public ServiceItemDTO toDTO(ServiceItem entity) {
        if (entity == null) return null;
        ServiceItemDTO dto = new ServiceItemDTO();
        dto.setId_wsh(entity.getId_wsh());
        dto.setMerchant_id_wsh(entity.getMerchant_id_wsh());
        dto.setName_wsh(entity.getName_wsh());
        dto.setType_wsh(entity.getType_wsh());
        dto.setCategory_id_wsh(entity.getCategory_id_wsh());
        if (entity.getCategory_id_wsh() != null) {
            ServiceCategory cat = categoryMapper.selectById(entity.getCategory_id_wsh());
            dto.setCategory_name_wsh(cat != null ? cat.getName_wsh() : null);
        }
        dto.setDescription_wsh(entity.getDescription_wsh());
        dto.setPrice_wsh(entity.getPrice_wsh());
        dto.setUnit_wsh(entity.getUnit_wsh());
        dto.setImages_wsh(entity.getImages_wsh());
        dto.setStatus_wsh(entity.getStatus_wsh());
        return dto;
    }
}
