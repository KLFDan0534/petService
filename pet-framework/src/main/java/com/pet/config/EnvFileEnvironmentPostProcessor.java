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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = locateEnvFile();
        if (envFile == null) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (!key.isEmpty()) {
                    properties.put(key, unquote(value));
                }
            }
        } catch (IOException ignored) {
            return;
        }
        if (properties.isEmpty()) {
            return;
        }
        MapPropertySource source = new MapPropertySource(SOURCE_NAME, properties);
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
    }

    private static Path locateEnvFile() {
        Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 5; i++) {
            Path candidate = base.resolve(".env");
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

    private static String unquote(String value) {
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