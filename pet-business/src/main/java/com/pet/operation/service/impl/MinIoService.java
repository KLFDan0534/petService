package com.pet.operation.service.impl;

import com.pet.common.BusinessException;
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MinIO 文件存储服务，提供文件上传、获取访问 URL 和删除等基础操作。
 * <p>
 * 所有文件以 UUID 重命名后存入指定目录，目录不存在时自动创建 bucket。
 */
@Service
public class MinIoService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-url}")
    private String publicUrl;

    public MinIoService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 上传文件到 MinIO 指定的目录。文件以 UUID 重命名，保留原始扩展名。
     * <p>
     * 如果 bucket 不存在则自动创建。上传成功返回 MinIO 上的对象路径。
     *
     * @param file      上传的多部分文件，不可为空
     * @param directory 存储目录（例如 "avatar"、"report"），不能为 null
     * @return MinIO 对象存储路径（格式：{directory}/{uuid}.{ext}）
     * @throws com.pet.common.BusinessException 文件为空、文件名为空或上传失败时抛出
     */
    public String uploadFile(MultipartFile file, String directory) {
        return uploadFile(file, directory, null, null);
    }

    /**
     * 上传文件到 MinIO 指定的目录，使用服务端派生（而非客户端）的扩展名与内容类型。
     * <p>
     * 产品图片上传使用本方法：扩展名/MIME 由内容校验结果决定，忽略调用方目录选择与声明。
     *
     * @param file         上传的多部分文件，不可为空
     * @param directory    存储目录（例如 "service"），不能为 null
     * @param extension    服务端派生的扩展名（不含点，如 "png"），null 时回退到原始文件名
     * @param contentType  服务端派生的内容类型（如 "image/png"），null 时使用客户端声明
     * @return MinIO 对象存储路径（格式：{directory}/{uuid}.{ext}）
     * @throws com.pet.common.BusinessException 文件为空、文件名为空或上传失败时抛出
     */
    public String uploadFile(MultipartFile file, String directory, String extension, String contentType) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("上传文件不能为空");
            }
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new BusinessException("文件名不能为空");
            }
            String ext = extension;
            if (ext == null || ext.isBlank()) {
                ext = "";
                if (originalFilename.contains(".")) {
                    ext = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
            } else {
                ext = "." + ext;
            }
            String objectName = directory + "/" + UUID.randomUUID() + ext;
            String derivedContentType = contentType != null && !contentType.isBlank()
                    ? contentType : file.getContentType();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(derivedContentType)
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的公开访问 URL
     * <p>
     * 拼接规则：{publicUrl}/{bucket}/{objectName}
     *
     * @param objectName MinIO 对象存储路径
     * @return 完整的文件访问 URL
     */
    public String getFileUrl(String objectName) {
        return publicUrl + "/" + bucket + "/" + objectName;
    }

    /**
     * 从 MinIO 删除指定的文件对象
     *
     * @param objectName MinIO 对象存储路径
     * @throws com.pet.common.BusinessException 删除失败时抛出
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }
}

