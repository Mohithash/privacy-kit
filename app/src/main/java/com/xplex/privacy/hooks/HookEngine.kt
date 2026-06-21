package com.xplex.privacy.hooks

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import com.xplex.privacy.data.SettingsClient

private const val TAG = "PrivacyKit.Engine"

/**
 * Resolves [HookDefinition]s against a target app's classloader and installs
 * them. Method hooks become real XposedBridge hooks that fire on every call.
 * Field hooks have no runtime interception - they run their Lua script's
 * "after" function exactly once, at install time, directly overwriting the
 * static field's value via reflection for the lifetime of this process.
 */
class HookEngine(private val context: Context, moduleAssets: android.content.res.AssetManager) {

    private val luaRunner = LuaScriptRunner(moduleAssets)

    fun installHooks(packageName: String, classLoader: ClassLoader, definitions: List<HookDefinition>) {
        val enabledIds = SettingsClient.getEnabledHookIds(context, packageName)
        if (enabledIds.isEmpty()) {
            Log.i(TAG, "No hooks enabled for $packageName, skipping")
            return
        }

        for (definition in definitions) {
            if (!definition.enabled) continue
            if (definition.id !in enabledIds) continue

            try {
                if (definition.isFieldHook) {
                    installFieldHook(packageName, classLoader, definition)
                } else {
                    installMethodHook(packageName, classLoader, definition)
                }
                SettingsClient.recordDiagnostic(
                    context, packageName, definition.id,
                    com.xplex.privacy.data.SettingsDatabase.STATUS_INSTALLED, null
                )
            } catch (t: ClassNotFoundException) {
                recordNotApplicable(packageName, definition, t)
            } catch (t: NoSuchMethodException) {
                recordNotApplicable(packageName, definition, t)
            } catch (t: NoSuchFieldException) {
                recordNotApplicable(packageName, definition, t)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to install hook ${definition.id} for $packageName", t)
                SettingsClient.recordDiagnostic(
                    context, packageName, definition.id,
                    com.xplex.privacy.data.SettingsDatabase.STATUS_FAILED, t.toString()
                )
            }
        }
    }

    /**
     * A class/method/field that simply doesn't exist in this app (e.g.
     * AdvertisingIdClient, which only exists in apps bundling Play Services'
     * ads-identifier library) isn't an install failure - it's an expected,
     * normal outcome that shouldn't show up as an error in diagnostics.
     */
    private fun recordNotApplicable(packageName: String, definition: HookDefinition, t: Throwable) {
        Log.i(TAG, "Hook ${definition.id} not applicable to $packageName: $t")
        SettingsClient.recordDiagnostic(
            context, packageName, definition.id,
            com.xplex.privacy.data.SettingsDatabase.STATUS_NOT_APPLICABLE, t.toString()
        )
    }

    private fun installFieldHook(packageName: String, classLoader: ClassLoader, definition: HookDefinition) {
        val clazz = Class.forName(definition.className, false, classLoader)
        val field = clazz.getDeclaredField(definition.fieldName)
        field.isAccessible = true

        val param = XParam(context, packageName, methodParam = null, field = field)
        val changed = luaRunner.run(definition.luaScript, "after", definition.id, param)
        if (changed) {
            Log.i(TAG, "Field hook ${definition.id} applied for $packageName")
        }
    }

    private fun installMethodHook(packageName: String, classLoader: ClassLoader, definition: HookDefinition) {
        val clazz = Class.forName(definition.className, false, classLoader)
        val paramTypes = definition.parameterTypes.map { resolveType(it, classLoader) }.toTypedArray()

        val member = if (definition.methodName == "<init>") {
            clazz.getDeclaredConstructor(*paramTypes)
        } else {
            clazz.getDeclaredMethod(definition.methodName, *paramTypes)
        }

        XposedBridge.hookMethod(member, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                runScript("before", param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                runScript("after", param)
            }

            private fun runScript(function: String, methodHookParam: MethodHookParam) {
                try {
                    val xParam = XParam(context, packageName, methodHookParam, field = null)
                    luaRunner.run(definition.luaScript, function, definition.id, xParam)
                } catch (t: Throwable) {
                    Log.e(TAG, "Hook ${definition.id} threw during $function", t)
                }
            }
        })

        Log.i(TAG, "Installed method hook ${definition.id} for $packageName")
    }

    private fun resolveType(name: String, classLoader: ClassLoader): Class<*> {
        return when (name) {
            "boolean" -> Boolean::class.javaPrimitiveType!!
            "byte" -> Byte::class.javaPrimitiveType!!
            "char" -> Char::class.javaPrimitiveType!!
            "short" -> Short::class.javaPrimitiveType!!
            "int" -> Int::class.javaPrimitiveType!!
            "long" -> Long::class.javaPrimitiveType!!
            "float" -> Float::class.javaPrimitiveType!!
            "double" -> Double::class.javaPrimitiveType!!
            "void" -> Void.TYPE
            else -> Class.forName(name, false, classLoader)
        }
    }
}
