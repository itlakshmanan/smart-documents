package com.smartdocument.ordermanagement.repository

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for Order entity persistence operations.
 *
 * This repository extends JpaRepository to provide standard CRUD operations
 * for Order entities. It also includes custom query methods for finding
 * orders by customer ID and status combinations.
 *
 * The repository provides:
 * - Standard CRUD operations (Create, Read, Update, Delete)
 * - Custom query methods for business-specific operations
 * - Automatic query generation based on method names
 * - Transaction management through Spring Data JPA
 * - Support for order history and status-based filtering
 *
 * All operations are automatically wrapped in transactions and
 * the repository is managed by Spring's dependency injection container.
 * Orders are typically created through the checkout process and then
 * managed through status updates.
 *
 * @property Order The entity type managed by this repository
 * @property Long The type of the entity's primary key
 */
@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    /**
     * Finds all orders for a specific customer.
     *
     * Retrieves all orders associated with the specified customer ID,
     * regardless of their status. This method is useful for displaying
     * a customer's complete order history.
     *
     * Orders are returned in the order they were created (typically
     * newest first due to ID generation strategy).
     *
     * @param customerId Unique identifier for the customer
     * @return List of Order entities for the customer, empty list if none found
     */
    fun findByCustomerId(customerId: String): List<Order>

    /**
     * Finds orders for a specific customer with a particular status.
     *
     * Retrieves orders that match both the customer ID and the specified
     * order status. This method is useful for filtering orders by their
     * current state (e.g., finding all pending orders for a customer).
     *
     * Common use cases include:
     * - Finding all pending orders for order management
     * - Retrieving delivered orders for customer satisfaction
     * - Locating cancelled orders for refund processing
     *
     * @param customerId Unique identifier for the customer
     * @param status The order status to filter by
     * @return List of Order entities matching the criteria, empty list if none found
     */
    fun findByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order>
}
