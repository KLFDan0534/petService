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
        // 超过大小上限直接拒绝
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
        // 魔数不匹配说明不是支持的光栅格式
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
        // 解码不出图像视为截断或损坏
        if (image == null) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DECODE_FAILED, "图片解码失败(截断或损坏)");
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
            throw new BusinessException(400, BookingErrorCode.IMAGE_DIMENSIONS_EXCEEDED,
                    "图片尺寸不能超过 " + maxDimension + "x" + maxDimension);
        }
        // 有效图片后仍带脚本标记按 polyglot 拒绝
        assertNoHtmlMarkers(bytes, detected);
        return new DetectedImage(detected, detectedMime(detected),
                image.getWidth(), image.getHeight());
    }

    /**
     * 【校验客户端声明的 MIME 与检测结果是否一致】
     *
     * 业务作用：防止伪造 Content-Type，要求声明的 MIME 与 magic bytes 推导出的 MIME 相同。
     *
     * 调用场景：validate 检测出真实格式之后、解码之前。
     *
     * 调用链：ProductImageValidator.validate → assertDeclaredMimeMatches → detectedMime。
     *
     * 数据处理：声明为空、空白或 application/octet-stream 时直接放行；否则要求以小写 image/ 开头且与 detectedMime 结果忽略大小写相等。
     *
     * 业务规则：不信任客户端 MIME，只以检测结果为准；octet-stream 视为未声明。
     *
     * 状态影响：无。
     *
     * 异常情况：声明非 image/ 开头或与检测结果不一致时抛 BusinessException(400, IMAGE_FORMAT_UNSUPPORTED)。
     *
     * 注意事项：放行 octet-stream 是为了兼容浏览器或客户端不填类型的场景。
     */
    private void assertDeclaredMimeMatches(String declared, String detected) {
        // 未声明类型时放行，以魔数结果为准
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
        // 按 PNG/JPEG/GIF/BMP 顺序比对魔数
        if (isPng(bytes)) return EXT_PNG;
        if (isJpeg(bytes)) return EXT_JPEG;
        if (isGif(bytes)) return EXT_GIF;
        if (isBmp(bytes)) return EXT_BMP;
        return null;
    }

    /**
     * 【判断是否为 PNG】
     *
     * 业务作用：按 PNG 文件头魔数判断格式。
     *
     * 调用场景：detectMagic 依次尝试各格式时调用。
     *
     * 调用链：validate → detectMagic → isPng。
     *
     * 数据处理：要求至少 8 字节，且前 8 字节依次为 89 50 4E 47 0D 0A 1A 0A。
     *
     * 业务规则：长度不足直接返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：逐字节用 & 0xFF 转无符号比较，避免 Java byte 符号位导致误判。
     */
    private boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    /**
     * 【判断是否为 JPEG】
     *
     * 业务作用：按 JPEG 文件头魔数判断格式。
     *
     * 调用场景：detectMagic 依次尝试各格式时调用。
     *
     * 调用链：validate → detectMagic → isJpeg。
     *
     * 数据处理：要求至少 3 字节，且前 3 字节为 FF D8 FF。
     *
     * 业务规则：长度不足直接返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只校验文件头，不校验尾部 EOI 标记。
     */
    private boolean isJpeg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    /**
     * 【判断是否为 GIF】
     *
     * 业务作用：按 GIF 文件头判断格式。
     *
     * 调用场景：detectMagic 依次尝试各格式时调用。
     *
     * 调用链：validate → detectMagic → isGif。
     *
     * 数据处理：要求至少 6 字节，且前 4 字节为 G I F 8。
     *
     * 业务规则：长度不足直接返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：只比较 GIF8，不区分 87a 与 89a 版本。
     */
    private boolean isGif(byte[] b) {
        return b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
    }

    /**
     * 【判断是否为 BMP】
     *
     * 业务作用：按 BMP 文件头判断格式。
     *
     * 调用场景：detectMagic 依次尝试各格式时调用。
     *
     * 调用链：validate → detectMagic → isBmp。
     *
     * 数据处理：要求至少 2 字节，且前 2 字节为 B M。
     *
     * 业务规则：长度不足直接返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：BM 只有两个字节，是最容易被伪造的魔数，最终仍依赖后续解码校验。
     */
    private boolean isBmp(byte[] b) {
        return b.length >= 2 && b[0] == 'B' && b[1] == 'M';
    }

    /**
     * 【由格式推导 MIME 类型】
     *
     * 业务作用：把内部格式标识映射为规范 MIME 字符串。
     *
     * 调用场景：validate 组装 DetectedImage、assertDeclaredMimeMatches 比对声明值。
     *
     * 调用链：validate / assertDeclaredMimeMatches → detectedMime。
     *
     * 数据处理：png→image/png、jpg→image/jpeg、gif→image/gif、bmp→image/bmp，其它返回 application/octet-stream。
     *
     * 业务规则：扩展名与 MIME 一律由检测结果派生，不采信客户端声明。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：未知格式返回 octet-stream，调用方需注意该值与「未声明」语义重合。
     */
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
        // 按单字节编码转小写，避免多字节字符干扰匹配
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