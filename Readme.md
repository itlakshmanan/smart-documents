# Bookstore Management System (HLD & LLD)

## 1. Project Scope

This project delivers a *microservices*-based online bookstore with two Spring Boot services: **Book Inventory** and **Order Management**. It covers book catalog management (metadata and inventory tracking) and customer shopping features (cart, orders, payment simulation). The system uses Kotlin, Spring Boot, and a shared MySQL database. All APIs are protected by Basic Authentication and the services are containerized using Docker Compose. In scope are: catalog CRUD, search/filter, cart checkout, order processing, and simulated payment confirmation.

## 2. Background and Objective

Modern e-commerce platforms often adopt microservices to handle complexity. By splitting functionality into separate services, the system becomes more modular and scalable. For example, an online bookstore can be decomposed into bounded contexts like Inventory and Orders. In a pure microservices approach each service typically has its own database; here we follow a **schema-per-service** pattern on a single MySQL instance to isolate data while sharing infrastructure. The objective is to design a scalable, maintainable bookstore system that meets the requirements, using the specified tech stack and demonstrating industry best practices.

## 3. Functional Requirements

- **Book Inventory Service** (Catalog Management)
  
  - **Add/Update/Delete Books**: Admin can create, modify, or remove book records with metadata (title, author, genre, ISBN, price).
  
  - **Inventory Tracking**: Maintain stock count for each book; update stock on purchases or restocking.
  
  - **Search & Filter**: Support querying books by title, author, genre, ISBN, language, publisher, published date with pagination and sorting.
  
  - **Get Book Details**: Retrieve book metadata and availability.
  
  - **Service API**: Expose REST endpoints (e.g. `GET /books`, `POST /books`) for these operations.

- **Order Management Service** (Cart & Checkout)
  
  - **Shopping Cart**: Allow adding/removing books to a user’s cart. Cart data is temporary until checkout.
  
  - **Place Order**: On checkout, create an order record with items and total amount.
  
  - **Order Status Tracking**: Manage order life cycle (e.g. *Pending*, *Confirmed*, *Shipped*).
  
  - **Payment Simulation**: Simulate payment processing – e.g. automatically confirm payment after a delay.
  
  - **Inventory Reservation**: Check and reserve stock when orders are placed, and decrement stock on payment.
  
  - **Order API**: REST endpoints like `POST /orders`, `GET /orders/{id}`, including linking to Inventory for stock checks.

These functional requirements align with common e-commerce patterns for inventory and order subsystems.

## 4. Non-Functional Requirements

- **Performance**: APIs should respond in under 200ms under normal load to ensure a smooth user experience.

- **Scalability**: Microservices should scale independently to handle traffic spikes (e.g. during holiday sales).

- **Security**: All APIs use Basic Authentication. Sensitive operations are protected and HTTPS is assumed in production.

- **Reliability/Availability**: Target 99.9% uptime. Microservice isolation improves fault tolerance. DB backups are essential due to shared DB usage.

- **Maintainability**: Modular Kotlin + Spring Boot code with clear structure. Dockerization ensures consistent dev/prod environments.

- **Usability & Monitoring**: Services should log key events and support metrics for health monitoring.

- **Portability/Deployment**: Docker Compose enables fast setup across environments with minimal config overhead.

## 5. High-Level Architecture

At a high level, two Spring Boot microservices (Book and Order) run in separate Docker containers. Both services expose REST APIs and connect to the shared MySQL database (using separate schemas to simulate database-per-service isolation). Clients (web/mobile) communicate with these services over HTTP(S), providing Basic Auth credentials. The architecture is designed to leverage containers for deployment: as Atlassian notes, “containers are the primary means of deploying microservices” and tools like Docker streamline resource allocation. The diagram below illustrates the main components and interactions:

```mermaid
flowchart LR
  %% Client
  subgraph "Client"
    UI["User / Client"]
  end

  %% Security
  subgraph "Security"
    AUTH(("Basic Auth"))
  end

  %% Book Inventory
  subgraph "Book Inventory"
    INV["Book Service"]
    DB_INV["Book DB\nMySQL"]
  end

  %% Order Management
  subgraph "Order Management"
    ORD["Order Service"]
    DB_ORD["Order DB\nMySQL"]
  end

  %% Service authentication
  INV -- Uses --> AUTH
  ORD -- Uses --> AUTH

  %% Client access
  UI -- "Book Info Request" --> INV
  UI -- "Order Placement" --> ORD

  %% DB access
  INV -- "Read/Write" --> DB_INV
  ORD -- "Read/Write" --> DB_ORD

  %% Inter-service communication
  ORD -- "Fetch Book Info" --> INV
```

- **Book Inventory Service** – Manages the book catalog and stock.

- **Order Management Service** – Handles shopping carts, orders, and payment simulation.

- **MySQL Database** – One instance hosting two schemas (e.g. `books` and `orders`) to isolate data.

- **Basic Auth** – Each service is configured with Spring Security to require HTTP Basic credentials.

This decomposition follows microservices principles: each service runs autonomously with its own logic and data access, allowing independent deployment and scaling.

### 5.1 Event-Driven Architecture with RabbitMQ

To enable asynchronous, decoupled communication between microservices, the system adopts an event-driven architecture using **RabbitMQ** as a message broker. The **OrderManagementService** is responsible for publishing domain events (such as order placement and order cancellation) to RabbitMQ when key actions occur. The **BookInventoryService** subscribes to these event queues and reacts accordingly—for example, decrementing inventory when an order is placed, or restoring stock when an order is cancelled.

This approach allows the OrderManagementService and BookInventoryService to operate independently, improving scalability and resilience. Services do not need to wait for synchronous responses, and failures in one service do not directly impact the other. RabbitMQ is configured via Spring Boot in both services, and Docker Compose provisions the broker for local development and testing.

**Key Event Flows:**

- When an order is placed, the OrderManagementService publishes an `OrderPlacedEvent`. The BookInventoryService listens for this event and decrements the relevant book's stock.
- If an order is cancelled, the OrderManagementService publishes an `OrderCancelledEvent`, and the BookInventoryService restores the stock for the cancelled items.

This event-driven pattern supports eventual consistency and loose coupling between microservices, making the system more robust and adaptable to future changes.

```mermaid
sequenceDiagram
    participant Client
    participant OrderService as OrderManagementService
    participant MQ as RabbitMQ
    participant InventoryService as BookInventoryService

    Client->>OrderService: Place Order Request
    OrderService->>OrderService: Process Order
    OrderService-->>MQ: Publish OrderPlacedEvent
    MQ-->>InventoryService: Deliver OrderPlacedEvent
    InventoryService->>InventoryService: Decrement Stock

    Note over Client, InventoryService: -- Later --

    Client->>OrderService: Cancel Order Request
    OrderService->>OrderService: Process Cancellation
    OrderService-->>MQ: Publish OrderCancelledEvent
    MQ-->>InventoryService: Deliver OrderCancelledEvent
    InventoryService->>InventoryService: Restore Stock
```

## 6. Low-Level Flow Design

### 6.1 Order Flow

Below is a **sequence diagram** capturing a typical *order placement* workflow:

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant OrderManagementService
    participant OrderDB
    participant BookInventoryService
    participant BookDB
    participant RabbitMQ

    User->>OrderManagementService: Place Order (bookId, qty)
    OrderManagementService->>BookInventoryService: GET /books/{id}
    BookInventoryService->>BookDB: Fetch Book Info
    BookDB-->>BookInventoryService: Return Book Info
    BookInventoryService-->>OrderManagementService: Return book details + stock

    OrderManagementService->>OrderDB: Save Order with Status = PENDING
    OrderManagementService->>OrderManagementService: Simulate Payment
    OrderManagementService->>OrderDB: Update Order Status = CONFIRMED

    OrderManagementService-->>RabbitMQ: Publish OrderPlacedEvent
    RabbitMQ-->>BookInventoryService: Deliver OrderPlacedEvent
    BookInventoryService->>BookDB: Decrement Stock

    OrderManagementService-->>User: Created (Order Confirmed)

```

### 6.2 Book Search + Stock Lookup

Search and filter handled via query parameters.

```mermaid
sequenceDiagram
    participant User
    participant BookService
    participant BookDB

    User->>BookService: GET /books?author=Kumar
    BookService->>BookDB: SELECT * WHERE author = ?
    BookDB-->>BookService: book list
    BookService-->>User: book response
```

## 7. Database Design Overview

We use MySQL with a *schema-per-service* approach. For example, one schema (`books`) holds book data, and another (`orders`) holds order data. This isolates each service's tables while using one DB server. Suggested tables include:

- **`books`** (Inventory schema): `(book_id PK, title, author, genre, isbn, price, stock_qty, language, publisher, published_date, created_at, updated_at)` – stores book metadata and stock level.

- **`orders`** (Orders schema): `(order_id PK, user_id, total_amount, status, created_at, updated_at)` – one row per order.

- **`order_items`** (Orders schema): `(item_id PK, order_id FK, book_id FK, quantity, unit_price)` – items in each order.

- **`cart_items`** (Orders schema, optional): `(user_id, book_id FK, quantity)` – temporary cart contents. Could be replaced by pending orders.

In each table, foreign keys link data (e.g. `order_items.book_id -> books.book_id`). We follow relational normalization rules (no duplicate book info in orders – we store price at time of order). The **database-per-service** pattern says each microservice should have its own schema or tables. As noted in the FTGO example, each service can have unique credentials to its own schema on a shared MySQL server. In deployment, each Spring Boot service would connect with a URL like `jdbc:mysql://db:3306/books` or `.../orders`, using separate database users for isolation.

## 8. API Security Design (Basic Auth)

We use **HTTP Basic Authentication** for all service APIs. In Spring Boot/Security, this is enabled by configuring the security filter chain to require authentication and calling `.httpBasic()`. For example:

```java
http.authorizeRequests().anyRequest().authenticated()
    .and().httpBasic();
```

Authorized clients must include an `Authorization` header with each request. The header value is `"Basic <token>"` where `<token>` is Base64-encoded `username:password`. For instance, the header might be:

```
Authorization: Basic dXNlcjpwYXNzd29yZA==
```

(where `dXNlcjpwYXNzd29yZA==` is the Base64 of `user:password`).

- **User Store**: For this project, credentials can be hard-coded or stored in-memory (via `InMemoryUserDetailsManager`) or in the `users` table.

- **Access Control**: All endpoints require authentication. We might allow an unauthenticated health-check or public endpoints (`/health`) if needed by adding `.antMatchers("/health").permitAll()`.

- **Security Note**: Basic Auth sends credentials with each request. In production, we would use HTTPS to encrypt credentials in transit.

Using Basic Auth keeps the design simple while securing the APIs. Spring's implementation handles the challenge/response flow and decoding as shown in the documentation.

## 9. AI Tools Utilization Summary

- **Cursor** – Used as an AI coding assistant to autocomplete code snippets and refactor Kotlin/Spring Boot code during development. It also assisted in generating unit tests, resolving compiler issues, and scaffolding service layers quickly.

- **ChatGPT** – Leveraged to gather information on best practices and to help draft this documentation. It helped structure the design, suggest architectural patterns (e.g. schema-per-service, CAP theorem trade-offs), and refine explanations in Markdown.

- **Warp AI Terminal** – Used to run and debug CLI commands (e.g., `docker-compose`, `gradlew`, and test runs) with intelligent AI command correction and autocomplete. It enhanced development speed and reduced terminal errors.

- **Mermaid** – Used to generate architecture and sequence diagrams in Markdown using textual definitions. It simplified the visualization of service interactions, data flows, and request lifecycles directly in the README without external tools.

## 10. Deployment Overview (Docker Compose)

The entire system is orchestrated via a **`docker-compose.yml`**. Docker Compose allows defining multiple containers (services, networks, volumes) in one YAML file. Key points:

- **Services**: We define four services: `book-inventory-service` (Spring Boot app), `order-management-service` (Spring Boot app), `mysql` and `rabbitmq`.

- **Image/Build**: Each Spring Boot service can be built into a Docker image (e.g. using a Dockerfile), or built on-the-fly via `build:`. The MySQL service uses an official MySQL image.

- **Environment Variables**: Database connection details (JDBC URL, user, password) are passed to each service container via environment settings. Example: `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/books`.

- **Dependencies**: Use `depends_on` so that the database container starts before the services.

- **Networks**: All services share a Docker network, allowing them to communicate by container name (`mysql` and `rabbitmq`).

- **Volumes**: Mount a volume for MySQL data to persist data across restarts.

- **Startup**: Running `docker-compose up -d` will start all containers (BookInventoryService, OrderManagementService, MySQL) in one command. Docker Compose ensures the correct order and networking.

To start invidual containers use below commands:

```yaml
docker-compose up -d mysql
docker-compose up -d rabbitmq
docker-compose up book-inventory-service
docker-compose up order-management-service
```

## 11. Scalability and Maintainability Considerations

- **Independent Scaling**: Each microservice can be scaled horizontally using Docker (e.g., `--scale book-inventory-service=3`). This supports traffic spikes by adding instances behind a load balancer.

- **Vertical Scaling**: Less common, but containers can be given more resources (CPU/RAM) when needed.

- **Database Bottlenecks**: MySQL can limit throughput. In production, we can add read replicas, caching (e.g., Redis), or adopt managed DB solutions. Schema-per-service reduces cross-service coupling.

- **Database Migrations**: Flyway is used to manage DB schema changes safely and consistently across environments. Versioned SQL migrations help avoid drift and ensure repeatable deployments.

- **Fault Tolerance**: Services are isolated—failures in one won’t impact others. Docker restart policies and health checks improve resilience.

- **Modularity**: Changes to one service (e.g., book schema) won’t affect the other. Microservices enable faster iterations and safer deployments.

- **CI/CD Ready**: Each service can have its own pipeline. Shared libraries (like DTOs) and well-defined APIs (via OpenAPI) simplify maintenance.

- **Observability**: Use **AWS CloudWatch** for centralized logging, metrics, and alarms across microservices. Integrate with **AWS X-Ray** for distributed tracing to identify latency issues and request failures in production.

- **API Versioning**: Versioned endpoints (e.g., `/api/v1/books`) allow services to evolve independently.

- **Trade-offs Managed**: Microservices introduce overhead, but we keep complexity low by using only two services and avoiding premature event-driven designs.

- **Future-Proof Stack**: Kotlin, Spring Boot, and Docker offer strong community support and ease onboarding.

By following these practices, the system can grow in capacity and adapt to new features with minimal friction.

## 12. Production Deployment Strategy (AWS + CI/CD)

### **12.1 Deployment Infrastructure (AWS):**

In a production environment, we plan to deploy microservices using AWS services:

- **Compute**:
  
  - **Amazon EC2** instances to host containerized Spring Boot microservices.
  
  - **Elastic Beanstalk** (Multi-container Docker environment) to manage deployment, scaling, and health monitoring with minimal operational overhead.

- **Networking & Load Balancing**:
  
  - **Application Load Balancer (ALB)** for routing requests to appropriate microservice endpoints.
  
  - **VPC**, **Subnets**, **Security Groups** to isolate and secure resources.

- **Database**:
  
  - **Amazon RDS (MySQL)** for scalable and managed relational database.
  
  - Automated backups and multi-AZ configuration for high availability and disaster recovery.

- **Monitoring & Logs**:
  
  - **CloudWatch Logs** for application logs.
  
  - **CloudWatch Metrics and Alarms** for infrastructure and service monitoring.

- **CloudFormation**:
  
  - Infrastructure as Code (IaC) using CloudFormation templates to provision EC2, ALB, RDS, and other components reliably and repeatably.

### **12.2 CI/CD Pipeline:**

- **Source Control**:
  
  - GitHub, GitLab or AWS CodeCommit to manage source code

- **CI/CD Tooling**:
  
  - **GitHub Actions**, **GitLab CI**, or **AWS CodePipeline** for automation.
  
  - CI/CD flow: Build → Test → Dockerize → Deploy to Elastic Beanstalk.

- **CI Stages**:
  
  - Run unit and integration tests.
  
  - Verify database schema migrations via **Flyway**.

- **CD Stages**:
  
  - Deploy new versions to **Elastic Beanstalk** environments.
  
  - Health check and rolling updates.
  
  - Blue/Green deployment strategies (if needed).

## 13. Future Enhancements

### 13.1 Functional Features

- **Book Recommendations** – Implement a recommendation engine based on purchase history, browsing behavior, or genre preferences to improve user engagement and upselling.

- **E-Book Selling** – Enable digital product support by offering downloadable ePub or PDF files, with license control and optional DRM integration.

- **Book Cover Images** – Add support for storing and displaying book cover images (hosted via S3 or local storage) to enhance product listings and UI.

- **User Reviews & Ratings** – Allow users to rate and review books. This improves trust and helps future buyers make decisions.

- **Wishlist / Save for Later** – Add a personal wishlist feature for customers to save books for future purchases.

- **Multi-language Book Support** – Extend catalog with language filters and multilingual metadata.



### 13.2 Technical & Architectural Enhancements

- **Replace Basic Auth** – Migrate to JWT or OAuth2 authentication for improved security and scalability (e.g., using Spring Security + Keycloak or Auth0).

- **Caching Layer** – Add Redis for frequently accessed data such as book listings, search filters, and recommendations to reduce DB load and improve response time.

- **Rate Limiting & Throttling** – Use an API Gateway to add IP-based throttling, authentication rate limits, and abuse protection.

- **Database Isolation** – Migrate to independent databases per service (e.g., one MySQL instance for orders, one for inventory) to improve fault isolation and scalability.

- **CI/CD Improvements** – Add automated integration and E2E test pipelines with Docker Compose, GitHub Actions, and staging deployment support.

## 14. Conclusion

This system applies clean microservices architecture with Kotlin and Spring Boot to model a real-world online bookstore. It is secure, scalable, and deployable via Docker and AWS. Core workflows are modular and production-ready, with room for future improvements like JWT auth and async messaging. The design emphasizes simplicity, maintainability, and practical engineering trade-offs.

# Project Implementation Summary

### ✅ Completed Features

## **Book Inventory Service**

- **Core CRUD Operations**: Complete implementation of Create, Read, Update, Delete operations for books
- **Advanced Search & Filtering**: Multi-criteria search with pagination, sorting, and filtering by title, author, genre, ISBN, language, publisher, and published date
- **Inventory Management**: Stock quantity tracking and updates with validation
- **REST API Endpoints**: Full RESTful API with proper HTTP status codes and error handling
- **Data Validation**: Comprehensive input validation and business rule enforcement
- **Security**: Basic Authentication implementation with role-based access control
- **Database Integration**: JPA/Hibernate integration with MySQL using schema-per-service pattern
- **Exception Handling**: Global exception handler with standardized error responses
- **API Documentation**: OpenAPI/Swagger documentation for all endpoints

## **Order Management Service**

- **Shopping Cart Management**: Complete cart operations (add, update, remove, clear, get cart items)
- **Order Processing**: Order creation, status tracking, and lifecycle management
- **Payment Simulation**: Automated payment processing with configurable delays
- **Inventory Integration**: Real-time stock validation and reservation during order placement
- **Order Status Workflow**: Complete order lifecycle (PENDING → CONFIRMED → SHIPPED → DELIVERED/CANCELLED)
- **Cross-Service Communication**: HTTP client integration with Book Inventory Service
- **Cart Persistence**: Database-backed cart storage with user isolation
- **Order History**: Customer order retrieval and tracking capabilities

## **Infrastructure & DevOps**

- **Docker Containerization**: Complete Docker setup for all services and database
- **Docker Compose**: Orchestrated multi-service deployment with proper networking
- **Event-Driven Architecture**: RabbitMQ integration for event-driven communication.
- **Database Setup**: MySQL with separate schemas for service isolation
- **Environment Configuration**: Comprehensive environment variable management
- **Service Discovery**: Inter-service communication via Docker networking

## **Testing & Quality Assurance**

- **Unit & Service-Layer Testing**: Comprehensive test coverage for all service and business logic layers
- **Integration Testing**: Integration tests ensure the correct behavior of REST APIs and database interactions, providing confidence in the system's end-to-end functionality.
- **Test Data Management**: Proper test data setup and cleanup
- **Mock Testing**: Service layer testing with mocked dependencies

## **Code Quality & Architecture**

- **Clean Architecture**: Proper separation of concerns with Controller-Service-Repository layers
- **DTO Pattern**: Request/Response DTOs for API contracts
- **Mapper Pattern**: Clean data transformation between layers
- **Exception Handling**: Custom exception hierarchy with proper error codes
- **Logging**: Structured logging throughout the application
- **Configuration Management**: Externalized configuration with profiles

## **Technology Stack**

- **Backend**: Kotlin + Spring Boot 3.x
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Security**: Spring Security with Basic Authentication
- **Testing**: JUnit 5, Mockito
- **Documentation**: OpenAPI 3.0 with Swagger UI
- **Containerization**: Docker + Docker Compose
- **Build Tool**: Gradle with Kotlin DSL

## **Service URLs & Documentation**

- **Book Inventory Service**: `http://localhost:8081`
  - **API Base Path**: `http://localhost:8081/api/v1/books`
  - **Swagger UI**: `http://localhost:8081/swagger-ui.html`
  - **OpenAPI Spec**: `http://localhost:8081/v3/api-docs`
- **Order Management Service**: `http://localhost:8082`
  - **API Base Path**: `http://localhost:8082/api/v1`
  - **Cart Endpoints**: `http://localhost:8082/api/v1/cart`
  - **Order Endpoints**: `http://localhost:8082/api/v1/orders`
  - **Swagger UI**: `http://localhost:8082/swagger-ui.html`
  - **OpenAPI Spec**: `http://localhost:8082/v3/api-docs`
- **MySQL Database**: `localhost:3306`
  - **Books Schema**: `books` database
  - **Orders Schema**: `orders` database

## **Authentication Credentials**

- **Book Inventory Service**:
  - Username: `bookadmin`
  - Password: `bookpass123`
- **Order Management Service**:
  - Username: `orderadmin`
  - Password: `orderpass123`

## **Database Schema**

- **Production Database**: MySQL 8.0 with separate schemas for service isolation
- **Test Database**: H2 in-memory database for unit testing
- **Books Schema**: Complete book catalog with metadata and inventory tracking
- **Orders Schema**: Order management with cart, orders, and order items tables
- **User Isolation**: Separate database users for service isolation
- **User Authentication**: In-memory user management (not database-stored)

## **API Endpoints**

- **Book Inventory**: 8 endpoints covering all CRUD and search operations
- **Order Management**: 8 endpoints for cart and order management
- **Authentication**: All endpoints secured with Basic Auth
- **Error Handling**: Standardized error responses with proper HTTP status codes

## **Security Features**

- **Authentication**: HTTP Basic Authentication for all endpoints
- **Authorization**: Role-based access control
- **Input Validation**: Comprehensive request validation
- **Error Handling**: Secure error responses without information leakage

The implementation successfully delivers all functional requirements outlined in the design document, with a robust, scalable, and maintainable microservices architecture ready for production deployment.
