package com.xplex.privacy.data

import android.content.Context
import android.os.Bundle
import android.util.Log

private const val TAG = "PrivacyKit.Client"
const val PROVIDER_AUTHORITY = "com.xplex.privacy.provider"

/**
 * Called from inside hooked apps' own processes (and from the management UI)
 * to reach [PrivacyContentProvider] over IPC. Every failure is swallowed and
 * logged rather than thrown - a settings lookup failing must never crash the
 * app a hook is running inside.
 */
object SettingsClient {

    private fun call(context: Context, method: String, packageName: String, extra: Bundle.() -> Unit = {}): Bundle? {
        return try {
            val args = Bundle().apply {
                putString(ARG_PACKAGE, packageName)
                extra()
            }
            context.contentResolver.call(
                android.net.Uri.parse("content://$PROVIDER_AUTHORITY"),
                method, null, args
            )
        } catch (t: Throwable) {
            Log.w(TAG, "call($method, $packageName) failed: $t")
            null
        }
    }

    fun getSetting(context: Context, packageName: String, name: String): String? {
        return call(context, METHOD_GET_SETTING, packageName) { putString(ARG_NAME, name) }
            ?.getString(ARG_VALUE)
    }

    fun putSetting(context: Context, packageName: String, name: String, value: String?) {
        call(context, METHOD_PUT_SETTING, packageName) {
            putString(ARG_NAME, name)
            value?.let { putString(ARG_VALUE, it) }
        }
    }

    fun getAllSettings(context: Context, packageName: String): Map<String, String?> {
        val result = call(context, METHOD_GET_ALL_SETTINGS, packageName) ?: return emptyMap()
        val keys = result.getStringArray("keys") ?: return emptyMap()
        val values = result.getStringArray("values") ?: return emptyMap()
        return keys.indices.associate { keys[it] to values.getOrNull(it) }
    }

    fun isHookEnabled(context: Context, packageName: String, hookId: String): Boolean {
        return call(context, METHOD_IS_HOOK_ENABLED, packageName) { putString(ARG_HOOK_ID, hookId) }
            ?.getBoolean(ARG_ENABLED) ?: false
    }

    fun getEnabledHookIds(context: Context, packageName: String): Set<String> {
        val result = call(context, METHOD_GET_ENABLED_HOOK_IDS, packageName) ?: return emptySet()
        return result.getStringArray("hookIds")?.toSet() ?: emptySet()
    }

    fun setHookEnabled(context: Context, packageName: String, hookId: String, enabled: Boolean) {
        call(context, METHOD_SET_HOOK_ENABLED, packageName) {
            putString(ARG_HOOK_ID, hookId)
            putBoolean(ARG_ENABLED, enabled)
        }
    }

    fun resetPackage(context: Context, packageName: String) {
        call(context, METHOD_RESET_PACKAGE, packageName)
    }

    fun recordDiagnostic(context: Context, packageName: String, hookId: String, status: String, message: String?) {
        call(context, METHOD_RECORD_DIAGNOSTIC, packageName) {
            putString(ARG_HOOK_ID, hookId)
            putString(ARG_STATUS, status)
            message?.let { putString(ARG_MESSAGE, it) }
        }
    }

    data class DiagnosticEntry(val hookId: String, val status: String, val message: String?, val timestamp: Long)

    fun getDiagnostics(context: Context, packageName: String): List<DiagnosticEntry> {
        val result = call(context, METHOD_GET_DIAGNOSTICS, packageName) ?: return emptyList()
        val hookIds = result.getStringArray("hookIds") ?: return emptyList()
        val statuses = result.getStringArray("statuses") ?: return emptyList()
        val messages = result.getStringArray("messages") ?: return emptyList()
        val timestamps = result.getLongArray("timestamps") ?: return emptyList()
        return hookIds.indices.map { i ->
            DiagnosticEntry(hookIds[i], statuses[i], messages.getOrNull(i)?.ifBlank { null }, timestamps[i])
        }
    }

    /**
     * Presets are global, not per-app - always called with this app's own
     * package as the acting identity, which [PrivacyContentProvider.verifyCaller]
     * permits since the management app legitimately owns that package.
     */
    fun savePreset(context: Context, presetName: String, values: Map<String, String>) {
        call(context, METHOD_SAVE_PRESET, context.packageName) {
            putString(ARG_PRESET_NAME, presetName)
            putStringArray("hookIds", values.keys.toTypedArray())
            putStringArray("values", values.values.toTypedArray())
        }
    }

    fun getPreset(context: Context, presetName: String): Map<String, String> {
        val result = call(context, METHOD_GET_PRESET, context.packageName) { putString(ARG_PRESET_NAME, presetName) }
            ?: return emptyMap()
        val hookIds = result.getStringArray("hookIds") ?: return emptyMap()
        val values = result.getStringArray("values") ?: return emptyMap()
        return hookIds.indices.associate { hookIds[it] to values[it] }
    }

    fun listPresets(context: Context): List<String> {
        val result = call(context, METHOD_LIST_PRESETS, context.packageName) ?: return emptyList()
        return result.getStringArray("presetNames")?.toList() ?: emptyList()
    }

    fun deletePreset(context: Context, presetName: String) {
        call(context, METHOD_DELETE_PRESET, context.packageName) { putString(ARG_PRESET_NAME, presetName) }
    }

    /** One query for dashboard stats, instead of one IPC round trip per installed app. */
    fun listConfiguredPackages(context: Context): List<String> {
        val result = call(context, METHOD_LIST_CONFIGURED_PACKAGES, context.packageName) ?: return emptyList()
        return result.getStringArray("packages")?.toList() ?: emptyList()
    }
}
