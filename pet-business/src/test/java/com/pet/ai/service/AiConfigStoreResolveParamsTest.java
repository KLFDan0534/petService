package com.pet.ai.service;

import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.mapper.AiConfigWshMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link AiConfigStore#resolveParams} 的回归测试。
 *
 * <p>覆盖改造前分散在 LlmChatServiceImpl（agent 链）与 AiChatServiceImpl（cs 链）里的
 * 同一套「DB 优先、yml 兜底」规则，确保合并到一处后行为完全一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class AiConfigStoreResolveParamsTest {

    @Mock
    private AiConfigWshMapper mapper;

    private AiConfigStore store() {
        return new AiConfigStore(mapper);
    }

    @Test
    void noActiveConfig_fallsBackToYml() {
        when(mapper.selectOne(any())).thenReturn(null);

        AiConfigStore.ModelParams p = store().resolveParams(
                AiConfigWsh.USAGE_AGENT, "https://yml/v1", "yml-key", "yml-model", 4096, 0.7);

        assertEquals("https://yml/v1", p.endpoint());
        assertEquals("yml-key", p.apiKey());
        assertEquals("yml-model", p.model());
        assertEquals(4096, p.maxTokens());
        assertEquals(0.7, p.temperature());
    }

    @Test
    void activeConfig_winsOverYml() {
        when(mapper.selectOne(any())).thenReturn(cfg("https://db/v1", "db-key", "db-model",
                Integer.valueOf(8192), new BigDecimal("1.0")));

        AiConfigStore.ModelParams p = store().resolveParams(
                AiConfigWsh.USAGE_CS, "https://yml/v1", "yml-key", "yml-model", 4096, 0.7);

        assertEquals("https://db/v1", p.endpoint());
        assertEquals("db-key", p.apiKey());
        assertEquals("db-model", p.model());
        assertEquals(8192, p.maxTokens());
        assertEquals(1.0, p.temperature());
    }

    @Test
    void activeConfigWithNullMaxTokensAndTemperature_fallsBackPerField() {
        when(mapper.selectOne(any())).thenReturn(cfg("https://db/v1", "db-key", "db-model", null, null));

        AiConfigStore.ModelParams p = store().resolveParams(
                AiConfigWsh.USAGE_AGENT, "https://yml/v1", "yml-key", "yml-model", 4096, 0.7);

        // endpoint / key / model 仍取 DB
        assertEquals("https://db/v1", p.endpoint());
        assertEquals("db-key", p.apiKey());
        assertEquals("db-model", p.model());
        // 两个数值字段按字段级回退 yml（与改造前一致）
        assertEquals(4096, p.maxTokens());
        assertEquals(0.7, p.temperature());
    }

    @Test
    void activeConfigWithNullApiKey_returnsNullKeyForCallerToDecide() {
        // 本方法不校验 Key，是否降级由调用方决定（agent 返回 null、cs 抛异常）
        when(mapper.selectOne(any())).thenReturn(cfg("https://db/v1", null, "db-model", 8192, BigDecimal.ONE));

        AiConfigStore.ModelParams p = store().resolveParams(
                AiConfigWsh.USAGE_AGENT, "https://yml/v1", "yml-key", "yml-model", 4096, 0.7);

        assertNull(p.apiKey());
    }

    @Test
    void configTableMissing_fallsBackToYmlInsteadOfThrowing() {
        when(mapper.selectOne(any())).thenThrow(new RuntimeException("Table 'ai_config_wsh' doesn't exist"));

        AiConfigStore.ModelParams p = store().resolveParams(
                AiConfigWsh.USAGE_CS, "https://yml/v1", "yml-key", "yml-model", 4096, 0.7);

        assertEquals("https://yml/v1", p.endpoint());
        assertEquals("yml-key", p.apiKey());
    }

    @Test
    void chatCompletionsUrl_stripsTrailingSlashes() {
        AiConfigStore s = store();
        assertEquals("https://api.b.ai/v1/chat/completions", s.chatCompletionsUrl("https://api.b.ai/v1"));
        assertEquals("https://api.b.ai/v1/chat/completions", s.chatCompletionsUrl("https://api.b.ai/v1/"));
        assertEquals("https://api.b.ai/v1/chat/completions", s.chatCompletionsUrl("https://api.b.ai/v1//"));
    }

    private static AiConfigWsh cfg(String endpoint, String apiKey, String model,
                                   Integer maxTokens, BigDecimal temperature) {
        AiConfigWsh c = new AiConfigWsh();
        c.setUsage_wsh(AiConfigWsh.USAGE_AGENT);
        c.setEndpoint_wsh(endpoint);
        c.setApi_key_wsh(apiKey);
        c.setModel_wsh(model);
        c.setMax_tokens_wsh(maxTokens);
        c.setTemperature_wsh(temperature);
        c.setEnabled_wsh(1);
        return c;
    }
}
