package com.exoboost.app.feature.profiles

import com.exoboost.app.feature.profiles.model.AppProfile
import com.exoboost.app.feature.toolbox.model.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProfileTest {

    @Test
    fun testYouTubeDefaultProfile() {
        val ytProfile = AppProfile.createDefault("com.google.android.youtube", "YouTube")
        assertTrue(ytProfile.isEnabled)
        assertEquals("cinema", ytProfile.stylePresetId)
        assertEquals(125, ytProfile.volumeBoostPercent)
        assertTrue(ytProfile.enabledToolIds.contains(ToolRegistry.ID_SCREEN_OFF))
        assertTrue(ytProfile.enabledToolIds.contains(ToolRegistry.ID_STYLE))
    }

    @Test
    fun testWhatsAppDefaultProfileIsDisabled() {
        val waProfile = AppProfile.createDefault("com.whatsapp", "WhatsApp")
        assertFalse("WhatsApp toolbox should be disabled by default", waProfile.isEnabled)
    }

    @Test
    fun testSpotifyDefaultProfile() {
        val spotifyProfile = AppProfile.createDefault("com.spotify.music", "Spotify")
        assertTrue(spotifyProfile.isEnabled)
        assertEquals(150, spotifyProfile.volumeBoostPercent)
        assertTrue(spotifyProfile.enabledToolIds.contains(ToolRegistry.ID_AUDIO))
    }
}
