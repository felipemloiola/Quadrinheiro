package com.example

import com.example.data.CharacterDirectory
import com.example.data.FavoritePortraits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Patch2CharacterIdentityAndPortraitTest {

    @Test
    fun civilIdentitiesAreCorrectlyDisambiguatedForClassicMantles() {
        val flashBarry = CharacterDirectory.display("Flash", "Barry Allen", "DC Comics")
        val flashWally = CharacterDirectory.display("Flash", "Wally West", "DC Comics")
        val spideyPeter = CharacterDirectory.display("Spider-Man", "Peter Parker", "Marvel")
        val spideyMiles = CharacterDirectory.display("Spider-Man", "Miles Morales", "Marvel")
        val swampThing = CharacterDirectory.display("Swamp Thing", "", "DC / Vertigo")
        val daredevil = CharacterDirectory.display("Daredevil", "", "Marvel")
        val sandmanMorpheus = CharacterDirectory.display("Sandman", "", "DC / Vertigo")
        val sandmanMarvel = CharacterDirectory.display("Sandman", "Flint Marko", "Marvel")

        assertEquals("Flash (Barry Allen)", flashBarry)
        assertEquals("Flash (Wally West)", flashWally)
        assertEquals("Homem-Aranha (Peter Parker)", spideyPeter)
        assertEquals("Homem-Aranha (Miles Morales)", spideyMiles)
        assertEquals("Monstro do Pântano (Alec Holland)", swampThing)
        assertEquals("Demolidor (Matt Murdock)", daredevil)
        assertEquals("Sandman (Morpheus)", sandmanMorpheus)
        assertEquals("Homem-Areia (Flint Marko)", sandmanMarvel)
    }

    @Test
    fun favoritePortraitsPreservesOfficialProviderImageUrls() {
        val current = "https://comicvine.gamespot.com/a/uploads/scale_small/1/123/456.jpg"
        val alts = listOf(
            "https://comicvine.gamespot.com/a/uploads/original/1/123/789.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/example.jpg/200px-example.jpg"
        )
        val merged = FavoritePortraits.merge(current, alts)

        assertTrue(merged.isNotEmpty())
        assertEquals(current, merged.first())
    }
}
