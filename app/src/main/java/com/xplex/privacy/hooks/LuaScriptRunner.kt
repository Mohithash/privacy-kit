package com.xplex.privacy.hooks

import android.content.res.AssetManager
import android.util.Log
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.BufferedInputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PrivacyKit.Lua"

/**
 * Loads and runs Lua hook scripts from assets/lua/<name>.lua, read via
 * [ModuleAssets] since this data lives in Privacy Kit's own APK. Scripts are
 * compiled once and cached; each script gets its own Globals per calling
 * thread (LuaJ's Globals/coroutine state isn't safe to share across
 * concurrently-executing threads), matching the previous project's approach.
 */
class LuaScriptRunner(private val moduleAssets: AssetManager) {

    private val compiledScripts = ConcurrentHashMap<String, Prototype>()
    private val globalsPerScriptAndThread = ConcurrentHashMap<String, ThreadLocal<Globals>>()

    private fun loadScript(scriptName: String): Prototype? {
        return compiledScripts.getOrPut(scriptName) {
            val assetPath = "lua/$scriptName.lua"
            val bytes = try {
                BufferedInputStream(moduleAssets.open(assetPath)).use { it.readBytes() }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to read lua script asset: $assetPath", t)
                return null
            }
            val globals = JsePlatform.standardGlobals()
            try {
                globals.loadPrototype(bytes.inputStream(), scriptName, "t")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to compile lua script: $scriptName", t)
                return null
            }
        }
    }

    private fun globalsFor(scriptName: String): Globals {
        val threadLocal = globalsPerScriptAndThread.getOrPut(scriptName) {
            ThreadLocal.withInitial { JsePlatform.standardGlobals() }
        }
        return threadLocal.get()!!
    }

    /**
     * Runs the named Lua function ("before" or "after") in [scriptName],
     * passing [hookId] and [param]. Returns true if the script handled the
     * call (matching the convention of the previous project's scripts,
     * which return `true, oldValue, newValue` when they actually changed
     * something, `false` otherwise). Any script error is caught and logged;
     * a broken hook script must never crash the app it's running inside.
     */
    fun run(scriptName: String, functionName: String, hookId: String, param: XParam): Boolean {
        val prototype = loadScript(scriptName) ?: return false
        return try {
            val globals = globalsFor(scriptName)
            val closure = LuaClosure(prototype, globals)
            closure.call()

            val function = globals.get(functionName)
            if (function.isnil()) return false

            val result = function.invoke(
                arrayOf(
                    CoerceJavaToLua.coerce(hookId) as LuaValue,
                    CoerceJavaToLua.coerce(param) as LuaValue
                )
            )
            result.arg(1).optboolean(false)
        } catch (t: Throwable) {
            Log.e(TAG, "Error running $scriptName.$functionName for hook $hookId", t)
            false
        }
    }
}
