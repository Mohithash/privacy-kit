package com.xplex.privacy.hooks

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import java.lang.reflect.Field

private const val TAG = "PrivacyKit.XParam"

/**
 * The object Lua hook scripts actually interact with - exposed as a plain
 * Kotlin class because LuaJ's CoerceJavaToLua works on any public JVM class
 * regardless of source language. Every public method here becomes callable
 * from Lua as `param:methodName(...)`.
 *
 * Wraps either a method-hook callback (param != null) or a one-time field
 * overwrite (field != null), matching the two hook shapes in
 * [HookDefinition].
 */
class XParam(
    private val context: Context,
    private val packageName: String,
    private val methodParam: XC_MethodHook.MethodHookParam?,
    private val field: Field?
) {
    @Throws(Throwable::class)
    fun getResult(): Any? {
        return if (field != null) field.get(null) else methodParam?.result
    }

    @Throws(Throwable::class)
    fun setResult(value: Any?) {
        if (field != null) {
            field.isAccessible = true
            field.set(null, value)
        } else {
            methodParam?.result = value
        }
    }

    fun getThis(): Any? = methodParam?.thisObject

    fun getArgument(index: Int): Any? {
        val args = methodParam?.args ?: return null
        return if (index in args.indices) args[index] else null
    }

    fun setArgument(index: Int, value: Any?) {
        val args = methodParam?.args ?: return
        if (index in args.indices) args[index] = value
    }

    fun getSetting(name: String): String? {
        return com.xplex.privacy.data.SettingsClient.getSetting(context, packageName, name)
    }

    fun log(message: String) {
        Log.i(TAG, "[$packageName] $message")
    }
}
