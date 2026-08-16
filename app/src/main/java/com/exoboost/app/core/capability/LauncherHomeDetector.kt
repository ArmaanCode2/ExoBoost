package com.exoboost.app.core.capability

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object LauncherHomeDetector {

    private var cachedHomePackages: Set<String> = emptySet()
    private var lastCacheTime: Long = 0L

    private val commonSystemPackages = setOf(
        "com.android.systemui",
        "android",
        "com.android.settings",
        "com.google.android.settings.intelligence",
        "com.google.android.inputmethod.latin",
        "com.touchtype.swiftkey",
        "com.samsung.android.honeyboard",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.miui.securityadd",
        "com.miui.securitycenter",
        "com.miui.cleanmaster"
    )

    private val transientSystemUiAndImePackages = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.inputmethod.latin",
        "com.touchtype.swiftkey",
        "com.samsung.android.honeyboard",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.miui.securityadd",
        "com.miui.securitycenter",
        "com.miui.cleanmaster"
    )

    fun isTransientSystemUiOrIme(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return transientSystemUiAndImePackages.contains(packageName)
    }

    fun isHomeScreen(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return getHomePackages(context).contains(packageName)
    }

    fun isHomeScreenOrSystem(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return true
        if (commonSystemPackages.contains(packageName)) return true
        return isHomeScreen(context, packageName)
    }

    fun getHomePackages(context: Context): Set<String> {
        val now = System.currentTimeMillis()
        if (cachedHomePackages.isNotEmpty() && now - lastCacheTime < 30000) {
            return cachedHomePackages
        }

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (_: Exception) {
            emptyList()
        }

        val allHome = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val packages = mutableSetOf<String>()
        resolveInfos.forEach { packages.add(it.activityInfo.packageName) }
        allHome.forEach { packages.add(it.activityInfo.packageName) }

        // Also add known standard OEM launcher packages as safeguard
        packages.add("com.miui.home")
        packages.add("com.sec.android.app.launcher")
        packages.add("com.google.android.apps.nexuslauncher")
        packages.add("com.huawei.android.launcher")
        packages.add("com.oppo.launcher")
        packages.add("com.oneplus.launcher")

        cachedHomePackages = packages
        lastCacheTime = now
        return packages
    }
}
