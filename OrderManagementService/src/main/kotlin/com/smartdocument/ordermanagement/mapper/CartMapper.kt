package com.smartdocument.ordermanagement.mapper

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.model.CartItem
import com.smartdocument.ordermanagement.dto.CartResponseDto
import com.smartdocument.ordermanagement.dto.CartItemResponseDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper(componentModel = "spring")
interface CartMapper {
    @Mapping(source = "cartItems", target = "items")
    fun toCartResponseDto(cart: Cart): CartResponseDto
    fun toCartItemResponseDto(cartItem: CartItem): CartItemResponseDto
    fun toCartResponseDtoList(carts: List<Cart>): List<CartResponseDto>
}
