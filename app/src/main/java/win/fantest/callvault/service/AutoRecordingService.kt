package win.fantest.callvault.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import win.fantest.callvault.core.calls.CallDirection
import win.fantest.callvault.core.calls.CallSession
import win.fantest.callvault.core.calls.CallSessionTracker
import win.fantest.callvault.core.calls.CallState
import win.fantest.callvault.core.calls.CallStateMonitor
import win.fantest.callvault.core.recorder.MicrophoneRecorderEngine
import win.fantest.callvault.core.recorder.RecorderState
import win.fantest.callvault.core.rules.RecordingRulesRepository
import win.fantest.callvault.core.storage.RecordingLibrary
import java.io.File

class AutoRecordingService : Service() {
    private val recorder by lazy { MicrophoneRecorderEngine(this) }
    private val library by lazy { RecordingLibrary(this) }
    private val rulesRepository by lazy { RecordingRulesRepository(this) }
    private val tracker = CallSessionTracker()

    private var recordingSession: CallSession? = null
    private var recordingStartedAt: Long? = null

    private val monitor by lazy {
        CallStateMonitor(this) { state -> handleState(state) }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification("Monitoring call state")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        monitor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!rulesRepository.load().enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        updateNotification("Automatic recording enabled")
        return START_STICKY
    }

    override fun onDestroy() {
        monitor.stop()
        finishRecording(forceKeep = true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleState(state: CallState) {
        val completed = tracker.onStateChanged(state)

        when (state) {
            CallState.OFFHOOK -> {
                val session = tracker.activeSession()
                if (session != null &&
                    recorder.state == RecorderState.IDLE &&
                    shouldRecord(session)
                ) {
                    startRecording(session)
                }
            }

            CallState.IDLE -> {
                if (completed != null && recorder.state == RecorderState.RECORDING) {
                    finishRecording(forceKeep = false, completedSession = completed)
                }
            }

            CallState.RINGING -> updateNotification("Incoming call detected")
        }
    }

    private fun shouldRecord(session: CallSession): Boolean {
        val rules = rulesRepository.load()
        if (!rules.enabled) return false

        return when (session.direction) {
            CallDirection.INCOMING -> rules.recordIncoming
            CallDirection.OUTGOING_OR_UNKNOWN -> rules.recordOutgoingOrUnknown
            CallDirection.UNKNOWN -> false
        }
    }

    private fun startRecording(session: CallSession) {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val output = File(baseDir, "recordings/call_" + System.currentTimeMillis() + ".m4a")

        try {
            recorder.start(output)
            recordingSession = session
            recordingStartedAt = System.currentTimeMillis()
            updateNotification("Recording active call")
        } catch (_: Throwable) {
            recordingSession = null
            recordingStartedAt = null
            updateNotification("Recording start failed")
        }
    }

    private fun finishRecording(
        forceKeep: Boolean,
        completedSession: CallSession? = recordingSession
    ) {
        if (recorder.state != RecorderState.RECORDING) return

        try {
            val file = recorder.stop() ?: return
            val started = recordingStartedAt ?: file.lastModified()
            val duration = (System.currentTimeMillis() - started).coerceAtLeast(0L)
            val rules = rulesRepository.load()

            if (forceKeep || duration >= rules.minimumDurationMs) {
                library.register(
                    file = file,
                    session = completedSession,
                    durationMs = duration
                )
                updateNotification("Call recording saved")
            } else {
                file.delete()
                updateNotification("Short recording discarded")
            }
        } catch (_: Throwable) {
            recorder.cancel()
            updateNotification("Recording stop failed")
        } finally {
            recordingSession = null
            recordingStartedAt = null
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("CallVault")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Automatic call recording",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        private const val CHANNEL_ID = "callvault_auto_recording"
        private const val NOTIFICATION_ID = 7001
    }
}
