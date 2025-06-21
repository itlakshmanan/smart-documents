package com.smartdocument.ordermanagement.config

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
        const val API_TITLE = "Order Management Service API"
        const val API_DESCRIPTION = "REST API for managing customer carts and orders"
        const val API_VERSION = "1.0.0"
        const val CONTACT_NAME = "Smart Document Team"
        const val SERVER_URL_DEV = "http://localhost:8082"
        const val SERVER_DESC_DEV = "Development server"
        const val SERVER_URL_API = "http://localhost:8082/api/v1"
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
                    .description(API_DESCRIPTION)
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
                            .description("HTTP Basic Authentication. Use the configured username and password. Default credentials: username=orderadmin, password=orderpass123")
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
