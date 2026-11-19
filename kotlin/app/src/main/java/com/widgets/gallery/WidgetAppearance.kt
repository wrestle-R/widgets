package com.widgets.gallery

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.view.View
import kotlin.math.roundToInt

internal object WidgetAppearance {
    private const val MIN = 60
    private const val MAX = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences("widget_appearance", Context.MODE_PRIVATE)

    private fun opacity(context: Context, id: Int): Int =
        prefs(context).getInt(id.toString(), MAX).coerceIn(MIN, MAX)

    fun light(context: Context, id: Int): Boolean = prefs(context).getBoolean("light.$id", false)

    fun alpha(context: Context, id: Int): Int = (opacity(context, id) * 255 / 100f).roundToInt()

    fun remove(context: Context, ids: IntArray) {
        val editor = prefs(context).edit()
        ids.forEach { id -> editor.remove(id.toString()).remove("light.$id") }
        editor.apply()
    }

    fun show(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).roundToInt()
            setPadding(padding, padding / 2, padding, padding / 2)
        }
        val changes = mutableListOf<() -> Unit>()

        for (kind in WidgetKind.entries) {
            val ids = manager.getAppWidgetIds(kind.component(context)).sorted()
            val title = Catalog.details(context, kind).title
            for ((index, id) in ids.withIndex()) {
                val name = if (ids.size == 1) title else "$title ${index + 1}"
                val original = opacity(context, id)
                val originallyLight = light(context, id)
                val label = TextView(context).apply {
                    text = "$name · $original%"
                    textSize = 16f
                }
                content.addView(label)
                val slider = SeekBar(context).apply {
                    min = MIN
                    max = MAX
                    progress = original
                    contentDescription = "$name background opacity"
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                            label.text = "$name · $value%"
                        }
                        override fun onStartTrackingTouch(bar: SeekBar) = Unit
                        override fun onStopTrackingTouch(bar: SeekBar) = Unit
                    })
                }
                content.addView(slider)
                val theme = Switch(context).apply {
                    text = "Light theme"
                    contentDescription = "$name light theme"
                    isChecked = originallyLight
                    minHeight = (48 * resources.displayMetrics.density).roundToInt()
                }
                content.addView(theme)
                content.addView(View(context), LinearLayout.LayoutParams(1, (14 * context.resources.displayMetrics.density).roundToInt()))
                changes.add {
                    val value = slider.progress
                    if ((value != original || theme.isChecked != originallyLight) &&
                        id in manager.getAppWidgetIds(kind.component(context))) {
                        prefs(context).edit()
                            .putInt(id.toString(), value)
                            .putBoolean("light.$id", theme.isChecked)
                            .apply()
                        updateWidgets(context, kind, intArrayOf(id))
                    }
                }
            }
        }

        val dialog = AlertDialog.Builder(context).setTitle("Widget appearance")
        if (changes.isEmpty()) {
            dialog.setMessage("Add a widget to your home screen to change its theme and opacity.")
                .setPositiveButton("OK", null).show()
            return
        }
        dialog.setView(ScrollView(context).apply { addView(content) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> changes.forEach { it() } }
            .show()
    }
}
