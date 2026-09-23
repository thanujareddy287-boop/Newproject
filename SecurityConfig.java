package com.example.studentapp.security;

import com.example.studentapp.service.CustomUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          RateLimitFilter rateLimitFilter) {
        this.userDetailsService = userDetailsService;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    SecurityContextRepository contextRepository)
            throws Exception {

        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();

        http
            .cors(cors -> {})
            .csrf(csrfConfig -> csrfConfig
                    .csrfTokenRepository(csrf)
                    .ignoringRequestMatchers("/actuator/health"))
            .securityContext(context -> context.securityContextRepository(contextRepository))
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(3))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/index.html", "/login.html", "/register.html",
                            "/dashboard.html", "/css/**", "/js/**",
                            "/api/auth/register", "/api/auth/login", "/api/csrf",
                            "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/students").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/students/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/students/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasRole("ADMIN")
                    .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(204);
                    })
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
            .headers(headers -> headers
                    .contentTypeOptions(content -> {})
                    .frameOptions(frame -> frame.deny())
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives(
                                    "default-src 'self'; " +
                                    "script-src 'self'; " +
                                    "style-src 'self'; " +
                                    "img-src 'self' data:; " +
                                    "object-src 'none'; " +
                                    "base-uri 'self'; " +
                                    "frame-ancestors 'none'"))
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Same-origin frontend needs no CORS, but this allows a separately hosted
        // frontend when APP_FRONTEND_ORIGIN is supplied.
        configuration.setAllowedOrigins(List.of(
                System.getenv().getOrDefault("APP_FRONTEND_ORIGIN", "http://localhost:8080")
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
