package com.exoboost.app.feature.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandlePositionCalculatorTest {

    @Test
    fun testNormalizedYCalculation() {
        val screenHeight = 2400
        val handleHeight = 192
        val density = 3.0f

        val minSafeY = (48 * density).toInt() // 144
        val maxSafeY = screenHeight - handleHeight - (48 * density).toInt() // 2400 - 192 - 144 = 2064

        // Test Top Preset (20%)
        val topY = calculateSafeYPixelSimulated(0.20f, handleHeight, screenHeight, density)
        assertEquals(144 + ((2064 - 144) * 0.20f).toInt(), topY)
        assertTrue("Top Y must be below status bar", topY >= minSafeY)

        // Test Center Preset (45%)
        val centerY = calculateSafeYPixelSimulated(0.45f, handleHeight, screenHeight, density)
        assertEquals(144 + ((2064 - 144) * 0.45f).toInt(), centerY)
        assertTrue("Center Y must be greater than Top Y", centerY > topY)

        // Test Bottom Preset (75%)
        val bottomY = calculateSafeYPixelSimulated(0.75f, handleHeight, screenHeight, density)
        assertEquals(144 + ((2064 - 144) * 0.75f).toInt(), bottomY)
        assertTrue("Bottom Y must be less than maxSafeY", bottomY <= maxSafeY)
    }

    @Test
    fun testLandscapeRotationAdaptation() {
        val portraitHeight = 2400
        val landscapeHeight = 1080
        val handleHeight = 192
        val density = 3.0f
        val yPercent = 0.45f

        val portraitY = calculateSafeYPixelSimulated(yPercent, handleHeight, portraitHeight, density)
        val landscapeY = calculateSafeYPixelSimulated(yPercent, handleHeight, landscapeHeight, density)

        assertTrue("Portrait Y coordinate must scale to larger height", portraitY > landscapeY)
        assertTrue("Landscape Y coordinate must stay safely within landscape bounds", landscapeY <= (landscapeHeight - handleHeight - (48 * density).toInt()))
    }

    @Test
    fun testClampingOutOfBounds() {
        val screenHeight = 2400
        val handleHeight = 192
        val density = 3.0f

        // When out of range (< 0.05f or > 0.95f), should clamp safely
        val belowMin = calculateSafeYPixelSimulated(-0.5f, handleHeight, screenHeight, density)
        val atMin = calculateSafeYPixelSimulated(0.05f, handleHeight, screenHeight, density)
        assertEquals("Negative percent should clamp to min 5%", atMin, belowMin)

        val aboveMax = calculateSafeYPixelSimulated(1.5f, handleHeight, screenHeight, density)
        val atMax = calculateSafeYPixelSimulated(0.95f, handleHeight, screenHeight, density)
        assertEquals("Percent > 1.0 should clamp to max 95%", atMax, aboveMax)
    }

    private fun calculateSafeYPixelSimulated(yPercent: Float, heightPx: Int, screenHeight: Int, density: Float): Int {
        val minSafeY = (48 * density).toInt()
        val maxSafeY = (screenHeight - heightPx - (48 * density).toInt()).coerceAtLeast(minSafeY)

        val normalized = yPercent.coerceIn(0.05f, 0.95f)
        return minSafeY + ((maxSafeY - minSafeY) * normalized).toInt()
    }
}
