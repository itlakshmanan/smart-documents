package com.smartdocument.ordermanagement.mapper

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderItem
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.dto.OrderItemResponseDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

/**
 * Mapper interface for converting between Order entities and DTOs.
 *
 * This mapper provides type-safe conversion between domain entities and
 * response DTOs for order-related operations. It uses MapStruct to generate
 * efficient mapping code at compile time.
 *
 * The mapper handles:
 * - Conversion from Order entity to OrderResponseDto
 * - Conversion from OrderItem entity to OrderItemResponseDto
 * - Conversion from lists of Order entities to lists of OrderResponseDto
 * - Mapping of order items and their associated data
 *
 * MapStruct automatically generates the implementation of this interface,
 * providing efficient and type-safe object mapping without reflection.
 * The mapper preserves all order information including status, timestamps,
 * and historical pricing data.
 */
@Mapper(componentModel = "spring")
interface OrderMapper {

    /**
     * Converts an Order entity to an OrderResponseDto.
     *
     * Maps all order properties including order ID, customer ID, status,
     * creation timestamp, total amount, and associated order items.
     * The mapping preserves historical pricing information captured
     * at the time of order creation.
     *
     * @param order The Order entity to convert
     * @return OrderResponseDto containing the order data
     */
    fun toOrderResponseDto(order: Order): OrderResponseDto

    /**
     * Converts an OrderItem entity to an OrderItemResponseDto.
     *
     * Maps all order item properties including book ID, quantity,
     * price at time of order, and calculated subtotal. This mapping
     * preserves the historical pricing information that was captured
     * when the order was created.
     *
     * @param orderItem The OrderItem entity to convert
     * @return OrderItemResponseDto containing the order item data
     */
    fun toOrderItemResponseDto(orderItem: OrderItem): OrderItemResponseDto

    /**
     * Converts a list of Order entities to a list of OrderResponseDto objects.
     *
     * Applies the toOrderResponseDto mapping to each order in the list,
     * maintaining the order and structure of the original list. Useful
     * for bulk operations and order history retrieval.
     *
     * @param orders List of Order entities to convert
     * @return List of OrderResponseDto objects
     */
    fun toOrderResponseDtoList(orders: List<Order>): List<OrderResponseDto>
}
