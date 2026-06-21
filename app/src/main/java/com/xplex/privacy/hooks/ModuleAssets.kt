package com.xplex.privacy.hooks

import android.content.res.AssetManager
import android.util.Log

private const val TAG = "PrivacyKit.ModuleAssets"

/**
 * A Context obtained inside a hooked app's process resolves `.assets` to
 * *that app's* APK, not Privacy Kit's own. hooks.json and the Lua scripts
 * live inside Privacy Kit's APK, so they have to be read via an AssetManager
 * pointed directly at the module's own APK path (captured in
 * [XposedEntry.initZygote]), not through any target app's Context.
 */
object ModuleAssets {
    @Volatile
    private var modulePath: String? = null

    @Volatile
    private var assetManager: AssetManager? = null

    fun setModulePath(path: String) {
        modulePath = path
    }

    fun get(): AssetManager? {
        assetManager?.let { return it }
        val path = modulePath ?: return null
        return try {
            val manager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPath.invoke(manager, path)
            assetManager = manager
            manager
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create module AssetManager for $path", t)
            null
        }
    }
}
