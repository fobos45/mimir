package com.mimir.app.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mimir.app.MainActivity
import com.mimir.app.R
import com.mimir.app.data.AppDatabase
import com.mimir.app.data.MimirBridge
import kotlinx.coroutines.*
import androidx.room.Room

class ConnectionService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    companion object {
        const val CHANNEL_ONGOING  = "mimir_ongoing_v1"
        const val CHANNEL_MESSAGES = "mimir_messages_v3"   // v3 = IMPORTANCE_HIGH
        const val CHANNEL_CALLS    = "mimir_calls_v1"
        const val NOTIFICATION_ID  = 1
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "mimir.db").build()
        startForeground(NOTIFICATION_ID, buildServiceNotification())
        startMimir()
        collectEvents()
    }

    private fun startMimir() {
        MimirBridge.start(
            context  = applicationContext,
            seedHex  = getOrCreateSeed(),
            yggPeers = listOf(
                "tcp://de1.mimir.im:7743",
                "tcp://de2.mimir.im:7743",
                "tcp://sk1.mimir.im:7743",
            ),
            trackers = listOf("de1.mimir.im:7744"),
            filesDir = filesDir.absolutePath,
        )
    }

    private fun collectEvents() = scope.launch {
        MimirBridge.events.collect { event ->
            when (event) {
                is MimirBridge.Event.MessageReceived -> handleIncomingMessage(event)
                is MimirBridge.Event.IncomingCall    -> showCallNotification(event.pubkeyHex)
                is MimirBridge.Event.PeerConnected   -> db.contacts().setOnline(event.pubkeyHex, true, System.currentTimeMillis())
                is MimirBridge.Event.PeerDisconnected -> db.contacts().setOnline(event.pubkeyHex, false, System.currentTimeMillis())
                is MimirBridge.Event.MessageDelivered -> db.messages().markDelivered(event.guid)
                is MimirBridge.Event.FileSendProgress -> db.messages().updateUploadProgress(
                    event.guid, event.sent.toFloat() / event.total.coerceAtLeast(1)
                )
                is MimirBridge.Event.FileReceiveProgress -> db.messages().updateDownloadProgress(
                    event.guid, event.received.toFloat() / event.total.coerceAtLeast(1)
                )
                else -> {}
            }
        }
    }

    private suspend fun handleIncomingMessage(event: MimirBridge.Event.MessageReceived) {
        val text = String(event.data, Charsets.UTF_8)
        val msg = com.mimir.app.data.Message(
            guid          = event.guid,
            contactPubkey = event.pubkeyHex,
            isOutgoing    = false,
            msgType       = event.msgType,
            text          = if (event.msgType == 1) text else "",
            sendTime      = event.sendTime,
        )
        db.messages().upsert(msg)
        db.contacts().incrementUnread(event.pubkeyHex)

        // Уведомление
        val contact = db.contacts().byKey(event.pubkeyHex)
        val name = contact?.nickname ?: event.pubkeyHex.take(8) + "…"
        showMessageNotification(name, if (event.msgType == 1) text else "📎 Файл", event.pubkeyHex)
    }

    private fun showMessageNotification(name: String, preview: String, pubkeyHex: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_chat", pubkeyHex)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(this, pubkeyHex.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(name)
            .setContentText(preview)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(pubkeyHex.hashCode(), n)
    }

    private fun showCallNotification(pubkeyHex: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("incoming_call", pubkeyHex)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Входящий звонок")
            .setContentText(pubkeyHex.take(8) + "…")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pi, true)
            .build()
        getSystemService(NotificationManager::class.java).notify(99, n)
    }

    private fun buildServiceNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("Mimir активен")
            .setContentText("Защищённые сообщения работают")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Ongoing (низкий приоритет — просто статус)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ONGOING, "Фоновая работа", NotificationManager.IMPORTANCE_MIN
        ).apply { setShowBadge(false) })

        // Сообщения (IMPORTANCE_HIGH — звук + всплытие)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_MESSAGES, "Сообщения", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
        })

        // Звонки (IMPORTANCE_HIGH + fullscreen intent)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_CALLS, "Звонки", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
        })
    }

    private fun getOrCreateSeed(): String {
        val prefs = getSharedPreferences("mimir_prefs", MODE_PRIVATE)
        return prefs.getString("seed_hex", null) ?: run {
            val seed = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            val hex = seed.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("seed_hex", hex).apply()
            hex
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); MimirBridge.stop(); super.onDestroy() }
}
