package com.example

import com.example.data.ComicVineCatalog
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiFixesStage1Test {
    @Test
    fun authorsAndYearAreCapturedFaithfullyWithoutFabrication() {
        val json = JSONObject("""{
            "id": "1001",
            "name": "Watchmen",
            "volume": "Watchmen",
            "authors": "Alan Moore, Dave Gibbons",
            "date": "1986-09-01",
            "isbn": "9788573516883"
        }""")
        val entry = ComicVineCatalog.comic(json)
        assertEquals("Alan Moore, Dave Gibbons", entry.authors)
        assertEquals(1986, entry.releaseYear)
        assertEquals("9788573516883", entry.isbn)
    }

    @Test
    fun unknownYearDefaultsToZeroNotTwentyTwentySix() {
        val json = JSONObject("""{
            "id": "1002",
            "name": "Classic Comic Without Year",
            "creators": [{"name": "Stan Lee"}, {"name": "Jack Kirby"}]
        }""")
        val entry = ComicVineCatalog.comic(json)
        assertEquals(0, entry.releaseYear)
        assertEquals("Stan Lee, Jack Kirby", entry.authors)
    }
}
