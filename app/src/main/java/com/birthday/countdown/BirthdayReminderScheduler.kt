package com.birthday.countdown

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object BirthdayReminderScheduler {
    const val ACTION_BIRTHDAY_REMINDER = "com.birthday.countdown.ACTION_BIRTHDAY_REMINDER"
    private const val PREFS = "birthday_reminder"
    private const val MONTH = "month"
    private const val DAY = "day"
    private const val REQUEST_CODE = 7419

    fun schedule(context: Context, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day !in 1..31) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(MONTH, month)
            .putInt(DAY, day)
            .apply()
        return scheduleNext(context, month, day)
    }

    fun rescheduleSaved(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val month = prefs.getInt(MONTH, 0)
        val day = prefs.getInt(DAY, 0)
        return if (month in 1..12 && day in 1..31) scheduleNext(context, month, day) else false
    }

    private fun scheduleNext(context: Context, month: Int, day: Int): Boolean {
        val now = Calendar.getInstance()
        var alarmTime = dateAtNine(now.get(Calendar.YEAR), month, day)
        if (alarmTime.timeInMillis <= now.timeInMillis) {
            alarmTime = dateAtNine(now.get(Calendar.YEAR) + 1, month, day)
        }

        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            action = ACTION_BIRTHDAY_REMINDER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime.timeInMillis,
                pendingIntent
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun dateAtNine(year: Int, month: Int, day: Int): Calendar {
        val firstOfMonth = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val safeDay = minOf(day, firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, safeDay, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
