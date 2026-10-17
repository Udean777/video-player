package app.subtle.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.subtle.player.core.player.NowPlaying
import app.subtle.player.core.player.PlaybackHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps playback alive while the app is in the background and offers play/pause and
 * next/previous from the notification. The engine itself stays owned by the player screen;
 * this service only drives [PlaybackHolder].
 */
@AndroidEntryPoint
class PlaybackService : Service() {

    @Inject lateinit var holder: PlaybackHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            holder.nowPlaying.collect { nowPlaying ->
                if (nowPlaying.title.isNotBlank()) {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(nowPlaying))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> holder.togglePlayPause()
            ACTION_NEXT -> holder.onNext?.invoke()
            ACTION_PREVIOUS -> holder.onPrevious?.invoke()
            ACTION_STOP -> {
                holder.detach()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val nowPlaying = holder.nowPlaying.value
        if (nowPlaying.title.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification(nowPlaying))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.playback_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(nowPlaying: NowPlaying): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(nowPlaying.title.ifBlank { getString(R.string.app_name) })
            .setContentText(getString(R.string.playback_background))
            .setContentIntent(activityIntent())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(nowPlaying.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (nowPlaying.hasPrevious) {
            builder.addAction(android.R.drawable.ic_media_previous, getString(R.string.previous_video), action(ACTION_PREVIOUS))
        }
        builder.addAction(
            if (nowPlaying.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            getString(if (nowPlaying.isPlaying) R.string.pause else R.string.play),
            action(ACTION_TOGGLE),
        )
        if (nowPlaying.hasNext) {
            builder.addAction(android.R.drawable.ic_media_next, getString(R.string.next_video), action(ACTION_NEXT))
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.close), action(ACTION_STOP))
        return builder.build()
    }

    private fun activityIntent(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
    }

    private fun action(action: String): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, action.hashCode(), intent(this).setAction(action), flags)
    }

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE = "app.subtle.player.TOGGLE"
        private const val ACTION_NEXT = "app.subtle.player.NEXT"
        private const val ACTION_PREVIOUS = "app.subtle.player.PREVIOUS"
        private const val ACTION_STOP = "app.subtle.player.STOP"

        fun intent(context: Context): Intent = Intent(context, PlaybackService::class.java)
    }
}
