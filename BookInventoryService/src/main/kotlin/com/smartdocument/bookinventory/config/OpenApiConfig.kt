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

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Book Inventory Service API")
                    .description("""
                        REST API for managing book inventory in the Smart Documents bookstore system.

                        ## Features
                        - Book CRUD operations (Create, Read, Update, Delete)
                        - Advanced search and filtering
                        - Inventory management
                        - Genre management

                        ## Authentication
                        This API uses HTTP Basic Authentication. Include the Authorization header with each request:
                        ```
                        Authorization: Basic Ym9va2FkbWluOmJvb2twYXNzMTIz
                        ```

                        **Default Credentials:**
                        - Username: `bookadmin`
                        - Password: `bookpass123`
                    """.trimIndent())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Smart Documents Team")
                            .email("support@smartdocuments.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8081").description("Local Development Server"),
                    Server().url("http://book-inventory-service:8081").description("Docker Container Server")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("basicAuth"))
            .components(
                Components()
                    .addSecuritySchemes(
                        "basicAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")
                            .description("HTTP Basic Authentication")
                    )
            )
    }
}
