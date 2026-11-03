package ru.itmo.orb

import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.orb.api.toPageResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageResponseTest {
    @Test
    fun `page dto exposes stable pagination fields`() {
        val page = PageImpl(listOf("a", "b"), PageRequest.of(0, 2), 3L)
        val dto = page.toPageResponse()

        assertEquals(listOf("a", "b"), dto.content)
        assertEquals(0, dto.number)
        assertEquals(2, dto.size)
        assertEquals(3L, dto.totalElements)
        assertEquals(2, dto.totalPages)
        assertTrue(dto.first)
        assertFalse(dto.last)
    }
}
