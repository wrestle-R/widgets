package com.widgets.gallery

import android.content.Context
import kotlin.math.roundToInt

internal data class Appearance(val opacity: Int = 100, val light: Boolean = false, val design: String = "classic") {
    val alpha: Int get() = (opacity * 255 / 100f).roundToInt()
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
            prefs.getBoolean("light.$id", false),
            prefs.getString("design.$id", "classic").orEmpty(),
        )
    }

    fun save(context: Context, id: Int, appearance: Appearance): Boolean {
        if (id !in UnlockRefresh.ids(context)) return false
        prefs(context).edit()
            .putInt(id.toString(), appearance.opacity)
            .putBoolean("light.$id", appearance.light)
            .putString("design.$id", appearance.design)
            .apply()
        UnlockRefresh.enqueue(context, intArrayOf(id))
        return true
    }

    fun remove(context: Context, ids: IntArray) {
        val editor = prefs(context).edit()
        ids.forEach { id -> editor.remove(id.toString()).remove("light.$id").remove("design.$id") }
        editor.apply()
    }
}
