package com.xplex.privacy.detection

import android.util.Log
import java.io.File

/**
 * Root/emulator/Xposed detection data and matching logic. Ported from the
 * previous XPL-EX project's Evidence.java (itself building on the original
 * XPrivacyLua). All field names, list contents, and matching semantics are
 * preserved exactly - this is a direct translation, not a redesign.
 */
object Evidence {
    private const val TAG = "PrivacyKit.Evidence"

    val DEFECT_IDS: List<String> = listOf("9774d56d682e549c", "unknown", "000000000000000")

    val SU_PATHS: List<String> = listOf(
        "/system/app/superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su",
        "/data/adb/magisk", "/data/adb/ksu", "/data/adb/ap",
        "/data/adb/modules", "/data/adb/ksud", "/data/adb/apd",
        "/system/app/Superuser.apk", "/system/app/SuperSU",
        "/system/etc/init.d", "/system/xbin/daemonsu"
    )

    val ROOT_PACKAGES: List<String> = listOf(
        "io.github.vvb2060.magisk", "io.github.vvb2060.magisk.lit",
        "com.noshufou.android.su", "com.noshufou.android.su.elite",
        "eu.chainfire.supersu", "com.koushikdutta.superuser",
        "com.thirdparty.superuser", "com.yellowes.su",
        "com.topjohnwu.magisk", "io.github.huskydg.magisk",
        "me.weishu.kernelsu", "me.bmax.apatch",
        "com.topjohnwu.magisk.alpha", "com.topjohnwu.magisk.canary",
        "io.github.vvb2060.magisk.canary"
    )

    val BAD_APPS: List<String> = listOf(
        "com.oasisfeng.greenify", "com.koushikdutta.rommanager",
        "com.dimonvideo.luckypatcher", "com.chelpus.lackypatch",
        "com.ramdroid.appquarantine", "eu.faircode.xlua",
        "org.lsposed.manager", "com.tsng.hidemyapplist",
        "rikka.appops", "com.guoshi.httpcanary", "com.httpcanary.pro",
        "com.aistra.hail", "github.tornaco.android.thanos.pro",
        "com.mrchandler.disableprox", "org.adaway",
        "dev.ukanth.ufirewall.donate", "more.shizuku.privileged.api",
        "ru.bluecat.android.xposed.mods.appsettings", "com.qingyu.rm",
        "lozn.hookui", "me.jsonet.jshook", "com.zhenxi.jnitrace",
        "com.zhenxi.fundex2", "com.zhenxi.funelf",
        "cn.wq.myandroidtools", "ccc71.at.free",
        "com.sanmer.mrepo", "com.fox2code.mmm",
        "me.rhunk.snapenhance", "eu.faircode.xlua.pro",
        "com.berdik.letmedowngrade", "biz.bokhorst.xprivacy",
        "cn.qssq666.systool", "com.wind.cotter",
        "player.normal.np", "lozn.godhand",
        "me.weishu.exp", "com.joeykrim.rootcheck",
        "com.scottyab.rootbeer.sample", "com.devadvance.rootcloak",
        "com.devadvance.rootcloakplus", "com.zachspong.temprootremovejb",
        "com.amphoras.hidemyroot", "com.saurik.substrate",
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager", "com.formyhm.hideroot",
        "me.weishu.kernelsu", "me.bmax.apatch"
    )

    val CLOAK_APPS: List<String> = listOf(
        "com.koushikdutta.rommanager", "com.dimonvideo.luckypatcher",
        "com.chelpus.lackypatch", "com.ramdroid.appquarantine"
    )

    val SU_MANAGERS: List<String> = listOf("busybox", "su", "magisk", "ksu", "ksud", "apd", "apatch")

    val SU_PATHS_EX: List<String> = listOf(
        "/data/local/", "/data/local/bin/", "/data/local/xbin/",
        "/sbin/", "/su/bin/", "/system/bin/", "/system/bin/.ext/",
        "/system/bin/failsafe/", "/system/sd/xbin/",
        "/system/usr/we-need-root/", "/system/xbin/",
        "/system/xbin/daemonsu/", "/system/etc/init.d/99SuperSUDaemon/",
        "/system/bin/.ext/.su/", "/system/etc/.has_su_daemon/",
        "/system/etc/.installed_su_daemon/", "/cache/", "/data/", "/dev/",
        "/data/adb/", "/data/adb/modules/", "/data/adb/ksu/",
        "/data/adb/ap/"
    )

    val NON_WRITABLE_DIRS: List<String> = listOf(
        "/system", "/system/bin", "/system/sbin", "/system/xbin", "/vendor/bin", "/sbin", "/etc"
    )

    val EMULATOR_APPS: List<String> = listOf(
        "com.bignox.appcenter", "com.bluestacks.settings", "com.bluestacks.filemanager",
        "com.genymotion.superuser", "org.greatfruit.andy.ime", "com.kaopu001.tiantianserver",
        "com.tiantian.ime", "com.microvirt.installer", "com.android.ld.appstore",
        "com.ldmnq.launcher3", "com.jide.Appstore"
    )

    val EMULATOR_FILES_RC: List<String> = listOf(
        "init.android_x86.rc", "ueventd.android_x86.rc", "fstab.android_x86", "x86.prop",
        "ueventd.ttVM_x86.rc", "init.ttVM_x86.rc", "fstab.ttVM_x86", "fstab.vbox86",
        "init.vbox86.rc", "ueventd.vbox86.rc", "ueventd.android_x86_64.rc",
        "init.android_x86_64.rc", "fstab.goldfish", "init.goldfish.rc", "init.superuser.rc"
    )

    val EMULATOR_SYS_FILES: List<String> = listOf(
        "/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace", "/system/bin/qemu-props"
    )

    val EMULATOR_DEV_FILES: List<String> = listOf("/dev/socket/qemud", "/dev/qemu_pipe")

    val EMULATOR_FILES: List<String> = listOf(
        "init.ranchu.rc", "init.remixos.rc", "init.andy.rc", "ueventd.andy.rc",
        "bin/genybaseband", "bin/genymotion-vbox-sf", "ueventd.nox.rc", "init.nox.rc",
        "/system/bin/noxd"
    )

    val EMULATOR_PROPS: List<String> = listOf(
        "ro.kernel.qemu", "init.svc.qemu-props", "qemu.hw.mainkeys", "qemu.sf.fake_camera",
        "qemu.sf.lcd_density", "ro.kernel.android.qemud", "qmu.adb.secure", "qemu.gles",
        "qemu.logcat", "qemu.timezone", "ro.kernel.qemu.encrypt", "ro.kernel.qemu.gles",
        "ro.kernel.qemu.gltransport", "ro.kernel.qemu.opengles.version", "ro.kernel.qemu.vsync",
        "ro.kernel.qemu.wifi", "ro.qemu.initrc"
    )

    val ROOT_PROPS: List<String> = listOf(
        "vzw.os.rooted", "magisk",
        "persist.log.tag.LSPosed", "persist.log.tag.LSPosed-Bridge",
        "init.svc.magisk_daemon", "init.svc.magisk_pfs",
        "persist.magisk.hide", "ro.magisk.disable"
    )

    val PROC_MAPS_ARTIFACTS: List<String> = listOf(
        "magisk", "lspd", "lsposed", "edxposed", "riru",
        "zygisk", "libmemtrack_real", "libriru", "xposed",
        "substrate", "frida", "sandhook", "yahfa",
        "lsplant", "pine", "whale", "epichook"
    )

    val PROC_MOUNT_ARTIFACTS: List<String> = listOf(
        "magisk", "core/mirror", "core/img",
        "ksu", "apatch", "/sbin/.magisk",
        "tmpfs /system/", "tmpfs /vendor/",
        "devpts /dev/pts"
    )

    val PROC_SELF_PATHS: List<String> = listOf(
        "/proc/self/maps", "/proc/self/mounts",
        "/proc/self/mountinfo", "/proc/self/mountstats",
        "/proc/self/status", "/proc/self/cmdline",
        "/proc/mounts"
    )

    val ROOT_PROP_SPOOFS: Map<String, String> = mapOf(
        "ro.debuggable" to "0",
        "ro.secure" to "1",
        "ro.build.selinux" to "0",
        "ro.build.tags" to "release-keys",
        "ro.build.type" to "user",
        "service.bootanim.exit" to "1"
    )

    val EMULATOR_MANUFACTURER_NAMES: List<String> = listOf("unknown", "Genymotion", "AndyOS")
    val EMULATOR_BRAND_NAMES: List<String> = listOf("generic", "generic_x86", "Android", "AndyOS")
    val EMULATOR_DEVICE_NAMES: List<String> = listOf("AndyOSX", "Droid4X", "generic", "generic_x86", "vbox86p")
    val EMULATOR_HARDWARE_NAMES: List<String> = listOf("goldfish", "vbox86", "andy", "ranchu", "ttVM_x86", "android_x86")
    val EMULATOR_MODEL_NAMES: List<String> = listOf("sdk", "google_sdk", "Android SDK built for x86", "generic")
    val EMULATOR_PRODUCT_NAMES: List<String> = listOf("vbox86p", "Genymotion", "Driod4X", "AndyOSX", "remixemu")

    val BAD_STACK_XPOSED: List<String> = listOf(
        "xposed", "lsposed", "sandhook", "xlua", "luaj",
        "lsphooker", "yahfa", "lsplant", "pine", "whale",
        "edxposed", "substrate", "frida", "epichook"
    )

    val DANGEROUS_PROPERTIES: Map<String, String> = mapOf(
        "[ro.debuggable]" to "[1]",
        "[ro.secure]" to "[0]"
    )

    const val PROP_DEBUGGABLE = "ro.debuggable"
    const val PROP_DEBUGGABLE_GOOD = "0"
    const val PROP_DEBUGGABLE_BAD = "1"

    const val PROP_SECURE = "ro.secure"
    const val PROP_SECURE_GOOD = "1"
    const val PROP_SECURE_BAD = "0"

    const val SETTING_ROOT = "hide.root*.bool"
    const val SETTING_QEMU_EMULATOR = "qemu.emulator*.bool"
    const val SETTING_EMULATOR = "hide.emulator*.bool"

    const val BAD_TAGS = "test-keys"
    const val BAD_IP = "0.0.0.0"
    const val BAD_NAME = "goldfish"
    const val EMULATOR_SHARE_FOLDER = "windows/BstSharedFolder"
    const val BAD_PRODUCT_NAME_PATTERN = ".*_?sdk_?.*"

    const val FILTER_EMULATOR = 0x1
    const val FILTER_ROOT = 0x2
    const val FILTER_EMULATOR_ROOT = 0x3

    /**
     * Strips Xposed/hook-framework frames from a stack trace so apps that
     * dump+inspect their own call stack don't see evidence of being hooked.
     */
    fun stack(trace: Array<StackTraceElement>): Array<StackTraceElement> {
        val elements = ArrayList<StackTraceElement>()
        var hasEndInvoke = false
        var hasInitZygote = false

        for (i in trace.indices.reversed()) {
            val el = trace[i]
            val cName = el.className.lowercase()
            val mName = el.methodName.lowercase()

            if (cName == "com.android.internal.os" && mName == "zygoteinit") {
                if (!hasInitZygote) {
                    hasInitZygote = true
                    elements.add(el)
                }
                continue
            }

            var found = false
            for (xE in BAD_STACK_XPOSED) {
                if (cName.contains(xE)) {
                    Log.w(TAG, "Found a Stack Trace Element: class:[$cName]")
                    found = true
                    hasEndInvoke = true
                    break
                }
            }

            if (found) continue
            if (hasEndInvoke && cName == "java.lang.reflect.method" && mName == "invoke") {
                hasEndInvoke = false
                continue
            } else {
                elements.add(el)
            }
        }

        elements.reverse()
        return elements.toTypedArray()
    }

    fun containsProperty(data: String, code: Int): Boolean {
        if (code == FILTER_EMULATOR || code == FILTER_EMULATOR_ROOT)
            for (s in EMULATOR_PROPS)
                if (s.contains(data))
                    return true

        if (code == FILTER_ROOT || code == FILTER_EMULATOR_ROOT)
            for (s in ROOT_PACKAGES)
                if (s.contains(data))
                    return true

        return false
    }

    fun property(propertyName: String, code: Int): Boolean {
        if ((code == FILTER_EMULATOR || code == FILTER_EMULATOR_ROOT) && EMULATOR_PROPS.contains(propertyName)) return true
        if (code == FILTER_ROOT || code == FILTER_EMULATOR_ROOT) return ROOT_PROPS.contains(propertyName)
        return false
    }

    fun fileArray(files: Array<File>?, code: Int): Array<File>? {
        if (files == null || files.isEmpty()) return files
        return files.toList().filterNot { file(it, code) }.toTypedArray()
    }

    fun fileList(files: List<File>?, code: Int): List<File>? {
        if (files == null || files.isEmpty()) return files
        return files.filterNot { file(it, code) }
    }

    fun stringArray(values: Array<String>?, code: Int): Array<String>? {
        if (values == null || values.isEmpty()) return values
        return stringList(values.toList(), code)?.toTypedArray()
    }

    fun stringList(values: List<String>?, code: Int): List<String>? {
        if (values == null || values.isEmpty()) return values
        return values.filterNot { v ->
            if (!v.contains("/")) file(null, v, code) else file(v, code)
        }
    }

    fun packageName(packageName: String, code: Int): Boolean {
        val pLow = packageName.lowercase()
        if ((code == FILTER_ROOT || code == FILTER_EMULATOR_ROOT) &&
            (ROOT_PACKAGES.contains(pLow) || BAD_APPS.contains(pLow) || CLOAK_APPS.contains(pLow))
        ) return true
        if (code == FILTER_EMULATOR || code == FILTER_EMULATOR_ROOT) return EMULATOR_APPS.contains(pLow)
        return false
    }

    fun file(path: String, code: Int): Boolean = file(File(path), code)
    fun file(file: File, code: Int): Boolean = file(file.absolutePath, file.name, code)

    fun file(fileFull: String?, fileName: String?, code: Int): Boolean {
        // Matches the original exactly: when fileFull is null, ffLow falls back to
        // the RAW fileName (not lowercased) - an existing asymmetry, not a typo.
        val ffLow = if (fileFull == null) fileName else fileFull.lowercase()
        val fnLow = (fileName ?: fileFull?.let { File(it).name })?.lowercase()

        if (code == FILTER_EMULATOR || code == FILTER_EMULATOR_ROOT) {
            if (ffLow != null) {
                for (f in EMULATOR_SYS_FILES) if (ffLow.startsWith(f)) return true
                for (f in EMULATOR_DEV_FILES) if (ffLow.startsWith(f)) return true
                for (f in EMULATOR_FILES_RC) if (ffLow.contains(f.lowercase())) return true
                for (f in EMULATOR_FILES) if (ffLow.contains(f.lowercase())) return true
            }

            if (EMULATOR_APPS.contains(fnLow)) return true

            if (ffLow != null && ffLow.contains(EMULATOR_SHARE_FOLDER.lowercase())) return true
        }

        if (code == FILTER_ROOT || code == FILTER_EMULATOR_ROOT) {
            for (f in SU_PATHS) if (f.equals(fileFull, ignoreCase = true)) return true
            if (SU_MANAGERS.contains(fnLow)) return true
            return ROOT_PACKAGES.contains(ffLow) || CLOAK_APPS.contains(ffLow) || BAD_APPS.contains(ffLow)
        }

        return false
    }

    fun isProcSelfPath(path: String?): Boolean {
        if (path == null) return false
        val lower = path.lowercase()
        return PROC_SELF_PATHS.any { lower.contains(it) }
    }

    fun isProcMapsLine(line: String?): Boolean {
        if (line == null) return false
        val lower = line.lowercase()
        return PROC_MAPS_ARTIFACTS.any { lower.contains(it) }
    }

    fun isProcMountLine(line: String?): Boolean {
        if (line == null) return false
        val lower = line.lowercase()
        return PROC_MOUNT_ARTIFACTS.any { lower.contains(it) }
    }

    fun spoofRootProperty(propName: String?): String? {
        if (propName == null) return null
        return ROOT_PROP_SPOOFS[propName]
    }

    fun isRootProperty(propName: String?): Boolean {
        if (propName == null) return false
        if (ROOT_PROPS.contains(propName)) return true
        return ROOT_PROP_SPOOFS.containsKey(propName)
    }

    fun isEmulatorProperty(propName: String?): Boolean {
        if (propName == null) return false
        return EMULATOR_PROPS.contains(propName)
    }
}
