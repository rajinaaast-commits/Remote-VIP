package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.volume.TvVolumeController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Remote", appName)
  }

  @Test
  fun `volumeController initializes and returns status`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = TvVolumeController(context)
    val status = controller.getStatus()
    assertNotNull(status)
    assertNotNull(status.lastActionMessage)
  }
}
