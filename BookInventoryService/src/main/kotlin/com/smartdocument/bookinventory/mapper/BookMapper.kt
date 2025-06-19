package com.smartdocument.bookinventory.mapper

import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import com.smartdocument.bookinventory.model.Book
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers
import java.time.LocalDate

@Mapper(componentModel = "spring", imports = [LocalDate::class])
interface BookMapper {
    companion object {
        val INSTANCE: BookMapper = Mappers.getMapper(BookMapper::class.java)
    }

    @Mappings(
        Mapping(target = "id", ignore = true),
        Mapping(target = "publishedDate", expression = "java(LocalDate.parse(dto.getPublishedDate()))"),
        Mapping(target = "createdAt", ignore = true),
        Mapping(target = "updatedAt", ignore = true)
    )
    fun toEntity(dto: BookRequestDto): Book

    @Mappings(
        Mapping(target = "id", source = "id"),
        Mapping(target = "publishedDate", expression = "java(LocalDate.parse(dto.getPublishedDate()))"),
        Mapping(target = "createdAt", ignore = true),
        Mapping(target = "updatedAt", ignore = true)
    )
    fun toEntityWithId(dto: BookRequestDto, id: Long): Book

    @Mapping(target = "publishedDate", expression = "java(book.getPublishedDate().toString())")
    fun toResponseDto(book: Book): BookResponseDto
}
