package com.example

import com.example.data.PersonalizedDiscovery
import com.example.data.ProfileShowcase
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersonalizedDiscoveryTest {
    private fun book(id: String, title: String, author: String = "Autora diferente", publisher: String = "DC") =
        ComicEntry(id, id, title, authors = author, publisher = publisher)
    @Test fun favoritePublisherStillInfluencesColdStart() {
        val marvel = book("marvel", "Uma HQ", publisher = "Marvel Comics")
        val dc = book("dc", "Outra HQ", publisher = "DC Comics")
        assertEquals("dc", PersonalizedDiscovery.recommend(listOf(marvel, dc), emptyList(), "DC / Vertigo").first().id)
    }
    @Test fun favoriteSnapshotOutsideTheCatalogCanSeedCreatorRecommendations() {
        val favorite = book("favorite", "Watchmen", "Alan Moore")
        val related = book("related", "V de Vingança", "Alan Moore", "Vertigo")
        val other = book("other", "Uma história diferente", "Outra pessoa")
        val settings = AppSettings(profileFavoriteComicIds = listOf(favorite.id), profileShowcaseDetails = mapOf("comic:${favorite.id}" to ProfileShowcase.comicSnapshot(favorite)))
        assertEquals(listOf("related"), PersonalizedDiscovery.recommend(listOf(other, related), emptyList(), settings = settings).map { it.id })
    }
    @Test fun portugueseCharacterMatchesEnglishTitleAndCivilianDescription() {
        val hero = book("hero", "Swamp Thing", "Autor um")
        val civil = book("civil", "Uma aventura", "Autor dois").copy(synopsis = "Alec Holland enfrenta um novo desafio.")
        val other = book("other", "Outro universo", "Autor três")
        val settings = AppSettings(profileFavoriteCharacters = listOf("Monstro do Pântano (Alec Holland)"))
        assertEquals(setOf("hero", "civil"), PersonalizedDiscovery.recommend(listOf(other, civil, hero), emptyList(), settings = settings).map { it.id }.toSet())
    }
    @Test fun characterMatchingDoesNotUsePartialWords() {
        val settings = AppSettings(profileFavoriteCharacters = listOf("Ravena"))
        assertTrue(PersonalizedDiscovery.recommend(listOf(book("other", "Ravencroft")), emptyList(), settings = settings).isEmpty())
    }
    @Test fun allSavedStatusesAreExcludedEvenWithDifferentProviderIds() {
        val library = ReadingStatus.entries.mapIndexed { i, status -> book("owned-$i", "História $i").copy(status = status) }
        val duplicates = library.mapIndexed { i, comic -> comic.copy(id = "remote-$i", externalId = "remote-$i") }
        val fresh = book("fresh", "História nova")
        assertEquals(listOf("fresh"), PersonalizedDiscovery.recommend(duplicates + fresh, library, "DC").map { it.id })
    }
    @Test fun monthlyIssueNumbersRemainDistinct() {
        val first = book("first", "Batman").copy(issueNumber = 404, format = "Mensal")
        val next = first.copy(id = "next", externalId = "next", issueNumber = 405)
        assertEquals(listOf("next"), PersonalizedDiscovery.recommend(listOf(first, next), listOf(first), "DC").map { it.id })
    }
    @Test fun aLowRatingDoesNotBecomePositiveCreatorAffinity() {
        val disliked = book("read", "Uma leitura antiga", "Pessoa específica").copy(rating = 1f, status = ReadingStatus.READ)
        val candidate = book("new", "Outro trabalho", "Pessoa específica")
        assertTrue(PersonalizedDiscovery.recommend(listOf(candidate), listOf(disliked)).isEmpty())
    }
    @Test fun successiveAccountsDoNotSharePersonalizationState() {
        val batman = book("batman", "Batman: Ano Um")
        val daredevil = book("daredevil", "Daredevil", publisher = "Marvel")
        val pool = listOf(batman, daredevil)
        assertEquals(listOf("batman"), PersonalizedDiscovery.recommend(pool, emptyList(), settings = AppSettings(profileFavoriteCharacters = listOf("Batman"))).map { it.id })
        assertEquals(listOf("daredevil"), PersonalizedDiscovery.recommend(pool, emptyList(), settings = AppSettings(profileFavoriteCharacters = listOf("Demolidor"))).map { it.id })
    }
    @Test fun differentProvidersDoNotRepeatTheSameWork() {
        val original = book("a", "Batman: Ano Um")
        val duplicate = original.copy(id = "b", externalId = "b")
        assertEquals(1, PersonalizedDiscovery.recommend(listOf(original, duplicate), emptyList(), "DC").size)
    }
    @Test fun publisherAloneDoesNotInventAHistoryRelationship() {
        val read = book("read", "Uma obra independente", "Pessoa um")
        val other = book("other", "Outra trama", "Pessoa dois")
        assertTrue(PersonalizedDiscovery.recommend(listOf(other), listOf(read)).isEmpty())
    }
}
