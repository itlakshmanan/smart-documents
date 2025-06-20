package com.smartdocument.bookinventory.mapper

import com.smartdocument.bookinventory.dto.BookRequestDto
import com.smartdocument.bookinventory.dto.BookResponseDto
import com.smartdocument.bookinventory.model.Book
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * MapStruct mapper for converting between Book entities and DTOs.
 * Handles mapping from request DTOs to entities and from entities to response DTOs.
 */
@Mapper(componentModel = "spring", imports = [LocalDate::class, LocalDateTime::class])
interface BookMapper {
    companion object {
        val INSTANCE: BookMapper = Mappers.getMapper(BookMapper::class.java)
    }

    /**
     * Maps a BookRequestDto to a Book entity for creation.
     * - Ignores the id field (auto-generated)
     * - Parses publishedDate from String to LocalDate
     * - Sets createdAt and updatedAt to now
     */
    @Mappings(
        Mapping(target = "id", ignore = true),
        Mapping(target = "publishedDate", expression = "java(LocalDate.parse(dto.getPublishedDate()))"),
        Mapping(target = "createdAt", expression = "java(LocalDateTime.now())"),
        Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    )
    fun toEntity(dto: BookRequestDto): Book

    /**
     * Maps a BookRequestDto and id to a Book entity for update.
     * - Sets the id field
     * - Parses publishedDate from String to LocalDate
     * - Sets createdAt and updatedAt to now
     */
    @Mappings(
        Mapping(target = "id", source = "id"),
        Mapping(target = "publishedDate", expression = "java(LocalDate.parse(dto.getPublishedDate()))"),
        Mapping(target = "createdAt", expression = "java(LocalDateTime.now())"),
        Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    )
    fun toEntityWithId(dto: BookRequestDto, id: Long): Book

    /**
     * Maps a Book entity to a BookResponseDto.
     * - Converts publishedDate from LocalDate to String
     */
    @Mapping(target = "publishedDate", expression = "java(book.getPublishedDate().toString())")
    fun toResponseDto(book: Book): BookResponseDto
}
