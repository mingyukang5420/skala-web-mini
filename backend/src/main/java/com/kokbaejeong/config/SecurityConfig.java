package com.kokbaejeong.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kokbaejeong.security.JwtAuthenticationFilter;
import com.kokbaejeong.security.JwtTokenProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                                                    ObjectMapper objectMapper) {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtAuthenticationFilter(jwtTokenProvider, objectMapper));
        registrationBean.addUrlPatterns("/admin/*");
        return registrationBean;
    }
}
