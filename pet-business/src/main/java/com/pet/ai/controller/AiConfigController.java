package com.pet.ai.controller;

import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.service.AiConfigService;
import com.pet.ai.service.AiConfigStore;
import com.pet.ai.service.AiConfigStore.ConfigView;
import com.pet.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 多配置管理接口（仅管理员可用）。
 * <p>管理多套 AI 配置（增删改/启用/测试），按用途各启一套；DB 无生效时可由运行链路回退 yml。</p>
 */
@RestController
@RequestMapping("/api/ai/configs")
@Tag(name = "【AI 配置】多配置管理", description = "管理员管理多套 AI 大模型配置（增删改/启用/测试）")
@PreAuthorize("hasRole('ADMIN')")
public class AiConfigController {

    private final AiConfigStore configStore;
    private final AiConfigService aiConfigService;

    // agent 用途的 yml 兜底值（ai.deepseek.*）
    @Value("${ai.deepseek.endpoint:https://api.b.ai/v1}")
    private String agentYmlEndpoint;
    @Value("${ai.deepseek.model:glm-5.3-flash}")
    private String agentYmlModel;
    @Value("${ai.deepseek.max-tokens:8192}")
    private int agentYmlMaxTokens;
    @Value("${ai.deepseek.temperature:1.0}")
    private double agentYmlTemperature;
    @Value("${ai.deepseek.api-key:}")
    private String agentYmlApiKey;

    /**
     * 业务作用：注入 AiConfigStore（多配置读写）与 AiConfigService（yml 兜底配置）。
     * 注意事项：yml 兜底值通过 @Value 注入到独立字段，不经过构造器。
     */
    public AiConfigController(AiConfigStore configStore, AiConfigService aiConfigService) {
        this.configStore = configStore;
        this.aiConfigService = aiConfigService;
    }

    /**
     * 业务作用：返回全部 AI 配置，API Key 做脱敏展示。
     * 数据处理：直接返回 ConfigView 列表，脱敏在 Store 层完成。
     * 业务规则：仅 ADMIN 可访问（类级 @PreAuthorize）。
     * 异常情况：非管理员由安全框架拦截返回 403。
     * 注意事项：不返回明文 API Key。
     */
    @GetMapping
    @Operation(summary = "配置列表", description = "返回全部 AI 配置（API Key 脱敏）")
    public Result<List<ConfigView>> list() {
        return Result.success(configStore.listAll());
    }

    /**
     * 业务作用：把请求体转成实体后新增一套 AI 配置，可选择直接启用。
     * 业务规则：仅 ADMIN；usage 决定配置用途，enable 只接受 Boolean.TRUE 视为启用。
     * 异常情况：参数非法由 Store 层抛业务异常。
     * 注意事项：enable 为 null 时按不启用处理（Boolean.TRUE.equals 判断）。
     */
    @PostMapping
    @Operation(summary = "新增配置", description = "新增一套 AI 配置；enable=true 时直接启用（自动停用同用途其它）")
    public Result<ConfigView> create(@RequestBody AiConfigRequest request) {
        AiConfigWsh cfg = toEntity(request);
        return Result.success(configStore.create(cfg, Boolean.TRUE.equals(request.getEnable())));
    }

    /**
     * 业务作用：按 ID 更新配置的名称、端点、密钥、模型、token 上限与温度。
     * 业务规则：仅 ADMIN；usage 不可修改，因此 patch 中不设置 usage_wsh。
     * 异常情况：配置不存在时由 Store 层抛 404。
     * 注意事项：API Key 留空表示不修改，具体由 Store 判断空值；此处无条件把请求值塞进 patch。
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新配置", description = "API Key 留空表示不修改；usage 不可修改")
    public Result<ConfigView> update(@PathVariable Long id, @RequestBody AiConfigRequest request) {
        AiConfigWsh patch = new AiConfigWsh();
        patch.setName_wsh(request.getName());
        patch.setEndpoint_wsh(request.getEndpoint());
        patch.setApi_key_wsh(request.getApiKey());
        patch.setModel_wsh(request.getModel());
        patch.setMax_tokens_wsh(request.getMaxTokens());
        patch.setTemperature_wsh(request.getTemperature());
        return Result.success(configStore.update(id, patch));
    }

    /**
     * 业务作用：切换某套配置的启用状态。
     * 业务规则：仅 ADMIN；启用某配置时同用途的其它配置会被自动停用。
     * 异常情况：配置不存在时由 Store 层抛 404。
     * 注意事项：请求体缺 enabled 键会被当作 false 执行停用，而不是报错。
     */
    @PostMapping("/{id}/enable")
    @Operation(summary = "启用/停用配置", description = "启用时同用途其它配置自动停用")
    public Result<ConfigView> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return Result.success(configStore.setEnabled(id, enabled));
    }

    /**
     * 业务作用：逻辑删除指定配置。
     * 业务规则：仅 ADMIN；为逻辑删除，不做物理删除。
     * 异常情况：无显式异常，配置不存在时由 Store 决定行为。
     * 注意事项：删除启用项后运行链路会回退到 yml 兜底值，不会中断服务。
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "逻辑删除；若删除启用项，同用途将无启用（运行链路回退 yml）")
    public Result<Void> delete(@PathVariable Long id) {
        configStore.delete(id);
        return Result.success();
    }

    /**
     * 业务作用：用该配置的密钥与模型向 OpenAI 兼容端点发一次极简对话请求，验证可用性。
     * 业务规则：仅 ADMIN；缺少 API Key 时直接返回失败而不发请求。
     * 异常情况：所有异常都被捕获，统一返回 success=false 与错误信息，不向上抛；配置不存在时 apiKey 为 null 走「缺少 API Key」分支。
     * 注意事项：每次调用都 new 一个 RestTemplate，未复用连接池；失败响应只截取前 200 字符返回。
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试连通性", description = "按该配置发送简单请求，验证 OpenAI 兼容端点可用")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            RestTemplate testRest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String apiKey = null;
            String model = null;
            String url = null;
            // 测试目标：优先取该 id 的持久化配置
            AiConfigWsh cfg = configStore.getByIdRaw(id);
            if (cfg != null) {
                apiKey = cfg.getApi_key_wsh();
                model = cfg.getModel_wsh();
                url = configStore.chatCompletionsUrl(cfg.getEndpoint_wsh());
            }
            if (apiKey == null || apiKey.isBlank()) {
                result.put("success", false);
                result.put("message", "该配置缺少 API Key");
                return Result.success(result);
            }
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            List<Map<String, String>> messages = new java.util.ArrayList<>();
            messages.add(Map.of("role", "user", "content", "你好，请回复'OK'"));
            body.put("messages", messages);
            body.put("max_tokens", 200);

            String raw = testRest.postForObject(url, new HttpEntity<>(body, headers), String.class);
            if (raw != null && raw.contains("choices")) {
                result.put("success", true);
                result.put("message", "AI 服务连接成功");
                result.put("endpoint", url);
                result.put("model", model);
            } else {
                result.put("success", false);
                result.put("message", "AI 服务返回异常: " + (raw != null ? raw.substring(0, Math.min(raw.length(), 200)) : "空响应"));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        return Result.success(result);
    }

    /**
     * 业务作用：按用途返回实际生效的配置，数据库无启用项时回退到 yml 配置。
     * 数据处理：
     * 有启用配置时返回 source=db 及脱敏后的配置字段；无启用配置时按用途分支：cs 用途取 AiConfigService 的 yml 值，其它用途取 ai.deepseek.* 注入值，均标记 source=yml。
     * 业务规则：仅 ADMIN；API Key 一律脱敏输出。
     * 异常情况：usage 缺失时由 Spring 抛参数缺失异常返回 400。
     * 注意事项：yml 兜底只区分 cs 与「其它」两类用途，非 cs 用途共用同一组 ai.deepseek.* 配置。
     */
    @GetMapping("/effective")
    @Operation(summary = "当前生效配置", description = "按用途返回生效配置；无启用时返回 yml 兜底值并标注 source=yml")
    public Result<Map<String, Object>> effective(@RequestParam String usage) {
        Map<String, Object> result = new LinkedHashMap<>();
        AiConfigWsh active = configStore.getActiveConfig(usage);
        if (active != null) {
            ConfigView view = configStore.toView(active);
            result.put("source", "db");
            result.put("id", view.getId());
            result.put("name", view.getName());
            result.put("usage", view.getUsage());
            result.put("endpoint", view.getEndpoint());
            result.put("apiKey", view.getApiKey());
            result.put("model", view.getModel());
            result.put("maxTokens", view.getMaxTokens());
            result.put("temperature", view.getTemperature());
        } else if (AiConfigWsh.USAGE_CS.equals(usage)) {
            result.put("source", "yml");
            result.put("endpoint", aiConfigService.getEndpoint());
            result.put("apiKey", configStore.maskKey(aiConfigService.getApiKey()));
            result.put("model", aiConfigService.getModel());
            result.put("maxTokens", aiConfigService.getMaxTokens());
            result.put("temperature", BigDecimal.valueOf(aiConfigService.getTemperature()));
        } else {
            result.put("source", "yml");
            result.put("endpoint", agentYmlEndpoint);
            result.put("apiKey", configStore.maskKey(agentYmlApiKey));
            result.put("model", agentYmlModel);
            result.put("maxTokens", agentYmlMaxTokens);
            result.put("temperature", BigDecimal.valueOf(agentYmlTemperature));
        }
        return Result.success(result);
    }

    /**
     * 业务作用：把 AiConfigRequest 的字段逐一映射为 AiConfigWsh 实体。
     * 业务规则：无校验，合法性由 Store 层负责。
     * 注意事项：未使用 BeanUtils，字段需手工保持同步。
     */
    private AiConfigWsh toEntity(AiConfigRequest request) {
        AiConfigWsh cfg = new AiConfigWsh();
        cfg.setName_wsh(request.getName());
        cfg.setUsage_wsh(request.getUsage());
        cfg.setEndpoint_wsh(request.getEndpoint());
        cfg.setApi_key_wsh(request.getApiKey());
        cfg.setModel_wsh(request.getModel());
        cfg.setMax_tokens_wsh(request.getMaxTokens());
        cfg.setTemperature_wsh(request.getTemperature());
        return cfg;
    }

    @Data
    public static class AiConfigRequest {
        private String name;
        private String usage;
        private String endpoint;
        private String apiKey;
        private String model;
        private Integer maxTokens;
        private BigDecimal temperature;
        private Boolean enable;
    }
}