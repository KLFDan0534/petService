package com.pet.admin;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pet.ai.entity.AiConfigWsh;
import com.pet.ai.mapper.AiConfigWshMapper;
import com.pet.ai.service.AiConfigStore;
import com.pet.ai.service.AiConfigStore.ConfigView;
import com.pet.common.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 多配置存储服务测试：启用互斥、更新保留 key、删除校验、生效读取。
 */
@ExtendWith(MockitoExtension.class)
class AiConfigStoreTest {

    @Mock private AiConfigWshMapper mapper;

    private AiConfigStore store;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis-Plus 实体 Lambda 缓存（无 Spring 环境的单元测试需要）
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiConfigWsh.class);
        store = new AiConfigStore(mapper);
    }

    @Test
    void createWithEnableOnDisablesOtherSameUsage() {
        AiConfigWsh cfg = baseConfig("b.ai", "agent");
        when(mapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ConfigView view = store.create(cfg, true);

        assertNotNull(view);
        assertEquals(Integer.valueOf(1), cfg.getEnabled_wsh());
        verify(mapper).update(isNull(), any(LambdaUpdateWrapper.class)); // 停用同用途其它
        verify(mapper).insert(any(AiConfigWsh.class));
    }

    @Test
    void createWithInvalidUsageRejected() {
        AiConfigWsh cfg = baseConfig("bad", "unknown");

        assertThrows(BusinessException.class, () -> store.create(cfg, false));
        verify(mapper, never()).insert(any(AiConfigWsh.class));
    }

    @Test
    void updateWithBlankKeyKeepsOriginalKey() {
        AiConfigWsh exist = baseConfig("b.ai", "agent");
        exist.setId_wsh(1L);
        exist.setApi_key_wsh("sk-origin");
        when(mapper.selectById(1L)).thenReturn(exist);

        AiConfigWsh patch = new AiConfigWsh();
        patch.setName_wsh("新名字");
        patch.setApi_key_wsh("");

        ConfigView view = store.update(1L, patch);

        assertEquals("新名字", view.getName());
        assertEquals("sk-origin", exist.getApi_key_wsh());
        verify(mapper).updateById(exist);
    }

    @Test
    void deleteMissingConfigRejected() {
        when(mapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> store.delete(99L));
        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    void getActiveConfigReturnsEnabledRow() {
        AiConfigWsh active = baseConfig("b.ai", "cs");
        active.setEnabled_wsh(1);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(active);

        AiConfigWsh result = store.getActiveConfig("cs");

        assertNotNull(result);
        assertTrue(Integer.valueOf(1).equals(result.getEnabled_wsh()));
    }

    private AiConfigWsh baseConfig(String name, String usage) {
        AiConfigWsh cfg = new AiConfigWsh();
        cfg.setName_wsh(name);
        cfg.setUsage_wsh(usage);
        cfg.setEndpoint_wsh("https://api.b.ai/v1");
        cfg.setApi_key_wsh("sk-test");
        cfg.setModel_wsh("glm-5.3-flash");
        cfg.setMax_tokens_wsh(8192);
        cfg.setTemperature_wsh(new BigDecimal("1.0"));
        return cfg;
    }
}