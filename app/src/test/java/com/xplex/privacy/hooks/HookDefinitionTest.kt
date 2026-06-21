package com.xplex.privacy.hooks

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HookDefinitionTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `field hook is detected by hash prefix`() {
        val hook = HookDefinition(
            id = "device.build.model",
            className = "android.os.Build",
            methodName = "#MODEL",
            luaScript = "value_override"
        )
        assertTrue(hook.isFieldHook)
        assertEquals("MODEL", hook.fieldName)
    }

    @Test
    fun `method hook is not a field hook`() {
        val hook = HookDefinition(
            id = "device.build.getSerial",
            className = "android.os.Build",
            methodName = "getSerial",
            luaScript = "value_override"
        )
        assertFalse(hook.isFieldHook)
    }

    @Test
    fun `enabled defaults to true when omitted`() {
        val text = """{"id":"x","className":"a.B","methodName":"m","luaScript":"s"}"""
        val hook = json.decodeFromString<HookDefinition>(text)
        assertTrue(hook.enabled)
        assertTrue(hook.parameterTypes.isEmpty())
    }

    @Test
    fun `parses a full hooks file with multiple entries`() {
        val text = """
            {
              "hooks": [
                {"id": "a", "className": "x.Y", "methodName": "#Z", "luaScript": "s1"},
                {"id": "b", "className": "x.Y", "methodName": "doThing", "parameterTypes": ["int"], "enabled": false, "luaScript": "s2"}
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<HookDefinitionFile>(text)
        assertEquals(2, parsed.hooks.size)

        val a = parsed.hooks[0]
        assertTrue(a.isFieldHook)
        assertEquals("Z", a.fieldName)
        assertTrue(a.enabled)

        val b = parsed.hooks[1]
        assertFalse(b.isFieldHook)
        assertEquals(listOf("int"), b.parameterTypes)
        assertFalse(b.enabled)
    }

    @Test
    fun `device json asset is well-formed and matches schema`() {
        // Mirrors the actual file shipped under assets/hooks/device.json, so
        // a schema mismatch here would have broken the real asset too.
        val text = """
            {
              "hooks": [
                {
                  "id": "device.build.model",
                  "className": "android.os.Build",
                  "methodName": "#MODEL",
                  "parameterTypes": [],
                  "enabled": true,
                  "luaScript": "value_override",
                  "description": "Spoofs Build.MODEL for this app, if a fingerprint profile sets one."
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<HookDefinitionFile>(text)
        assertEquals(1, parsed.hooks.size)
        assertEquals("device.build.model", parsed.hooks[0].id)
    }
}
