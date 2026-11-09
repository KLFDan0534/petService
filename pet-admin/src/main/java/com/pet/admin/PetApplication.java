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
    /**
     * 【应用启动入口】
     *
     * 业务作用：启动 Spring Boot 应用，启动前先把代理配置写入 JVM 网络属性。
     *
     * 调用场景：java -jar 或 IDE 直接运行时调用。
     *
     * 调用链：main → applyJvmProxyFromDotEnv → SpringApplication.run。
     *
     * 数据处理：无返回值，异常直接抛出由 JVM 处理。
     *
     * 业务规则：代理必须在创建任何 HttpClient 之前设置，因此放在 run 之前。
     *
     * 状态影响：启动整个应用上下文。
     *
     * 异常情况：端口占用、配置错误等由 Spring 启动期抛出。
     *
     * 注意事项：不能调整到 run 之后调用，否则已创建的 HttpClient 不会生效。
     */
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

    /**
     * 【取首个非空白值】
     *
     * 业务作用：在多个候选值中取第一个非空白字符串。
     *
     * 调用场景：读取代理配置时依次尝试 .env 与系统环境变量的多种写法。
     *
     * 调用链：applyJvmProxyFromDotEnv → firstNonBlank。
     *
     * 数据处理：命中即返回 trim 后的值。
     *
     * 业务规则：null 与空白字符串均视为无效。
     *
     * 状态影响：无。
     *
     * 异常情况：全部无效时返回 null。
     *
     * 注意事项：可变参数个数不限，调用方自行保证顺序优先级。
     */
    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /**
     * 【写入 JVM 代理属性】
     *
     * 业务作用：把代理地址解析后写入 scheme.proxyHost 与 scheme.proxyPort。
     *
     * 调用场景：applyJvmProxyFromDotEnv 拿到代理地址后调用。
     *
     * 调用链：applyJvmProxyFromDotEnv → setProxyProps → System.setProperty。
     *
     * 数据处理：缺少协议前缀时补 http://，解析 URI 取 host 与 port。
     *
     * 业务规则：host 为空或端口不大于 0 时不设置，保持直连。
     *
     * 状态影响：修改 JVM 全局系统属性。
     *
     * 异常情况：URI 解析失败被吞掉，按直连处理。
     *
     * 注意事项：代理地址必须显式带端口，否则不会生效。
     */
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