package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.model.*
import com.example.ui.components.renderBannerCrop
import com.example.widget.WidgetVisualRenderer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CatalogRevision51Test {
    @Test fun resetClearsAllOldPrivateBooksAndFavoriteCoversButRetainsIdentityAndCharacters() {
        val storage=SafeStorage(ApplicationProvider.getApplicationContext<Context>())
        val uid="epoch51-test"
        val settings=AppSettings(profileFavoriteComicIds=listOf("old"),profileFavoriteCharacters=listOf("Batman"),profileBannerUrl="https://example.com/banner",
            profileShowcaseDetails=mapOf("comic:old" to "old snapshot","character:Batman" to "https://example.com/batman"))
        storage.saveState(listOf(ComicEntry("old","","Antiga",authors="Autor",publisher="Editora")),ReaderProfile(name="Identidade"),settings,uid)
        storage.saveShowcase(uid,settings)
        val key=SafeStorage.ACCOUNT_STATE_PREFIX+uid
        storage.setItem(key,JSONObject(storage.getItem(key)!!).apply{remove("catalogEpoch")}.toString())
        try {
            val state=storage.loadAccountState(uid)!!
            assertTrue(state.comics.isEmpty());assertTrue(state.settings.profileFavoriteComicIds.isEmpty())
            assertFalse(state.settings.profileShowcaseDetails.containsKey("comic:old"))
            assertEquals("Identidade",state.profile.name);assertEquals(listOf("Batman"),state.settings.profileFavoriteCharacters)
            assertEquals(settings.profileBannerUrl,state.settings.profileBannerUrl)
            assertEquals(CatalogEpoch.VERSION,JSONObject(storage.getItem(key)!!).getInt("catalogEpoch"))
            val fresh=ComicEntry("new","cv-volume-1","Nova",authors="Autor",publisher="Editora",updatedAt=System.currentTimeMillis())
            storage.saveState(listOf(fresh),state.profile,state.settings,uid)
            assertEquals(listOf("new"),storage.loadAccountState(uid)!!.comics.map{it.id})
        }finally{storage.removeItem(key);storage.removeItem("profile_showcase_$uid")}
    }
    @Test fun canonicalHalAndBruceComeBeforeNamesakesInEitherLanguage() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val heroes=CharacterDirectory.parse(context.assets.open("character-seed.json").bufferedReader().use{it.readText()})
        assertEquals("Lanterna Verde (Hal Jordan)",CharacterDirectory.match(heroes,"Lanterna Verde").first().name)
        assertEquals("Lanterna Verde (Hal Jordan)",CharacterDirectory.match(heroes,"Hal Jordan").first().name)
        assertTrue(CharacterDirectory.match(heroes,"Batman").first().name.contains("Bruce Wayne"))
    }
    @Test fun widgetsRespectExplicitAppThemeBeforeSystemTheme() {
        assertTrue(WidgetVisualRenderer.resolveWidgetDark("dark",false))
        assertFalse(WidgetVisualRenderer.resolveWidgetDark("light",true))
        assertTrue(WidgetVisualRenderer.resolveWidgetDark("system",true))
        assertFalse(WidgetVisualRenderer.resolveWidgetDark("system",false))
    }
    @Test fun bannerCropProducesBoundedPanoramicImage() {
        val image=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val data=renderBannerCrop(image,IntSize(600,200),2f,Offset(0f,100f))
        val bytes=Base64.decode(data.substringAfter("base64,"),Base64.DEFAULT)
        val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        assertEquals(1200,decoded.width);assertEquals(400,decoded.height);assertTrue(bytes.size<150000)
        image.recycle();decoded.recycle()
    }
    @Test fun autoPublicationQueueIsDurableAndIsolatedByAccount() {
        val storage=SafeStorage(ApplicationProvider.getApplicationContext<Context>())
        val comic=ComicEntry("queue-test","cv-123","HQ",authors="Autor",publisher="Editora",rating=4f,updatedAt=51)
        try{
            CommunityAutoSync.enqueue(storage,"guest_local",comic)
            assertFalse(CommunityAutoSync.hasPending(storage,"guest_local"))
            CommunityAutoSync.enqueue(storage,"alice51",comic)
            assertTrue(CommunityAutoSync.hasPending(storage,"alice51"))
            assertFalse(CommunityAutoSync.hasPending(storage,"bob51"))
            CommunityAutoSync.enqueue(storage,"alice51",comic.copy(updatedAt=52,review="Mais recente"))
            val queue=JSONObject(storage.getItem("community_pending_alice51")!!)
            assertEquals(1,queue.length());assertEquals(52,queue.getJSONObject(comic.id).getLong("rev"))
        }finally{storage.removeItem("community_pending_alice51")}
    }
}
