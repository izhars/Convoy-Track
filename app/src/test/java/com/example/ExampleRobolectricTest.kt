package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
class ExampleRobolectricTest {

  @Test
  fun `offline curated search returns results`() = kotlinx.coroutines.runBlocking {
    val repo = com.example.data.repository.RoutingRepository()
    val results = repo.searchPlaces("Goa")
    org.junit.Assert.assertTrue("Should find Goa places", results.isNotEmpty())
  }
}
