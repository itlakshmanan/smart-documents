package com.smartdocument.ordermanagement.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Configuration
@EnableWebSecurity
class SecurityConfig {

    private val logger: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${order.management.service.username:orderadmin}")
    private lateinit var username: String

    @Value("\${order.management.service.password:orderpass123}")
    private lateinit var password: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info("Configuring security filter chain with basic authentication")

        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
            }
            .httpBasic { }

        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        logger.info("Creating in-memory user details service with username: {}", username)

        val userDetails: UserDetails = User.builder()
            .username(username)
            .password(passwordEncoder().encode(password))
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(userDetails)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
