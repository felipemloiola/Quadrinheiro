package com.example

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSearchApi() {
    kotlinx.coroutines.runBlocking {
      val engine = com.example.data.MultiApiSearchEngine()
      val results = mutableListOf<com.example.model.ComicEntry>()
      try {
        engine.searchOnlineProgressive("batman", "MULTI", "ALL", true)
          .catch { e -> println("FLOW ERROR: ${e.message}"); e.printStackTrace() }
          .collect { list ->
            println("Progressive batch received: size=${list.size}")
            results.clear()
            results.addAll(list)
          }
      } catch (e: Throwable) {
        println("COLLECT EXCEPTION: ${e.message}")
        e.printStackTrace()
      }
      println("FINAL results count: ${results.size}")
    }
  }
}
