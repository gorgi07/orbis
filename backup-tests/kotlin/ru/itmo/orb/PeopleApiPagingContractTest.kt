package ru.itmo.orb

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import ru.itmo.orb.api.PersonResponse
import ru.itmo.orb.api.PageResponse
import ru.itmo.orb.service.PersonService
import ru.itmo.orb.web.PersonApi
import kotlin.test.assertEquals

/** Prevents returning Spring Data PageImpl directly from the people API. */
class PeopleApiPagingContractTest {
    @Test
    fun `people endpoint returns stable pagination DTO`() {
        val service = Mockito.mock(PersonService::class.java)
        Mockito.`when`(service.list(0, 15, "id", "asc", null, null, null, null, null))
            .thenReturn(PageImpl(emptyList<PersonResponse>()))

        val response: PageResponse<PersonResponse> = PersonApi(service)
            .list(0, 15, "id", "asc", null, null, null, null, null)

        assertEquals(0L, response.totalElements)
        assertEquals(emptyList<PersonResponse>(), response.content)
    }
}
