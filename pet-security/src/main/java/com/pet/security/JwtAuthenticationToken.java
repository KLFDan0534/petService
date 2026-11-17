package com.pet.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT认证令牌
 *
 * 业务作用：自定义的Spring Security AuthenticationToken，封装JWT认证通过后的用户身份信息和权限信息
 * 业务规则：构造时自动调用setAuthenticated(true)标记为已认证状态；getCredentials返回null（无凭据）
 * 注意事项：authorities参数为null时表示该用户无特殊权限，但不影响认证状态
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;

    /**
     * 构造JWT认证令牌
     *
     * 业务作用：创建已认证的JwtAuthenticationToken实例，封装用户身份和权限信息
     * 业务规则：调用setAuthenticated(true)标记为已认证；authorities可为空
     * 注意事项：authorities为null时Spring Security的hasRole/hasAuthority判断均返回false
     */
    public JwtAuthenticationToken(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.username = username;
        setAuthenticated(true);
    }

    /**
     * 获取凭证信息
     *
     * 业务作用：返回认证凭证，JWT认证中令牌已在过滤器中验证完毕，此处返回null
     * 业务规则：JWT无状态认证模式下，令牌无需在认证对象中保存，返回null符合设计
     * 注意事项：返回null为正常行为，非异常情况
     */
    @Override
    public Object getCredentials() {
        return null;
    }

    /**
     * 获取主体信息
     *
     * 业务作用：返回认证主体的详细信息，此处直接返回JwtAuthenticationToken本身作为Principal
     * 业务规则：返回自身实例，调用方可通过该对象获取userId和username
     * 注意事项：调用方如需获取用户信息，需强转为JwtAuthenticationToken类型
     */
    @Override
    public Object getPrincipal() {
        return this;
    }

    /**
     * 获取用户名
     *
     * 业务作用：返回当前认证用户的用户名，用于Spring Security的权限表达式判断
     * 业务规则：返回值与JWT令牌中的username claim一致
     * 注意事项：username为用户登录名，非用户ID或昵称
     */
    @Override
    public String getName() {
        return username;
    }

    /** 令牌是否带 ADMIN 角色；token 为 null 或无权限时返回 false。 */
    public static boolean isAdmin(JwtAuthenticationToken token) {
        if (token == null || token.getAuthorities() == null) {
            return false;
        }
        return token.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

}
