package com.pet.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.mapper.AiConfigWshMapper;
import com.pet.common.BusinessException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 多配置存储服务。
 * <p>管理员在后台维护多套 AI 配置（增删改、启用/停用、测试），按用途各启用一套；DB 无生效配置时由调用方回退 yml 默认。</p>
 */
@Service
@Slf4j
public class AiConfigStore {

    private final AiConfigWshMapper mapper;

    public AiConfigStore(AiConfigWshMapper mapper) {
        this.mapper = mapper;
    }

    /** 全部配置（脱敏视图）；表未创建/查询异常时降级返回空列表（不打断主流程）。 */
    public List<ConfigView> listAll() {
        return safeRead(() -> mapper.selectList(new LambdaQueryWrapper<AiConfigWsh>()
                        .orderByAsc(AiConfigWsh::getUsage_wsh)
                        .orderByDesc(AiConfigWsh::getEnabled_wsh)
                        .orderByDesc(AiConfigWsh::getId_wsh))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList()),
                java.util.Collections.emptyList());
    }

    /** 新增配置；enable=true 时直接启用（自动停用同用途其它配置）。 */
    public ConfigView create(AiConfigWsh cfg, boolean enable) {
        validateUsage(cfg.getUsage_wsh());
        requireText(cfg.getEndpoint_wsh(), "API 地址不能为空");
        requireText(cfg.getApi_key_wsh(), "API Key 不能为空");
        requireText(cfg.getModel_wsh(), "模型不能为空");
        normalize(cfg);
        return safeWrite(() -> {
            if (enable) {
                disableAll(cfg.getUsage_wsh(), null);
                cfg.setEnabled_wsh(1);
            } else {
                cfg.setEnabled_wsh(0);
            }
            mapper.insert(cfg);
            return toView(cfg);
        });
    }

    /** 更新配置；apiKey 留空则保留原值；usage 不可修改。 */
    public ConfigView update(Long id, AiConfigWsh patch) {
        return safeWrite(() -> {
            AiConfigWsh exist = requireConfig(id);
            if (StringUtils.hasText(patch.getName_wsh())) exist.setName_wsh(patch.getName_wsh().trim());
            if (StringUtils.hasText(patch.getEndpoint_wsh())) exist.setEndpoint_wsh(patch.getEndpoint_wsh());
            if (StringUtils.hasText(patch.getApi_key_wsh())) exist.setApi_key_wsh(patch.getApi_key_wsh());
            if (StringUtils.hasText(patch.getModel_wsh())) exist.setModel_wsh(patch.getModel_wsh());
            if (patch.getMax_tokens_wsh() != null && patch.getMax_tokens_wsh() > 0) exist.setMax_tokens_wsh(patch.getMax_tokens_wsh());
            if (patch.getTemperature_wsh() != null) exist.setTemperature_wsh(patch.getTemperature_wsh());
            normalize(exist);
            mapper.updateById(exist);
            return toView(exist);
        });
    }

    /** 逻辑删除配置；若删除的是启用项，同用途将无启用（运行链路回退 yml）。 */
    public void delete(Long id) {
        safeWrite(() -> {
            requireConfig(id);
            mapper.deleteById(id);
            return null;
        });
    }

    /** 启用/停用指定配置；启用时自动停用同用途其它配置（每用途最多一条启用）。 */
    public ConfigView setEnabled(Long id, boolean enabled) {
        return safeWrite(() -> {
            AiConfigWsh exist = requireConfig(id);
            if (enabled) {
                disableAll(exist.getUsage_wsh(), id);
                exist.setEnabled_wsh(1);
            } else {
                exist.setEnabled_wsh(0);
            }
            mapper.updateById(exist);
            return toView(exist);
        });
    }

    /** 读取指定用途的当前生效配置；无则返回 null（调用方回退 yml）。表未创建时同样返回 null 让调用方回退。 */
    public AiConfigWsh getActiveConfig(String usage) {
        validateUsage(usage);
        return safeRead(() -> mapper.selectOne(new LambdaQueryWrapper<AiConfigWsh>()
                        .eq(AiConfigWsh::getUsage_wsh, usage)
                        .eq(AiConfigWsh::getEnabled_wsh, 1)
                        .last("LIMIT 1")),
                null);
    }

    /** 按 id 读取原始配置（含明文 API Key，仅供服务端测试/调用使用）。 */
    public AiConfigWsh getByIdRaw(Long id) {
        return safeWrite(() -> requireConfig(id));
    }

    /** 表未创建/查询失败时降级：写操作明确告知先执行建表 SQL。 */
    private <T> T safeRead(java.util.function.Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("AI 配置表读取失败（建表语句见 docs/database/full-schema.sql 中 ai_config_wsh）: {}", e.getMessage());
            return fallback;
        }
    }

    private <T> T safeWrite(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException be) {
            throw be; // 保留 400/404 等业务语义
        } catch (Exception e) {
            log.error("AI 配置写入失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "AI 配置功能不可用，请确认已建 ai_config_wsh 表（见 docs/database/full-schema.sql）");
        }
    }

    /** 构建 chat/completions 完整 URL（去除末尾斜杠）。 */
    public String chatCompletionsUrl(String endpoint) {
        String base = endpoint == null ? "" : endpoint.replaceAll("/+$", "");
        return base + "/chat/completions";
    }

    /** 脱敏视图 */
    public ConfigView toView(AiConfigWsh cfg) {
        ConfigView view = new ConfigView();
        view.setId(cfg.getId_wsh());
        view.setName(cfg.getName_wsh());
        view.setUsage(cfg.getUsage_wsh());
        view.setEndpoint(cfg.getEndpoint_wsh());
        view.setApiKey(maskKey(cfg.getApi_key_wsh()));
        view.setModel(cfg.getModel_wsh());
        view.setMaxTokens(cfg.getMax_tokens_wsh());
        view.setTemperature(cfg.getTemperature_wsh());
        view.setEnabled(Integer.valueOf(1).equals(cfg.getEnabled_wsh()));
        return view;
    }

    /** 脱敏 API Key：保留前后 4 位，中间打码。 */
    public String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private AiConfigWsh requireConfig(Long id) {
        AiConfigWsh exist = mapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "AI 配置不存在");
        }
        return exist;
    }

    private void disableAll(String usage, Long exceptId) {
        LambdaUpdateWrapper<AiConfigWsh> uw = new LambdaUpdateWrapper<AiConfigWsh>()
                .eq(AiConfigWsh::getUsage_wsh, usage)
                .set(AiConfigWsh::getEnabled_wsh, 0);
        if (exceptId != null) {
            uw.ne(AiConfigWsh::getId_wsh, exceptId);
        }
        mapper.update(null, uw);
    }

    private void normalize(AiConfigWsh cfg) {
        cfg.setEndpoint_wsh(cfg.getEndpoint_wsh().replaceAll("/+$", ""));
        if (cfg.getMax_tokens_wsh() == null || cfg.getMax_tokens_wsh() <= 0) {
            cfg.setMax_tokens_wsh(8192);
        }
        if (cfg.getTemperature_wsh() == null) {
            cfg.setTemperature_wsh(BigDecimal.ONE);
        }
    }

    private void validateUsage(String usage) {
        if (!AiConfigWsh.USAGE_AGENT.equals(usage) && !AiConfigWsh.USAGE_CS.equals(usage)) {
            throw new BusinessException(400, "用途仅支持 agent / cs");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
    }

    /** 配置脱敏视图（前端展示用） */
    @Getter
    @Setter
    public static class ConfigView {
        private Long id;
        private String name;
        private String usage;
        private String endpoint;
        private String apiKey;
        private String model;
        private Integer maxTokens;
        private BigDecimal temperature;
        private Boolean enabled;
    }
}