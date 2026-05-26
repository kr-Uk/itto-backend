package com.itto.backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itto.backend.presentation.dto.common.ApiResponse;
import com.itto.backend.presentation.security.JwtAuthFilter;
import com.itto.backend.presentation.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> headers
                        .contentTypeOptions(opts -> {})
                        .frameOptions(frame -> frame.deny())
                        .cacheControl(cache -> {})
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).denyAll()
                        .requestMatchers(
                                "/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/system/version",
                                "/api/v1/hobbies",
                                "/api/v1/nickname/validate",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 미인증 요청은 401(UNAUTHORIZED) — FE가 401에서만 토큰 자동 갱신을 시도하므로 403이 아닌 401을 명시
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.error(401, "인증이 필요합니다", "UNAUTHORIZED")
                            );
                        })
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtProvider);
    }
}
