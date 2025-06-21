package com.smartdocument.bookinventory.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

/**
 * OpenAPI configuration for the Book Inventory Service.
 *
 * This configuration class sets up comprehensive API documentation using OpenAPI 3.0
 * specification. It provides detailed information about the service, authentication,
 * available endpoints, and usage examples.
 *
 * Features configured:
 * - Service title, description, and version information
 * - Contact information for the development team
 * - Multiple server configurations (development and API base path)
 * - HTTP Basic Authentication scheme documentation
 * - Comprehensive API description with authentication details
 * - Support for external OpenAPI specification files
 *
 * The configuration includes detailed documentation about:
 * - Authentication requirements and credentials
 * - Available features and capabilities
 * - Server endpoints and their purposes
 * - Security schemes and requirements
 *
 * @property API_TITLE The title of the API documentation
 * @property API_DESCRIPTION Brief description of the API functionality
 * @property API_VERSION Current version of the API
 * @property CONTACT_NAME Name of the development team
 * @property SERVER_URL_DEV Development server URL
 * @property SERVER_DESC_DEV Description of the development server
 * @property SERVER_URL_API API base path URL
 * @property SERVER_DESC_API Description of the API base path
 * @property OPENAPI_YAML External OpenAPI specification file name
 */
@Configuration
class OpenApiConfig {
    companion object {
        const val API_TITLE = "Book Inventory Service API"
        const val API_DESCRIPTION = "REST API for managing book inventory operations"
        const val API_VERSION = "1.0.0"
        const val CONTACT_NAME = "Smart Document Team"
        const val SERVER_URL_DEV = "http://localhost:8081"
        const val SERVER_DESC_DEV = "Development server"
        const val SERVER_URL_API = "http://localhost:8081/api/v1"
        const val SERVER_DESC_API = "API base path"
        const val OPENAPI_YAML = "openapi.yaml"
    }

    private val logger: Logger = LoggerFactory.getLogger(OpenApiConfig::class.java)

    /**
     * Creates and configures the OpenAPI specification for the service.
     *
     * Builds a comprehensive OpenAPI document that includes:
     * - Service information (title, description, version)
     * - Contact information for the development team
     * - Multiple server configurations for different environments
     * - Security scheme definitions for HTTP Basic Authentication
     * - Detailed API description with authentication instructions
     *
     * The configuration provides clear documentation for developers
     * on how to authenticate and use the API endpoints.
     *
     * @return OpenAPI specification object with complete configuration
     */
    @Bean
    fun customOpenAPI(): OpenAPI {
        logger.info("Configuring OpenAPI documentation")

        return OpenAPI()
            .info(
                Info()
                    .title(API_TITLE)
                    .description("""
                        REST API for managing book inventory operations

                        ## Authentication
                        This API uses HTTP Basic Authentication. All endpoints require authentication.

                        **Default Credentials:**
                        - **Username:** `bookadmin`
                        - **Password:** `bookpass123`
                        - **Base64 Encoded:** `Ym9va2FkbWluOmJvb2twYXNzMTIz`

                        **Example Authorization Header:**
                        ```
                        Authorization: Basic Ym9va2FkbWluOmJvb2twYXNzMTIz
                        ```

                        ## Features
                        - Book CRUD operations (Create, Read, Update, Delete)
                        - Advanced search and filtering with multiple criteria
                        - Inventory management
                        - Genre management
                        - Pagination and sorting support
                    """.trimIndent())
                    .version(API_VERSION)
                    .contact(
                        Contact()
                            .name(CONTACT_NAME)
                    )
            )
            .addServersItem(
                Server()
                    .url(SERVER_URL_DEV)
                    .description(SERVER_DESC_DEV)
            )
            .addServersItem(
                Server()
                    .url(SERVER_URL_API)
                    .description(SERVER_DESC_API)
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "basicAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")
                            .description("HTTP Basic Authentication. Use the configured username and password. Default credentials: username=bookadmin, password=bookpass123")
                    )
            )
            .addSecurityItem(
                SecurityRequirement().addList("basicAuth")
            )
    }

    /**
     * Loads external OpenAPI specification from a YAML file.
     *
     * This method allows maintaining API documentation separately from code,
     * providing flexibility for complex API specifications. The external
     * specification can be used to supplement or override the programmatically
     * generated OpenAPI configuration.
     *
     * @return String content of the external OpenAPI YAML file, or null if file not found
     */
    fun loadExternalOpenApiSpec(): String? {
        return try {
            val resource = ClassPathResource(OPENAPI_YAML)
            StreamUtils.copyToString(resource.inputStream, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
