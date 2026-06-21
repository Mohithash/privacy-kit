package com.xplex.privacy

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "XPLEX.Entry"

/**
 * Classic Xposed API entry point, declared via assets/xposed_init.
 * Scaffold milestone: just proves the module loads and gets called.
 * The actual hook engine (JSON+Lua, ported from XPL-EX) comes next.
 */
class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        Log.i(TAG, "initZygote modulePath=${startupParam.modulePath}")
    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        Log.i(TAG, "Loaded package=${param.packageName} process=${param.processName}")
    }
}
