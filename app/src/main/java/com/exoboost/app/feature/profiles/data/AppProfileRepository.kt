package com.exoboost.app.feature.profiles.data

import android.content.Context
import android.content.pm.PackageManager
import com.exoboost.app.core.capability.LauncherHomeDetector
import com.exoboost.app.feature.overlay.policy.SidebarVisibilityPolicy
import com.exoboost.app.feature.overlay.policy.VisibilityEvaluation
import com.exoboost.app.feature.profiles.model.AppInfo
import com.exoboost.app.feature.profiles.model.AppProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

typealias VisibilityDecision = VisibilityEvaluation

class AppProfileRepository(private val context: Context) {

    private val profileManager = AppProfileManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var cachedProfiles: Map<String, AppProfile> = emptyMap()

    init {
        scope.launch {
            profileManager.profilesFlow.collect { map ->
                cachedProfiles = map
            }
        }
    }

    val profilesFlow: Flow<Map<String, AppProfile>> = profileManager.profilesFlow

    fun getProfile(packageName: String): AppProfile {
        return cachedProfiles[packageName] ?: AppProfile.createDefault(packageName, getAppDisplayName(packageName))
    }

    fun isExplicitlyConfigured(packageName: String): Boolean {
        return cachedProfiles.containsKey(packageName)
    }

    fun isProtected(packageName: String): Boolean {
        return getProfile(packageName).isProtected || SidebarVisibilityPolicy.defaultSensitivePackages.contains(packageName)
    }

    fun getAppDisplayName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    fun evaluateVisibility(
        packageName: String?,
        isOverlayGranted: Boolean = true,
        isMasterEnabled: Boolean = true
    ): VisibilityDecision {
        return SidebarVisibilityPolicy.evaluate(
            context = context,
            currentPackage = packageName,
            isOverlayPermissionGranted = isOverlayGranted,
            isMasterSwitchEnabled = isMasterEnabled,
            repository = this
        )
    }

    fun isExoBoostEnabledForPackage(packageName: String?): Boolean {
        return evaluateVisibility(packageName).shouldShow
    }

    suspend fun getInstalledLaunchableApps(): List<AppInfo> {
        return profileManager.getInstalledLaunchableApps()
    }

    suspend fun saveProfile(profile: AppProfile) {
        profileManager.saveProfile(profile)
    }

    suspend fun deleteProfile(packageName: String) {
        profileManager.deleteProfile(packageName)
    }

    suspend fun toggleAppEnabled(packageName: String, displayName: String, enabled: Boolean) {
        profileManager.toggleAppEnabled(packageName, displayName, enabled)
    }

    suspend fun toggleAppProtected(packageName: String, displayName: String, isProtected: Boolean) {
        profileManager.toggleAppProtected(packageName, displayName, isProtected)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppProfileRepository? = null

        fun getInstance(context: Context): AppProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppProfileRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
