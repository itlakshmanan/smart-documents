package com.smartdocument.bookinventory.service

import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import com.smartdocument.bookinventory.model.Book

object BookMapper {
    fun toEntity(dto: BookRequestDto, id: Long? = null): Book = Book(
        id = id ?: 0,
        title = dto.title,
        author = dto.author,
        isbn = dto.isbn,
        genre = dto.genre,
        price = dto.price,
        quantity = dto.quantity,
        description = dto.description
    )

    fun toResponseDto(book: Book): BookResponseDto = BookResponseDto(
        id = book.id,
        title = book.title,
        author = book.author,
        isbn = book.isbn,
        genre = book.genre,
        price = book.price,
        quantity = book.quantity,
        description = book.description
    )
}
