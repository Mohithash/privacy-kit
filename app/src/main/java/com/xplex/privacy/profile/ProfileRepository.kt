package com.xplex.privacy.profile

import android.content.Context
import com.xplex.privacy.data.SettingsClient
import com.xplex.privacy.hooks.HookDefinition

/**
 * Applies/reads/resets a privacy profile for one target app. The management
 * UI is the same app that owns [com.xplex.privacy.data.PrivacyContentProvider],
 * so these calls go through the same cross-process call() path a hooked
 * app's own process would use - just acting on behalf of a different
 * package than its own (which [com.xplex.privacy.data.PrivacyContentProvider.verifyCaller]
 * explicitly allows for this app).
 */
class ProfileRepository(private val context: Context) {

    fun applyRandomProfile(packageName: String, allHooks: List<HookDefinition>) {
        val values = FingerprintRandomizer.randomProfile()
        applyProfile(packageName, values, allHooks)
    }

    fun applyProfile(packageName: String, values: Map<String, String>, allHooks: List<HookDefinition>) {
        for (hook in allHooks) {
            val value = values[hook.id]
            if (value != null) {
                SettingsClient.putSetting(context, packageName, hook.id, value)
                SettingsClient.setHookEnabled(context, packageName, hook.id, true)
            }
        }
    }

    fun currentValues(packageName: String): Map<String, String?> =
        SettingsClient.getAllSettings(context, packageName)

    fun enabledHookIds(packageName: String): Set<String> =
        SettingsClient.getEnabledHookIds(context, packageName)

    fun setHookEnabled(packageName: String, hookId: String, enabled: Boolean) {
        SettingsClient.setHookEnabled(context, packageName, hookId, enabled)
    }

    fun setValue(packageName: String, hookId: String, value: String) {
        SettingsClient.putSetting(context, packageName, hookId, value)
    }

    fun reset(packageName: String) {
        SettingsClient.resetPackage(context, packageName)
    }
}
