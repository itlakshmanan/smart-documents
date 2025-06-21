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

@Configuration
class OpenApiConfig {

    private val logger: Logger = LoggerFactory.getLogger(OpenApiConfig::class.java)

    @Bean
    fun customOpenAPI(): OpenAPI {
        logger.info("Configuring OpenAPI documentation")

        return OpenAPI()
            .info(
                Info()
                    .title("Order Management Service API")
                    .description("""
                        ## Overview
                        The Order Management Service provides comprehensive cart and order management capabilities for the Smart Documents online bookstore.

                        ### Key Features
                        - **Cart Management**: Add, update, remove, and clear cart items
                        - **Order Processing**: Create orders from cart with inventory validation
                        - **Payment Integration**: Simulated payment processing with success/failure handling
                        - **Order Status Tracking**: Complete order lifecycle management
                        - **Inventory Integration**: Real-time inventory validation with BookInventoryService

                        ### Business Flow
                        1. **Cart Operations**: Users can manage their shopping cart with various items
                        2. **Checkout Process**: Cart checkout validates inventory and creates orders
                        3. **Payment Processing**: Simulated payment with automatic inventory updates
                        4. **Order Management**: Track and update order status throughout the lifecycle

                        ### Authentication
                        All API endpoints require HTTP Basic Authentication using the configured credentials.

                        ### Error Handling
                        The service provides detailed error responses with appropriate HTTP status codes and descriptive messages.
                    """.trimIndent())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Smart Documents Team")
                            .email("support@smartdocuments.com")
                            .url("https://smartdocuments.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8082")
                        .description("Local Development Server"),
                    Server()
                        .url("https://order-management-service.smartdocuments.com")
                        .description("Production Server")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "basicAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")
                            .description("HTTP Basic Authentication. Use the configured username and password.")
                    )
            )
            .addSecurityItem(
                SecurityRequirement().addList("basicAuth")
            )
    }
}
