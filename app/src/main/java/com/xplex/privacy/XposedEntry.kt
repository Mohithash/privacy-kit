package com.xplex.privacy

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.xplex.privacy.hooks.HookEngine
import com.xplex.privacy.hooks.HookRegistry
import com.xplex.privacy.hooks.ModuleAssets
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "PrivacyKit.Entry"

/**
 * Classic Xposed API entry point, declared via assets/xposed_init. Hooks
 * Application creation (the same point the previous project used) to get a
 * real Context for the target app, then hands off to [HookEngine].
 */
class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        ModuleAssets.setModulePath(startupParam.modulePath)
        Log.i(TAG, "initZygote modulePath=${startupParam.modulePath}")
    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        // Never hook ourselves, and never hook the system process here -
        // that needs its own scope/timing and isn't wired up yet.
        if (param.packageName == "com.xplex.privacy" || param.packageName == "android") return

        val isTiramisuOrHigher = Build.VERSION.SDK_INT >= 33
        val targetClassName = if (isTiramisuOrHigher) "android.app.Instrumentation" else "android.app.LoadedApk"
        val targetMethodName = if (isTiramisuOrHigher) "newApplication" else "makeApplication"

        try {
            val targetClass = Class.forName(targetClassName, false, param.classLoader)
            XposedBridge.hookAllMethods(targetClass, targetMethodName, object : XC_MethodHook() {
                private var made = false

                override fun afterHookedMethod(hookParam: MethodHookParam) {
                    if (made) return
                    made = true
                    try {
                        val app = hookParam.result as? Application ?: return
                        onApplicationReady(param, app)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed handling Application for ${param.packageName}", t)
                    }
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to hook $targetClassName.$targetMethodName for ${param.packageName}", t)
        }
    }

    private fun onApplicationReady(param: XC_LoadPackage.LoadPackageParam, context: Context) {
        val moduleAssets = ModuleAssets.get()
        if (moduleAssets == null) {
            Log.e(TAG, "No module AssetManager available, skipping hooks for ${param.packageName}")
            return
        }

        val definitions = HookRegistry.loadAll(moduleAssets)
        val engine = HookEngine(context, moduleAssets)
        engine.installHooks(param.packageName, param.classLoader, definitions)
    }
}
