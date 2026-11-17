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

    /**
     * 【构造方法】注入 AI 配置 Mapper
     *
     * 业务作用：由 Spring 注入 AiConfigWshMapper，供配置的读写使用。
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     * 注意事项：依赖为 final 字段。
     */
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

    /**
     * 【解析模型调用参数】DB 生效配置优先，缺失字段回退 yml 默认值。
     *
     * 业务作用：把「取哪套配置、哪个字段用 DB、哪个字段用 yml」这一层规则收敛到唯一一处。 此前 agent 与 cs 两条模型调用链各自手写同样的 5 字段 null 级联，改一处容易漏另一处。
     * 业务规则：
     * <ul>
     * <li>该用途无生效配置 → 整组返回 yml 默认值</li>
     * <li>有生效配置 → endpoint/apiKey/model 直接取 DB 值（即使为 null，与改造前一致）</li>
     * <li>maxTokens/temperature 为 null 时回退 yml 默认值（与改造前一致）</li>
     * </ul>
     * 注意事项：本方法只做字段合并，**不校验 Key 是否为空、不抛异常**。 是否可用、失败时返回 null 还是抛错，由各调用方按自身语义决定（agent 链返回 null 降级，cs 链抛异常转人工）。
     *
     * @param usage         用途，取值 {@link AiConfigWsh#USAGE_AGENT} 或 {@link AiConfigWsh#USAGE_CS}
     * @param ymlEndpoint   yml 兜底 endpoint
     * @param ymlApiKey     yml 兜底 apiKey
     * @param ymlModel      yml 兜底 model
     * @param ymlMaxTokens  yml 兜底 maxTokens
     * @param ymlTemperature yml 兜底 temperature
     * @return 解析后的参数，永不为 null
     */
    public ModelParams resolveParams(String usage, String ymlEndpoint, String ymlApiKey,
                                     String ymlModel, int ymlMaxTokens, double ymlTemperature) {
        AiConfigWsh cfg = getActiveConfig(usage);
        if (cfg == null) {
            return new ModelParams(ymlEndpoint, ymlApiKey, ymlModel, ymlMaxTokens, ymlTemperature);
        }
        return new ModelParams(
                cfg.getEndpoint_wsh(),
                cfg.getApi_key_wsh(),
                cfg.getModel_wsh(),
                cfg.getMax_tokens_wsh() != null ? cfg.getMax_tokens_wsh() : ymlMaxTokens,
                cfg.getTemperature_wsh() != null ? cfg.getTemperature_wsh().doubleValue() : ymlTemperature);
    }

    /** 一次模型调用所需的参数快照。 */
    public record ModelParams(String endpoint, String apiKey, String model,
                              int maxTokens, double temperature) {
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

    /**
     * 【写操作异常包装】统一把写库异常转为业务异常
     *
     * 业务作用：包裹 create/update/delete/setEnabled/getByIdRaw 的写操作，保留业务异常语义，把其它异常统一转为 500 并提示建表。
     * 业务规则：BusinessException 原样抛出（保留 400/404）；其它异常记 error 日志后抛 BusinessException(500)。
     * 异常情况：非业务异常一律转换为 BusinessException(500)，原始堆栈仅写入日志。
     * 注意事项：与 safeRead 的降级策略不同——读失败静默返回兜底值，写失败必须报错；错误提示里硬编码了建表脚本路径 docs/database/full-schema.sql。
     */
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

    /**
     * 【加载配置】按ID取配置并在缺失时抛错
     *
     * 业务作用：update、delete、setEnabled、getByIdRaw 的公共加载与存在性校验。
     * 业务规则：配置必须存在。
     * 异常情况：配置不存在抛 BusinessException(404, "AI 配置不存在")。
     * 注意事项：id 为 null 时 selectById 返回 null，同样抛 404 而非 400。
     */
    private AiConfigWsh requireConfig(Long id) {
        AiConfigWsh exist = mapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "AI 配置不存在");
        }
        return exist;
    }

    /**
     * 【停用同用途配置】把指定用途下其它配置全部置为停用
     *
     * 业务作用：保证每种用途（agent/cs）最多只有一条启用配置。
     * 业务规则：只影响同一 usage 的记录。
     * 异常情况：数据库异常向上抛出，由外层 safeWrite 包装。
     * 注意事项：create 时传入 exceptId=null（新记录尚未插入，无需排除）；setEnabled 时传入自身ID避免把自己关掉。
     */
    private void disableAll(String usage, Long exceptId) {
        LambdaUpdateWrapper<AiConfigWsh> uw = new LambdaUpdateWrapper<AiConfigWsh>()
                .eq(AiConfigWsh::getUsage_wsh, usage)
                .set(AiConfigWsh::getEnabled_wsh, 0);
        if (exceptId != null) {
            uw.ne(AiConfigWsh::getId_wsh, exceptId);
        }
        mapper.update(null, uw);
    }

    /**
     * 【配置规范化】去除地址末尾斜杠并补默认值
     *
     * 业务作用：统一 endpoint 格式，为 max_tokens 与 temperature 填充默认值。
     * 业务规则：默认 maxTokens=8192、temperature=1。
     * 异常情况：endpoint_wsh 为 null 时 replaceAll 抛 NullPointerException，本方法未判空。
     * 注意事项：
     * create 前已通过 requireText 保证 endpoint 非空，但 update 中若传入空白 endpoint 会被 StringUtils.hasText 挡在赋值之外，因此不会触发 NPE。
     */
    private void normalize(AiConfigWsh cfg) {
        cfg.setEndpoint_wsh(cfg.getEndpoint_wsh().replaceAll("/+$", ""));
        if (cfg.getMax_tokens_wsh() == null || cfg.getMax_tokens_wsh() <= 0) {
            cfg.setMax_tokens_wsh(8192);
        }
        if (cfg.getTemperature_wsh() == null) {
            cfg.setTemperature_wsh(BigDecimal.ONE);
        }
    }

    /**
     * 【用途校验】只允许 agent 与 cs 两种用途
     *
     * 业务作用：限制配置的用途取值，保证启用唯一性按用途分组有效。
     * 业务规则：必须精确等于 USAGE_AGENT 或 USAGE_CS，区分大小写。
     * 异常情况：不合法抛 BusinessException(400, "用途仅支持 agent / cs")。
     * 注意事项：新增用途需同时改常量与本方法的判断，update 不校验用途（usage 不可修改）。
     */
    private void validateUsage(String usage) {
        if (!AiConfigWsh.USAGE_AGENT.equals(usage) && !AiConfigWsh.USAGE_CS.equals(usage)) {
            throw new BusinessException(400, "用途仅支持 agent / cs");
        }
    }

    /**
     * 【必填校验】字符串为空时抛出指定提示
     *
     * 业务作用：create 时校验 endpoint、api_key、model 三个必填字段。
     * 业务规则：null、空串、全空白均视为空。
     * 异常情况：为空抛 BusinessException(400)，文案由调用方传入。
     * 注意事项：只校验非空，不做格式校验（如 URL 合法性、Key 长度）。
     */
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