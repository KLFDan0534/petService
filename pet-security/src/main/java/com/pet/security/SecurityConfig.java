package com.pet.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security安全配置
 *
 * 业务作用：配置全局HTTP安全策略，包括CORS、CSRF、会话管理、URL权限规则、JWT过滤器注册和密码编码器
 * 业务规则：无状态会话(STATELESS)；公开接口（登录、注册、静态资源、GET查询接口）无需认证；其余请求需认证
 * 异常情况：认证失败时返回401状态码
 * 注意事项：CORS和CSRF均禁用（适用于前后端分离架构）；NonApiRequestMatcher确保非API路径放行（如静态页面）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 业务作用：注入 JWT 认证过滤器，供安全过滤器链注册。
     * 异常情况：无；过滤器 Bean 缺失时启动失败。
     * 注意事项：该过滤器在 securityFilterChain 中被插到 UsernamePasswordAuthenticationFilter 之前。
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置安全过滤器链
     *
     * 业务作用：定义Spring Security的过滤器链，配置URL权限规则、禁用CSRF/CORS、设置无状态会话、注册JWT过滤器
     * 业务规则：登录/注册/api/auth/**完全公开；GET查询接口对商家/服务/评价/分类等公开；文件服务公开；非API路径放行；其余需认证
     * 异常情况：配置错误时抛出Exception，由Spring框架处理
     * 注意事项：JwtAuthenticationFilter在UsernamePasswordAuthenticationFilter之前执行，确保JWT优先认证
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // 无状态会话，认证完全依赖 JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/admin/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // 公开浏览类 GET 接口免登录
                .requestMatchers(HttpMethod.GET, "/api/merchants/**", "/api/keepers/**", "/api/services/**", "/api/ratings/**", "/api/categories/**", "/api/service-categories/**", "/api/notices/active", "/api/geo/config", "/api/order-events/stream", "/api/chat-events/stream", "/api/notification-events/stream").permitAll()
                .requestMatchers("/api/files/**").permitAll()
                // 非 /api 前缀视为前端页面直接放行
                .requestMatchers(new NonApiRequestMatcher()).permitAll()
                .anyRequest().authenticated()
            )
            // JWT 过滤器插在账号密码认证之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class NonApiRequestMatcher implements RequestMatcher {
        /**
         * 业务作用：判定请求 URI 是否属于非 API 路径，用于放行静态页面与前端路由。
         * 业务规则：不属于上述前缀的路径视为前端资源，直接放行。
         * 注意事项：放行范围较宽，新增后端接口必须使用 /api/ 前缀，否则会绕过认证。
         */
        @Override
        public boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return !uri.startsWith("/api/") && !uri.startsWith("/swagger-ui/") && !uri.startsWith("/api-docs/")
                && !uri.startsWith("/v3/");
        }
    }

    /**
     * 密码编码器
     *
     * 业务作用：提供BCrypt强哈希算法对用户密码进行加密存储和比对验证
     * 调用场景：用户注册时加密密码、用户登录时验证密码、修改密码时加密新密码
     * 业务规则：使用BCrypt算法，每次加密生成不同的salt，相同密码加密结果不同
     * 注意事项：BCrypt是自适应哈希算法，安全性高但计算较慢，适合密码存储场景
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
