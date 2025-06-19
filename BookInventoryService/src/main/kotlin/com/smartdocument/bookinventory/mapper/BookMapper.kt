package com.smartdocument.bookinventory.mapper

import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import com.smartdocument.bookinventory.model.Book
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

@Mapper(componentModel = "spring")
interface BookMapper {
    companion object {
        val INSTANCE: BookMapper = Mappers.getMapper(BookMapper::class.java)
    }

    @Mapping(target = "id", ignore = true)
    fun toEntity(dto: BookRequestDto): Book

    @Mapping(target = "id", source = "id")
    fun toEntityWithId(dto: BookRequestDto, id: Long): Book

    fun toResponseDto(book: Book): BookResponseDto
}
