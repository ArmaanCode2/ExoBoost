package com.exoboost.app.feature.toolbox

import com.exoboost.app.feature.toolbox.model.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    @Test
    fun testAllToolsHaveUniqueIds() {
        val tools = ToolRegistry.allTools
        val ids = tools.map { it.id }
        assertEquals("Each tool must have a unique ID", ids.size, ids.distinct().size)
    }

    @Test
    fun test12ToolsAreRegistered() {
        val tools = ToolRegistry.allTools
        assertEquals("Exactly 12 tools must be registered in Phase 9/10", 12, tools.size)
    }

    @Test
    fun testDefaultActiveToolsAreValid() {
        val defaultIds = ToolRegistry.defaultActiveIds
        assertTrue("Default active tools cannot be empty", defaultIds.isNotEmpty())

        for (id in defaultIds) {
            val tool = ToolRegistry.getToolById(id)
            assertNotNull("Tool with id $id must exist in registry", tool)
        }
    }

    @Test
    fun testToolRetrievalById() {
        val screenshotTool = ToolRegistry.getToolById(ToolRegistry.ID_SCREENSHOT)
        assertNotNull(screenshotTool)
        assertEquals("Screenshot", screenshotTool?.title)

        val volumeBoostTool = ToolRegistry.getToolById(ToolRegistry.ID_VOLUME_BOOST)
        assertNotNull(volumeBoostTool)
        assertEquals("Volume Boost", volumeBoostTool?.title)

        val styleTool = ToolRegistry.getToolById(ToolRegistry.ID_STYLE)
        assertNotNull(styleTool)
        assertEquals("Style", styleTool?.title)
    }
}
