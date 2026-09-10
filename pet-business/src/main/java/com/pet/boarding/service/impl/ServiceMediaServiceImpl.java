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

    @Override
    @Transactional
    public void replaceMedia(Long serviceId, List<ServiceMediaItemDTO> items) {
        ServiceItem service = serviceItemMapper.selectByIdForUpdate(serviceId);
        if (service == null) {
            throw new BusinessException(404, BookingErrorCode.SERVICE_NOT_FOUND, "服务产品不存在");
        }
        List<ServiceMediaItemDTO> input = items == null ? Collections.emptyList() : items;
        if (input.isEmpty()) {
            mediaMapper.deleteByServiceId(serviceId);
            log.info("replaceMedia() 已清空 serviceId={} 的媒体记录", serviceId);
            return;
        }
        validateInput(input);

        Set<Long> fileIds = new HashSet<>();
        for (ServiceMediaItemDTO item : input) {
            fileIds.add(item.getFile_id_wsh());
        }
        List<FileRecord> records = fileRecordMapper.selectBatchIds(fileIds);
        Map<Long, FileRecord> byId = new LinkedHashMap<>();
        for (FileRecord record : records) {
            byId.put(record.getId_wsh(), record);
        }
        for (Long fileId : fileIds) {
            FileRecord record = byId.get(fileId);
            if (record == null) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_FILE_NOT_FOUND,
                        "文件记录不存在: " + fileId);
            }
            if (!PURPOSE_PRODUCT.equals(record.getPurpose_wsh())) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_FILE_PURPOSE_MISMATCH,
                        "文件不是产品用途: " + fileId);
            }
            if (service.getMerchant_id_wsh() == null
                    || !service.getMerchant_id_wsh().equals(record.getMerchant_id_wsh())) {
                throw new BusinessException(403, BookingErrorCode.MEDIA_FILE_MERCHANT_MISMATCH,
                        "文件归属商家与服务产品不一致: " + fileId);
            }
        }

        mediaMapper.deleteByServiceId(serviceId);
        for (ServiceMediaItemDTO item : input) {
            ServiceMedia media = new ServiceMedia();
            media.setService_id_wsh(serviceId);
            media.setFile_id_wsh(item.getFile_id_wsh());
            media.setSort_order_wsh(item.getSort_order_wsh());
            media.setIs_cover_wsh(item.getIs_cover_wsh());
            mediaMapper.insert(media);
        }
        log.info("replaceMedia() 已将 serviceId={} 替换为 {} 条媒体记录", serviceId, input.size());
    }

    private void validateInput(List<ServiceMediaItemDTO> items) {
        if (items.size() > maxCount) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_LIMIT_EXCEEDED,
                    "产品图片不能超过 " + maxCount + " 张");
        }
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
            Integer sort = item.getSort_order_wsh();
            if (sort == null || sort < 0 || !sorts.add(sort)) {
                throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_ORDER,
                        "排序序号必须为非负且互不重复");
            }
            if (item.getIs_cover_wsh() != null && item.getIs_cover_wsh() == COVER_YES) {
                covers++;
            }
        }
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            expected.add(i);
        }
        if (!sorts.equals(expected)) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_ORDER,
                    "排序序号必须连续 0..N-1");
        }
        if (covers != 1) {
            throw new BusinessException(400, BookingErrorCode.MEDIA_INVALID_COVER,
                    "非空图册必须有且仅有一张封面");
        }
    }

    @Override
    public List<ServiceMediaDTO> listMedia(Long serviceId) {
        List<ServiceMedia> rows = mediaMapper.selectByServiceIdOrdered(serviceId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> fileIds = new HashSet<>();
        for (ServiceMedia row : rows) {
            fileIds.add(row.getFile_id_wsh());
        }
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
            if (record != null) {
                dto.setUrl_wsh(minIoService.getFileUrl(record.getObject_name_wsh()));
                dto.setOriginal_name_wsh(record.getOriginal_name_wsh());
            }
            dto.setCreated_at_wsh(row.getCreated_at_wsh());
            result.add(dto);
        }
        return result;
    }

    @Override
    public Map<Long, List<ServiceMediaDTO>> listMediaByServiceIds(Collection<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ServiceMedia> rows = mediaMapper.selectByServiceIdsOrdered(serviceIds);
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
        Map<Long, List<ServiceMediaDTO>> grouped = new LinkedHashMap<>();
        for (ServiceMedia row : rows) {
            FileRecord record = byId.get(row.getFile_id_wsh());
            ServiceMediaDTO dto = new ServiceMediaDTO();
            dto.setId_wsh(row.getId_wsh());
            dto.setFile_id_wsh(row.getFile_id_wsh());
            dto.setSort_order_wsh(row.getSort_order_wsh());
            dto.setIs_cover_wsh(row.getIs_cover_wsh());
            if (record != null) {
                dto.setUrl_wsh(minIoService.getFileUrl(record.getObject_name_wsh()));
                dto.setOriginal_name_wsh(record.getOriginal_name_wsh());
            }
            dto.setCreated_at_wsh(row.getCreated_at_wsh());
            grouped.computeIfAbsent(row.getService_id_wsh(), k -> new ArrayList<>()).add(dto);
        }
        return grouped;
    }

    @Override
    public List<String> resolveTrustedLegacyImages(String legacyImages) {
        if (legacyImages == null || legacyImages.isBlank()) {
            return Collections.emptyList();
        }
        String prefix = minIoService.getFileUrl("");
        List<String> trusted = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String raw : legacyImages.split(",")) {
            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }
            String objectName = value.startsWith(prefix) ? value.substring(prefix.length()) : null;
            if (objectName == null || objectName.isEmpty()) {
                unresolved.add(value);
                continue;
            }
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
        if (!unresolved.isEmpty()) {
            log.warn("resolveTrustedLegacyImages() 仅审计的未解析值: {}", unresolved);
        }
        return trusted;
    }

    @Override
    public FileRecord uploadProductImage(Long userId, Long merchantId, MultipartFile file) {
        ProductImageValidator.DetectedImage detected = imageValidator.validate(file);
        String objectName = minIoService.uploadFile(file, "service",
                detected.extension(), detected.contentType());
        FileRecord record = new FileRecord();
        record.setOriginal_name_wsh(file.getOriginalFilename());
        record.setObject_name_wsh(objectName);
        record.setSize_wsh(file.getSize());
        record.setContent_type_wsh(detected.contentType());
        record.setPurpose_wsh(PURPOSE_PRODUCT);
        record.setMerchant_id_wsh(merchantId);
        record.setUser_id_wsh(userId);
        try {
            fileRecordService.create(record);
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