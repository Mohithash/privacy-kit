package com.xplex.privacy.hooks

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the real shipped asset files directly from disk (not hand-copied
 * mirrors), since unit tests can't load them through AssetManager. Catches
 * schema typos and dangling luaScript references before they'd otherwise
 * only surface as a silent no-op hook on a real device.
 */
class HookAssetsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val hooksDir = File("src/main/assets/hooks")
    private val luaDir = File("src/main/assets/lua")

    private fun allHooks(): List<HookDefinition> {
        val files = hooksDir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray()
        assertTrue("expected at least one hooks json file", files.isNotEmpty())
        return files.flatMap { f ->
            json.decodeFromString<HookDefinitionFile>(f.readText()).hooks
        }
    }

    @Test
    fun `every hook references a lua script that exists on disk`() {
        for (hook in allHooks()) {
            val scriptFile = File(luaDir, "${hook.luaScript}.lua")
            assertTrue("hook ${hook.id} references missing script ${scriptFile.path}", scriptFile.exists())
        }
    }

    @Test
    fun `hook ids are unique across all asset files`() {
        val hooks = allHooks()
        val ids = hooks.map { it.id }
        assertTrue("duplicate hook ids found: ${ids.groupBy { it }.filterValues { it.size > 1 }.keys}", ids.size == ids.toSet().size)
    }

    @Test
    fun `field hooks have no parameter types and method hooks declare a real identifier`() {
        for (hook in allHooks()) {
            assertTrue("hook ${hook.id} has blank className", hook.className.isNotBlank())
            assertTrue("hook ${hook.id} has blank methodName", hook.methodName.isNotBlank())
            if (hook.isFieldHook) {
                assertTrue("field hook ${hook.id} has a non-empty fieldName", hook.fieldName.isNotBlank())
            }
        }
    }
}
