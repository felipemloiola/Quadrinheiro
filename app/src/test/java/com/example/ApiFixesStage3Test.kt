package com.example

import com.example.data.CharacterDirectory
import com.example.data.CharacterResult
import org.junit.Assert.*
import org.junit.Test

class ApiFixesStage3Test {

    @Test
    fun sandmanVertigoIsNotConfusedWithMarvelSandman() {
        val dcSandman = CharacterDirectory.display("Sandman", "Morpheus", "DC / Vertigo")
        assertEquals("Sandman (Morpheus)", dcSandman)

        val marvelSandman = CharacterDirectory.display("Sandman", "William Baker", "Marvel")
        assertEquals("Homem-Areia (William Baker)", marvelSandman)
    }

    @Test
    fun greenLanternCivilIdentitiesArePreservedDistinguishably() {
        val hal = CharacterDirectory.display("Hal Jordan", "Hal Jordan", "DC Comics")
        assertEquals("Lanterna Verde (Hal Jordan)", hal)

        val john = CharacterDirectory.display("John Stewart", "John Stewart", "DC Comics")
        assertEquals("Lanterna Verde (John Stewart)", john)

        assertNotEquals(hal, john)
    }

    @Test
    fun matchMaintainsBothHalJordanAndJohnStewartAsDistinctOptions() {
        val list = listOf(
            CharacterResult(id = "1", name = "Lanterna Verde (Hal Jordan)", originalName = "Hal Jordan", image = "https://example.com/hal.jpg", aliases = listOf("Hal Jordan")),
            CharacterResult(id = "2", name = "Lanterna Verde (John Stewart)", originalName = "John Stewart", image = "https://example.com/john.jpg", aliases = listOf("John Stewart"))
        )
        val results = CharacterDirectory.match(list, "Lanterna Verde")
        assertEquals(2, results.size)
        assertTrue(results.any { it.name.contains("Hal Jordan") })
        assertTrue(results.any { it.name.contains("John Stewart") })
    }
}
