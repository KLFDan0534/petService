package com.pet.boarding.service.impl;

import com.pet.boarding.dto.ServiceMediaItemDTO;
import com.pet.boarding.entity.ServiceItem;
import com.pet.boarding.mapper.ServiceItemMapper;
import com.pet.boarding.mapper.ServiceMediaMapper;
import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import com.pet.operation.entity.FileRecord;
import com.pet.operation.mapper.FileRecordMapper;
import com.pet.operation.service.FileRecordService;
import com.pet.operation.service.impl.MinIoService;
import com.pet.operation.service.impl.ProductImageValidator;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U1: 服务产品图册替换与产品图片上传。
 * <p>
 * 替换语义：空图册合法；一张图自动成为封面；0..10 张；重复文件/排序、双封面、
 * 非产品用途文件、跨商家文件拒绝；并发替换由父行锁串行化（另见
 * ProductMediaRowLockConcurrencyTest）。上传在文件记录落库失败时补偿删除对象。
 */
@ExtendWith(MockitoExtension.class)
class ServiceMediaServiceImplTest {

    @Mock private ServiceMediaMapper mediaMapper;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private FileRecordMapper fileRecordMapper;
    @Mock private FileRecordService fileRecordService;
    @Mock private MinIoService minIoService;
    @Mock private ProductImageValidator imageValidator;

    private ServiceMediaServiceImpl service;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ServiceItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), com.pet.boarding.entity.ServiceMedia.class);
    }

    @BeforeEach
    void setUp() {
        service = new ServiceMediaServiceImpl(mediaMapper, serviceItemMapper, fileRecordMapper,
                fileRecordService, minIoService, imageValidator, 10);
    }

    private ServiceItem ownerService(long id, long merchantId) {
        ServiceItem item = new ServiceItem();
        item.setId_wsh(id);
        item.setMerchant_id_wsh(merchantId);
        return item;
    }

    private ServiceMediaItemDTO item(long fileId, int sort, int cover) {
        ServiceMediaItemDTO dto = new ServiceMediaItemDTO();
        dto.setFile_id_wsh(fileId);
        dto.setSort_order_wsh(sort);
        dto.setIs_cover_wsh(cover);
        return dto;
    }

    private FileRecord productFile(long id, long merchantId) {
        FileRecord record = new FileRecord();
        record.setId_wsh(id);
        record.setPurpose_wsh("product");
        record.setMerchant_id_wsh(merchantId);
        return record;
    }

    @Test
    void replaceMediaClearsGalleryWhenEmpty() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        service.replaceMedia(1L, List.of());
        verify(mediaMapper).deleteByServiceId(1L);
    }

    @Test
    void oneImageBecomesCover() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        when(fileRecordMapper.selectBatchIds(anyCollection())).thenReturn(List.of(productFile(11L, 100L)));
        service.replaceMedia(1L, List.of(item(11L, 0, 1)));
        verify(mediaMapper).deleteByServiceId(1L);
        verify(mediaMapper).insert(any(com.pet.boarding.entity.ServiceMedia.class));
    }

    @Test
    void tenOrderedImagesAccepted() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        List<FileRecord> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            records.add(productFile(20L + i, 100L));
        }
        when(fileRecordMapper.selectBatchIds(anyCollection())).thenReturn(records);
        List<ServiceMediaItemDTO> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(item(20L + i, i, i == 0 ? 1 : 0));
        }
        service.replaceMedia(1L, items);
        verify(mediaMapper).deleteByServiceId(1L);
        verify(mediaMapper, org.mockito.Mockito.times(10)).insert(any(com.pet.boarding.entity.ServiceMedia.class));
    }

    @Test
    void moreThanTenImagesRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        List<ServiceMediaItemDTO> items = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            items.add(item(100L + i, i, i == 0 ? 1 : 0));
        }
        BusinessException e = assertThrows(BusinessException.class, () -> service.replaceMedia(1L, items));
        assertEquals(BookingErrorCode.MEDIA_LIMIT_EXCEEDED, e.getErrorCode());
        verify(mediaMapper, never()).deleteByServiceId(any());
    }

    @Test
    void duplicateFileRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1), item(11L, 1, 0))));
        assertEquals(BookingErrorCode.MEDIA_DUPLICATE_FILE, e.getErrorCode());
    }

    @Test
    void duplicateOrderRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1), item(12L, 0, 0))));
        assertEquals(BookingErrorCode.MEDIA_INVALID_ORDER, e.getErrorCode());
    }

    @Test
    void nonContiguousOrderRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1), item(12L, 2, 0))));
        assertEquals(BookingErrorCode.MEDIA_INVALID_ORDER, e.getErrorCode());
    }

    @Test
    void twoCoversRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1), item(12L, 1, 1))));
        assertEquals(BookingErrorCode.MEDIA_INVALID_COVER, e.getErrorCode());
    }

    @Test
    void missingCoverRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 0), item(12L, 1, 0))));
        assertEquals(BookingErrorCode.MEDIA_INVALID_COVER, e.getErrorCode());
    }

    @Test
    void foreignMerchantFileRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        when(fileRecordMapper.selectBatchIds(anyCollection())).thenReturn(List.of(productFile(11L, 999L)));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1))));
        assertEquals(BookingErrorCode.MEDIA_FILE_MERCHANT_MISMATCH, e.getErrorCode());
        verify(mediaMapper, never()).deleteByServiceId(any());
    }

    @Test
    void nonProductPurposeFileRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        FileRecord record = productFile(11L, 100L);
        record.setPurpose_wsh("avatar");
        when(fileRecordMapper.selectBatchIds(anyCollection())).thenReturn(List.of(record));
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1))));
        assertEquals(BookingErrorCode.MEDIA_FILE_PURPOSE_MISMATCH, e.getErrorCode());
    }

    @Test
    void missingFileRecordRejected() {
        when(serviceItemMapper.selectByIdForUpdate(1L)).thenReturn(ownerService(1L, 100L));
        when(fileRecordMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(1L, List.of(item(11L, 0, 1))));
        assertEquals(BookingErrorCode.MEDIA_FILE_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void unknownServiceRejected() {
        when(serviceItemMapper.selectByIdForUpdate(99L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.replaceMedia(99L, List.of(item(11L, 0, 1))));
        assertEquals(BookingErrorCode.SERVICE_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void uploadCompensatesObjectWhenRecordCreateFails() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        ProductImageValidator.DetectedImage detected =
                new ProductImageValidator.DetectedImage("png", "image/png", 10, 10);
        when(imageValidator.validate(file)).thenReturn(detected);
        when(minIoService.uploadFile(eq(file), eq("service"), eq("png"), eq("image/png")))
                .thenReturn("service/abc.png");
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(fileRecordService).create(any(FileRecord.class));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.uploadProductImage(1L, 100L, file));
        assertEquals(BookingErrorCode.PRODUCT_IMAGE_RECORD_FAILED, e.getErrorCode());
        verify(minIoService).deleteFile("service/abc.png");
    }

    @Test
    void uploadRejectsBeforeUploadWhenValidationFails() {
        MockMultipartFile file = new MockMultipartFile("file", "a.svg", "image/svg+xml", new byte[]{1});
        when(imageValidator.validate(file)).thenThrow(
                new BusinessException(400, BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, "only raster"));
        assertThrows(BusinessException.class, () -> service.uploadProductImage(1L, 100L, file));
        verify(minIoService, never()).uploadFile(any(), anyString());
    }

    @Test
    void legacyResolutionKeepsOnlyResolvableTrustedValues() {
        when(minIoService.getFileUrl("")).thenReturn("/minio/pet-service/");
        FileRecord record = productFile(11L, 100L);
        record.setObject_name_wsh("service/uuid.png");
        when(fileRecordMapper.selectOne(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<FileRecord, String, ?> wrapper =
                    (com.baomidou.mybatisplus.core.conditions.AbstractWrapper<FileRecord, String, ?>) invocation.getArgument(0);
            wrapper.getSqlSegment();
            return wrapper.getParamNameValuePairs().containsValue("service/uuid.png") ? record : null;
        });

        List<String> trusted = service.resolveTrustedLegacyImages(
                "/minio/pet-service/service/uuid.png,https://evil.com/x.png,  ,/minio/pet-service/ghost.png");

        assertEquals(1, trusted.size());
        assertEquals("/minio/pet-service/service/uuid.png", trusted.get(0));
        verify(fileRecordMapper, org.mockito.Mockito.times(2)).selectOne(any());
    }
}