package com.grindrplus.utils

import de.robv.android.xposed.XposedHelpers.callMethod
import java.lang.reflect.Method

object RetrofitUtils {
    fun findPOSTMethod(clazz: Class<*>, value: String): Method? {
        return clazz.declaredMethods.find { method ->
            method.annotations.any {
                it.annotationClass.java.name == "retrofit2.http.POST"
                        && callMethod(it, "value") == value
            }
        }
    }

    fun Method.isPOST(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.POST"
                    && callMethod(it, "value") == value
        }
    }

    fun Method.isDELETE(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.DELETE"
                    && callMethod(it, "value") == value
        }
    }

    fun Method.isGET(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.GET"
                    && callMethod(it, "value") == value
        }
    }
}