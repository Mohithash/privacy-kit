package com.xplex.privacy.hooks

import kotlinx.serialization.Serializable

/**
 * One hook, declared as data rather than code. A method hook (methodName
 * with no "#" prefix) installs an actual XposedBridge hook that fires on
 * every call. A field hook (methodName prefixed with "#") has no runtime
 * interception at all - static final fields are read directly via getstatic,
 * so instead the Lua script's "after" function runs exactly once, when the
 * target app's process starts, and overwrites the field's value via
 * reflection. This matches how the previous XPL-EX project's Build.MODEL-style
 * hooks actually worked under the hood.
 */
@Serializable
data class HookDefinition(
    val id: String,
    val className: String,
    val methodName: String,
    val parameterTypes: List<String> = emptyList(),
    val enabled: Boolean = true,
    val luaScript: String,
    val description: String = ""
) {
    val isFieldHook: Boolean get() = methodName.startsWith("#")
    val fieldName: String get() = methodName.removePrefix("#")

    /**
     * Derived from the id's existing namespace prefix (e.g. "device.build.model"
     * -> "device") rather than a separate JSON field - every hook id already
     * has this structure, so there's nothing new to keep in sync when adding
     * a hook.
     */
    val categoryKey: String get() = id.substringBefore('.', missingDelimiterValue = id)

    val categoryLabel: String get() = when (categoryKey) {
        "device" -> "Device fingerprint"
        "telephony" -> "Telephony identifiers"
        "settings" -> "System identifiers"
        "wifi" -> "WiFi"
        "bluetooth" -> "Bluetooth"
        "pm" -> "App visibility"
        "location" -> "Location"
        "ads" -> "Advertising"
        else -> categoryKey.replaceFirstChar { it.uppercase() }
    }
}

@Serializable
data class HookDefinitionFile(
    val hooks: List<HookDefinition>
)
