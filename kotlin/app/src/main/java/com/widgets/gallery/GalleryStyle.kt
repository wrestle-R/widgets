package com.widgets.gallery

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal class GalleryStyle(private val activity: Activity) {
    val dark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val background = if (dark) 0xff101214.toInt() else 0xfff5f6ef.toInt()
    val textColor = if (dark) 0xfff4f5f4.toInt() else 0xff1c251f.toInt()
    val muted = if (dark) 0xffafb7bc.toInt() else 0xff515e53.toInt()
    val panel = if (dark) 0xff181d20.toInt() else 0xffecefe6.toInt()
    val line = if (dark) 0xff363e43.toInt() else 0xffc6cec1.toInt()
    val accent = if (dark) 0xffd6f6a2.toInt() else 0xff324f32.toInt()
    private val onAccent = if (dark) 0xff192114.toInt() else 0xfff8fbee.toInt()
    private val selectedFill = if (dark) 0xff27312a.toInt() else 0xffdbe7d4.toInt()
    private val displayTypeface by lazy {
        Typeface.Builder(activity.resources.assets, "fonts/space_grotesk.ttf")
            .setFontVariationSettings("'wght' 600").build()
    }

    fun text(value: String, size: Int, color: Int = textColor, heading: Boolean = false) =
        TextView(activity).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            includeFontPadding = false
            if (heading) {
                typeface = displayTypeface
                letterSpacing = -.035f
                isAccessibilityHeading = true
            }
        }

    fun button(
        label: String,
        primary: Boolean = false,
        selected: Boolean = false,
        action: () -> Unit,
    ) = Button(activity).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        minHeight = dp(48)
        stateListAnimator = null
        setPadding(dp(10), dp(9), dp(10), dp(9))
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isSelected = selected
        setTextColor(ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(muted, if (primary) onAccent else if (selected) accent else textColor)
        ))
        val fill = if (primary) accent else if (selected) selectedFill else panel
        background = RippleDrawable(
            ColorStateList.valueOf(if (primary == dark) 0x33000000 else 0x26ffffff),
            shape(fill, 13, if (primary) null else if (selected) 0xff5b704d.toInt() else line),
            null
        )
        setOnClickListener { action() }
    }

    fun shape(color: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    fun column() = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    fun LinearLayout.place(view: View, top: Int = 0, height: Int = -2) {
        addView(view, LinearLayout.LayoutParams(-1, if (height > 0) dp(height) else height).apply {
            topMargin = dp(top)
        })
    }

    fun dp(value: Int) = (activity.resources.displayMetrics.density * value + .5f).toInt()
}
