package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Convoy Track", appName)
  }

  @Test
  fun `offline curated search returns results`() = kotlinx.coroutines.runBlocking {
    val repo = com.example.data.repository.RoutingRepository()
    val results = repo.searchPlaces("Goa")
    org.junit.Assert.assertTrue("Should find Goa places", results.isNotEmpty())
  }
}
