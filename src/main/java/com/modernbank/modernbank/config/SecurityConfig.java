
package com.modernbank.modernbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {

    @Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler) throws Exception {

    return http
            .csrf(csrf -> csrf.disable())

.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        HttpMethod.POST,
        "/api/v1/auth/register",
        "/api/v1/auth/login"
    ).permitAll()
    .requestMatchers(
        "/actuator/health",
        "/actuator/health/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**"
    ).permitAll()
    .anyRequest().authenticated()
)

            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(Customizer.withDefaults())
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(accessDeniedHandler)
)
            .build();
}
}
