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

/**
 * Security configuration for the Order Management Service.
 *
 * This configuration class sets up HTTP Basic Authentication for all API endpoints
 * except for health checks and documentation endpoints. It provides a secure
 * authentication mechanism using in-memory user management.
 *
 * Security features configured:
 * - HTTP Basic Authentication for all API endpoints
 * - CSRF protection disabled (stateless API)
 * - Public access to actuator endpoints for health monitoring
 * - Public access to Swagger UI and API documentation
 * - BCrypt password encoding for secure password storage
 * - In-memory user management with configurable credentials
 *
 * Default credentials (configurable via properties):
 * - Username: orderadmin
 * - Password: orderpass123
 * - Role: ADMIN
 *
 * @property username Configurable username for authentication (default: orderadmin)
 * @property password Configurable password for authentication (default: orderpass123)
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    private val logger: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${order.management.service.username}")
    private lateinit var username: String

    @Value("\${order.management.service.password}")
    private lateinit var password: String

    /**
     * Builds and configures the HTTP security filter chain.
     *
     * This method establishes the security configuration for incoming HTTP requests.
     * It defines which endpoints are publicly accessible and which require authentication.
     * The configuration includes:
     * - Disabling CSRF protection for stateless API design
     * - Configuring endpoint access rules
     * - Enabling HTTP Basic Authentication
     *
     * @param http The HttpSecurity builder to configure
     * @return A configured SecurityFilterChain instance
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        logger.info("Configuring security filter chain with basic authentication")

        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
            }
            .httpBasic { }

        return http.build()
    }

    /**
     * Creates an in-memory user details service for authentication.
     *
     * Sets up a single user with ADMIN role using the configured
     * username and password. The password is encoded using BCrypt
     * for secure storage.
     *
     * @return UserDetailsService with configured user
     */
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

    /**
     * Provides BCrypt password encoder for secure password hashing.
     *
     * BCrypt is used for password encoding as it provides strong
     * cryptographic hashing with built-in salt generation.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
