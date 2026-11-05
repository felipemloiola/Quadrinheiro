package com.example

import com.example.data.CatalogDetails
import com.example.data.CharacterResult
import com.example.data.FavoritePortraits
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritePortraitsTest {
    private fun character(id: String, name: String, original: String = "Batman", aliases: List<String> = emptyList()) =
        CharacterResult(id, name, original, "", aliases)
    private fun details(id: String, images: List<String>, image: String = "", type: String = "character") =
        CatalogDetails(id, type, "Batman", image, "", "", "", "", "", images, emptyList(), emptyList(), emptyList())
    @Test fun onlyExplicitComicVineIdsAreReused() {
        assertEquals("1699", FavoritePortraits.linkedId(JSONObject().put("id", "cv-1699").toString()))
        for (id in listOf("1699", "sh-1699", "mt-1699", "cv-volume-1699", "cv-0")) {
            assertNull(FavoritePortraits.linkedId(JSONObject().put("id", id).toString()))
        }
    }
    @Test fun civilianIdentitySeparatesDifferentVersionsOfTheSameHero() {
        val bruce = character("cv-101", "Batman (Bruce Wayne)")
        val terry = character("cv-202", "Batman (Terry McGinnis)")
        assertEquals("202", FavoritePortraits.exactId("Batman (Terry McGinnis)", "Batman", listOf(bruce, terry)))
    }
    @Test fun ambiguousGenericNamesAreNotGuessed() {
        val candidates = listOf(character("cv-101", "Batman (Bruce Wayne)"), character("cv-202", "Batman (Terry McGinnis)"))
        assertNull(FavoritePortraits.exactId("Batman", "Batman", candidates))
    }
    @Test fun originalAndCivilianNamesBridgeTranslatedDisplayLabels() {
        val hal = character("cv-303", "Green Lantern (Hal Jordan)", "Green Lantern", listOf("Hal Jordan"))
        assertEquals("303", FavoritePortraits.exactId("Lanterna Verde (Hal Jordan)", "Green Lantern", listOf(hal)))
    }
    @Test fun savedChoiceStaysFirstAndDuplicateResolutionsAreRemoved() {
        val selected = "https://images.example.com/selected.jpg"
        val wide = "https://comicvine.gamespot.com/a/uploads/original/1/art.jpg"
        val images = FavoritePortraits.merge(selected, listOf(wide, wide.replace("/original/", "/scale_super/")))
        assertEquals(listOf(selected, wide), images)
        assertEquals("data:image/png;base64,chosen", FavoritePortraits.merge("data:image/png;base64,chosen", emptyList()).single())
    }
    @Test fun resourceArtworkIsBoundedAndUsesTheHigherResolutionFirst() {
        val main = "https://comicvine.gamespot.com/a/uploads/scale_super/1/main.jpg"
        val medium = main.replace("/scale_super/", "/scale_medium/")
        val extras = (1..10).map { "https://comicvine.gamespot.com/a/uploads/original/1/art$it.jpg" }
        val result = FavoritePortraits.resourceImages(details("1699", listOf(main, medium, "https://evil.test/a/uploads/a.jpg") + extras, medium), "1699")
        assertEquals(6, result.size); assertEquals(main, result.first()); assertFalse(result.any { it.contains("evil.test") })
    }
    @Test fun imagesFromAnotherResourceOrResourceTypeAreNotAttached() {
        val art = listOf("https://comicvine.gamespot.com/a/uploads/original/1/art.jpg")
        assertTrue(FavoritePortraits.resourceImages(details("202", art), "101").isEmpty())
        assertTrue(FavoritePortraits.resourceImages(details("101", art, type = "volume"), "101").isEmpty())
    }
}
