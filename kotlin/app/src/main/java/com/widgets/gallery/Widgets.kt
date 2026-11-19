package com.widgets.gallery

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.Process
import android.widget.RemoteViews
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WidgetKind(val key: String, val icon: Int, val provider: Class<out GalleryWidget>) {
    UNLOCKS("unlocks", R.drawable.ic_unlocks, UnlocksWidget::class.java),
    BATTERY("battery", R.drawable.ic_battery, BatteryWidget::class.java),
    DATE("date", R.drawable.ic_date, DateWidget::class.java),
    COUNTER("counter", R.drawable.ic_counter, CounterWidget::class.java);

    fun component(context: Context) = ComponentName(context, provider)
}

data class WidgetDetails(val title: String, val sample: Reading)

object Catalog {
    private var cached: Map<String, WidgetDetails>? = null

    fun details(context: Context, kind: WidgetKind): WidgetDetails {
        val details = cached ?: load(context).also { cached = it }
        return details.getValue(kind.key)
    }

    private fun load(context: Context): Map<String, WidgetDetails> {
        val json = JSONArray(context.assets.open("widgets.json").bufferedReader().use { it.readText() })
        return (0 until json.length()).associate { index ->
            val row = json.getJSONObject(index)
            row.getString("id") to WidgetDetails(
                row.getString("title"),
                Reading(row.getString("sample"), row.getString("label"))
            )
        }
    }
}

data class Reading(val value: String, val label: String)

object Readings {
    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean =
        context.getSystemService(AppOpsManager::class.java).checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED

    fun read(context: Context, kind: WidgetKind): Reading = when (kind) {
        WidgetKind.UNLOCKS -> unlocks(context)
        WidgetKind.BATTERY -> {
            val level = context.getSystemService(BatteryManager::class.java)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            Reading(if (level in 0..100) "$level%" else "—", "Battery remaining")
        }
        WidgetKind.DATE -> {
            val today = LocalDate.now()
            Reading(today.dayOfMonth.toString(),
                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM", Locale.getDefault())))
        }
        WidgetKind.COUNTER -> Reading(counter(context).getLong("count", 0).toString(), "Tap to count")
    }

    private fun unlocks(context: Context): Reading {
        if (!hasUsageAccess(context)) return Reading("—", "Enable Usage Access")
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val events = try {
            context.getSystemService(UsageStatsManager::class.java)
                .queryEvents(midnight, System.currentTimeMillis())
        } catch (_: SecurityException) {
            null
        } ?: return Reading("—", "Unlocks unavailable")
        val event = UsageEvents.Event()
        var count = 0
        while (events.getNextEvent(event)) {
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
        }
        return Reading(count.toString(), "Phone unlocks today")
    }

    private fun counter(context: Context) = context.getSharedPreferences("counter", Context.MODE_PRIVATE)

    fun increment(context: Context) {
        val prefs = counter(context)
        prefs.edit().putLong("count", prefs.getLong("count", 0) + 1).apply()
    }
}

abstract class GalleryWidget(private val kind: WidgetKind) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateWidgets(context, kind, ids)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetAppearance.remove(context, ids)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateWidgets(context, kind, intArrayOf(id))
    }
}

class UnlocksWidget : GalleryWidget(WidgetKind.UNLOCKS)
class BatteryWidget : GalleryWidget(WidgetKind.BATTERY)
class DateWidget : GalleryWidget(WidgetKind.DATE)
class CounterWidget : GalleryWidget(WidgetKind.COUNTER) {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == COUNT_ACTION) {
            Readings.increment(context)
            updateWidgets(context, WidgetKind.COUNTER)
        } else {
            super.onReceive(context, intent)
        }
    }
}

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                updateWidgets(context, WidgetKind.DATE)
                updateWidgets(context, WidgetKind.UNLOCKS)
            }
        }
    }
}

private const val COUNT_ACTION = "com.widgets.gallery.INCREMENT"

fun updateWidgets(context: Context, kind: WidgetKind, ids: IntArray? = null, reading: Reading? = null) {
    val manager = AppWidgetManager.getInstance(context)
    val targetIds = ids ?: manager.getAppWidgetIds(kind.component(context))
    if (targetIds.isEmpty()) return
    val value = reading ?: Readings.read(context, kind)
    val click = if (kind == WidgetKind.COUNTER) {
        PendingIntent.getBroadcast(context, 0, Intent(context, CounterWidget::class.java).setAction(COUNT_ACTION),
            PendingIntent.FLAG_IMMUTABLE)
    } else {
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
    }

    for (id in targetIds) {
        val views = widgetViews(context, kind, value).apply {
            if (WidgetAppearance.light(context, id)) {
                setImageViewResource(R.id.widget_surface, R.drawable.widget_background_light)
                setTextColor(R.id.widget_value, 0xff191d19.toInt())
                setTextColor(R.id.widget_label, 0xff343c35.toInt())
            }
            setInt(R.id.widget_surface, "setImageAlpha", WidgetAppearance.alpha(context, id))
            setOnClickPendingIntent(android.R.id.background, click)
        }
        manager.updateAppWidget(id, views)
    }
}

fun widgetViews(context: Context, kind: WidgetKind, reading: Reading): RemoteViews {
    return RemoteViews(context.packageName, R.layout.widget).apply {
        setImageViewResource(R.id.widget_icon, kind.icon)
        setTextViewText(R.id.widget_value, reading.value)
        setTextViewText(R.id.widget_label, reading.label)
    }
}
