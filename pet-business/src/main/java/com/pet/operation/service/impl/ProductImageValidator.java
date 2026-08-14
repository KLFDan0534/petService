package com.pet.operation.service.impl;

import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 产品图片内容校验器。
 * <p>
 * 服务端权威校验：仅允许可解码的光栅格式（PNG/JPEG/GIF/BMP），
 * 校验 magic bytes + 成功解码 + 字节/尺寸上限，拒绝 SVG/HTML/
 * 多语种文件（polyglot）与截断内容，扩展名与 MIME 一律由检测结果派生，
 * 不信任客户端声明的 MIME（spoofed MIME 与声明冲突即拒绝）。
 */
@Component
public class ProductImageValidator {

    public static final String EXT_PNG = "png";
    public static final String EXT_JPEG = "jpg";
    public static final String EXT_GIF = "gif";
    public static final String EXT_BMP = "bmp";

    public static final long DEFAULT_MAX_BYTES = 10 * 1024 * 1024L;
    public static final int DEFAULT_MAX_DIMENSION = 4096;

    private final long maxBytes;
    private final int maxDimension;

    /**
     * Spring 构造：从配置读取产品图片上限。
     */
    public ProductImageValidator(
            @org.springframework.beans.factory.annotation.Value("${product-media.max-bytes:10485760}") long maxBytes,
            @org.springframework.beans.factory.annotation.Value("${product-media.max-dimension:4096}") int maxDimension) {
        this.maxBytes = maxBytes;
        this.maxDimension = maxDimension;
    }

    /**
     * 测试构造：使用默认上限。
     */
    ProductImageValidator() {
        this(DEFAULT_MAX_BYTES, DEFAULT_MAX_DIMENSION);
    }

    /**
     * 校验并识别一张产品图片。
     *
     * @param file 待校验的上传文件
     * @return 检测结果（扩展名、MIME、解码尺寸）
     */
    public DetectedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, "上传文件不能为空");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_TOO_LARGE,
                    "图片大小不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DECODE_FAILED, "读取上传内容失败");
        }
        String detected = detectMagic(bytes);
        if (detected == null) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED,
                    "仅支持 PNG/JPEG/GIF/BMP 光栅图片");
        }
        assertDeclaredMimeMatches(file.getContentType(), detected);

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DECODE_FAILED, "图片解码失败(截断或损坏)");
        }
        if (image == null) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DECODE_FAILED, "图片解码失败(截断或损坏)");
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DIMENSIONS_EXCEEDED,
                    "图片尺寸不能超过 " + maxDimension + "x" + maxDimension);
        }
        assertNoHtmlMarkers(bytes, detected);
        return new DetectedImage(detected, detectedMime(detected),
                image.getWidth(), image.getHeight());
    }

    private void assertDeclaredMimeMatches(String declared, String detected) {
        if (declared == null || declared.isBlank()
                || "application/octet-stream".equalsIgnoreCase(declared)) {
            return;
        }
        if (!declared.toLowerCase().startsWith("image/")
                || !detectedMime(detected).equalsIgnoreCase(declared)) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED,
                    "声明的MIME与检测内容不一致(拒绝spoofed MIME)");
        }
    }

    /**
     * 检测 magic bytes，返回规范的格式标识，未知/HTML/SVG 返回 null。
     */
    private String detectMagic(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        if (isPng(bytes)) return EXT_PNG;
        if (isJpeg(bytes)) return EXT_JPEG;
        if (isGif(bytes)) return EXT_GIF;
        if (isBmp(bytes)) return EXT_BMP;
        return null;
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private boolean isGif(byte[] b) {
        return b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
    }

    private boolean isBmp(byte[] b) {
        return b.length >= 2 && b[0] == 'B' && b[1] == 'M';
    }

    private String detectedMime(String format) {
        return switch (format) {
            case EXT_PNG -> "image/png";
            case EXT_JPEG -> "image/jpeg";
            case EXT_GIF -> "image/gif";
            case EXT_BMP -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 多语种文件（polyglot）检测：有效光栅头之后仍携带 HTML/脚本标记则拒绝。
     */
    private void assertNoHtmlMarkers(byte[] bytes, String detected) {
        String lower = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).toLowerCase();
        if (lower.contains("<script") || lower.contains("<html") || lower.contains("<!doctype")
                || lower.contains("<?php") || lower.contains("javascript:") || lower.contains("<iframe")
                || lower.contains("<svg")) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_CONTENT_SUSPICIOUS,
                    "图片内容携带HTML/脚本标记(拒绝polyglot)");
        }
    }

    /**
     * 图片检测结果：扩展名与 MIME 由内容派生，供存储与记录使用。
     */
    public record DetectedImage(String extension, String contentType, int width, int height) {
    }
}