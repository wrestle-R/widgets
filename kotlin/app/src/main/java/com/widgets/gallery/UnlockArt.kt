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
                body < 88f -> compact()
                design.id == "orbit" -> orbit()
                design.id == "signal" -> signal()
                design.id == "ticket" -> ticket()
                design.id == "terminal" -> terminal()
                else -> classic()
            }
            canvas.restore()
        }

        private fun compact() {
            val artWidth = min(width, body * 2.4f)
            val artHeight = min(height, body * 1.6f)
            val left = (width - artWidth) / 2f
            val top = (height - artHeight) / 2f
            val centerX = width / 2f
            val labelSize = (body * .15f).coerceIn(6f, 11f)
            val shortLabel = if (reading.count == null) "Set up" else "Unlocks"
            val textWidth = artWidth - 2f * inset
            if (wide) {
                compactWide(left, artWidth, labelSize, shortLabel)
                return
            }
            when (design.id) {
                "orbit" -> {
                    val centerY = top + artHeight * .44f
                    orbitMark(centerX, centerY, body * .31f)
                    number(centerX, centerY, body * .40f, body * .51f, center = true)
                    lineText(shortLabel, centerX, top + artHeight * .88f, labelSize, textWidth, quiet, center = true)
                }
                "signal" -> {
                    number(inset, top + artHeight * .31f, body * .47f, textWidth)
                    lineText(shortLabel, inset, top + artHeight * .62f, labelSize, textWidth, quiet)
                    chart(inset, width - inset, top + artHeight * .73f, top + artHeight * .87f)
                }
                "ticket" -> {
                    round(0f, 0f, maxOf(2f, body * .045f), height, 1f, accent)
                    perforation(top + artHeight * .66f, inset, width - inset, vertical = false)
                    number(inset, top + artHeight * .38f, body * .5f, textWidth)
                    lineText(shortLabel, inset, top + artHeight * .87f, labelSize, textWidth, quiet)
                }
                "terminal" -> {
                    lineText("UNLOCKS", inset, top + artHeight * .25f, labelSize, textWidth, accent, mono = true)
                    number(inset, top + artHeight * .51f, body * .44f, textWidth, mono = true)
                    val baseline = top + artHeight * .84f
                    val label = if (reading.count == null) "SET UP" else "TODAY"
                    lineText(label, inset, baseline, labelSize * .85f, textWidth - 5f, quiet, mono = true)
                    val cursor = body * .09f
                    round(width - inset - cursor, baseline - cursor, width - inset, baseline, 1f, accent)
                }
                else -> {
                    lock(inset, top + artHeight * .10f, body * .23f)
                    number(inset, top + artHeight * .53f, body * .46f, textWidth)
                    lineText(shortLabel, inset, top + artHeight * .85f, labelSize, textWidth, quiet)
                }
            }
        }

        private fun compactWide(left: Float, artWidth: Float, labelSize: Float, label: String) {
            val x = left + inset
            val middle = left + artWidth * .52f
            val right = left + artWidth - inset
            val terminal = design.id == "terminal"
            if (design.id == "orbit") {
                val center = left + artWidth * .28f
                orbitMark(center, height / 2f, body * .34f)
                number(center, height / 2f, body * .42f, body * .55f, center = true)
                lineText(label, middle, height * .56f, labelSize, right - middle, quiet)
                return
            }
            number(x, height * .41f, body * .48f, middle - x - 3f, mono = terminal)
            lineText(label, x, height * .79f, labelSize, middle - x - 3f, quiet, mono = terminal)
            when (design.id) {
                "signal" -> chart(middle, right, height * .27f, height * .75f)
                "ticket" -> {
                    round(0f, 0f, maxOf(2f, body * .045f), height, 1f, accent)
                    perforation(middle, inset, height - inset, vertical = true)
                    val today = if (reading.count == null) "" else "Today"
                    lineText(today, middle + inset / 2f, height * .56f, labelSize, right - middle - inset / 2f, accent)
                }
                "terminal" -> {
                    val cursor = body * .13f
                    round(right - cursor, height * .64f - cursor, right, height * .64f, 1f, accent)
                }
                else -> {
                    val mark = body * .38f
                    lock((middle + right - mark) / 2f, (height - mark) / 2f, mark)
                }
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
            orbitMark(cx, cy, radius)
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
            chart(plotLeft, plotRight, plotTop, plotBottom)
        }

        private fun ticket() {
            val split = if (wide) width * .68f else height * .72f
            round(0f, 0f, 4f, height, 2f, accent)
            perforation(split, inset, (if (wide) height else width) - inset, vertical = wide)
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

        private fun orbitMark(cx: Float, cy: Float, radius: Float) {
            ring(cx, cy, radius, tint(accent, 140))
            ring(cx, cy, radius * .84f, tint(accent, 62))
            circle(cx, cy - radius, maxOf(1.5f, radius * .075f), accent)
        }

        private fun chart(left: Float, right: Float, top: Float, bottom: Float) {
            if (reading.buckets.isEmpty()) return
            val peak = maxOf(1, reading.buckets.maxOrNull() ?: 1)
            val bar = (right - left) / reading.buckets.size
            reading.buckets.forEachIndexed { i, count ->
                val x = left + i * bar
                val barHeight = maxOf(1.25f, (bottom - top) * count / peak)
                round(x, bottom - barHeight, x + bar * .68f, bottom, 1.5f,
                    if (count == 0) tint(accent, 70) else accent)
            }
        }

        private fun perforation(position: Float, start: Float, end: Float, vertical: Boolean) {
            paint.color = tint(accent, 130)
            paint.strokeWidth = 1f
            var offset = start
            while (offset < end) {
                val stop = min(offset + 3f, end)
                if (vertical) canvas.drawLine(position, offset, position, stop, paint)
                else canvas.drawLine(offset, position, stop, position, paint)
                offset += 6f
            }
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
