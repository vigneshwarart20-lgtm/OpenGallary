package com.example

import android.net.Uri
import com.example.data.model.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun testMediaItemFormatting() {
        val item = MediaItem(
            id = 1L,
            uri = Uri.parse("content://media/external/images/media/1"),
            name = "test.mp4",
            size = 15 * 1024 * 1024L,
            durationMs = 125000L,
            isVideo = true
        )

        assertEquals("2:05", item.formattedDuration)
        assertTrue(item.formattedSize.contains("15.0 MB"))
    }
}
