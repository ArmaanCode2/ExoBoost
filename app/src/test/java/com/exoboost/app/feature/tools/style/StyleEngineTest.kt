package com.exoboost.app.feature.tools.style

import com.exoboost.app.feature.tools.style.model.StylePresetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleEngineTest {

    @Test
    fun testAll11PresetsExist() {
        val presets = StylePresetType.values()
        assertEquals("11 presets must exist in StylePresetType", 11, presets.size)
    }

    @Test
    fun testOriginalPresetHasNeutralValues() {
        val original = StylePresetType.ORIGINAL.getParameters()
        assertEquals(0.0f, original.brightness, 0.001f)
        assertEquals(1.0f, original.contrast, 0.001f)
        assertEquals(1.0f, original.saturation, 0.001f)
        assertEquals(0.0f, original.temperature, 0.001f)
        assertEquals(false, original.isMonochrome)
    }

    @Test
    fun testBwPresetIsMonochrome() {
        val bw = StylePresetType.BW.getParameters()
        assertTrue("B&W preset must enable monochrome flag", bw.isMonochrome)
        assertEquals(0.0f, bw.saturation, 0.001f)
    }

    @Test
    fun testPresetSelectionUpdatesState() {
        StyleEngine.selectPreset(StylePresetType.CINEMA)
        assertEquals(StylePresetType.CINEMA, StyleEngine.activePreset.value)
        assertEquals(1.22f, StyleEngine.activeParameters.value.contrast, 0.001f)

        StyleEngine.reset()
        assertEquals(StylePresetType.ORIGINAL, StyleEngine.activePreset.value)
    }
}
