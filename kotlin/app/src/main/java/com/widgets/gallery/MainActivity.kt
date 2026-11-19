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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.UUID

class MainActivity : Activity() {
    private data class PlacedWidget(val kind: WidgetKind, val id: Int, val name: String)

    private val ui by lazy { GalleryStyle(this) }
    private val manager by lazy { AppWidgetManager.getInstance(this) }
    private lateinit var scroll: ScrollView
    private lateinit var toolbar: LinearLayout
    private lateinit var bottomBar: LinearLayout
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

        content = ui.column().apply { setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(16)) }
        toolbar = ui.column().apply { setPadding(ui.dp(18), ui.dp(4), ui.dp(18), ui.dp(10)) }
        bottomBar = ui.column().apply { setPadding(ui.dp(18), ui.dp(8), ui.dp(18), ui.dp(5)) }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(centered(content))
        }
        setContentView(ui.column().apply {
            setBackgroundColor(ui.background)
            addView(centered(toolbar))
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(View(this@MainActivity).apply { setBackgroundColor(ui.line) }, LinearLayout.LayoutParams(-1, ui.dp(1)))
            addView(centered(bottomBar))
            setOnApplyWindowInsetsListener { view, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                }
                insets
            }
        })
    }

    private fun centered(column: LinearLayout) = FrameLayout(this).apply {
        val available = resources.configuration.screenWidthDp
        val width = if (available > 560) ui.dp(560) else -1
        addView(column, FrameLayout.LayoutParams(width, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
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
        toolbar.removeAllViews()
        bottomBar.removeAllViews()
        with(ui) {
            toolbar.place(header())
            body = column()
            content.place(body)
        }
        if (onHome) renderHome() else renderGallery()
        renderBottom()
        scroll.post { scroll.scrollTo(0, offset) }
    }

    private fun header() = with(ui) {
        LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text("Widgets", 32, heading = true), LinearLayout.LayoutParams(0, -2, 1f))
            val target = if (dark) "Light" else "Dark"
            addView(button(target) { toggleTheme() }.apply {
                contentDescription = "Switch to ${target.lowercase()} mode"
                icon(if (dark) R.drawable.ic_sun else R.drawable.ic_moon, color = accent)
            }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
    }

    private fun renderBottom() = with(ui) {
        if (!onHome) {
            bottomBar.place(button("Add to home", primary = true) { pin(kind) }.apply {
                minimumHeight = dp(52)
                icon(R.drawable.ic_counter, color = currentTextColor)
            })
        }
        val browse = button("Browse", selected = !onHome, quiet = true) { showHome(false) }.apply {
            textSize = 12f
            icon(R.drawable.ic_grid, above = true, color = if (onHome) muted else accent)
        }
        val home = button("On home", selected = onHome, quiet = true) { showHome(true) }.apply {
            textSize = 12f
            icon(R.drawable.ic_phone, above = true, color = if (onHome) accent else muted)
        }
        bottomBar.place(equalRow(listOf(browse, home)), top = if (onHome) 0 else 6)
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
        val stageHeight = (resources.configuration.screenHeightDp * .36f).toInt().coerceIn(224, 344)
        body.place(previewStage(preview, side = 174, sample = needsAccess), height = stageHeight)
        body.place(text(details.title, 40, heading = true), top = 16)
        body.place(text(details.description, 14, muted), top = 6)

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
                icon(item.icon, above = true, color = if (item == current) accent else muted)
                setPadding(dp(2), dp(8), dp(2), dp(8))
                textSize = 12f
            }
        }
        val controlsPerRow = if (resources.configuration.screenWidthDp < 340 ||
            resources.configuration.fontScale >= 1.4f) 2 else 4
        selectors.chunked(controlsPerRow).forEach { row -> body.place(equalRow(row), top = 12) }
        if (needsAccess) {
            body.place(button("Enable Usage Access") { askForUsageAccess() }, top = 12)
        }
    }

    private fun renderHome() = with(ui) {
        val placed = WidgetKind.entries.flatMap { item ->
            val title = Catalog.details(this@MainActivity, item).title
            val ids = manager.getAppWidgetIds(item.component(this@MainActivity)).sorted()
            ids.mapIndexed { index, id -> PlacedWidget(item, id, if (ids.size == 1) title else "$title ${index + 1}") }
        }
        body.place(text("On your home screen", 35, heading = true), top = 5)
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
            body.place(placedPicker(placed), top = 14)
        }

        val saved = WidgetAppearance.read(this@MainActivity, selected.id)
        val appearance = draft ?: saved
        val preview = widgetPreview(selected.kind, Readings.read(this@MainActivity, selected.kind))
        styleWidget(preview, appearance)
        body.place(previewStage(preview, side = 140), top = 16, height = 204)
        body.place(text(selected.name, 26, heading = true), top = 18)

        body.place(equalRow(listOf(
            button("Dark widget", selected = !appearance.light) { changeTheme(appearance = false, saved) }.apply {
                icon(R.drawable.ic_moon, color = if (appearance.light) muted else accent)
            },
            button("Light widget", selected = appearance.light) { changeTheme(appearance = true, saved) }.apply {
                icon(R.drawable.ic_sun, color = if (appearance.light) accent else muted)
            },
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

    private fun placedPicker(placed: List<PlacedWidget>) = with(ui) {
        val row = LinearLayout(this@MainActivity)
        var active: View? = null
        for (widget in placed) {
            val selected = widget.id == selectedId
            val chip = button(widget.name, selected = selected) {
                if (!selected) {
                    selectedId = widget.id
                    draft = null
                    render(preserveScroll = true)
                }
            }.apply { icon(widget.kind.icon, size = 16, color = if (selected) accent else muted) }
            row.addView(chip, LinearLayout.LayoutParams(-2, -2).apply {
                if (row.childCount > 0) marginStart = dp(8)
            })
            if (selected) active = chip
        }
        HorizontalScrollView(this@MainActivity).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
            post { active?.let { scrollTo(it.left, 0) } }
        }
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
