package org.openrndr.internal

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object AppContextHolder {

    // This is the application context, not an Activity context
    lateinit var context: Context
}