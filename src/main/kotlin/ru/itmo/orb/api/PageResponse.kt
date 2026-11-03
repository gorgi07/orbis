package ru.itmo.orb.api

import org.springframework.data.domain.Page

/** Stable outward-facing paging contract, independent of Spring Data PageImpl serialization. */
data class PageResponse<T>(
    val content: List<T>,
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

fun <T : Any> Page<T>.toPageResponse(): PageResponse<T> = PageResponse(
    content = content,
    number = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    first = isFirst,
    last = isLast
)
