package com.widgets.gallery

import android.appwidget.AppWidgetManager
import android.content.Context
import kotlin.math.roundToInt

internal data class Appearance(val opacity: Int = 100, val light: Boolean = false) {
    val alpha: Int get() = (opacity * 255 / 100f).roundToInt()
    val surface: Int get() = if (light) R.drawable.widget_background_light else R.drawable.widget_background
    val valueColor: Int get() = if (light) 0xff191d19.toInt() else 0xffffffff.toInt()
    val labelColor: Int get() = if (light) 0xff343c35.toInt() else 0xffbfc4ca.toInt()
}

internal object WidgetAppearance {
    const val MIN_OPACITY = 60
    const val MAX_OPACITY = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences("widget_appearance", Context.MODE_PRIVATE)

    fun read(context: Context, id: Int): Appearance {
        val prefs = prefs(context)
        return Appearance(
            prefs.getInt(id.toString(), MAX_OPACITY).coerceIn(MIN_OPACITY, MAX_OPACITY),
            prefs.getBoolean("light.$id", false)
        )
    }

    fun save(context: Context, kind: WidgetKind, id: Int, appearance: Appearance): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (id !in manager.getAppWidgetIds(kind.component(context))) return false
        prefs(context).edit()
            .putInt(id.toString(), appearance.opacity)
            .putBoolean("light.$id", appearance.light)
            .apply()
        updateWidgets(context, kind, intArrayOf(id))
        return true
    }

    fun remove(context: Context, ids: IntArray) {
        val editor = prefs(context).edit()
        ids.forEach { id -> editor.remove(id.toString()).remove("light.$id") }
        editor.apply()
    }
}
