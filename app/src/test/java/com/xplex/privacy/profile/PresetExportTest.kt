package com.xplex.privacy.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PresetExportTest {

    @Test
    fun `encode then decode round-trips multiple presets exactly`() {
        val presets = mapOf(
            "Pixel 8 US" to mapOf("device.build.model" to "Pixel 8", "device.build.brand" to "google"),
            "Empty preset" to emptyMap()
        )

        val decoded = PresetExport.decode(PresetExport.encode(presets))

        assertEquals(presets, decoded)
    }

    @Test
    fun `decode rejects malformed json instead of silently returning empty`() {
        assertThrows(Exception::class.java) {
            PresetExport.decode("{ not valid json")
        }
    }

    @Test
    fun `decode rejects valid json with the wrong shape`() {
        assertThrows(Exception::class.java) {
            PresetExport.decode("""{"unrelated": true}""")
        }
    }
}
