package com.lms.config;

import com.lms.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless JWT security.
 *
 *   request -> JwtAuthFilter (Bearer token -> user from DB) -> authorization rules below
 *
 * Roles are plain authorities named "ADMIN" and "LIBRARIAN" (see {@link AdminUserDetails}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AdminUserDetailsService userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /** When true the API itself enforces ADMIN / LIBRARIAN roles (mirrors the Angular route guards). */
    @Value("${app.security.enforce-roles:false}")
    private boolean enforceRoles;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AdminUserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())                                   // stateless API, no cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // no / invalid / expired token -> 401 (the Angular interceptor signs the user out on 401)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/auth/**", "/h2-console/**", "/swagger-ui/**",
                        "/v3/api-docs/**", "/swagger-ui.html", "/actuator/health/**").permitAll();
                auth.requestMatchers("/api/admin/**").hasAuthority("ADMIN");
                if (enforceRoles) {
                    // Member administration: ADMIN writes; both roles may look members up (issue/return desk)
                    auth.requestMatchers(HttpMethod.GET, "/api/members/**").hasAnyAuthority("ADMIN", "LIBRARIAN");
                    auth.requestMatchers("/api/members/**").hasAuthority("ADMIN");
                    // Catalogue and circulation: LIBRARIAN writes; both roles may read
                    auth.requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyAuthority("ADMIN", "LIBRARIAN");
                    auth.requestMatchers("/api/books/**").hasAuthority("LIBRARIAN");
                    auth.requestMatchers(HttpMethod.POST, "/api/issue", "/api/submit").hasAuthority("LIBRARIAN");
                }
                auth.anyRequest().authenticated();
            })
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))   // H2 console frames itself
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JwtAuthFilter is a @Component, which Spring Boot would also register as a plain servlet filter.
     * It belongs only in the security chain above, so switch the extra registration off.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
