package com.pet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 *
 * 业务作用：继承OncePerRequestFilter，在每个HTTP请求中拦截并解析Authorization头中的JWT令牌，
 *          完成令牌黑名单校验、令牌有效性验证、用户身份提取和权限注入
 *
 * 调用场景：Spring Security过滤器链中，在UsernamePasswordAuthenticationFilter之前执行
 *
 * 调用链：请求 → SecurityFilterChain ↓ JwtAuthenticationFilter ↓ UsernamePasswordAuthenticationFilter
 *
 * 数据处理：请求头Authorization → 提取Bearer Token → 黑名单校验 → JWT解析 → 构建Authentication → 设置SecurityContext
 *
 * 业务规则：每个请求只执行一次；不拦截白名单URL（由SecurityConfig配置）；黑名单校验依赖Redis
 *
 * 状态影响：修改SecurityContextHolder，注入当前请求的认证信息
 *
 * 异常情况：令牌在黑名单中返回401；令牌无效则不设置认证信息（放行后由SecurityConfig的认证入口点处理）
 *
 * 注意事项：Redis连接异常时黑名单校验静默失败（降级为仅做JWT校验），避免影响核心认证流程
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 执行JWT认证过滤
     *
     * 业务作用：从HTTP请求中提取JWT令牌，依次执行黑名单校验、令牌有效性校验、用户身份提取，最终将认证信息注入SecurityContext
     *
     * 调用场景：所有经过Spring Security过滤器链的HTTP请求
     *
     * 调用链：HttpServletRequest ↓ doFilterInternal ↓ JwtUtil.validateToken ↓ JwtUtil.getUserIdFromToken ↓ SecurityContextHolder
     *
     * 数据处理：解析请求头Authorization → 提取Bearer Token → Redis黑名单检查 → JWT验签 → 提取userId/roles/username → 构建JwtAuthenticationToken → 设置SecurityContext
     *
     * 业务规则：无Bearer令牌时直接放行；令牌在黑名单中返回401 JSON；JWT校验失败仅放行不设置认证；校验成功注入完整认证信息到SecurityContext
     *
     * 状态影响：修改SecurityContextHolder中的SecurityContext，设置当前线程的认证信息
     *
     * 异常情况：Redis连接异常时黑名单校验降级；JWT令牌过期/无效时静默放行（不设认证）
     *
     * 注意事项：无论是否设置认证信息，最终都必须调用filterChain.doFilter确保请求继续传递
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String blacklisted = redisTemplate.opsForValue().get("blacklist:token:" + token);
                if (blacklisted != null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token expired\"}");
                    return;
                }
            } catch (Exception ignored) {
            }
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                List<String> roles = jwtUtil.getRolesFromToken(token);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());
                String username = jwtUtil.parseToken(token).get("username", String.class);
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(userId, username, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
