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

    /** Saves this app's currently-configured values as a reusable named preset. */
    fun saveAsPreset(packageName: String, presetName: String) {
        val values = currentValues(packageName).filterValues { it != null }.mapValues { it.value!! }
        SettingsClient.savePreset(context, presetName, values)
    }

    /** Applies a previously-saved preset to a (possibly different) target app. */
    fun applyPreset(packageName: String, presetName: String, allHooks: List<HookDefinition>) {
        val values = SettingsClient.getPreset(context, presetName)
        applyProfile(packageName, values, allHooks)
    }

    fun listPresetNames(): List<String> = SettingsClient.listPresets(context)

    fun configuredPackages(): List<String> = SettingsClient.listConfiguredPackages(context)

    fun deletePreset(presetName: String) {
        SettingsClient.deletePreset(context, presetName)
    }

    /** Serializes every saved preset to JSON, for writing to a file the user picks. */
    fun exportAllPresetsAsJson(): String {
        val presets = listPresetNames().associateWith { SettingsClient.getPreset(context, it) }
        return PresetExport.encode(presets)
    }

    /**
     * Parses [json] and saves every preset it contains, overwriting any
     * existing preset with the same name. Throws on malformed input rather
     * than silently importing nothing - the caller (UI) decides how to
     * surface that to the user.
     */
    fun importPresetsFromJson(json: String): Int {
        val presets = PresetExport.decode(json)
        for ((name, values) in presets) {
            SettingsClient.savePreset(context, name, values)
        }
        return presets.size
    }
}
