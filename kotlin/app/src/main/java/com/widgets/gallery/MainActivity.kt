package com.widgets.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
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
    private lateinit var headerRegion: LinearLayout
    private lateinit var footer: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var body: LinearLayout
    private var designId = "classic"
    private var columns = 2
    private var rows = 2
    private var reading = Reading.sample
    private val previewSize: SizeF
        get() = SizeF(columns * 62f, rows * 62f)
    private var onHome = false
    private var expanded = false
    private var backRegistered = false
    private val backCallback by lazy { OnBackInvokedCallback { showGallery() } }
    private var selectedId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var draft: Appearance? = null
    private var restoredScroll: Int? = null
    private val liveUnlockPermission = 86

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
        expanded = savedInstanceState?.getBoolean("expanded") ?: false
        selectedId = savedInstanceState?.getInt("selectedId") ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (savedInstanceState?.containsKey("opacity") == true) {
            draft = Appearance(savedInstanceState.getInt("opacity"), savedInstanceState.getBoolean("light"),
                savedInstanceState.getString("draftDesign") ?: "classic")
        }

        headerRegion = ui.column().apply { setPadding(ui.dp(20), ui.dp(4), ui.dp(20), ui.dp(6)) }
        content = ui.column().apply { setPadding(ui.dp(20), ui.dp(14), ui.dp(20), ui.dp(18)) }
        footer = ui.column().apply { setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12)) }
        scroll = ScrollView(this).apply {
            addView(content)
        }
        val page = ui.column().apply {
            addView(headerRegion, LinearLayout.LayoutParams(-1, -2))
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, -2))
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(ui.background)
            val width = ui.dp(min(resources.configuration.screenWidthDp, 520))
            addView(page, FrameLayout.LayoutParams(width, -1, Gravity.CENTER_HORIZONTAL))
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                page.layoutParams = (page.layoutParams as FrameLayout.LayoutParams).apply {
                    width = min(ui.dp(resources.configuration.screenWidthDp) - bars.left - bars.right, ui.dp(520))
                        .coerceAtLeast(1)
                }
                insets
            }
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        reading = Readings.read(this)
        if (LiveUnlockService.running && !Readings.hasUsageAccess(this)) LiveUnlockService.stop(this)
        UnlockRefresh.schedule(this)
        UnlockRefresh.enqueue(this)
        render(preserveScroll = true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == liveUnlockPermission && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            enableLiveUnlocks()
        }
    }

    private fun enableLiveUnlocks() {
        if (!Readings.hasUsageAccess(this)) {
            askForUsageAccess()
            return
        }
        if (UnlockRefresh.ids(this).isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), liveUnlockPermission)
            return
        }
        if (!LiveUnlockService.start(this)) {
            Toast.makeText(this, "Could not start live unlock updates", Toast.LENGTH_SHORT).show()
        }
        render(preserveScroll = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("designId", designId)
        outState.putInt("columns", columns)
        outState.putInt("rows", rows)
        outState.putBoolean("onHome", onHome)
        outState.putBoolean("expanded", expanded)
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
        headerRegion.removeAllViews()
        content.removeAllViews()
        footer.removeAllViews()
        with(ui) {
            headerRegion.place(header())
            if (!expanded) {
                headerRegion.place(equalRow(listOf(
                    button("Gallery", selected = !onHome, quiet = true) { showGallery() },
                    button("On home", selected = onHome, quiet = true) { showHome() },
                )), top = 6)
            }
            body = content
        }
        when {
            expanded -> renderDetails()
            onHome -> renderHome()
            else -> renderGallery()
        }
        footer.visibility = if (footer.childCount == 0) View.GONE else View.VISIBLE
        if (Build.VERSION.SDK_INT >= 33 && backRegistered != expanded) {
            if (expanded) {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback)
            } else {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback)
            }
            backRegistered = expanded
        }
        scroll.post { scroll.scrollTo(0, offset) }
    }

    private fun header() = with(ui) {
        LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            val title = if (expanded) button("Gallery", quiet = true) { showGallery() }.apply {
                decorate(this, R.drawable.ic_arrow_left, 18, accent)
            } else text("Widgets", 22, heading = true).apply {
                decorate(this, R.mipmap.ic_launcher, 30)
            }
            title.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            val target = if (dark) "Light" else "Dark"
            addView(button(target, quiet = true) { toggleTheme() }.apply {
                contentDescription = "Switch to ${target.lowercase()} mode"
                decorate(this, if (dark) R.drawable.ic_sun else R.drawable.ic_moon, 17, accent)
            }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        }
    }

    private fun toggleTheme() {
        val mode = if (ui.dark) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        getSharedPreferences("app_appearance", MODE_PRIVATE).edit().putInt("night_mode", mode).apply()
        recreate()
    }

    private fun showGallery() {
        if (!expanded && !onHome) return
        expanded = false
        onHome = false
        render()
    }

    private fun showHome() {
        if (onHome) return
        onHome = true
        render()
    }

    @SuppressLint("GestureBackNavigation") // Android 13 and later use the registered callback.
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT < 33 && expanded) showGallery() else super.onBackPressed()
    }

    private fun renderGallery() = with(ui) {
        body.place(actionRow("App Unlocks", "Daily phone unlocks", R.drawable.ic_lock_keyhole) {
            expanded = true
            render()
        }, top = 6)
    }

    private fun renderDetails() = with(ui) {
        val design = Catalog.design(this@MainActivity, designId)
        val appearance = Appearance(light = !dark, design = design.id)
        val useSample = reading.count == null
        val preview = ImageView(this@MainActivity)
        val size = previewSize
        updatePreview(preview, appearance, size, if (useSample) Reading.sample else reading)
        body.place(text("App Unlocks", 34, heading = true))
        body.place(text("Daily phone unlocks", 15, muted), top = 4)
        body.place(previewStage(preview, appearance, size, useSample), top = 16, height = stageHeight())
        showPreviewControls(design.id) {
            designId = it
            render(preserveScroll = true)
        }
        if (!Readings.hasUsageAccess(this@MainActivity)) {
            body.place(button("Enable Usage Access", quiet = true) { askForUsageAccess() }, top = 8)
        }
        footer.place(button("Add to home", primary = true) { pin(design, appearance) })
    }

    private fun renderHome() = with(ui) {
        val placed = UnlockRefresh.ids(this@MainActivity).sorted()
        body.place(text("On your home screen", 32, heading = true))
        if (placed.isEmpty()) {
            body.place(text("Add App Unlocks to customize it here.", 15, muted), top = 10)
            body.place(button("Browse designs", primary = true) { showGallery() }, top = 16)
            return@with
        }

        val id = placed.firstOrNull { it == selectedId } ?: placed.first().also {
            selectedId = it
            draft = null
        }
        body.place(text("Update the counter after each unlock with a persistent notification.", 14, muted), top = 12)
        body.place(button(if (LiveUnlockService.running) "Turn off live unlock count" else "Turn on live unlock count", quiet = true) {
            if (LiveUnlockService.running) {
                LiveUnlockService.stop(this@MainActivity)
                render(preserveScroll = true)
            } else {
                enableLiveUnlocks()
            }
        }, top = 8)
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
        body.place(previewStage(preview, appearance, size), top = 16, height = stageHeight())
        showPreviewControls(appearance.design) { changeDraft(appearance.copy(design = it)) }
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
        footer.place(save)
    }

    private fun showPreviewControls(current: String, select: (String) -> Unit) = with(ui) {
        val choices = Catalog.designs(this@MainActivity)
        val currentDesign = Catalog.design(this@MainActivity, current)
        body.place(equalRow(listOf(
            actionRow("Design", currentDesign.title) {
                showPicker(AlertDialog.Builder(this@MainActivity).setTitle("Design")
                    .setSingleChoiceItems(choices.map { it.title }.toTypedArray(), choices.indexOf(currentDesign)) { dialog, index ->
                        dialog.dismiss()
                        select(choices[index].id)
                    }
                    .setNegativeButton("Cancel", null))
            },
            actionRow("Preview size", "$columns × $rows") { chooseSize() },
        )), top = 12)
    }

    private fun chooseSize() = with(ui) {
        val sizes = column().apply { setPadding(dp(22), dp(12), dp(22), dp(6)) }
        var width = columns
        var height = rows
        fun showChoices() {
            sizes.removeAllViews()
            sizes.place(sizeRow("Width", width) {
                width = it
                showChoices()
            })
            sizes.place(sizeRow("Height", height) {
                height = it
                showChoices()
            }, top = 8)
        }
        showChoices()
        showPicker(AlertDialog.Builder(this@MainActivity).setTitle("Preview size").setView(sizes)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { _, _ ->
                columns = width
                rows = height
                render(preserveScroll = true)
            })
    }

    private fun showPicker(builder: AlertDialog.Builder) = with(ui) {
        builder.show().apply {
            window?.setBackgroundDrawable(shape(background, 24, line))
            getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
            getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false
        }
    }

    private fun sizeRow(axis: String, selected: Int, change: (Int) -> Unit) = with(ui) {
        LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(axis, 13, muted), LinearLayout.LayoutParams(dp(56), -2))
            addView(equalRow((1..4).map { cells ->
                button(cells.toString(), selected = cells == selected, action = { change(cells) }).apply {
                    contentDescription = "$axis $cells ${if (cells == 1) "cell" else "cells"}"
                    setPadding(dp(4), dp(9), dp(4), dp(9))
                }
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

    private fun stageHeight(): Int {
        val height = resources.configuration.screenHeightDp
        return if (expanded) (height * .39f).toInt().coerceIn(128, 330)
        else (height * .34f).toInt().coerceIn(128, 268)
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
            val clearance = dp(16)
            preview.scaleType = ImageView.ScaleType.FIT_CENTER
            addView(preview, FrameLayout.LayoutParams(-1, -1).apply {
                setMargins(clearance, clearance, clearance, dp(if (sample) 44 else 16))
            })
            // Enlarge the in-app preview slightly at 1×1 while retaining its real artwork and ratio.
            val limit = dp(if (size.width == 62f && size.height == 62f) 100 else 74) / 62f
            addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    val margins = preview.layoutParams as FrameLayout.LayoutParams
                    val width = (right - left - margins.leftMargin - margins.rightMargin).coerceAtLeast(1)
                    val height = (bottom - top - margins.topMargin - margins.bottomMargin).coerceAtLeast(1)
                    val scale = minOf(limit, width / size.width, height / size.height)
                    preview.setPadding(
                        ((width - size.width * scale) / 2).toInt(), ((height - size.height * scale) / 2).toInt(),
                        ((width - size.width * scale) / 2).toInt(), ((height - size.height * scale) / 2).toInt(),
                    )
                }
            }
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

    private fun equalRow(buttons: List<View>) = LinearLayout(this).apply {
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
