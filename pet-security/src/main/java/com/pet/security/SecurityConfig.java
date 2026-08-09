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
 *
 * 调用场景：Spring Boot启动时自动加载，应用整个安全配置
 *
 * 调用链：Spring Security自动装配 ↓ SecurityConfig ↓ SecurityFilterChain
 *
 * 数据处理：配置HttpSecurity对象 → 定义URL匹配规则 → 注册自定义过滤器 → 构建SecurityFilterChain
 *
 * 业务规则：无状态会话(STATELESS)；公开接口（登录、注册、静态资源、GET查询接口）无需认证；其余请求需认证
 *
 * 状态影响：配置整个应用的安全策略
 *
 * 异常情况：认证失败时返回401状态码
 *
 * 注意事项：CORS和CSRF均禁用（适用于前后端分离架构）；NonApiRequestMatcher确保非API路径放行（如静态页面）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置安全过滤器链
     *
     * 业务作用：定义Spring Security的过滤器链，配置URL权限规则、禁用CSRF/CORS、设置无状态会话、注册JWT过滤器
     *
     * 调用场景：Spring Boot启动时，SecurityAutoConfiguration自动调用此Bean创建过滤器链
     *
     * 调用链：Spring Security ↓ SecurityFilterChain ↓ JwtAuthenticationFilter ↓ UsernamePasswordAuthenticationFilter
     *
     * 数据处理：从HttpSecurity构建SecurityFilterChain → 配置各URL匹配器的权限规则
     *
     * 业务规则：登录/注册/api/auth/**完全公开；GET查询接口对商家/服务/评价/分类等公开；文件服务公开；非API路径放行；其余需认证
     *
     * 状态影响：无，返回配置好的SecurityFilterChain实例
     *
     * 异常情况：配置错误时抛出Exception，由Spring框架处理
     *
     * 注意事项：JwtAuthenticationFilter在UsernamePasswordAuthenticationFilter之前执行，确保JWT优先认证
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/admin/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/merchants/**", "/api/keepers/**", "/api/services/**", "/api/ratings/**", "/api/categories/**", "/api/service-categories/**", "/api/notices/active", "/api/order-events/stream", "/api/chat-events/stream", "/api/notification-events/stream").permitAll()
                .requestMatchers("/api/files/**").permitAll()
                .requestMatchers(new NonApiRequestMatcher()).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class NonApiRequestMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return !uri.startsWith("/api/") && !uri.startsWith("/swagger-ui/") && !uri.startsWith("/api-docs/")
                && !uri.startsWith("/v3/") && !uri.startsWith("/h2-console/");
        }
    }

    /**
     * 密码编码器
     *
     * 业务作用：提供BCrypt强哈希算法对用户密码进行加密存储和比对验证
     *
     * 调用场景：用户注册时加密密码、用户登录时验证密码、修改密码时加密新密码
     *
     * 调用链：UserService.register/login ↓ passwordEncoder.encode/matches ↓ BCryptPasswordEncoder
     *
     * 数据处理：明文密码 → BCrypt哈希 → 密文存储；登录时明文密码与密文进行matches比对
     *
     * 业务规则：使用BCrypt算法，每次加密生成不同的salt，相同密码加密结果不同
     *
     * 状态影响：无，仅提供编码和匹配能力
     *
     * 异常情况：无
     *
     * 注意事项：BCrypt是自适应哈希算法，安全性高但计算较慢，适合密码存储场景
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
