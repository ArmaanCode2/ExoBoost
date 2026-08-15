package com.exoboost.app.feature.profiles.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.exoboost.app.feature.profiles.model.AppInfo
import com.exoboost.app.feature.profiles.model.AppProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.profilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "exoboost_app_profiles")

class AppProfileManager(private val context: Context) {

    private object Keys {
        val PROFILES_JSON = stringPreferencesKey("app_profiles_json")
    }

    val profilesFlow: Flow<Map<String, AppProfile>> = context.profilesDataStore.data.map { prefs ->
        val jsonStr = prefs[Keys.PROFILES_JSON] ?: "{}"
        parseProfilesJson(jsonStr)
    }

    suspend fun getInstalledLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Exclude ExoBoost itself
            if (!seenPackages.add(pkg)) continue

            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }

            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }

            appList.add(AppInfo(packageName = pkg, displayName = label, isSystemApp = isSystem))
        }

        appList.sortedBy { it.displayName.lowercase() }
    }

    suspend fun saveProfile(profile: AppProfile) {
        context.profilesDataStore.edit { prefs ->
            val jsonStr = prefs[Keys.PROFILES_JSON] ?: "{}"
            val map = parseProfilesJson(jsonStr).toMutableMap()
            map[profile.packageName] = profile
            prefs[Keys.PROFILES_JSON] = serializeProfilesJson(map)
        }
    }

    suspend fun deleteProfile(packageName: String) {
        context.profilesDataStore.edit { prefs ->
            val jsonStr = prefs[Keys.PROFILES_JSON] ?: "{}"
            val map = parseProfilesJson(jsonStr).toMutableMap()
            map.remove(packageName)
            prefs[Keys.PROFILES_JSON] = serializeProfilesJson(map)
        }
    }

    suspend fun toggleAppEnabled(packageName: String, displayName: String, enabled: Boolean) {
        context.profilesDataStore.edit { prefs ->
            val jsonStr = prefs[Keys.PROFILES_JSON] ?: "{}"
            val map = parseProfilesJson(jsonStr).toMutableMap()
            val existing = map[packageName] ?: AppProfile.createDefault(packageName, displayName)
            map[packageName] = existing.copy(isEnabled = enabled)
            prefs[Keys.PROFILES_JSON] = serializeProfilesJson(map)
        }
    }

    private fun parseProfilesJson(jsonStr: String): Map<String, AppProfile> {
        val result = mutableMapOf<String, AppProfile>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val obj = root.getJSONObject(pkg)

                val enabledTools = mutableSetOf<String>()
                val toolsArr = obj.optJSONArray("enabledToolIds") ?: JSONArray()
                for (i in 0 until toolsArr.length()) {
                    enabledTools.add(toolsArr.getString(i))
                }

                result[pkg] = AppProfile(
                    packageName = pkg,
                    displayName = obj.optString("displayName", pkg),
                    isEnabled = obj.optBoolean("isEnabled", true),
                    enabledToolIds = if (enabledTools.isNotEmpty()) enabledTools else AppProfile.createDefault(pkg, pkg).enabledToolIds,
                    volumeBoostPercent = obj.optInt("volumeBoostPercent", 100),
                    stylePresetId = obj.optString("stylePresetId", "original"),
                    handleSide = if (obj.has("handleSide")) obj.optString("handleSide") else null
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializeProfilesJson(map: Map<String, AppProfile>): String {
        val root = JSONObject()
        for ((pkg, profile) in map) {
            val obj = JSONObject().apply {
                put("displayName", profile.displayName)
                put("isEnabled", profile.isEnabled)
                put("volumeBoostPercent", profile.volumeBoostPercent)
                put("stylePresetId", profile.stylePresetId)
                if (profile.handleSide != null) {
                    put("handleSide", profile.handleSide)
                }
                val toolsArr = JSONArray()
                profile.enabledToolIds.forEach { toolsArr.put(it) }
                put("enabledToolIds", toolsArr)
            }
            root.put(pkg, obj)
        }
        return root.toString()
    }
}
