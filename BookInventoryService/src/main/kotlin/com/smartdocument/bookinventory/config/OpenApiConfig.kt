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
     * Load external OpenAPI specification from YAML file
     * This allows us to maintain API documentation separately from code
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
