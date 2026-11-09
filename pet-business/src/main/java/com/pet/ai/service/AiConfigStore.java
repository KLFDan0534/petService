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
     *
     * 调用场景：Spring 容器启动创建该 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造方法 → mapper 赋值。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：依赖缺失时由 Spring 抛出 Bean 创建异常。
     *
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
     *
     * 调用场景：create、update、delete、setEnabled、getByIdRaw 中调用。
     *
     * 调用链：create / update / delete / setEnabled / getByIdRaw → 本方法 → mapper 写操作。
     *
     * 数据处理：输入 Supplier → 输出其结果，不改变数据。
     *
     * 业务规则：BusinessException 原样抛出（保留 400/404）；其它异常记 error 日志后抛 BusinessException(500)。
     *
     * 状态影响：无额外影响，内部写库由 Supplier 完成。
     *
     * 异常情况：非业务异常一律转换为 BusinessException(500)，原始堆栈仅写入日志。
     *
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
     *
     * 调用场景：update、delete、setEnabled、getByIdRaw 中调用。
     *
     * 调用链：上述方法 → 本方法 → mapper.selectById。
     *
     * 数据处理：输入 id → 输出 AiConfigWsh。
     *
     * 业务规则：配置必须存在。
     *
     * 状态影响：无，只读。
     *
     * 异常情况：配置不存在抛 BusinessException(404, "AI 配置不存在")。
     *
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
     *
     * 调用场景：create（enable=true）与 setEnabled（enabled=true）中调用。
     *
     * 调用链：create / setEnabled → 本方法 → mapper.update（LambdaUpdateWrapper）。
     *
     * 数据处理：按 usage_wsh 匹配，批量 set enabled_wsh=0；exceptId 非空时排除该ID。
     *
     * 业务规则：只影响同一 usage 的记录。
     *
     * 状态影响：批量更新 ai_config_wsh 的 enabled_wsh 字段。
     *
     * 异常情况：数据库异常向上抛出，由外层 safeWrite 包装。
     *
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
     *
     * 调用场景：create 与 update 在落库前调用。
     *
     * 调用链：create / update → 本方法。
     *
     * 数据处理：endpoint_wsh 去掉末尾所有 /；max_tokens_wsh 为空或 <=0 时置 8192；temperature_wsh 为 null 时置 1。
     *
     * 业务规则：默认 maxTokens=8192、temperature=1。
     *
     * 状态影响：无，仅修改入参对象。
     *
     * 异常情况：endpoint_wsh 为 null 时 replaceAll 抛 NullPointerException，本方法未判空。
     *
     * 注意事项：create 前已通过 requireText 保证 endpoint 非空，但 update 中若传入空白 endpoint 会被 StringUtils.hasText 挡在赋值之外，因此不会触发 NPE。
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
     *
     * 调用场景：create 与 getActiveConfig 中调用。
     *
     * 调用链：create / getActiveConfig → 本方法 → 与 AiConfigWsh.USAGE_AGENT / USAGE_CS 比对。
     *
     * 数据处理：输入 usage → 无输出。
     *
     * 业务规则：必须精确等于 USAGE_AGENT 或 USAGE_CS，区分大小写。
     *
     * 状态影响：无。
     *
     * 异常情况：不合法抛 BusinessException(400, "用途仅支持 agent / cs")。
     *
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
     *
     * 调用场景：create 中连续调用三次。
     *
     * 调用链：create → 本方法 → StringUtils.hasText。
     *
     * 数据处理：输入 value 与错误文案 → 无输出。
     *
     * 业务规则：null、空串、全空白均视为空。
     *
     * 状态影响：无。
     *
     * 异常情况：为空抛 BusinessException(400)，文案由调用方传入。
     *
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