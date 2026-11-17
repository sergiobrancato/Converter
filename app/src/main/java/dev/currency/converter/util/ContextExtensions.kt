package dev.currency.converter.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps Compose's Context until the hosting Activity is found. */
fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
