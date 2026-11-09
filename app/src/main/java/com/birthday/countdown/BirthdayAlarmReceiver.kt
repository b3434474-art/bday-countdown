package com.birthday.countdown

import android.Manifest
import android.content.BroadcastReceiver
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class BirthdayAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            BirthdayReminderScheduler.rescheduleSaved(context)
            return
        }
        if (intent?.action != BirthdayReminderScheduler.ACTION_BIRTHDAY_REMINDER) return

        showBirthdayNotification(context)
        BirthdayReminderScheduler.rescheduleSaved(context)
    }

    private fun showBirthdayNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "birthday_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Birthday reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A reminder on your birthday"
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val openApp = PendingIntent.getActivity(context, 7420, openAppIntent, pendingFlags)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("It’s your birthday! 🎂")
            .setContentText("Have an amazing day. Your birthday countdown is celebrating with you!")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        manager.notify(7421, notification)
    }
}
