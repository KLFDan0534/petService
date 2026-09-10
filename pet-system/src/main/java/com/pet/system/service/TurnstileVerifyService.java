package com.pet.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 【Cloudflare Turnstile 验证服务】
 *
 * <p>在服务端对前端提交的 cf-turnstile-response token 调用 Cloudflare siteverify 接口，
 * 校验 {@code success === true} 且来源 Hostname 位于白名单内。仅当校验通过才放行业务处理。</p>
 *
 * <p>配置项（application.yml，均可由环境变量覆盖）：</p>
 * <ul>
 *   <li>turnstile.secret        = ${TURNSTILE_SECRET:}      —— 小部件密钥。未配置时跳过人机校验（fail-open，仅开发环境）；配置后自动启用强制校验（fail-closed）</li>
 *   <li>turnstile.allowed-hostnames = ${TURNSTILE_HOSTNAMES:localhost,127.0.0.1} —— 允许的前端 Hostname（生产环境建议移除 localhost/127.0.0.1）</li>
 * </ul>
 *
 * <p>说明：Cloudflare Turnstile 官方不支持原生移动 App（无 Android/iOS SDK），
 * 因此 Android 端不渲染小部件。开发环境（密钥未配置）采用 fail-open 保证登录可用；
 * 生产环境配置 TURNSTILE_SECRET 后，Web 端强制校验、原生 App 需另行接入（如 WebView 挑战或 App Token）。</p>
 */
@Slf4j
@Service
public class TurnstileVerifyService {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String secret;
    private final Set<String> allowedHostnames;

    public TurnstileVerifyService(
            @Value("${turnstile.secret:}") String secret,
            @Value("${turnstile.allowed-hostnames:localhost,127.0.0.1}") String allowedHostnames,
            ObjectMapper objectMapper) {
        this.secret = secret == null ? "" : secret.trim();
        String[] hosts = (allowedHostnames == null ? "" : allowedHostnames).split(",");
        this.allowedHostnames = Arrays.stream(hosts)
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toSet());
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * 校验 Turnstile token（无 User-Agent 信息）。
     *
     * @param token   前端提交的 cf-turnstile-response token
     * @param remoteIp 客户端真实 IP（优先取 X-Forwarded-For，其次 getRemoteAddr）
     * @return true 表示校验通过（success 且 hostname 在白名单内），否则 false
     */
    public boolean verify(String token, String remoteIp) {
        return verify(token, remoteIp, null);
    }

    /**
     * 校验 Turnstile token（带 User-Agent 信息）。
     *
     * <p>原生 Android/iOS 客户端（User-Agent 含 okhttp / Dalvik / okhttphttp / Android）不渲染
     * Cloudflare Turnstile 小部件，因此无法提交 token。对这类客户端自动跳过 Turnstile 校验（fail-open），
     * 由业务层自身的认证机制（JWT）保障安全。Web 端仍强制校验。</p>
     *
     * @param token     前端提交的 cf-turnstile-response token
     * @param remoteIp  客户端真实 IP
     * @param userAgent 客户端 User-Agent（可为 null）
     * @return true 表示校验通过或跳过，false 表示校验失败
     */
    public boolean verify(String token, String remoteIp, String userAgent) {
        if (secret.isBlank()) {
            // fail-open：密钥未配置（开发环境）时跳过人机校验，保证登录/注册可用；
            // 配置 TURNSTILE_SECRET 后自动切换回强制校验。
            log.warn("Turnstile 密钥未配置; 跳过人机校验(fail-open)。生产环境请配置 TURNSTILE_SECRET。");
            return true;
        }
        if (isNativeClient(userAgent)) {
            log.debug("检测到原生客户端 (User-Agent: {}); 跳过 Turnstile 校验。", userAgent);
            return true;
        }
        if (token == null || token.isBlank() || token.length() > 2048) {
            log.warn("Turnstile token 缺失或过长。");
            return false;
        }
        if (allowedHostnames.isEmpty()) {
            log.warn("Turnstile 允许的域名列表未配置。");
            return false;
        }
        try {
            String body = "secret=" + enc(secret)
                    + "&response=" + enc(token)
                    + "&remoteip=" + enc(remoteIp == null ? "" : remoteIp);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SITEVERIFY_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Turnstile siteverify 返回 HTTP {}", response.statusCode());
                return false;
            }
            JsonNode json = objectMapper.readTree(response.body());
            boolean success = json.path("success").asBoolean(false);
            String hostname = json.path("hostname").asText("");
            if (!success) {
                log.warn("Turnstile siteverify 校验失败: {}", json.path("error-codes"));
                return false;
            }
            if (!allowedHostnames.contains(hostname)) {
                log.warn("Turnstile 域名 '{}' 不在允许列表 {} 中", hostname, allowedHostnames);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Turnstile siteverify 调用失败", e);
            return false;
        }
    }

    /**
     * 判断是否为原生移动客户端（Android/iOS）。
     * OkHttp 默认 User-Agent 格式：okhttp/4.x.y；Android 系统 HttpUrlConnection 格式含 Dalvik。
     */
    private static boolean isNativeClient(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        String ua = userAgent.toLowerCase();
        return ua.contains("okhttp") || ua.contains("dalvik") || ua.contains("android");
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}