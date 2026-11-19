package com.widgets.gallery

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    private val foregroundColor = 0xfff8fafc.toInt()
    private val muted = 0xffa3b0c3.toInt()
    private val secondary = 0xff1e293b.toInt()
    private val backgroundColor = 0xff020617.toInt()
    private val previews = mutableMapOf<WidgetKind, View>()
    private lateinit var access: Button
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
            clipToPadding = false
        }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or
                    android.view.WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            }
            insets
        }
        val content = column().apply { setPadding(dp(20), dp(18), dp(20), dp(24)) }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(text("Widgets", 21, foregroundColor, bold = true).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.app_icon, 0, 0, 0)
            compoundDrawablePadding = dp(10)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }, spaced(16))
        content.addView(text("Widget gallery", 38, foregroundColor, bold = true))
        content.addView(text("Add a widget and resize it from your home screen.", 15, muted), spaced(16))

        access = button("Enable Usage Access") {
            AlertDialog.Builder(this).setTitle("Phone unlocks")
                .setMessage("Allow Usage Access for Widgets in Android settings. The app counts today's lock-screen events on this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open settings") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this, "Open Usage Access in Android settings.", Toast.LENGTH_LONG).show()
                    }
                }.show()
        }
        content.addView(access, spaced(14))

        for (group in WidgetKind.entries.chunked(2)) {
            val row = LinearLayout(this).apply { isBaselineAligned = false }
            group.forEachIndexed { index, kind ->
                val tile = galleryCard(kind)
                row.addView(tile, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index > 0) marginStart = dp(12)
                })
            }
            content.addView(row, spaced(12))
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        access.visibility = if (Readings.hasUsageAccess(this)) View.GONE else View.VISIBLE
        for (kind in WidgetKind.entries) refresh(kind)
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    private fun galleryCard(kind: WidgetKind): View {
        val widget = Catalog.details(this, kind)
        val card = column().apply {
            background = shape(0xff0f172a.toInt(), 16, 0xff26354b.toInt())
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }

        val preview = layoutInflater.inflate(R.layout.widget, card, false)
        preview.findViewById<TextView>(R.id.widget_icon).apply {
            text = widget.icon
            setTextColor(widget.color)
        }
        if (kind == WidgetKind.COUNTER) {
            preview.isFocusable = true
            preview.setOnClickListener {
                Readings.increment(this)
                refresh(kind)
            }
        }
        previews[kind] = preview
        card.addView(preview, LinearLayout.LayoutParams(-1, dp(132)))
        card.addView(text(widget.title, 16, foregroundColor, bold = true).apply {
            setPadding(dp(2), dp(8), dp(2), 0)
        })
        card.addView(button("Add to home") { pin(kind) }.apply {
            contentDescription = "Add ${widget.title} to home"
            textSize = 13f
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        return card
    }

    private fun pin(kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(this)
        val details = Catalog.details(this, kind)
        val title = details.title
        val extras = Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, widgetViews(this@MainActivity, kind, details.sample))
        }
        if (manager.isRequestPinAppWidgetSupported &&
            manager.requestPinAppWidget(kind.component(this), extras, null)) return

        AlertDialog.Builder(this).setTitle("Add $title")
            .setMessage("Touch and hold an empty spot on your home screen. Open Widgets, then find $title under Widgets.")
            .setPositiveButton("Got it", null).show()
    }

    private fun refresh(kind: WidgetKind) {
        val reading = Readings.read(this, kind)
        if (resumed) {
            val preview = previews.getValue(kind)
            preview.findViewById<TextView>(R.id.widget_value).text = reading.value
            preview.findViewById<TextView>(R.id.widget_label).text = reading.label
            if (kind == WidgetKind.COUNTER) preview.contentDescription = "Count is ${reading.value}. Add one"
        }
        updateWidgets(this, kind, reading = reading)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(foregroundColor)
        minHeight = dp(48)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = RippleDrawable(ColorStateList.valueOf(0x3394a3b8), shape(secondary, 10), null)
        setOnClickListener { action() }
    }

    private fun shape(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun spaced(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (resources.displayMetrics.density * value + .5f).toInt()
}
