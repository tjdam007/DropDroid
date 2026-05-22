package io.github.tjdam007.dropdroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DropDroidReceiverService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification(ApkDropServer.state.value))
    ApkDropServer.start(this)
    serviceScope.launch {
      ApkDropServer.state.collectLatest { state ->
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ApkDropServer.start(this)
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun buildNotification(state: ReceiverState): android.app.Notification {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val title = if (state.isReceiving) "Receiving ${state.receivingFileName}" else "DropDroid receiver is running"
    val text =
      when {
        state.isReceiving -> "${state.receivingBytes.toReadableSize()} / ${state.receivingTotalBytes.toReadableSize()}"
        state.isPaired -> "Ready for paired local transfers"
        else -> "Open DropDroid and scan the portal QR"
      }
    val builder =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(openIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)

    if (state.isReceiving && state.receivingTotalBytes > 0L) {
      builder.setProgress(100, (state.progress * 100).toInt().coerceIn(0, 100), false)
    } else {
      builder.setProgress(0, 0, false)
    }

    return builder.build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        "DropDroid receiver",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "Shows DropDroid local receiver status and transfer progress."
      }
    notificationManager.createNotificationChannel(channel)
  }

  private val notificationManager: NotificationManager
    get() = getSystemService(NotificationManager::class.java)

  private fun Long.toReadableSize(): String {
    if (this < 1024) return "$this B"
    val units = listOf("KB", "MB", "GB")
    var value = this.toDouble()
    var unit = "B"
    for (next in units) {
      value /= 1024.0
      unit = next
      if (value < 1024.0) break
    }
    return "%.1f %s".format(java.util.Locale.US, value, unit)
  }

  companion object {
    private const val CHANNEL_ID = "dropdroid_receiver"
    private const val NOTIFICATION_ID = 4001
  }
}
