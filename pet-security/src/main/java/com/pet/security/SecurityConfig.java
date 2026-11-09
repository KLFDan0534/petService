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

    /**
     * 【安全配置构造器】
     *
     * 业务作用：注入 JWT 认证过滤器，供安全过滤器链注册。
     *
     * 调用场景：Spring 容器启动时由依赖注入自动调用。
     *
     * 调用链：Spring IoC → 构造器 → 赋值 jwtAuthenticationFilter。
     *
     * 数据处理：仅字段赋值。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无；过滤器 Bean 缺失时启动失败。
     *
     * 注意事项：该过滤器在 securityFilterChain 中被插到 UsernamePasswordAuthenticationFilter 之前。
     */
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
         * 【非 API 路径匹配】
         *
         * 业务作用：判定请求 URI 是否属于非 API 路径，用于放行静态页面与前端路由。
         *
         * 调用场景：安全过滤器链对未显式匹配的请求做兜底判断时调用。
         *
         * 调用链：authorizeHttpRequests → NonApiRequestMatcher.matches → 前缀判断。
         *
         * 数据处理：取 requestURI，排除 /api/、/swagger-ui/、/api-docs/、/v3/ 四类前缀。
         *
         * 业务规则：不属于上述前缀的路径视为前端资源，直接放行。
         *
         * 状态影响：无。
         *
         * 异常情况：无。
         *
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
