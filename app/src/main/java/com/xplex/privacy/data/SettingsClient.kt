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
}
