package com.pet.boarding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.boarding.dto.ServiceMediaDTO;
import com.pet.boarding.dto.ServiceMediaItemDTO;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.entity.ServiceMedia;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.mapper.ServiceMediaMapper;
import com.pet.boarding.service.ServiceMediaService;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.mapper.FileRecordMapper;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.operation.service.impl.ProductImageValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ServiceMediaServiceImpl implements ServiceMediaService {

    public static final String PURPOSE_PRODUCT = "product";
    public static final int COVER_YES = 1;
    public static final int COVER_NO = 0;

    private final ServiceMediaMapper mediaMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final FileRecordMapper fileRecordMapper;
    private final FileRecordService fileRecordService;
    private final MinIoService minIoService;
    private final ProductImageValidator imageValidator;
    private final int maxCount;

    /**
     * 【服务媒体服务构造器】
     *
     * 业务作用：注入媒体、服务产品、文件记录相关依赖，并读取图册张数上限配置。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 构造器 → 读取 ${product-media.max-count:10}。
     *
     * 数据处理：字段赋值；maxCount 缺省为 10。
     *
     * 业务规则：图册上限由配置项控制。
     *
     * 状态影响：无。
     *
     * 异常情况：配置值非数字时启动失败。
     *
     * 注意事项：maxCount 为 final，运行期不可调整。
     */
    public ServiceMediaServiceImpl(ServiceMediaMapper mediaMapper,
                                   ServiceItemMapper serviceItemMapper,
                                   FileRecordMapper fileRecordMapper,
                                   FileRecordService fileRecordService,
                                   MinIoService minIoService,
                                   ProductImageValidator imageValidator,
                                   @org.springframework.beans.factory.annotation.Value("${product-media.max-count:10}") int maxCount) {
        this.mediaMapper = mediaMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.fileRecordService = fileRecordService;
        this.minIoService = minIoService;
        this.imageValidator = imageValidator;
        this.maxCount = maxCount;
    }

    /**
     * 【整体替换产品图册】
     *
     * 业务作用：整体替换某个服务产品的图册，先校验入参与文件归属，再删旧插新。
     *
     * 调用场景：商家在后台保存产品图册时调用。
     *
     * 调用链：Controller → replaceMedia → selectByIdForUpdate 行锁 → 空列表则清空返回 → validateInput → 校验文件归属 → deleteByServiceId → 逐条 insert。
     *
     * 数据处理：items 为 null 归一为空列表；空列表直接清空记录；非空则批量查 file_record，校验用途与商家归属后重建记录。
     *
     * 业务规则：服务产品必须存在；文件必须存在、用途为 product、归属商家与产品一致；张数、排序与封面约束见 validateInput。
     *
     * 状态影响：删除并重建 service_media 记录；对象存储中的图片不删除。
     *
     * 异常情况：产品不存在 → 404；文件不存在或用途不符 → 400；商家不匹配 → 403；校验失败整体回滚。
     *
     * 注意事项：@Transactional 配合 selectByIdForUpdate 防止并发保存交叉；旧图册对应的 MinIO 对象不会随记录删除而清理。
     */
    @Override
    @Transactional
    public void replaceMedia(Long serviceId, List<ServiceMediaItemDTO> items) {
        // 加行锁读服务，避免并发保存图册互相覆盖
        ServiceItem service = serviceItemMapper.selectByIdForUpdate(serviceId);
        if (service == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "服务产品不存在");
        }
        List<ServiceMediaItemDTO> input = items == null ? Collections.emptyList() : items;
        // 提交空列表表示清空整个图册
        if (input.isEmpty()) {
            // 清空该产品的旧图册记录
            mediaMapper.deleteByServiceId(serviceId);
            log.info("replaceMedia() 已清空 serviceId={} 的媒体记录", serviceId);
            return;
        }
        // 校验张数、排序连续性与封面唯一
        validateInput(input);

        Set<Long> fileIds = new HashSet<>();
        for (ServiceMediaItemDTO item : input) {
            fileIds.add(item.getFile_id_wsh());
        }
        // 批量取文件记录，供归属校验
        List<FileRecord> records = fileRecordMapper.selectBatchIds(fileIds);
        Map<Long, FileRecord> byId = new LinkedHashMap<>();
        for (FileRecord record : records) {
            byId.put(record.getId_wsh(), record);
        }
        for (Long fileId : fileIds) {
            FileRecord record = byId.get(fileId);
            // 引用的文件记录必须存在
            if (record == null) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_FILE_NOT_FOUND,
                        "文件记录不存在: " + fileId);
            }
            // 文件必须是产品用途上传的图
            if (!PURPOSE_PRODUCT.equals(record.getPurpose_wsh())) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_FILE_PURPOSE_MISMATCH,
                        "文件不是产品用途: " + fileId);
            }
            // 文件归属商家必须与产品一致
            if (service.getMerchant_id_wsh() == null
                    || !service.getMerchant_id_wsh().equals(record.getMerchant_id_wsh())) {
                throw new BusinessException(403, BookingErrorCode.MEDIA_FILE_MERCHANT_MISMATCH,
                        "文件归属商家与服务产品不一致: " + fileId);
            }
        }

        // 先整体删旧，再按提交顺序重建
        mediaMapper.deleteByServiceId(serviceId);
        for (ServiceMediaItemDTO item : input) {
            ServiceMedia media = new ServiceMedia();
            media.setService_id_wsh(serviceId);
            media.setFile_id_wsh(item.getFile_id_wsh());
            media.setSort_order_wsh(item.getSort_order_wsh());
            media.setIs_cover_wsh(item.getIs_cover_wsh()); // 封面标记原样落库
            mediaMapper.insert(media);
        }
        log.info("replaceMedia() 已将 serviceId={} 替换为 {} 条媒体记录", serviceId, input.size());
    }

    /**
     * 【图册入参校验】
     *
     * 业务作用：校验图册的张数、文件唯一性、排序连续性与封面唯一性。
     *
     * 调用场景：replaceMedia 写库前调用。
     *
     * 调用链：replaceMedia → validateInput。
     *
     * 数据处理：统计 file_id 去重与 sort_order 去重，核对排序集合等于 0..N-1，统计封面数量。
     *
     * 业务规则：张数不超过 maxCount；file_id 不可为 null 且不可重复；排序非负、互不重复且连续；非空图册有且仅有一张封面。
     *
     * 状态影响：无（只校验不写库）。
     *
     * 异常情况：任一规则不满足抛 BusinessException(400)，携带对应 BookingErrorCode。
     *
     * 注意事项：排序必须从 0 开始连续，前端需按索引提交。
     */
    private void validateInput(List<ServiceMediaItemDTO> items) {
        // 张数不能超过配置上限
        if (items.size() > maxCount) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_LIMIT_EXCEEDED,
                    "产品图片不能超过 " + maxCount + " 张");
        }
        // 同一文件不可重复引用
        Set<Long> fileIds = new HashSet<>();
        Set<Integer> sorts = new HashSet<>();
        int covers = 0;
        for (ServiceMediaItemDTO item : items) {
            if (item.getFile_id_wsh() == null) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_FILE_NOT_FOUND, "file_id_wsh 不能为空");
            }
            if (!fileIds.add(item.getFile_id_wsh())) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_DUPLICATE_FILE,
                        "重复引用文件: " + item.getFile_id_wsh());
            }
            // 排序号必须非负且互不重复
            Integer sort = item.getSort_order_wsh();
            if (sort == null || sort < 0 || !sorts.add(sort)) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_ORDER,
                        "排序序号必须为非负且互不重复");
            }
            if (item.getIs_cover_wsh() != null && item.getIs_cover_wsh() == COVER_YES) {
                covers++;
            }
        }
        // 排序必须正好是 0..N-1，不能跳号
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            expected.add(i);
        }
        if (!sorts.equals(expected)) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_ORDER,
                    "排序序号必须连续 0..N-1");
        }
        // 非空图册有且仅有一张封面
        if (covers != 1) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_COVER,
                    "非空图册必须有且仅有一张封面");
        }
    }

    /**
     * 【查询单个产品图册】
     *
     * 业务作用：查询单个服务产品的图册，并拼接可访问的图片地址。
     *
     * 调用场景：产品详情页、商家编辑页展示图册时调用。
     *
     * 调用链：Controller → listMedia → selectByServiceIdOrdered → selectBatchIds → minIoService.getFileUrl。
     *
     * 数据处理：先按排序查 media，再批量查 file_record 组装 URL 与原始文件名。
     *
     * 业务规则：无记录返回空列表；文件记录缺失时该条只返回基础字段，不抛异常。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
     * 注意事项：file_record 缺失会得到 url 为 null 的条目，前端需容错。
     */
    @Override
    public List<ServiceMediaDTO> listMedia(Long serviceId) {
        // 按排序查出该产品的媒体行
        List<ServiceMedia> rows = mediaMapper.selectByServiceIdOrdered(serviceId);
        // 没有图册直接返回空列表
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> fileIds = new HashSet<>();
        for (ServiceMedia row : rows) {
            fileIds.add(row.getFile_id_wsh());
        }
        // 批量查文件记录，避免逐条查询
        List<FileRecord> records = fileRecordMapper.selectBatchIds(fileIds);
        Map<Long, FileRecord> byId = new LinkedHashMap<>();
        for (FileRecord record : records) {
            byId.put(record.getId_wsh(), record);
        }
        List<ServiceMediaDTO> result = new ArrayList<>(rows.size());
        for (ServiceMedia row : rows) {
            FileRecord record = byId.get(row.getFile_id_wsh());
            ServiceMediaDTO dto = new ServiceMediaDTO();
            dto.setId_wsh(row.getId_wsh());
            dto.setFile_id_wsh(row.getFile_id_wsh());
            dto.setSort_order_wsh(row.getSort_order_wsh());
            dto.setIs_cover_wsh(row.getIs_cover_wsh());
            // 文件记录缺失时只返回基础字段
            if (record != null) {
                dto.setUrl_wsh(minIoService.getFileUrl(record.getObject_name_wsh())); // 按对象名实时拼访问地址
                dto.setOriginal_name_wsh(record.getOriginal_name_wsh());
            }
            dto.setCreated_at_wsh(row.getCreated_at_wsh());
            result.add(dto);
        }
        return result;
    }

    /**
     * 【批量查询产品图册】
     *
     * 业务作用：批量查询多个服务产品的图册，按产品 ID 分组返回。
     *
     * 调用场景：产品列表页一次性加载多个产品的图集时调用。
     *
     * 调用链：Controller → listMediaByServiceIds → selectByServiceIdsOrdered → 批量查 file_record → 按 service_id_wsh 分组。
     *
     * 数据处理：一次查全部 media，再一次批量查文件，最后分组。
     *
     * 业务规则：入参为空或无记录返回空 Map。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：无。
     *
     * 注意事项：用于避免列表页的 N+1 查询，返回的 Map 保持插入顺序。
     */
    @Override
    public Map<Long, List<ServiceMediaDTO>> listMediaByServiceIds(Collection<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 查询所有相关媒体记录
        // 一次查出所有产品的媒体行
        List<ServiceMedia> rows = mediaMapper.selectByServiceIdsOrdered(serviceIds);
        // 没有任何媒体行，返回空 Map
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> fileIds = new HashSet<>();
        for (ServiceMedia row : rows) {
            fileIds.add(row.getFile_id_wsh());
        }
        Map<Long, FileRecord> byId = new LinkedHashMap<>();
        for (FileRecord record : fileRecordMapper.selectBatchIds(fileIds)) {
            byId.put(record.getId_wsh(), record);
        }
        // 按产品 ID 分组，保持原有顺序
        Map<Long, List<ServiceMediaDTO>> grouped = new LinkedHashMap<>();
        for (ServiceMedia row : rows) {
            FileRecord record = byId.get(row.getFile_id_wsh());
            ServiceMediaDTO dto = new ServiceMediaDTO();
            dto.setId_wsh(row.getId_wsh());
            dto.setFile_id_wsh(row.getFile_id_wsh());
            dto.setSort_order_wsh(row.getSort_order_wsh());
            dto.setIs_cover_wsh(row.getIs_cover_wsh());
            if (record != null) {
                dto.setUrl_wsh(minIoService.getFileUrl(record.getObject_name_wsh())); // 按对象名实时拼访问地址
                dto.setOriginal_name_wsh(record.getOriginal_name_wsh());
            }
            dto.setCreated_at_wsh(row.getCreated_at_wsh());
            grouped.computeIfAbsent(row.getService_id_wsh(), k -> new ArrayList<>()).add(dto);
        }
        return grouped;
    }

    /**
     * 【解析历史图片地址】
     *
     * 业务作用：从历史逗号分隔的图片串中筛出能对上文件记录的受信地址。
     *
     * 调用场景：历史产品数据迁移或详情回显时调用。
     *
     * 调用链：调用方 → resolveTrustedLegacyImages → 截取 objectName → 按 object_name_wsh 查 file_record。
     *
     * 数据处理：只保留以当前存储前缀开头且能查到文件记录的项，其余记入 unresolved。
     *
     * 业务规则：非本系统前缀或查不到记录的值一律不返回。
     *
     * 状态影响：无（只读）。
     *
     * 异常情况：不抛异常；未解析项仅记 warn 日志。
     *
     * 注意事项：逐条查库，历史数据量大时性能较差；未解析项只审计不清理。
     */
    @Override
    public List<String> resolveTrustedLegacyImages(String legacyImages) {
        if (legacyImages == null || legacyImages.isBlank()) {
            return Collections.emptyList();
        }
        // 取当前存储前缀，用于判断是否本站资源
        String prefix = minIoService.getFileUrl("");
        List<String> trusted = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String raw : legacyImages.split(",")) {
            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }
            // 从完整 URL 截出对象名
            String objectName = value.startsWith(prefix) ? value.substring(prefix.length()) : null;
            if (objectName == null || objectName.isEmpty()) {
                unresolved.add(value);
                continue;
            }
            // 能对上文件记录才算受信地址
            FileRecord record = fileRecordMapper.selectOne(
                    new LambdaQueryWrapper<FileRecord>()
                            .eq(FileRecord::getObject_name_wsh, objectName)
                            .last("LIMIT 1"));
            if (record == null) {
                unresolved.add(value);
            } else {
                trusted.add(value);
            }
        }
        // 未解析项只记日志，不返回给调用方
        if (!unresolved.isEmpty()) {
            log.warn("resolveTrustedLegacyImages() 仅审计的未解析值: {}", unresolved);
        }
        return trusted;
    }

    /**
     * 【上传产品图片】
     *
     * 业务作用：校验并上传产品图片，落库文件记录，落库失败时补偿删除已上传对象。
     *
     * 调用场景：商家上传产品图册图片时调用。
     *
     * 调用链：Controller → uploadProductImage → imageValidator.validate → minIoService.uploadFile → fileRecordService.create → 失败则 deleteFile 补偿。
     *
     * 数据处理：由校验器得到扩展名与 contentType，写入 objectName、size、purpose=product、merchant_id、user_id。
     *
     * 业务规则：图片类型与大小由 ProductImageValidator 判定；文件记录必须绑定商家。
     *
     * 状态影响：新增一条 file_record；对象存储新增一个对象（失败时回滚删除）。
     *
     * 异常情况：文件记录保存失败 → 补偿删除对象后抛 BusinessException(500, PRODUCT_IMAGE_RECORD_FAILED)；补偿删除再失败仅记日志。
     *
     * 注意事项：对象存储与数据库不在同一事务，靠手工补偿保证最终一致。
     */
    @Override
    public FileRecord uploadProductImage(Long userId, Long merchantId, MultipartFile file) {
        // 校验图片格式、尺寸与大小
        ProductImageValidator.DetectedImage detected = imageValidator.validate(file);
        String objectName = minIoService.uploadFile(file, "service",
                detected.extension(), detected.contentType());
        FileRecord record = new FileRecord();
        record.setOriginal_name_wsh(file.getOriginalFilename());
        record.setObject_name_wsh(objectName);
        record.setSize_wsh(file.getSize());
        record.setContent_type_wsh(detected.contentType());
        record.setPurpose_wsh(PURPOSE_PRODUCT); // 标记产品用途，供图册校验使用
        record.setMerchant_id_wsh(merchantId);
        record.setUser_id_wsh(userId);
        try {
            fileRecordService.create(record);
        // 记录落库失败则删掉已上传对象，保证最终一致
        } catch (Exception e) {
            log.error("上传商品图片文件记录保存失败, 补偿删除对象: {}", objectName, e);
            try {
                minIoService.deleteFile(objectName);
            } catch (Exception cleanup) {
                log.error("上传商品图片补偿删除对象失败: {}", objectName, cleanup);
            }
            throw new BusinessException(500, BookingErrorCode.PRODUCT_IMAGE_RECORD_FAILED,
                    "文件记录保存失败，已回滚本次上传");
        }
        return record;
    }
}
