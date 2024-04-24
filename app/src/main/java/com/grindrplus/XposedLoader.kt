package com.grindrplus

import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import dalvik.system.DexClassLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedLoader : IXposedHookLoadPackage, IXposedHookZygoteInit {
    private lateinit var moduleApkPath: String

    override fun handleLoadPackage(p0: XC_LoadPackage.LoadPackageParam) {
        if (p0.packageName != GRINDR_PACKAGE_NAME) return
        GrindrPlus(p0, DexClassLoader(moduleApkPath,
            moduleApkPath.substringBeforeLast("/"),
            null, p0.classLoader)
        )
    }

    override fun initZygote(p0: IXposedHookZygoteInit.StartupParam) {
        moduleApkPath = p0.modulePath
    }
}