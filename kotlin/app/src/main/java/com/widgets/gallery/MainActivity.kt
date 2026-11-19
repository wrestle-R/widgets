package com.widgets.gallery

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.UUID
import kotlin.math.min

class MainActivity : Activity() {
    private data class PlacedWidget(val kind: WidgetKind, val id: Int, val name: String)

    private val ui by lazy { GalleryStyle(this) }
    private val manager by lazy { AppWidgetManager.getInstance(this) }
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var body: LinearLayout
    private var kind = WidgetKind.UNLOCKS
    private var onHome = false
    private var selectedId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var draft: Appearance? = null
    private var restoredScroll: Int? = null

    override fun attachBaseContext(base: Context) {
        val mode = base.getSharedPreferences("app_appearance", MODE_PRIVATE)
            .getInt("night_mode", Configuration.UI_MODE_NIGHT_UNDEFINED)
        if (mode == Configuration.UI_MODE_NIGHT_UNDEFINED) {
            super.attachBaseContext(base)
            return
        }
        val configuration = Configuration().apply { uiMode = mode }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredScroll = savedInstanceState?.getInt("scrollY")
        kind = WidgetKind.entries.firstOrNull { it.key == savedInstanceState?.getString("kind") }
            ?: WidgetKind.UNLOCKS
        onHome = savedInstanceState?.getBoolean("onHome") ?: false
        selectedId = savedInstanceState?.getInt("selectedId") ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (savedInstanceState?.containsKey("opacity") == true) {
            draft = Appearance(savedInstanceState.getInt("opacity"), savedInstanceState.getBoolean("light"))
        }

        content = ui.column().apply { setPadding(ui.dp(20), ui.dp(14), ui.dp(20), ui.dp(26)) }
        val page = FrameLayout(this).apply {
            val width = ui.dp(min(resources.configuration.screenWidthDp, 540))
            addView(content, FrameLayout.LayoutParams(width, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        }
        scroll = ScrollView(this).apply {
            setBackgroundColor(ui.background)
            isFillViewport = true
            addView(page)
            setOnApplyWindowInsetsListener { view, insets ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                }
                insets
            }
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        for (widget in WidgetKind.entries) updateWidgets(this, widget)
        render(preserveScroll = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("kind", kind.key)
        outState.putBoolean("onHome", onHome)
        outState.putInt("scrollY", scroll.scrollY)
        outState.putInt("selectedId", selectedId)
        draft?.let {
            outState.putInt("opacity", it.opacity)
            outState.putBoolean("light", it.light)
        }
    }

    private fun render(preserveScroll: Boolean = false) {
        val offset = restoredScroll ?: if (preserveScroll) scroll.scrollY else 0
        restoredScroll = null
        content.removeAllViews()
        with(ui) {
            content.place(header())
            content.place(equalRow(listOf(
                button("Gallery", selected = !onHome) { showHome(false) },
                button("On home", selected = onHome) { showHome(true) },
            )), top = 16)
            body = column()
            content.place(body, top = 24)
        }
        if (onHome) renderHome() else renderGallery()
        scroll.post { scroll.scrollTo(0, offset) }
    }

    private fun header() = with(ui) {
        LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text("Widgets", 20, heading = true).apply {
                val mark = getDrawable(R.drawable.app_icon)?.apply { setBounds(0, 0, dp(28), dp(28)) }
                setCompoundDrawablesRelative(mark, null, null, null)
                compoundDrawablePadding = dp(8)
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, -2, 1f))
            val target = if (dark) "Light" else "Dark"
            addView(button(target) { toggleTheme() }.apply {
                contentDescription = "Switch to ${target.lowercase()} mode"
                val icon = getDrawable(if (dark) R.drawable.ic_sun else R.drawable.ic_moon)?.mutate()?.apply {
                    setBounds(0, 0, dp(17), dp(17))
                    setTint(accent)
                }
                setCompoundDrawablesRelative(icon, null, null, null)
                compoundDrawablePadding = dp(6)
            }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
    }

    private fun toggleTheme() {
        val mode = if (ui.dark) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        getSharedPreferences("app_appearance", MODE_PRIVATE).edit().putInt("night_mode", mode).apply()
        recreate()
    }

    private fun showHome(value: Boolean) {
        if (onHome == value) return
        onHome = value
        render()
    }

    private fun renderGallery() = with(ui) {
        val current = kind
        val details = Catalog.details(this@MainActivity, current)
        val needsAccess = current == WidgetKind.UNLOCKS && !Readings.hasUsageAccess(this@MainActivity)
        val reading = if (needsAccess) details.sample else Readings.read(this@MainActivity, current)

        body.place(text("Widget gallery", 38, heading = true))
        val preview = widgetPreview(current, reading).apply {
            styleWidget(this, Appearance(light = !dark))
            if (current == WidgetKind.COUNTER) {
                isFocusable = true
                contentDescription = "Count is ${reading.value}. Add one"
                setOnClickListener {
                    Readings.increment(this@MainActivity)
                    updateWidgets(this@MainActivity, current)
                    render(preserveScroll = true)
                }
            }
        }
        body.place(previewStage(preview, side = 166, sample = needsAccess), top = 16, height = 246)

        val selectors = WidgetKind.entries.map { item ->
            val label = when (item) {
                WidgetKind.UNLOCKS -> "Unlocks"
                WidgetKind.BATTERY -> "Battery"
                WidgetKind.DATE -> "Date"
                WidgetKind.COUNTER -> "Counter"
            }
            button(label, selected = item == current) {
                if (kind != item) {
                    kind = item
                    render(preserveScroll = true)
                }
            }.apply {
                val icon = getDrawable(item.icon)?.mutate()?.apply {
                    setBounds(0, 0, dp(18), dp(18))
                    setTint(if (item == current) accent else muted)
                }
                setCompoundDrawablesRelative(null, icon, null, null)
                compoundDrawablePadding = dp(4)
                setPadding(dp(2), dp(8), dp(2), dp(8))
                textSize = 12f
            }
        }
        val controlsPerRow = if (resources.configuration.screenWidthDp < 340 ||
            resources.configuration.fontScale >= 1.4f) 2 else 4
        selectors.chunked(controlsPerRow).forEach { row -> body.place(equalRow(row), top = 10) }
        body.place(text(details.title, 27, heading = true), top = 21)
        body.place(text(details.description, 14, muted), top = 6)
        body.place(button("Add to home", primary = true) { pin(current) }, top = 16)
        if (needsAccess) {
            body.place(button("Enable Usage Access") { askForUsageAccess() }, top = 8)
        }
    }

    private fun renderHome() = with(ui) {
        val placed = WidgetKind.entries.flatMap { item ->
            val title = Catalog.details(this@MainActivity, item).title
            val ids = manager.getAppWidgetIds(item.component(this@MainActivity)).sorted()
            ids.mapIndexed { index, id -> PlacedWidget(item, id, if (ids.size == 1) title else "$title ${index + 1}") }
        }
        body.place(text("On your home screen", 36, heading = true))
        if (placed.isEmpty()) {
            body.place(text("Add a widget to change its theme and opacity here.", 15, muted), top = 10)
            body.place(button("Browse gallery", primary = true) { showHome(false) }, top = 20)
            return@with
        }
        val selected = placed.firstOrNull { it.id == selectedId } ?: placed.first().also {
            selectedId = it.id
            draft = null
        }
        if (placed.size > 1) {
            for (items in placed.chunked(2)) {
                body.place(equalRow(items.map { widget ->
                    button(widget.name, selected = widget.id == selectedId) {
                        if (selectedId != widget.id) {
                            selectedId = widget.id
                            draft = null
                            render(preserveScroll = true)
                        }
                    }
                }), top = 8)
            }
        }

        val saved = WidgetAppearance.read(this@MainActivity, selected.id)
        val appearance = draft ?: saved
        val preview = widgetPreview(selected.kind, Readings.read(this@MainActivity, selected.kind))
        styleWidget(preview, appearance)
        body.place(previewStage(preview, side = 140), top = 16, height = 204)
        body.place(text(selected.name, 26, heading = true), top = 18)

        body.place(equalRow(listOf(
            button("Dark widget", selected = !appearance.light) { changeTheme(appearance = false, saved) },
            button("Light widget", selected = appearance.light) { changeTheme(appearance = true, saved) },
        )), top = 12)
        val opacity = text(getString(R.string.widget_opacity_value, appearance.opacity), 14)
        body.place(opacity, top = 16)
        val save = button("Save appearance", primary = true) {
            val change = draft ?: return@button
            val updated = WidgetAppearance.save(this@MainActivity, selected.kind, selected.id, change)
            draft = null
            render(preserveScroll = true)
            Toast.makeText(this@MainActivity, if (updated) "Appearance saved" else "Widget removed", Toast.LENGTH_SHORT).show()
        }
        save.isEnabled = appearance != saved
        body.place(SeekBar(this@MainActivity).apply {
            min = WidgetAppearance.MIN_OPACITY
            max = WidgetAppearance.MAX_OPACITY
            progress = appearance.opacity
            minimumHeight = dp(48)
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
            contentDescription = "Background opacity"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val change = (draft ?: saved).copy(opacity = value)
                    draft = change
                    opacity.text = getString(R.string.widget_opacity_value, value)
                    styleWidget(preview, change)
                    save.isEnabled = change != saved
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }, top = 2)
        body.place(save, top = 8)
    }

    private fun changeTheme(appearance: Boolean, saved: Appearance) {
        draft = (draft ?: saved).copy(light = appearance)
        render(preserveScroll = true)
    }

    private fun widgetPreview(item: WidgetKind, reading: Reading): View =
        layoutInflater.inflate(R.layout.widget, FrameLayout(this), false).apply {
            findViewById<ImageView>(R.id.widget_icon).setImageResource(item.icon)
            findViewById<TextView>(R.id.widget_value).text = reading.value
            findViewById<TextView>(R.id.widget_label).text = reading.label
        }

    private fun styleWidget(view: View, appearance: Appearance) {
        view.findViewById<ImageView>(R.id.widget_surface).apply {
            setImageResource(appearance.surface)
            imageAlpha = appearance.alpha
        }
        view.findViewById<TextView>(R.id.widget_value).setTextColor(appearance.valueColor)
        view.findViewById<TextView>(R.id.widget_label).setTextColor(appearance.labelColor)
    }

    private fun previewStage(preview: View, side: Int, sample: Boolean = false) = with(ui) {
        FrameLayout(this@MainActivity).apply {
            background = shape(panel, 24, line)
            clipToOutline = true
            addView(ImageView(this@MainActivity).apply {
                setImageResource(if (dark) R.drawable.gallery_art else R.drawable.gallery_art_light)
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(-1, -1))
            addView(preview, FrameLayout.LayoutParams(dp(side), dp(side), Gravity.CENTER))
            if (sample) {
                addView(text("Sample preview", 12).apply {
                    background = shape(panel, 7, line)
                    setPadding(dp(8), dp(5), dp(8), dp(5))
                }, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
                    marginEnd = dp(12)
                    bottomMargin = dp(12)
                })
            }
        }
    }

    private fun equalRow(buttons: List<Button>) = LinearLayout(this).apply {
        isBaselineAligned = false
        buttons.forEachIndexed { index, button ->
            addView(button, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (index > 0) marginStart = ui.dp(6)
            })
        }
    }

    private fun askForUsageAccess() {
        AlertDialog.Builder(this).setTitle("Phone unlocks")
            .setMessage("Allow Usage Access so Widgets can count today’s lock-screen events on this device.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, "Open Usage Access in Android settings.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun pin(item: WidgetKind) {
        val details = Catalog.details(this, item)
        if (manager.isRequestPinAppWidgetSupported) {
            val appearance = Appearance(light = !ui.dark)
            val extras = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, widgetViews(this@MainActivity, item, details.sample, appearance))
            }
            val callback = if (appearance.light) lightPinCallback(item) else null
            if (manager.requestPinAppWidget(item.component(this), extras, callback)) return
            callback?.cancel()
        }

        AlertDialog.Builder(this).setTitle("Add ${details.title}")
            .setMessage("Touch and hold an empty spot on your home screen. Open the widget picker and find ${details.title}.")
            .setPositiveButton("Got it", null).show()
    }

    private fun lightPinCallback(item: WidgetKind): PendingIntent {
        val intent = Intent(this, item.provider).apply {
            action = PIN_LIGHT_WIDGET
            data = Uri.fromParts("widgets", UUID.randomUUID().toString(), null)
        }
        // The launcher adds the newly assigned widget ID to the callback intent.
        val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(this, 0, intent, mutable or PendingIntent.FLAG_ONE_SHOT)
    }

}
