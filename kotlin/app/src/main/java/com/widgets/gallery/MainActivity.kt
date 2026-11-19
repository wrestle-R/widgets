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
import android.util.SizeF
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Toast
import java.util.UUID
import kotlin.math.min

class MainActivity : Activity() {
    private val ui by lazy { GalleryStyle(this) }
    private val manager by lazy { AppWidgetManager.getInstance(this) }
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var body: LinearLayout
    private var designId = "classic"
    private var columns = 2
    private var rows = 2
    private var reading = Reading.sample
    private val previewSize: SizeF
        get() = SizeF(columns * 62f, rows * 62f)
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
        designId = savedInstanceState?.getString("designId") ?: "classic"
        columns = savedInstanceState?.getInt("columns", 2)?.coerceIn(1, 4) ?: 2
        rows = savedInstanceState?.getInt("rows", 2)?.coerceIn(1, 4) ?: 2
        onHome = savedInstanceState?.getBoolean("onHome") ?: false
        selectedId = savedInstanceState?.getInt("selectedId") ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (savedInstanceState?.containsKey("opacity") == true) {
            draft = Appearance(savedInstanceState.getInt("opacity"), savedInstanceState.getBoolean("light"),
                savedInstanceState.getString("draftDesign") ?: "classic")
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
        reading = Readings.read(this)
        UnlockRefresh.schedule(this)
        UnlockRefresh.enqueue(this)
        render(preserveScroll = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("designId", designId)
        outState.putInt("columns", columns)
        outState.putInt("rows", rows)
        outState.putBoolean("onHome", onHome)
        outState.putInt("scrollY", scroll.scrollY)
        outState.putInt("selectedId", selectedId)
        draft?.let {
            outState.putInt("opacity", it.opacity)
            outState.putBoolean("light", it.light)
            outState.putString("draftDesign", it.design)
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
        val design = Catalog.design(this@MainActivity, designId)
        val appearance = Appearance(light = !dark, design = design.id)
        val useSample = reading.count == null
        val preview = ImageView(this@MainActivity)
        val size = previewSize
        updatePreview(preview, appearance, size, if (useSample) Reading.sample else reading)
        body.place(text("App Unlocks", 38, heading = true))
        body.place(text("Count how often you unlock your phone.", 15, muted), top = 5)
        body.place(previewStage(preview, appearance, size, useSample), top = 16, height = 270)
        showDesignChoices(design.id) {
            designId = it
            render(preserveScroll = true)
        }
        body.place(text(design.description, 14, muted), top = 12)
        showSizes()
        body.place(button("Add to home", primary = true) { pin(design, appearance) }, top = 16)
        if (!Readings.hasUsageAccess(this@MainActivity)) {
            body.place(button("Enable Usage Access") { askForUsageAccess() }, top = 8)
        }
    }

    private fun renderHome() = with(ui) {
        val placed = UnlockRefresh.ids(this@MainActivity).sorted()
        body.place(text("On your home screen", 35, heading = true))
        if (placed.isEmpty()) {
            body.place(text("Add App Unlocks to customize it here.", 15, muted), top = 10)
            body.place(button("Browse designs", primary = true) { showHome(false) }, top = 16)
            return@with
        }

        val id = placed.firstOrNull { it == selectedId } ?: placed.first().also {
            selectedId = it
            draft = null
        }
        if (placed.size > 1) {
            placed.chunked(2).forEach { row ->
                body.place(equalRow(row.map { widgetId ->
                    val savedDesign = WidgetAppearance.read(this@MainActivity, widgetId).design
                    val name = Catalog.design(this@MainActivity, savedDesign).title
                    button("${placed.indexOf(widgetId) + 1} · $name", selected = widgetId == id) {
                        if (selectedId != widgetId) {
                            selectedId = widgetId
                            draft = null
                            render(preserveScroll = true)
                        }
                    }
                }), top = 8)
            }
        }

        val saved = WidgetAppearance.read(this@MainActivity, id)
        val appearance = draft ?: saved
        val preview = ImageView(this@MainActivity)
        val size = previewSize
        updatePreview(preview, appearance, size, reading)
        body.place(previewStage(preview, appearance, size), top = 16, height = 270)
        showSizes()
        showDesignChoices(appearance.design) { changeDraft(appearance.copy(design = it)) }
        body.place(equalRow(listOf(
            button("Dark widget", selected = !appearance.light) { changeDraft(appearance.copy(light = false)) },
            button("Light widget", selected = appearance.light) { changeDraft(appearance.copy(light = true)) },
        )), top = 12)

        val opacity = text(getString(R.string.widget_opacity_value, appearance.opacity), 14)
        body.place(opacity, top = 16)
        val save = button("Save appearance", primary = true) {
            val change = draft ?: return@button
            val updated = WidgetAppearance.save(this@MainActivity, id, change)
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
                    updatePreview(preview, change, size, reading)
                    save.isEnabled = change != saved
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }, top = 2)
        body.place(save, top = 8)
        body.place(text("To resize a placed widget, touch and hold it on your home screen.", 13, muted), top = 12)
    }

    private fun showDesignChoices(current: String, select: (String) -> Unit) = with(ui) {
        Catalog.designs(this@MainActivity).chunked(3).forEach { group ->
            body.place(equalRow(group.map { design ->
                button(design.title, selected = design.id == current) { select(design.id) }
            }), top = 8)
        }
    }

    private fun showSizes() = with(ui) {
        body.place(text("Preview size", 14), top = 15)
        body.place(sizeRow("Width", columns) { columns = it }, top = 6)
        body.place(sizeRow("Height", rows) { rows = it }, top = 6)
    }

    private fun sizeRow(axis: String, selected: Int, change: (Int) -> Unit) = with(ui) {
        LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(axis, 13, muted), LinearLayout.LayoutParams(dp(56), -2))
            addView(equalRow((1..4).map { cells ->
                button(cells.toString(), selected = cells == selected) {
                    if (cells != selected) {
                        change(cells)
                        render(preserveScroll = true)
                    }
                }.apply { contentDescription = "$axis $cells cells" }
            }), LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun changeDraft(appearance: Appearance) {
        draft = appearance
        render(preserveScroll = true)
    }

    private fun updatePreview(view: ImageView, appearance: Appearance, size: SizeF, reading: Reading) {
        val design = Catalog.design(this, appearance.design)
        view.setImageBitmap(UnlockArt.bitmap(size, design, reading, appearance))
        view.contentDescription = reading.accessibilityLabel
    }

    private fun previewStage(preview: ImageView, appearance: Appearance, size: SizeF, sample: Boolean = false) = with(ui) {
        FrameLayout(this@MainActivity).apply {
            background = shape(panel, 24, line)
            clipToOutline = true
            addView(ImageView(this@MainActivity).apply {
                setImageResource(if (appearance.light) R.drawable.gallery_art_light else R.drawable.gallery_art)
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(-1, -1))
            val fit = min(1f, (resources.configuration.screenWidthDp - 64).coerceAtLeast(120) / size.width)
            addView(preview, FrameLayout.LayoutParams(
                dp((size.width * fit).toInt()), dp((size.height * fit).toInt()), Gravity.CENTER,
            ))
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

    private fun pin(design: Design, appearance: Appearance) {
        if (manager.isRequestPinAppWidgetSupported) {
            val extras = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                    previewWidget(this@MainActivity, design, if (reading.count == null) Reading.sample else reading,
                        appearance, SizeF(60f, 60f)))
            }
            val intent = Intent(this, UnlocksWidget::class.java).apply {
                action = PIN_UNLOCKS_WIDGET
                data = Uri.fromParts("widgets", UUID.randomUUID().toString(), null)
                putExtra(EXTRA_DESIGN, design.id)
                putExtra(EXTRA_LIGHT, appearance.light)
            }
            // The launcher fills in the new widget ID.
            val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(this, 0, intent, mutable or PendingIntent.FLAG_ONE_SHOT)
            if (manager.requestPinAppWidget(UnlocksWidget.component(this), extras, callback)) return
            callback.cancel()
        }

        AlertDialog.Builder(this).setTitle("Add App Unlocks")
            .setMessage("Touch and hold an empty spot on your home screen. Open the widget picker and find App Unlocks.")
            .setPositiveButton("Got it", null).show()
    }
}
