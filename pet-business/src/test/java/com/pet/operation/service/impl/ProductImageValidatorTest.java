package com.pet.operation.service.impl;

import com.pet.common.BookingErrorCode;
import com.pet.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U1: 产品图片上传校验 —— 仅接受可解码的光栅格式（PNG/JPEG/GIF/BMP），
 * 校验 magic bytes + 解码 + 字节/尺寸上限，拒绝 SVG/HTML/polyglot/截断内容，
 * 扩展名与 MIME 由检测派生（spoofed MIME 拒绝）。
 */
class ProductImageValidatorTest {

    private ProductImageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductImageValidator();
    }

    private MockMultipartFile image(String format) throws Exception {
        return image(format, mimeFor(format));
    }

    private MockMultipartFile image(String format, String mime) throws Exception {
        BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out), "ImageIO must support " + format);
        return new MockMultipartFile("file", "test." + format, mime, out.toByteArray());
    }

    private String mimeFor(String format) {
        return switch (format) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    @Test
    void acceptsPng() throws Exception {
        ProductImageValidator.DetectedImage detected = validator.validate(image("png"));
        assertEquals("png", detected.extension());
        assertEquals("image/png", detected.contentType());
        assertEquals(100, detected.width());
        assertEquals(80, detected.height());
    }

    @Test
    void acceptsJpeg() throws Exception {
        ProductImageValidator.DetectedImage detected = validator.validate(image("jpg"));
        assertEquals("jpg", detected.extension());
        assertEquals("image/jpeg", detected.contentType());
    }

    @Test
    void acceptsGif() throws Exception {
        ProductImageValidator.DetectedImage detected = validator.validate(image("gif"));
        assertEquals("gif", detected.extension());
        assertEquals("image/gif", detected.contentType());
    }

    @Test
    void acceptsBmp() throws Exception {
        ProductImageValidator.DetectedImage detected = validator.validate(image("bmp"));
        assertEquals("bmp", detected.extension());
        assertEquals("image/bmp", detected.contentType());
    }

    @Test
    void emptyFileRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.png", "image/png", new byte[0])));
        assertEquals(BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, e.getErrorCode());
    }

    @Test
    void spoofedMimeRejected() throws Exception {
        byte[] png = renderPng();
        MockMultipartFile spoofed = file("a.html", "text/html", png);
        BusinessException e = assertThrows(BusinessException.class, () -> validator.validate(spoofed));
        assertEquals(BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, e.getErrorCode());
    }

    @Test
    void svgRejected() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>";
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8))));
        assertEquals(BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, e.getErrorCode());
    }

    @Test
    void htmlRejected() {
        String html = "<!DOCTYPE html><html><body>hello</body></html>";
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.html", "text/html", html.getBytes(StandardCharsets.UTF_8))));
        assertEquals(BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, e.getErrorCode());
    }

    @Test
    void polyglotRejected() throws Exception {
        byte[] png = renderPng();
        byte[] polyglot = new byte[png.length + "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(png, 0, polyglot, 0, png.length);
        System.arraycopy("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8), 0,
                polyglot, png.length, polyglot.length - png.length);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.png", "image/png", polyglot)));
        assertEquals(BookingErrorCode.IMAGE_CONTENT_SUSPICIOUS, e.getErrorCode());
    }

    @Test
    void truncatedRejected() throws Exception {
        byte[] png = renderPng();
        byte[] truncated = new byte[png.length / 2];
        System.arraycopy(png, 0, truncated, 0, truncated.length);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.png", "image/png", truncated)));
        assertEquals(BookingErrorCode.IMAGE_DECODE_FAILED, e.getErrorCode());
    }

    @Test
    void oversizeBytesRejected() {
        byte[] big = new byte[(int) ProductImageValidator.DEFAULT_MAX_BYTES + 1];
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.png", "image/png", big)));
        assertEquals(BookingErrorCode.IMAGE_TOO_LARGE, e.getErrorCode());
    }

    @Test
    void oversizeDimensionRejected() throws Exception {
        BufferedImage image = new BufferedImage(5000, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", out));
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.png", "image/png", out.toByteArray())));
        assertEquals(BookingErrorCode.IMAGE_DIMENSIONS_EXCEEDED, e.getErrorCode());
    }

    @Test
    void unsupportedFormatRejected() {
        byte[] text = "plain text content".getBytes(StandardCharsets.UTF_8);
        BusinessException e = assertThrows(BusinessException.class,
                () -> validator.validate(file("a.txt", "text/plain", text)));
        assertEquals(BookingErrorCode.IMAGE_FORMAT_UNSUPPORTED, e.getErrorCode());
    }

    private byte[] renderPng() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", out));
        return out.toByteArray();
    }
}