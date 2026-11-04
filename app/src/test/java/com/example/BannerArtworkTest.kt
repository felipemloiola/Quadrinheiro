package com.example

import android.graphics.Bitmap
import com.example.data.CatalogBanners
import java.io.ByteArrayOutputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BannerArtworkTest {
    private fun art(url: String = "https://comicvine.gamespot.com/a/uploads/original/1/art.png", w: Int = 1200, h: Int = 400) =
        JSONObject().put("url", url).put("width", w).put("height", h).put("title", "Batman")
    @Test fun acceptsVerifiedHorizontalArtwork() {
        val result = CatalogBanners.parse(JSONObject().put("artwork", JSONArray().put(art())))
        assertEquals(1, result.size); assertEquals(1200, result.single().width)
    }
    @Test fun rejectsVerticalSmallOrUnsafeImages() {
        val rows = JSONArray().put(art(w = 600, h = 900)).put(art(w = 300, h = 100))
            .put(art("https://evil.test/a/uploads/a.png")).put(art("http://comicvine.gamespot.com/a/uploads/a.png"))
            .put(art("https://user:password@comicvine.gamespot.com/a/uploads/a.png"))
            .put(art("https://comicvine.gamespot.com/a/uploads/a.png?api_key=not-allowed"))
        assertTrue(CatalogBanners.parse(JSONObject().put("artwork", rows)).isEmpty())
    }
    @Test fun ignoresLegacyUnverifiedUrlLists() {
        assertTrue(CatalogBanners.parse(JSONObject().put("items", JSONArray().put("https://comicvine.gamespot.com/a/uploads/a.png"))).isEmpty())
    }
    @Test fun deduplicatesResolutionsAndBoundsChoices() {
        val rows = JSONArray().put(art()).put(art("https://comicvine.gamespot.com/a/uploads/scale_super/1/art.png"))
        repeat(10) { rows.put(art("https://comicvine.gamespot.com/a/uploads/original/1/art$it.png")) }
        val result = CatalogBanners.parse(JSONObject().put("artwork", rows))
        assertEquals(6, result.size); assertEquals(result.size, result.map { it.url }.distinct().size)
    }
    @Test fun selectedArtworkIsDownsampledBeforeCropping() {
        val source = Bitmap.createBitmap(2400, 800, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        try { source.compress(Bitmap.CompressFormat.PNG, 100, out) } finally { source.recycle() }
        val decoded = CatalogBanners.decode(out.toByteArray())
        try { assertTrue(decoded.width <= 1800); assertTrue(decoded.height <= 1800); assertEquals(3.0, decoded.width.toDouble() / decoded.height, .01) }
        finally { decoded.recycle() }
    }
    @Test fun rejectsEmptyOrOversizedDownloadsBeforeDecoding() {
        for (bytes in listOf(ByteArray(0), ByteArray(6 * 1024 * 1024 + 1))) {
            try { CatalogBanners.decode(bytes); fail("Should reject invalid size") } catch (_: IllegalArgumentException) { }
        }
    }
}
