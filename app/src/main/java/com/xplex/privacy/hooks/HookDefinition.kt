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
}

@Serializable
data class HookDefinitionFile(
    val hooks: List<HookDefinition>
)
