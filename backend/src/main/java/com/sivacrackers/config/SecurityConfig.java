package com.sivacrackers.config;

import com.sivacrackers.security.AuthCookies;
import com.sivacrackers.security.JwtAuthenticationFilter;
import com.sivacrackers.security.JwtService;
import com.sivacrackers.security.RestAccessDeniedHandler;
import com.sivacrackers.security.RestAuthenticationEntryPoint;
import com.sivacrackers.user.UserRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security for the REST API.
 *
 * <ul>
 *   <li><b>Stateless</b>: no server session; identity comes from the HttpOnly auth cookie (JWT filter added in Phase 4.5).</li>
 *   <li><b>CSRF on</b> for every state-changing request (double-submit cookie: readable {@code XSRF-TOKEN}
 *       cookie, echoed in the {@code X-XSRF-TOKEN} header). Both cookies are Secure and SameSite.</li>
 *   <li><b>JSON</b> 401/403 responses, no login page, no HTTP Basic pop-up.</li>
 *   <li><b>CORS</b> only for origins explicitly listed in {@code CORS_ALLOWED_ORIGINS} (none by default).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private final AppProperties props;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtService jwtService;
    private final AuthCookies authCookies;
    private final UserRepository users;

    public SecurityConfig(AppProperties props,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          JwtService jwtService,
                          AuthCookies authCookies,
                          UserRepository users) {
        this.props = props;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtService = jwtService;
        this.authCookies = authCookies;
        this.users = users;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Session cookie → logged-in user for this request (see JwtAuthenticationFilter)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, authCookies, users),
                        UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .spa() // SPA-friendly token handling (raw token in cookie, BREACH-safe in responses)
                        .csrfTokenRepository(csrfTokenRepository))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // custom logout endpoint clears the cookie (Phase 4.5)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(f -> f.deny()))
                .authorizeHttpRequests(auth -> auth
                        // Public, read-only website data and the health check
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/public/**").permitAll()
                        // "Who am I" needs a session; login / register / logout / CSRF bootstrap are open
                        // (CSRF is still enforced on every POST)
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        // API documentation – only served when springdoc is enabled (dev profile)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Staff-only area: finer role checks use @PreAuthorize on each endpoint
                        .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "CASHIER")
                        // Everything else under /api needs a logged-in user; anything outside /api is denied
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll());
        return http.build();
    }

    /**
     * XSRF-TOKEN cookie: readable by the frontend (required for double-submit), Secure, SameSite, site-wide path.
     * A bean so the login controller can issue a fresh token after login/logout.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(CSRF_COOKIE);
        repository.setHeaderName(CSRF_HEADER);
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie
                .secure(props.cookie().secure())
                .sameSite(props.cookie().sameSite()));
        return repository;
    }

    /** Cross-origin access only for exact origins from CORS_ALLOWED_ORIGINS; with credentials; never wildcards. */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = props.cors().allowedOrigins();
        if (!origins.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("Content-Type", CSRF_HEADER));
            config.setAllowCredentials(true);
            config.setMaxAge(Duration.ofHours(1));
            source.registerCorsConfiguration("/api/**", config);
        }
        return source;
    }

    /** BCrypt with cost 12 for all stored passwords. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Not used for logging in (AuthService checks passwords; JwtAuthenticationFilter restores sessions).
     * Declaring it stops Spring Boot from creating a default in-memory user and printing its generated
     * password to the log – there is no default account of any kind.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Form/basic login is disabled");
        };
    }
}
