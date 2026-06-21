package com.xplex.privacy.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceTest {

    // --- file() / FILTER_ROOT ---

    @Test
    fun `file detects known su path`() {
        assertTrue(Evidence.file("/system/bin/su", Evidence.FILTER_ROOT))
    }

    @Test
    fun `file detects su manager by name`() {
        assertTrue(Evidence.file("/some/dir/magisk", "magisk", Evidence.FILTER_ROOT))
    }

    @Test
    fun `file ignores unrelated path under root filter`() {
        assertFalse(Evidence.file("/data/data/com.example.app/files/data.db", Evidence.FILTER_ROOT))
    }

    @Test
    fun `file root path ignored when filter is emulator only`() {
        assertFalse(Evidence.file("/system/bin/su", Evidence.FILTER_EMULATOR))
    }

    // --- file() / FILTER_EMULATOR ---

    @Test
    fun `file detects emulator sys file`() {
        assertTrue(Evidence.file("/system/bin/qemu-props", Evidence.FILTER_EMULATOR))
    }

    @Test
    fun `file detects emulator dev file`() {
        assertTrue(Evidence.file("/dev/qemu_pipe", Evidence.FILTER_EMULATOR))
    }

    @Test
    fun `file detects emulator app by name`() {
        assertTrue(Evidence.file("/data/app/com.bluestacks.settings", "com.bluestacks.settings", Evidence.FILTER_EMULATOR))
    }

    @Test
    fun `file emulator path ignored when filter is root only`() {
        assertFalse(Evidence.file("/dev/qemu_pipe", Evidence.FILTER_ROOT))
    }

    // --- file() / FILTER_EMULATOR_ROOT combined ---

    @Test
    fun `file combined filter catches both kinds`() {
        assertTrue(Evidence.file("/system/bin/su", Evidence.FILTER_EMULATOR_ROOT))
        assertTrue(Evidence.file("/dev/qemu_pipe", Evidence.FILTER_EMULATOR_ROOT))
    }

    // --- proc maps/mounts/self path detection ---

    @Test
    fun `isProcMapsLine detects magisk artifact`() {
        assertTrue(Evidence.isProcMapsLine("7f1234000-7f1235000 r-xp 00000000 00:00 0 /data/adb/magisk/magisk64"))
    }

    @Test
    fun `isProcMapsLine detects lspd artifact`() {
        assertTrue(Evidence.isProcMapsLine("7f1234000-7f1235000 r-xp 00000000 00:00 0 /system/lib64/liblspd.so"))
    }

    @Test
    fun `isProcMapsLine ignores normal library`() {
        assertFalse(Evidence.isProcMapsLine("7f1234000-7f1235000 r-xp 00000000 00:00 0 /system/lib64/libc.so"))
    }

    @Test
    fun `isProcMapsLine handles null`() {
        assertFalse(Evidence.isProcMapsLine(null))
    }

    @Test
    fun `isProcMountLine detects magisk mirror`() {
        assertTrue(Evidence.isProcMountLine("/dev/block/dm-1 /system/core/mirror ext4 ro 0 0"))
    }

    @Test
    fun `isProcMountLine ignores normal mount`() {
        assertFalse(Evidence.isProcMountLine("/dev/block/dm-1 /data ext4 rw 0 0"))
    }

    @Test
    fun `isProcSelfPath detects maps path`() {
        assertTrue(Evidence.isProcSelfPath("/proc/self/maps"))
    }

    @Test
    fun `isProcSelfPath is case insensitive`() {
        assertTrue(Evidence.isProcSelfPath("/PROC/SELF/MAPS"))
    }

    @Test
    fun `isProcSelfPath ignores unrelated path`() {
        assertFalse(Evidence.isProcSelfPath("/data/data/com.example/files/foo"))
    }

    // --- root property spoofing ---

    @Test
    fun `spoofRootProperty returns configured value`() {
        assertEquals("0", Evidence.spoofRootProperty("ro.debuggable"))
        assertEquals("release-keys", Evidence.spoofRootProperty("ro.build.tags"))
    }

    @Test
    fun `spoofRootProperty returns null for unknown prop`() {
        assertNull(Evidence.spoofRootProperty("ro.totally.unknown.prop"))
    }

    @Test
    fun `isRootProperty true for spoofed prop`() {
        assertTrue(Evidence.isRootProperty("ro.secure"))
    }

    @Test
    fun `isRootProperty true for detection only prop`() {
        assertTrue(Evidence.isRootProperty("persist.log.tag.LSPosed"))
    }

    @Test
    fun `isRootProperty false for unrelated prop`() {
        assertFalse(Evidence.isRootProperty("ro.product.model"))
    }

    // --- emulator property detection ---

    @Test
    fun `isEmulatorProperty true for qemu prop`() {
        assertTrue(Evidence.isEmulatorProperty("ro.kernel.qemu"))
        assertTrue(Evidence.isEmulatorProperty("qemu.hw.mainkeys"))
    }

    @Test
    fun `isEmulatorProperty false for unrelated prop`() {
        assertFalse(Evidence.isEmulatorProperty("ro.product.model"))
    }

    @Test
    fun `isEmulatorProperty handles null`() {
        assertFalse(Evidence.isEmulatorProperty(null))
    }

    // --- packageName() ---

    @Test
    fun `packageName detects root package under root filter`() {
        assertTrue(Evidence.packageName("com.topjohnwu.magisk", Evidence.FILTER_ROOT))
    }

    @Test
    fun `packageName detects emulator app under emulator filter`() {
        assertTrue(Evidence.packageName("com.genymotion.superuser", Evidence.FILTER_EMULATOR))
    }

    @Test
    fun `packageName ignores unrelated package`() {
        assertFalse(Evidence.packageName("com.example.unrelated", Evidence.FILTER_EMULATOR_ROOT))
    }

    @Test
    fun `packageName matching is case insensitive`() {
        assertTrue(Evidence.packageName("COM.TOPJOHNWU.MAGISK", Evidence.FILTER_ROOT))
    }

    // --- property() / containsProperty() ---

    @Test
    fun `property true for known emulator prop under emulator filter`() {
        assertTrue(Evidence.property("ro.kernel.qemu", Evidence.FILTER_EMULATOR))
    }

    @Test
    fun `property false for emulator prop under root only filter`() {
        assertFalse(Evidence.property("ro.kernel.qemu", Evidence.FILTER_ROOT))
    }

    // --- stringList() / fileList() ---

    @Test
    fun `stringList filters out matching bare names`() {
        val result = Evidence.stringList(listOf("magisk", "normal_file"), Evidence.FILTER_ROOT)
        assertEquals(listOf("normal_file"), result)
    }

    @Test
    fun `stringList filters out matching paths`() {
        val result = Evidence.stringList(listOf("/system/bin/su", "/data/data/com.example/x"), Evidence.FILTER_ROOT)
        assertEquals(listOf("/data/data/com.example/x"), result)
    }

    @Test
    fun `stringList passes through null and empty`() {
        assertNull(Evidence.stringList(null, Evidence.FILTER_ROOT))
        assertEquals(emptyList<String>(), Evidence.stringList(emptyList(), Evidence.FILTER_ROOT))
    }
}
