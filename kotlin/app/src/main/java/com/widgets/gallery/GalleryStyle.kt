package com.widgets.gallery

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal class GalleryStyle(private val activity: Activity) {
    val dark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val background = if (dark) 0xff170e0d.toInt() else 0xfff5ede3.toInt()
    val textColor = if (dark) 0xfff8eee4.toInt() else 0xff211713.toInt()
    val muted = if (dark) 0xffcfbbb2.toInt() else 0xff66544a.toInt()
    val panel = if (dark) 0xff271b19.toInt() else 0xffeee0d4.toInt()
    val line = if (dark) 0xff52403a.toInt() else 0xffcbbcaf.toInt()
    val accent = if (dark) 0xffff9a86.toInt() else 0xff9c2818.toInt()
    private val actionColor = 0xffff583b.toInt()
    private val actionInk = 0xff2c100b.toInt()
    private val selectedFill = if (dark) 0xff45231e.toInt() else 0xfff3d1c3.toInt()
    private val displayTypeface by lazy { Typeface.createFromAsset(activity.assets, "fonts/anton.ttf") }

    fun text(value: String, size: Int, color: Int = textColor, heading: Boolean = false) =
        TextView(activity).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            includeFontPadding = false
            if (heading) {
                typeface = displayTypeface
                letterSpacing = -.018f
                isAccessibilityHeading = true
            }
        }

    fun button(
        label: String,
        primary: Boolean = false,
        selected: Boolean = false,
        quiet: Boolean = false,
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
            intArrayOf(muted, if (primary) actionInk else if (selected) accent else textColor)
        ))
        val fill = when {
            primary -> actionColor
            selected -> selectedFill
            quiet -> this@GalleryStyle.background
            else -> panel
        }
        val stroke = when {
            primary || quiet -> null
            selected -> accent
            else -> line
        }
        background = StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), shape(panel, 13, line))
            addState(intArrayOf(), RippleDrawable(
                ColorStateList.valueOf(if (primary || !dark) 0x26000000 else 0x26ffffff),
                shape(fill, if (quiet) 20 else 13, stroke),
                null
            ))
        }
        setOnClickListener { action() }
    }

    fun Button.icon(resource: Int, size: Int = 18, above: Boolean = false, color: Int = textColor) {
        val image = activity.getDrawable(resource)?.mutate()?.apply {
            setBounds(0, 0, dp(size), dp(size))
            setTint(color)
        }
        if (above) setCompoundDrawablesRelative(null, image, null, null)
        else setCompoundDrawablesRelative(image, null, null, null)
        compoundDrawablePadding = dp(if (above) 4 else 8)
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
