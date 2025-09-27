package com.zaneschepke.wireguardautotunnel.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.broadcast.NotificationActionReceiver
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager.Companion.EXTRA_ID
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WireGuardNotification @Inject constructor(@ApplicationContext override val context: Context) :
    com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager {

    enum class NotificationChannels {
        VPN,
        AUTO_TUNNEL,
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    override fun createNotification(
        channel: NotificationChannels,
        title: String,
        actions: Collection<NotificationCompat.Action>,
        description: String,
        showTimestamp: Boolean,
        importance: Int,
        onGoing: Boolean,
        onlyAlertOnce: Boolean,
    ): Notification {
        notificationManager.createNotificationChannel(channel.asChannel())
        return channel
            .asBuilder()
            .apply {
                actions.forEach { addAction(it) }
                setContentTitle(title)
                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                )
                setContentText(description)
                setOnlyAlertOnce(onlyAlertOnce)
                setOngoing(onGoing)
                setPriority(NotificationCompat.PRIORITY_HIGH)
                setShowWhen(showTimestamp)
                setSmallIcon(R.drawable.ic_notification)
            }
            .build()
    }

    override fun createNotification(
        channel: NotificationChannels,
        title: StringValue,
        actions: Collection<NotificationCompat.Action>,
        description: StringValue,
        showTimestamp: Boolean,
        importance: Int,
        onGoing: Boolean,
        onlyAlertOnce: Boolean,
    ): Notification {
        return createNotification(
            channel,
            title.asString(context),
            actions,
            description.asString(context),
            showTimestamp,
            importance,
            onGoing,
            onlyAlertOnce,
        )
    }

    override fun createNotificationAction(
        notificationAction: NotificationAction,
        extraId: Int?,
    ): NotificationCompat.Action {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                extraId ?: 0,
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = notificationAction.name
                    if (extraId != null) putExtra(EXTRA_ID, extraId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                notificationAction.title(context).uppercase(),
                pendingIntent,
            )
            .build()
    }

    override fun remove(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun show(notificationId: Int, notification: Notification) {
        with(notificationManager) {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(notificationId, notification)
        }
    }

    private fun NotificationChannels.asBuilder(): NotificationCompat.Builder {
        return when (this) {
            NotificationChannels.AUTO_TUNNEL -> {
                NotificationCompat.Builder(
                    context,
                    context.getString(R.string.auto_tunnel_channel_id),
                )
            }
            NotificationChannels.VPN -> {
                NotificationCompat.Builder(context, context.getString(R.string.vpn_channel_id))
            }
        }
    }

    private fun NotificationChannels.asChannel(): NotificationChannel {
        return when (this) {
            NotificationChannels.VPN -> {
                NotificationChannel(
                        context.getString(R.string.vpn_channel_id),
                        context.getString(R.string.vpn_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        description = context.getString(R.string.vpn_channel_description)
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(false)
                        vibrationPattern = longArrayOf(100, 200, 300)
                    }
            }
            NotificationChannels.AUTO_TUNNEL -> {
                NotificationChannel(
                        context.getString(R.string.auto_tunnel_channel_id),
                        context.getString(R.string.auto_tunnel_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        description = context.getString(R.string.auto_tunnel_channel_description)
                        enableLights(true)
                        lightColor = Color.WHITE
                        enableVibration(false)
                        vibrationPattern = longArrayOf(100, 200, 300)
                    }
            }
        }
    }
}
