package com.widgets.gallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.SizeF
import kotlin.math.min
import kotlin.math.roundToInt

internal object UnlockArt {
    fun bitmap(size: SizeF, design: Design, reading: Reading, appearance: Appearance): Bitmap {
        val scale = min(2.3f, 440f / maxOf(size.width, size.height))
        val bitmap = Bitmap.createBitmap(
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap).apply {
            scale(bitmap.width / size.width, bitmap.height / size.height)
        }
        Layout(canvas, size.width, size.height, design, reading, appearance).draw()
        return bitmap
    }

    private class Layout(
        private val canvas: Canvas,
        private val width: Float,
        private val height: Float,
        private val design: Design,
        private val reading: Reading,
        appearance: Appearance,
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val body = min(width, height)
        private val inset = (body * .13f).coerceIn(5f, 19f)
        private val ink = if (appearance.light) 0xff171d19.toInt() else 0xfff5f6f2.toInt()
        private val quiet = if (appearance.light) 0xff47504b.toInt() else 0xffbfc7c1.toInt()
        private val accent = if (appearance.light) {
            Color.rgb(Color.red(design.accent) / 3, Color.green(design.accent) / 3, Color.blue(design.accent) / 3)
        } else design.accent
        private val background = if (appearance.light) 0x00eef0ec else 0x00171918
        private val alpha = appearance.alpha
        private val value = reading.count?.toString() ?: "—"
        private val wide = width / height >= 1.45f
        private val tall = height / width >= 1.45f

        fun draw() {
            val radius = (body * .22f).coerceIn(7f, 24f)
            round(0f, 0f, width, height, radius, background or (alpha shl 24))
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, width, height), radius, radius, Path.Direction.CW) })
            when {
                body < 64f || (body < 88f && height < 128f) -> compact()
                design.id == "orbit" -> orbit()
                design.id == "signal" -> signal()
                design.id == "ticket" -> ticket()
                design.id == "terminal" -> terminal()
                else -> classic()
            }
            canvas.restore()
        }

        private fun compact() {
            val gap = inset / 2f
            val x = if (wide) inset else width / 2f
            val center = !wide
            val markRadius = (body * .06f).coerceIn(2f, 5f)
            val orbit = design.id == "orbit"
            when (design.id) {
                "orbit" -> ring(width / 2, height / 2, body * .38f, tint(accent, 130))
                "ticket" -> round(0f, inset, 3f, height - inset, 1.5f, accent)
                "terminal" -> round(width - inset - 4f, height - inset - 6f, width - inset, height - inset, 1f, accent)
                "signal" -> if (reading.buckets.isNotEmpty()) {
                    val peak = maxOf(1, reading.buckets.maxOrNull() ?: 1)
                    val bar = (width - 2 * inset) / reading.buckets.size
                    reading.buckets.forEachIndexed { index, count ->
                        val left = inset + index * bar
                        val bottom = height - gap
                        round(left, bottom - maxOf(1f, body * .16f * count / peak), left + bar * .62f, bottom,
                            1f, tint(accent, if (count == 0) 75 else 225))
                    }
                }
                else -> round(inset, gap, inset + 2 * markRadius, gap + 2 * markRadius, markRadius, accent)
            }
            val available = if (orbit) width * .59f else width - inset * 2
            val hasChart = design.id == "signal" && reading.count != null
            number(x, height * if (hasChart) .36f else .48f, body * .5f, available, center,
                mono = design.id == "terminal")
            if (height >= 49f && design.id == "classic") {
                lineText(if (reading.count == null) "Set up" else "Unlocks", x, height - gap - 1f,
                    min(9f, body * .17f), available, quiet, center)
            }
            if (height >= 82f && hasChart) {
                lineText("Today", x, height * .62f, 9f, available, quiet, center)
            }
        }

        private fun classic() {
            val iconSize = min(25f, body * .2f)
            lock(inset, inset, iconSize)
            val x = inset
            val bottom = height - inset
            val labelSize = min(12f, body * .14f)
            val numberTop = inset + iconSize + 3f
            val numberBottom = bottom - labelSize - 3f
            number(x, (numberTop + numberBottom) / 2f, min(body * .49f, numberBottom - numberTop), width - 2 * inset)
            readingLabel(x, bottom, labelSize, width - 2 * inset, quiet)
        }

        private fun orbit() {
            val cx = if (wide) width * .31f else width / 2f
            val cy = if (tall) height * .42f else height / 2f
            val radius = body * .37f
            ring(cx, cy, radius, tint(accent, 115))
            ring(cx, cy, radius - 5f, tint(accent, 62))
            circle(cx, cy - radius, 2.5f, accent)
            number(cx, cy + if (wide) 0f else -body * .035f, body * .42f, body * .58f, center = true)
            val labelX = if (wide) width * .58f else cx
            val labelY = if (wide) cy + 4f else cy + body * .25f
            readingLabel(labelX, labelY, 10f, if (wide) width * .37f else radius * 1.6f, quiet, !wide)
        }

        private fun signal() {
            val plotRight = width - inset
            val plotLeft = if (wide) width * .51f else inset
            val plotBottom = height - inset - if (wide) 0f else 6f
            val plotTop = if (wide) inset else height * if (tall) .58f else .69f
            val numberX = inset
            val labelSize = 10f
            val labelGap = 4f
            val readingBottom = if (wide) height - inset else plotTop - 5f
            val numberSize = min(body * .42f, readingBottom - inset - labelSize - labelGap)
            val readingTop = (inset + readingBottom - numberSize - labelSize - labelGap) / 2f
            val available = if (wide) width * .46f - inset else width - 2 * inset
            number(numberX, readingTop + numberSize / 2f, numberSize, available)
            readingLabel(numberX, readingTop + numberSize + labelGap + labelSize, labelSize, available, quiet)
            if (reading.count != null) {
                val peak = maxOf(1, reading.buckets.maxOrNull() ?: 1)
                val bar = (plotRight - plotLeft) / reading.buckets.size
                reading.buckets.forEachIndexed { i, count ->
                    val barHeight = maxOf(2f, (plotBottom - plotTop) * count / peak)
                    val left = plotLeft + i * bar
                    round(left, plotBottom - barHeight, left + bar * .72f, plotBottom, 2f,
                        if (count == 0) tint(accent, 70) else accent)
                }
            }
        }

        private fun ticket() {
            val split = if (wide) width * .68f else height * .72f
            round(0f, 0f, 4f, height, 2f, accent)
            paint.color = tint(accent, 120)
            paint.strokeWidth = 1f
            val length = if (wide) height else width
            var offset = inset
            while (offset < length - inset) {
                if (wide) canvas.drawLine(split, offset, split, min(offset + 3f, height - inset), paint)
                else canvas.drawLine(offset, split, min(offset + 3f, width - inset), split, paint)
                offset += 6f
            }
            number(inset, height * if (wide) .54f else .39f, body * .57f,
                if (wide) split - 2 * inset else width - 2 * inset)
            readingLabel(if (wide) split + inset / 2 else inset,
                if (wide) height * .61f else height - inset, 11f,
                if (wide) width - split - inset else width - 2 * inset, accent)
        }

        private fun terminal() {
            val labelSize = min(11f, body * .13f)
            lineText("UNLOCKS", inset, inset + labelSize, labelSize, width - 2 * inset, accent, mono = true)
            val centerY = if (tall) height * .58f else height * .53f
            number(inset, centerY, body * .45f, width - 2 * inset, mono = true)
            val label = if (reading.count == null) "SET UP" else "TODAY"
            lineText(label, inset, height - inset, labelSize, width - 2 * inset - 8f, quiet, mono = true)
            round(width - inset - 5f, height - inset - 7f, width - inset, height - inset, 1f, accent)
        }

        private fun lock(x: Float, y: Float, size: Float) {
            circle(x + size / 2, y + size / 2, size / 2, tint(accent, 40))
            val left = x + size * .27f
            val top = y + size * .43f
            paint.color = accent
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(1f, size * .065f)
            canvas.drawRoundRect(left, top, left + size * .48f, top + size * .35f, 1.25f, 1.25f, paint)
            canvas.drawArc(left + size * .08f, y + size * .21f, left + size * .39f, y + size * .53f,
                180f, 185f, false, paint)
            paint.style = Paint.Style.FILL
        }

        private fun number(x: Float, y: Float, size: Float, maxWidth: Float, center: Boolean = false, mono: Boolean = false) {
            paint.typeface = if (mono) Typeface.MONOSPACE else Typeface.create("sans-serif-medium", Typeface.NORMAL)
            paint.color = ink
            paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            paint.textSize = size
            val measured = paint.measureText(value)
            if (measured > maxWidth && measured > 0) paint.textSize *= maxWidth / measured
            canvas.drawText(value, x, y - (paint.ascent() + paint.descent()) / 2f, paint)
        }

        private fun readingLabel(x: Float, baseline: Float, size: Float, maxWidth: Float, color: Int,
            center: Boolean = false) {
            val fallback = when (reading.label) {
                "Enable Usage Access" -> "Set up"
                "Unlock count unavailable" -> "Unavailable"
                else -> "Today"
            }
            lineText(reading.label, x, baseline, size, maxWidth, color, center, fallback = fallback)
        }

        private fun lineText(text: String, x: Float, baseline: Float, size: Float, maxWidth: Float, color: Int,
            center: Boolean = false, mono: Boolean = false, fallback: String = text) {
            paint.typeface = if (mono) Typeface.MONOSPACE else Typeface.DEFAULT
            paint.color = color
            paint.textSize = size
            paint.textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
            val label = if (paint.measureText(text) <= maxWidth) text else fallback
            val measured = paint.measureText(label)
            if (measured > maxWidth && maxWidth > 0) paint.textSize *= maxWidth / measured
            canvas.drawText(label, x, baseline, paint)
        }

        private fun ring(x: Float, y: Float, radius: Float, color: Int) {
            paint.color = color
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f
            canvas.drawCircle(x, y, radius, paint)
            paint.style = Paint.Style.FILL
        }

        private fun circle(x: Float, y: Float, radius: Float, color: Int) {
            paint.color = color
            canvas.drawCircle(x, y, radius, paint)
        }

        private fun round(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
            paint.color = color
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        }

        private fun tint(color: Int, alpha: Int) = (color and 0x00ffffff) or (alpha shl 24)
    }
}
