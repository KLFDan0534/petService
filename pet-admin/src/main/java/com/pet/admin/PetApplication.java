package com.pet.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.pet")
@MapperScan({"com.pet.system.mapper", "com.pet.boarding.mapper", "com.pet.customer.mapper", "com.pet.finance.mapper", "com.pet.operation.mapper", "com.pet.order.mapper", "com.pet.pet.mapper", "com.pet.ai.mapper", "com.pet.qualification.mapper", "com.pet.marketing.mapper", "com.pet.membership.mapper"})
@EnableAsync
@EnableScheduling
public class PetApplication {
    public static void main(String[] args) {
        applyJvmProxyFromDotEnv();
        SpringApplication.run(PetApplication.class, args);
    }

    /**
     * 启动早期把代理写进 JVM 网络属性，让 Java HttpClient 走系统代理。
     * <p>
     * Java HttpClient 默认不读取系统/环境变量中的代理，而 PowerShell/.NET 会，
     * 导致部分需代理才能访问的 AI 端点（如 api.b.ai）直连被网络策略阻断（connect timeout）。
     * 这里在创建任何 HttpClient 之前，从项目根 .env 或真实环境变量读取
     * HTTPS_PROXY / HTTP_PROXY，并设置 ${scheme}.proxyHost / ${scheme}.proxyPort。
     * 未配置代理时不做任何改动，行为与原先一致。
     * </p>
     */
    private static void applyJvmProxyFromDotEnv() {
        Map<String, String> env = readDotEnv();
        String httpsProxy = firstNonBlank(env.get("HTTPS_PROXY"), env.get("https_proxy"), System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"));
        if (httpsProxy != null) {
            setProxyProps("https", httpsProxy);
        }
        String httpProxy = firstNonBlank(env.get("HTTP_PROXY"), env.get("http_proxy"), System.getenv("HTTP_PROXY"), System.getenv("http_proxy"));
        if (httpProxy != null) {
            setProxyProps("http", httpProxy);
        }
        String noProxy = firstNonBlank(env.get("NO_PROXY"), env.get("no_proxy"), System.getenv("NO_PROXY"), System.getenv("no_proxy"));
        if (noProxy != null) {
            System.setProperty("http.nonProxyHosts", noProxy.replace(',', '|'));
        }
    }

    /** 项目根 .env 的简单 key=value 解析（失败返回空 map，不阻断启动）。 */
    private static Map<String, String> readDotEnv() {
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("../.env"),
                Path.of("pet-admin/.env"),
                Path.of("pet-admin/../.env"));
        try {
            for (Path p : candidates) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                Map<String, String> map = new java.util.HashMap<>();
                for (String line : Files.readAllLines(p)) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) {
                        continue;
                    }
                    int idx = t.indexOf('=');
                    if (idx > 0) {
                        map.put(t.substring(0, idx).trim(), t.substring(idx + 1).trim());
                    }
                }
                if (!map.isEmpty()) {
                    return map;
                }
            }
        } catch (Exception ignored) {
            // .env 读取失败不影响启动
        }
        return Map.of();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static void setProxyProps(String scheme, String proxy) {
        try {
            String normalized = proxy.startsWith("http") ? proxy : "http://" + proxy;
            java.net.URI uri = java.net.URI.create(normalized);
            if (uri.getHost() != null && uri.getPort() > 0) {
                System.setProperty(scheme + ".proxyHost", uri.getHost());
                System.setProperty(scheme + ".proxyPort", String.valueOf(uri.getPort()));
            }
        } catch (Exception ignored) {
            // 非法代理配置忽略，使用直连
        }
    }
}