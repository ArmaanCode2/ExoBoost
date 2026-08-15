package com.exoboost.app.feature.toolbox.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolAvailability {
    AVAILABLE,      // Live and fully implemented
    EXPERIMENTAL,   // Functional with device/HAL constraints
    PREVIEW,        // UI/Action preview, full pipeline in next phases
    DISABLED        // User disabled or system unsupported
}

data class ToolAction(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val order: Int = 0,
    val availability: ToolAvailability = ToolAvailability.AVAILABLE,
    val badge: String? = null,
    val description: String = ""
)
