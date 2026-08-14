package com.pet.boarding.service;

import com.pet.boarding.dto.ServiceMediaDTO;
import com.pet.boarding.dto.ServiceMediaItemDTO;
import com.pet.operation.entity.FileRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务产品图片聚合服务。
 * <p>
 * 职责：
 * <ul>
 *   <li>以整组替换的方式维护产品图册（排序 + 唯一封面），并在事务内锁定聚合根</li>
 *   <li>按服务端可信来源推导公开 URL，批量解析文件记录（无 N+1）</li>
 *   <li>兼容旧 images_wsh：仅返回能解析到内部文件记录的可信值</li>
 * </ul>
 */
public interface ServiceMediaService {

    /**
     * 替换某个服务的完整图册。
     * <p>
     * 事务内先对父服务行加排他锁（SELECT ... FOR UPDATE），再校验文件记录
     * 存在性、产品用途与商家归属，最后整体删除旧行并插入新行。
     * 并发替换以锁串行化，最终结果必为某个完整版本。
     *
     * @param serviceId 服务产品ID
     * @param items     图册条目（file_id_wsh + sort_order_wsh + is_cover_wsh）；
     *                  空集合表示清空图册
     */
    void replaceMedia(Long serviceId, List<ServiceMediaItemDTO> items);

    /**
     * 按排序查询某服务的图册，URL 由服务端可信来源批量推导。
     */
    List<ServiceMediaDTO> listMedia(Long serviceId);

    /**
     * 批量查询多个服务的图册（无 N+1）。
     * <p>
     * 一次查询全部媒体行 + 一次批量解析文件记录，按 serviceId 分组返回
     * 有序图册；没有图册的服务不在返回 Map 中。
     */
    Map<Long, List<ServiceMediaDTO>> listMediaByServiceIds(Collection<Long> serviceIds);

    /**
     * 旧版 images_wsh 兼容读取（KTD9）：仅返回能解析到配置的 MinIO 来源
     * 且存在于 file_record_wsh 中的可信值，保持原顺序；其余值仅审计记录。
     */
    List<String> resolveTrustedLegacyImages(String legacyImages);

    /**
     * 上传一张产品图片（服务端受管）。
     * <p>
     * 校验 magic bytes + 解码 + 字节/尺寸上限，仅允许光栅格式，拒绝
     * SVG/HTML/polyglot/截断内容；扩展名与内容类型由检测结果派生，
     * 存储目录由服务端指定，忽略调用方声明。对象上传成功但文件记录
     * 落库失败时，补偿删除刚上传的对象。
     *
     * @param userId     上传人用户ID
     * @param merchantId 服务端归属商家ID（MERCHANT 自动派生，ADMIN 显式指定）
     * @param file       上传的图片文件
     * @return 落库后的文件记录（含 id_wsh）
     */
    FileRecord uploadProductImage(Long userId, Long merchantId, MultipartFile file);
}