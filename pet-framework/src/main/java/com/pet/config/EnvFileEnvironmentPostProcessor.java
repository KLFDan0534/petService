package com.pet.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时读取项目根目录 .env 文件，把 KEY=VALUE 注入 Spring Environment，
 * 使 ${VAR} 占位符（如 GAO_MAP_API_KEY）无需依赖外部环境变量即可解析。
 * 真实系统环境变量优先级更高，.env 仅作为兜底。
 */
public class EnvFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "dotEnvFile";

    /**
     * 【加载 .env 到 Environment】
     *
     * 业务作用：在配置解析前把项目根目录 .env 的键值注入 Spring Environment，供占位符解析。
     *
     * 调用场景：Spring Boot 启动早期，Environment 准备阶段自动调用。
     *
     * 调用链：SpringApplication → postProcessEnvironment → locateEnvFile → 逐行解析 → MapPropertySource 注册。
     *
     * 数据处理：跳过空行与 # 注释行，按首个等号切分键值，值经 unquote 去引号。
     *
     * 业务规则：.env 作为兜底，优先级低于系统环境变量；文件不存在或读取失败直接返回。
     *
     * 状态影响：向 Environment 追加名为 dotEnvFile 的 PropertySource。
     *
     * 异常情况：IOException 被吞掉并返回，不影响启动。
     *
     * 注意事项：插入位置在 systemEnvironment 之后、其他源之前，保证真实环境变量优先。
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 找不到 .env 文件直接跳过加载
        Path envFile = locateEnvFile();
        if (envFile == null) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                // 跳过空行与 # 注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                // 没有等号或键名为空视为非法行
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (!key.isEmpty()) {
                    properties.put(key, unquote(value));
                }
            }
        // 读取失败按无配置处理，不影响启动
        } catch (IOException ignored) {
            return;
        }
        if (properties.isEmpty()) {
            return;
        }
        MapPropertySource source = new MapPropertySource(SOURCE_NAME, properties);
        // 插到系统环境变量之后，保证真实环境变量优先
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
    }

    /**
     * 【定位 .env 文件】
     *
     * 业务作用：从当前工作目录向上逐层查找 .env 文件。
     *
     * 调用场景：postProcessEnvironment 开始处理时调用。
     *
     * 调用链：postProcessEnvironment → locateEnvFile → 逐级 resolve 判断。
     *
     * 数据处理：最多向上查找 5 层目录，命中即返回绝对路径。
     *
     * 业务规则：只认普通文件，目录同名不算命中。
     *
     * 状态影响：无（只读文件系统）。
     *
     * 异常情况：未找到返回 null，由调用方直接返回。
     *
     * 注意事项：以 user.dir 为起点，因此启动目录不同可能导致找不到 .env。
     */
    private static Path locateEnvFile() {
        Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        // 从工作目录向上最多查找五层目录
        for (int i = 0; i < 5; i++) {
            Path candidate = base.resolve(".env");
            // 同名目录不算命中，只认普通文件
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            base = base.getParent();
            if (base == null) {
                break;
            }
        }
        return null;
    }

    /**
     * 【去除包裹引号】
     *
     * 业务作用：去掉配置值首尾成对的单引号或双引号。
     *
     * 调用场景：解析 .env 每行的值之后调用。
     *
     * 调用链：postProcessEnvironment → unquote。
     *
     * 数据处理：长度不小于 2 且首尾引号同类型时截取中间部分。
     *
     * 业务规则：仅处理成对引号，不处理转义。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：值内部含引号时原样保留。
     */
    private static String unquote(String value) {
        // 至少两个字符才可能首尾成对包裹
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}