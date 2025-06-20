package com.smartdocument.ordermanagement.mapper

import com.smartdocument.ordermanagement.model.Order
import com.smartdocument.ordermanagement.model.OrderItem
import com.smartdocument.ordermanagement.dto.OrderResponseDto
import com.smartdocument.ordermanagement.dto.OrderItemResponseDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper(componentModel = "spring")
interface OrderMapper {
    fun toOrderResponseDto(order: Order): OrderResponseDto
    fun toOrderItemResponseDto(orderItem: OrderItem): OrderItemResponseDto
    fun toOrderResponseDtoList(orders: List<Order>): List<OrderResponseDto>
}
