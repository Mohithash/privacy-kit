package com.xplex.privacy.hooks

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.json.Json

private const val TAG = "PrivacyKit.Registry"

/**
 * Loads every JSON file under assets/hooks (each one a [HookDefinitionFile])
 * and exposes the combined, flattened hook list. Mirrors the previous
 * project's "scan every hooks.json under assets" approach, just with a fixed
 * directory instead of a recursive filename scan. Reads from [ModuleAssets] -
 * this data lives in Privacy Kit's own APK, not whatever app the hook is
 * running inside.
 */
object HookRegistry {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: List<HookDefinition>? = null

    fun loadAll(assets: AssetManager): List<HookDefinition> {
        cached?.let { return it }

        val files = try {
            assets.list("hooks") ?: emptyArray()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to list assets/hooks", t)
            emptyArray()
        }

        val all = mutableListOf<HookDefinition>()
        for (fileName in files) {
            if (!fileName.endsWith(".json")) continue
            try {
                val text = assets.open("hooks/$fileName").bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<HookDefinitionFile>(text)
                all.addAll(parsed.hooks)
                Log.i(TAG, "Loaded ${parsed.hooks.size} hooks from $fileName")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to parse hooks/$fileName", t)
            }
        }

        cached = all
        return all
    }
}
