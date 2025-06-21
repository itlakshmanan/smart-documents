package com.smartdocument.ordermanagement.mapper

import com.smartdocument.ordermanagement.model.Cart
import com.smartdocument.ordermanagement.model.CartItem
import com.smartdocument.ordermanagement.dto.CartResponseDto
import com.smartdocument.ordermanagement.dto.CartItemResponseDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

/**
 * Mapper interface for converting between Cart entities and DTOs.
 *
 * This mapper provides type-safe conversion between domain entities and
 * response DTOs for cart-related operations. It uses MapStruct to generate
 * efficient mapping code at compile time.
 *
 * The mapper handles:
 * - Conversion from Cart entity to CartResponseDto
 * - Conversion from CartItem entity to CartItemResponseDto
 * - Conversion from lists of Cart entities to lists of CartResponseDto
 * - Field mapping between different naming conventions
 *
 * MapStruct automatically generates the implementation of this interface,
 * providing efficient and type-safe object mapping without reflection.
 *
 * @property cartItems Mapping from Cart.cartItems to CartResponseDto.items
 */
@Mapper(componentModel = "spring")
interface CartMapper {

    /**
     * Converts a Cart entity to a CartResponseDto.
     *
     * Maps all cart properties including customer ID, creation timestamp,
     * and associated cart items. The cart items are mapped using the
     * field mapping from "cartItems" to "items".
     *
     * @param cart The Cart entity to convert
     * @return CartResponseDto containing the cart data
     */
    @Mapping(source = "cartItems", target = "items")
    fun toCartResponseDto(cart: Cart): CartResponseDto

    /**
     * Converts a CartItem entity to a CartItemResponseDto.
     *
     * Maps all cart item properties including book ID, quantity,
     * price, and calculated subtotal.
     *
     * @param cartItem The CartItem entity to convert
     * @return CartItemResponseDto containing the cart item data
     */
    fun toCartItemResponseDto(cartItem: CartItem): CartItemResponseDto

    /**
     * Converts a list of Cart entities to a list of CartResponseDto objects.
     *
     * Applies the toCartResponseDto mapping to each cart in the list,
     * maintaining the order and structure of the original list.
     *
     * @param carts List of Cart entities to convert
     * @return List of CartResponseDto objects
     */
    fun toCartResponseDtoList(carts: List<Cart>): List<CartResponseDto>
}
