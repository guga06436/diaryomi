package com.diaryomi

import com.diaryomi.domain.model.GazetteResult
import com.diaryomi.data.local.entity.SearchResultEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SyncAndDeduplicationTest {

    @Test
    fun testDateLogicCoversCurrentDayAndNextDay() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val lastCheckedDateToday = today
        val fromDate1 = when {
            lastCheckedDateToday.isBefore(today) -> lastCheckedDateToday
            else -> today
        }
        val toDate1 = tomorrow
        assertEquals(today, fromDate1)
        assertEquals(tomorrow, toDate1)

        val lastCheckedDatePast = today.minusDays(3)
        val fromDate2 = when {
            lastCheckedDatePast.isBefore(today) -> lastCheckedDatePast
            else -> today
        }
        val toDate2 = tomorrow
        assertEquals(today.minusDays(3), fromDate2)
        assertEquals(tomorrow, toDate2)
    }

    @Test
    fun testDeduplicationLogicFiltersExistingItems() {
        val existingEntities = listOf(
            SearchResultEntity(
                id = 1L,
                searchId = 10L,
                title = "Diário TCE-RN nº 3680 • PORTARIA Nº 10",
                date = LocalDate.now().toEpochDay(),
                snippet = "Snippet 1",
                url = "https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/312#pub-100",
                isRead = true
            )
        )

        val incomingResults = listOf(
            GazetteResult(
                title = "Diário TCE-RN nº 3680 • PORTARIA Nº 10",
                date = LocalDate.now(),
                snippet = "Snippet 1",
                url = "https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/312#pub-100"
            ),
            GazetteResult(
                title = "Diário TCE-RN nº 3680 • PORTARIA Nº 11",
                date = LocalDate.now(),
                snippet = "Snippet 2",
                url = "https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/312#pub-101"
            )
        )

        val newEntities = incomingResults.filter { result ->
            val resultEpochDay = result.date.toEpochDay()
            val alreadyExists = existingEntities.any { existing ->
                (existing.url.isNotBlank() && result.url.isNotBlank() &&
                    (existing.url == result.url || 
                     (existing.url.substringBefore("#") == result.url.substringBefore("#") && existing.title == result.title))) ||
                (existing.title == result.title && existing.date == resultEpochDay)
            }
            !alreadyExists
        }

        assertEquals(1, newEntities.size)
        assertEquals("Diário TCE-RN nº 3680 • PORTARIA Nº 11", newEntities[0].title)
    }

    @Test
    fun testTceRnUrlFragmentStripping() {
        val storedUrl = "https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/312#pub-12345"
        val cleanUrl = storedUrl.substringBefore("#")
        assertEquals("https://sisdocs.tce.rn.gov.br/api/v1/DiarioEletronico/download/312", cleanUrl)
    }
}
