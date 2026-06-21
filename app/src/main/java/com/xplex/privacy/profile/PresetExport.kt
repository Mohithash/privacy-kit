package com.xplex.privacy.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PresetExportEntry(val name: String, val values: Map<String, String>)

@Serializable
data class PresetExportFile(val presets: List<PresetExportEntry>)

/**
 * Pure encode/decode logic for sharing presets between devices, kept
 * separate from [ProfileRepository] (which needs a Context for the
 * ContentProvider/file IO) so this part is plain-JVM unit-testable.
 */
object PresetExport {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun encode(presets: Map<String, Map<String, String>>): String {
        val entries = presets.map { (name, values) -> PresetExportEntry(name, values) }
        return json.encodeToString(PresetExportFile(entries))
    }

    /** Throws if [text] isn't valid - callers should catch and surface a real error, not silently no-op. */
    fun decode(text: String): Map<String, Map<String, String>> {
        val parsed = json.decodeFromString<PresetExportFile>(text)
        return parsed.presets.associate { it.name to it.values }
    }
}
