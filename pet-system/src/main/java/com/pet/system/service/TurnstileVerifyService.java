package com.pet.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
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
 *   <li>turnstile.proxy        = ${TURNSTILE_PROXY:}        —— 访问 siteverify 时使用的 HTTP 代理（如 http://127.0.0.1:7897）。Java HttpClient 不会自动读取系统代理，直连被阻断时必须显式配置</li>
 *   <li>turnstile.fail-open-on-error = ${TURNSTILE_FAIL_OPEN_ON_ERROR:false} —— 验证服务"不可达"（网络/代理故障、HTTP 非 200）时是否放行；默认 false 保持严格，本地开发可置 true 避免被完全锁死</li>
 * </ul>
 *
 * <p>说明：Cloudflare Turnstile 官方不支持原生移动 App（无 Android/iOS SDK），
 * 因此 Android 端不渲染小部件。开发环境（密钥未配置）采用 fail-open 保证登录可用；
 * 生产环境配置 TURNSTILE_SECRET 后，Web 端强制校验、原生 App 需另行接入（如 WebView 挑战或 App Token）。</p>
 *
 * <p>【重要】校验失败与校验服务故障是两件事：
 * token 无效/过期/域名不符属于"校验失败"，任何配置下都拒绝；
 * 网络不通、代理未配置、siteverify 返回非 200 属于"服务故障"，按 fail-open-on-error 决定是否放行。</p>
 */
@Slf4j
@Service
public class TurnstileVerifyService {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String secret;
    private final Set<String> allowedHostnames;
    private final boolean failOpenOnError;

    /**
     * 【Turnstile 校验服务构造器】
     *
     * 业务作用：读取密钥、域名白名单、代理与降级开关，并构建带超时的 HttpClient。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 读取 turnstile.* 配置 → 解析白名单 → 构建 ProxySelector → 构建 HttpClient。
     *
     * 数据处理：密钥 trim 后保存；白名单按逗号切分并去空；连接超时固定 5 秒。
     *
     * 业务规则：代理配置为空时直连；配置格式非法时记 warn 并退回直连。
     *
     * 状态影响：创建一个 HttpClient 实例，生命周期与应用一致。
     *
     * 异常情况：配置解析异常不会中断启动，代理非法仅降级为直连。
     *
     * 注意事项：Java HttpClient 不读取系统代理，必须显式配置 turnstile.proxy 才能走代理。
     */
    public TurnstileVerifyService(
            @Value("${turnstile.secret:}") String secret,
            @Value("${turnstile.allowed-hostnames:localhost,127.0.0.1}") String allowedHostnames,
            @Value("${turnstile.proxy:}") String proxy,
            @Value("${turnstile.fail-open-on-error:false}") boolean failOpenOnError,
            ObjectMapper objectMapper) {
        this.secret = secret == null ? "" : secret.trim();
        String[] hosts = (allowedHostnames == null ? "" : allowedHostnames).split(",");
        this.allowedHostnames = Arrays.stream(hosts)
                .map(String::trim)
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toSet());
        this.failOpenOnError = failOpenOnError;
        this.objectMapper = objectMapper;
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5));
        ProxySelector proxySelector = buildProxySelector(proxy);
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        this.httpClient = builder.build();
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
        if (token != null && token.length() > 2048) {
            log.warn("Turnstile token 过长（{} 字符），拒绝。", token.length());
            return false;
        }
        if (token == null || token.isBlank()) {
            // 前端小部件未能渲染（脚本被墙、域名未授权、浏览器未走代理）时拿不到 token，
            // 与“机器人故意不提交 token”在协议上无法区分，因此交由 fail-open-on-error 决定：
            // 本地开发放行（否则用户被永久锁死），生产严格模式拒绝。
            return degrade("前端未提交 token（小部件可能未渲染）", null);
        }
        if (allowedHostnames.isEmpty()) {
            log.warn("Turnstile 允许的域名列表未配置。");
            return false;
        }
        HttpResponse<String> response;
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
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return degrade("调用 siteverify 被中断", e);
        } catch (Exception e) {
            // 典型场景：本机直连 challenges.cloudflare.com 被阻断（SSLHandshakeException / ConnectException / timeout）。
            // 这类属于"验证服务不可达"，不等于"用户没通过验证"。
            return degrade("调用 siteverify 失败: " + e.getClass().getSimpleName(), e);
        }
        if (response.statusCode() != 200) {
            return degrade("siteverify 返回 HTTP " + response.statusCode(), null);
        }
        try {
            JsonNode json = objectMapper.readTree(response.body());
            boolean success = json.path("success").asBoolean(false);
            String hostname = json.path("hostname").asText("");
            if (!success) {
                // 明确的人机校验失败（token 无效/过期/重复使用/密钥错误），任何配置下都拒绝
                log.warn("Turnstile siteverify 校验失败: {}", json.path("error-codes"));
                return false;
            }
            if (!allowedHostnames.contains(hostname)) {
                log.warn("Turnstile 域名 '{}' 不在允许列表 {} 中", hostname, allowedHostnames);
                return false;
            }
            return true;
        } catch (Exception e) {
            // 返回体无法解析，属于服务异常而非用户未通过验证
            return degrade("解析 siteverify 响应失败", e);
        }
    }

    /**
     * 验证服务不可达时的降级处理。
     *
     * <p>fail-open-on-error 为 true（本地开发）时放行，避免网络/代理故障导致全员无法登录；
     * 为 false（生产默认）时拒绝，保持安全边界。</p>
     */
    private boolean degrade(String reason, Exception e) {
        if (failOpenOnError) {
            log.warn("Turnstile 校验无法完成（{}）; 已按 turnstile.fail-open-on-error=true 放行。"
                    + "请检查网络或 turnstile.proxy 配置。", reason, e);
            return true;
        }
        log.warn("Turnstile 校验无法完成（{}）; 按严格模式拒绝本次请求。"
                + "若为本地开发环境，可配置 turnstile.proxy 或 turnstile.fail-open-on-error=true。", reason, e);
        return false;
    }

    /**
     * 解析代理配置，形如 http://127.0.0.1:7897 或 127.0.0.1:7897；为空则返回 null（直连）。
     */
    private static ProxySelector buildProxySelector(String proxy) {
        if (proxy == null || proxy.isBlank()) {
            return null;
        }
        String value = proxy.trim();
        int schemeIdx = value.indexOf("://");
        if (schemeIdx >= 0) {
            value = value.substring(schemeIdx + 3);
        }
        int slashIdx = value.indexOf('/');
        if (slashIdx >= 0) {
            value = value.substring(0, slashIdx);
        }
        int atIdx = value.lastIndexOf('@');
        if (atIdx >= 0) {
            value = value.substring(atIdx + 1);
        }
        int colonIdx = value.lastIndexOf(':');
        if (colonIdx <= 0) {
            log.warn("Turnstile 代理配置格式不正确: '{}'，已忽略并使用直连。", proxy);
            return null;
        }
        String host = value.substring(0, colonIdx);
        int port;
        try {
            port = Integer.parseInt(value.substring(colonIdx + 1));
        } catch (NumberFormatException e) {
            log.warn("Turnstile 代理端口不正确: '{}'，已忽略并使用直连。", proxy);
            return null;
        }
        log.info("Turnstile siteverify 将通过代理 {}:{} 访问。", host, port);
        return ProxySelector.of(new InetSocketAddress(host, port));
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

    /**
     * 【URL 编码】
     *
     * 业务作用：对表单参数做 UTF-8 的 URL 编码，避免特殊字符破坏请求体。
     *
     * 调用场景：verify 拼接 siteverify 表单时对 secret、token、remoteip 编码。
     *
     * 调用链：verify → enc → URLEncoder.encode。
     *
     * 数据处理：统一使用 UTF-8 字符集。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无；URLEncoder 对 UTF-8 不会抛 UnsupportedEncodingException。
     *
     * 注意事项：空格会被编码为加号，符合表单编码规范。
     */
    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
