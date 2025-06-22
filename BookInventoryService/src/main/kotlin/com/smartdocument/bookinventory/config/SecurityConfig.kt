package com.smartdocument.bookinventory.config

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
 * Security configuration for the Book Inventory Service.
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
 * - Username: bookadmin
 * - Password: bookpass123
 * - Role: ADMIN
 *
 * @property username Configurable username for authentication (default: bookadmin)
 * @property password Configurable password for authentication (default: bookpass123)
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    private val logger: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${book.inventory.service.username}")
    private lateinit var username: String

    @Value("\${book.inventory.service.password}")
    private lateinit var password: String

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * Sets up the security rules for different endpoints:
     * - Actuator endpoints (/actuator/) are publicly accessible for health monitoring
     * - Swagger UI and API documentation endpoints are publicly accessible
     * - All other endpoints require HTTP Basic Authentication
     * - CSRF protection is disabled for stateless API design
     *
     * @param http HttpSecurity object to configure
     * @return SecurityFilterChain with configured security rules
     */
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
