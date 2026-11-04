package com.example

import android.graphics.Bitmap
import com.example.data.CatalogCredit
import com.example.data.CatalogDetails
import com.example.data.ComicBannerResolver
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComicBannerResolverTest {
    private fun detail(type: String = "issue", banner: String = "", covers: List<String> = emptyList(), characters: List<CatalogCredit> = emptyList()) =
        CatalogDetails("1", type, "Batman", "", banner, "", "", "", "", covers, characters, emptyList(), emptyList())
    @Test fun candidatesComeOnlyFromTheResourceAndPublicProviderUploads() {
        val wide = "https://comicvine.gamespot.com/a/uploads/original/1/art.jpg"
        val data = detail(banner = wide, covers = listOf(wide.replace("/original/", "/scale_super/"),
            "http://comicvine.gamespot.com/a/uploads/a.jpg", "https://127.0.0.1/a/uploads/a.jpg", "https://evil.test/a/uploads/a.jpg"))
        assertEquals(listOf(wide), ComicBannerResolver.candidates(data))
    }
    @Test fun candidateCountAndPixelDimensionsAreBounded() {
        val urls = (1..20).map { "https://comicvine.gamespot.com/a/uploads/original/1/art$it.jpg" }
        assertEquals(6, ComicBannerResolver.candidates(detail(covers = urls)).size)
        assertTrue(ComicBannerResolver.landscape(1200, 400))
        assertFalse(ComicBannerResolver.landscape(600, 900))
        assertFalse(ComicBannerResolver.landscape(300, 100))
        assertFalse(ComicBannerResolver.landscape(40000, 10000))
    }
    @Test fun characterFallbackRequiresAnActualCreditAndANameInTheTitle() {
        val batman = CatalogCredit("1699", "Batman")
        assertEquals(batman, ComicBannerResolver.creditedCharacter(detail(characters = listOf(batman)), "Batman: Ano Um"))
        assertNull(ComicBannerResolver.creditedCharacter(detail(characters = listOf(batman)), "Outra história"))
        assertNull(ComicBannerResolver.creditedCharacter(detail(), "Batman: Ano Um"))
    }
    @Test fun metronAndComicVineCharacterIdsAreNeverInterchanged() {
        val mt = CatalogCredit("mt-1699", "Batman")
        val cv = CatalogCredit("1699", "Batman")
        assertEquals(mt, ComicBannerResolver.creditedCharacter(detail(type = "metron-issue", characters = listOf(cv, mt)), "Batman"))
        assertEquals(cv, ComicBannerResolver.creditedCharacter(detail(characters = listOf(mt, cv)), "Batman"))
        assertNull(ComicBannerResolver.creditedCharacter(detail(type = "person", characters = listOf(cv)), "Batman"))
    }
    @Test fun translatedCharacterNamesCanMatchTheBookTitle() {
        val credit = CatalogCredit("123", "Swamp Thing")
        assertEquals(credit, ComicBannerResolver.creditedCharacter(detail(characters = listOf(credit)), "Monstro do Pântano"))
    }
    @Test fun imageProbesRejectCredentialsAndNonImageApiPaths() {
        val data = detail(covers = listOf("https://user:password@comicvine.gamespot.com/a/uploads/a.jpg",
            "https://comicvine.gamespot.com/a/uploads/a.jpg?api_key=not-allowed", "https://metron.cloud/api/issue/1/"))
        assertTrue(ComicBannerResolver.candidates(data).isEmpty())
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun nativeHeaderReadingAcceptsLandscapeAndRejectsPortraitWithoutFullBitmapDecode() {
        fun png(width: Int, height: Int): ByteArray {
            val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val out = ByteArrayOutputStream()
            try { assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, out)) } finally { source.recycle() }
            return out.toByteArray()
        }
        assertTrue(ComicBannerResolver.landscape(png(1200, 400)))
        assertFalse(ComicBannerResolver.landscape(png(600, 900)))
        assertFalse(ComicBannerResolver.landscape(ByteArray(0)))
        assertFalse(ComicBannerResolver.landscape(ByteArray(65537)))
    }
}
