package com.widgets.gallery

import android.app.AppOpsManager
import android.app.Application
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.SizeF
import android.widget.RemoteViews
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class Design(val id: String, val title: String, val description: String, val accent: Int)

internal object Catalog {
    @Volatile private var cached: List<Design>? = null

    fun designs(context: Context): List<Design> = cached ?: synchronized(this) {
        cached ?: context.assets.open("widgets.json").bufferedReader().use { input ->
            val json = JSONArray(input.readText())
            List(json.length()) { index ->
                val row = json.getJSONObject(index)
                Design(row.getString("id"), row.getString("title"),
                    row.getString("description"), Color.parseColor(row.getString("color")))
            }
        }.also { cached = it }
    }

    fun design(context: Context, id: String): Design {
        val list = designs(context)
        return list.firstOrNull { it.id == id } ?: list.first()
    }
}

internal data class Reading(val count: Int?, val buckets: List<Int>, val label: String) {
    val accessibilityLabel: String
        get() = if (count == null) "Phone unlocks. $label" else "$count phone unlocks today"

    companion object {
        val sample = Reading(85, listOf(0, 2, 16, 21, 19, 14, 9, 4), "Unlocks today")
    }
}

internal object Readings {
    @Suppress("DEPRECATION")
    fun hasUsageAccess(context: Context): Boolean =
        context.getSystemService(AppOpsManager::class.java).checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED

    fun read(context: Context): Reading {
        if (!hasUsageAccess(context)) return Reading(null, emptyList(), "Enable Usage Access")
        val zone = ZoneId.systemDefault()
        val midnight = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val stats = context.getSystemService(UsageStatsManager::class.java)
        val events = try {
            if (Build.VERSION.SDK_INT >= 35) {
                stats.queryEvents(android.app.usage.UsageEventsQuery.Builder(midnight, now)
                    .setEventTypes(UsageEvents.Event.KEYGUARD_HIDDEN).build())
            } else {
                stats.queryEvents(midnight, now)
            }
        } catch (_: SecurityException) {
            null
        } ?: return Reading(null, emptyList(), "Unlock count unavailable")

        val buckets = IntArray(8)
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                val hour = Instant.ofEpochMilli(event.timeStamp).atZone(zone).hour
                buckets[hour / 3]++
            }
        }
        return Reading(buckets.sum(), buckets.toList(), "Unlocks today")
    }
}

internal const val PIN_UNLOCKS_WIDGET = "com.widgets.gallery.PIN_UNLOCKS_WIDGET"
internal const val EXTRA_DESIGN = "design"
internal const val EXTRA_LIGHT = "light"

class UnlocksWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PIN_UNLOCKS_WIDGET) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val design = Catalog.design(context, intent.getStringExtra(EXTRA_DESIGN).orEmpty())
            WidgetAppearance.save(context, id, Appearance(light = intent.getBooleanExtra(EXTRA_LIGHT, false), design = design.id))
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        UnlockRefresh.schedule(context)
        refresh(context, null)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        refresh(context, intArrayOf(id))
    }

    private fun refresh(context: Context, targets: IntArray?) {
        val pending = goAsync()
        UnlockRefresh.enqueue(context, targets) { pending?.finish() }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetAppearance.remove(context, ids)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(JobScheduler::class.java).cancel(UnlockRefresh.JOB_ID)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, UnlocksWidget::class.java)
    }
}

internal object UnlockRefresh {
    const val JOB_ID = 85
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (scheduler.getPendingJob(JOB_ID) == null && ids(context).isNotEmpty()) {
            scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(context, UnlockRefreshJob::class.java))
                .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setPersisted(true)
                .build())
        }
    }

    fun enqueue(
        context: Context,
        targets: IntArray? = null,
        delayMillis: Long = 0,
        complete: () -> Unit = {},
    ) = executor.schedule({
        try {
            refresh(context.applicationContext, targets)
        } finally {
            complete()
        }
    }, delayMillis, TimeUnit.MILLISECONDS)

    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(UnlocksWidget.component(context))

    private fun refresh(context: Context, targets: IntArray?) {
        val manager = AppWidgetManager.getInstance(context)
        val active = ids(context)
        val updates = targets?.filter(active::contains)?.toIntArray() ?: active
        if (updates.isEmpty()) return

        val reading = Readings.read(context)
        val click = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        for (id in updates) {
            val appearance = WidgetAppearance.read(context, id)
            val design = Catalog.design(context, appearance.design)
            val options = manager.getAppWidgetOptions(id)
            fun render(size: SizeF) = previewWidget(context, design, reading, appearance, size).apply {
                setOnClickPendingIntent(R.id.widget_art, click)
            }
            val sizes = if (Build.VERSION.SDK_INT >= 31) sizes(options) else emptyList()
            val views = if (Build.VERSION.SDK_INT >= 31 && sizes.isNotEmpty()) {
                RemoteViews(sizes.associateWith(::render))
            } else {
                val portrait = SizeF(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 64).coerceAtLeast(32).toFloat(),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 64).coerceAtLeast(32).toFloat(),
                )
                val landscape = SizeF(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 64).coerceAtLeast(32).toFloat(),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 64).coerceAtLeast(32).toFloat(),
                )
                RemoteViews(render(landscape), render(portrait))
            }
            manager.updateAppWidget(id, views)
        }
    }

    @android.annotation.TargetApi(31)
    @Suppress("DEPRECATION")
    private fun sizes(options: Bundle): List<SizeF> =
        options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            .orEmpty().filter { it.width > 0 && it.height > 0 }.distinct().take(8)
}

class UnlockRefreshJob : JobService() {
    private var work: java.util.concurrent.Future<*>? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = UnlockRefresh.enqueue(this) { jobFinished(params, false) }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel(false)
        return false
    }
}

class UnlockDateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        val pending = goAsync()
        UnlockRefresh.schedule(context)
        UnlockRefresh.enqueue(context) { pending.finish() }
    }
}

class WidgetsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_USER_PRESENT) return
                val pending = goAsync()
                UnlockRefresh.enqueue(context, delayMillis = 500) { pending.finish() }
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(receiver, filter)
    }
}

internal fun previewWidget(
    context: Context,
    design: Design,
    reading: Reading,
    appearance: Appearance,
    size: SizeF = SizeF(100f, 100f),
): RemoteViews = RemoteViews(context.packageName, R.layout.widget).apply {
    setImageViewBitmap(R.id.widget_art, UnlockArt.bitmap(size, design, reading, appearance))
    setContentDescription(R.id.widget_art, reading.accessibilityLabel)
}
