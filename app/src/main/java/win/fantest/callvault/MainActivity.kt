package win.fantest.callvault

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import win.fantest.callvault.core.calls.CallSessionTracker
import win.fantest.callvault.core.calls.CallState
import win.fantest.callvault.core.calls.CallStateMonitor
import win.fantest.callvault.core.recorder.MicrophoneRecorderEngine
import win.fantest.callvault.core.recorder.RecorderState
import java.io.File

class MainActivity : Activity() {
    private val recorder by lazy { MicrophoneRecorderEngine(this) }
    private val sessionTracker = CallSessionTracker()
    private lateinit var statusView: TextView
    private lateinit var callStateView: TextView
    private var pendingStart = false

    private val callMonitor by lazy {
        CallStateMonitor(this) { state ->
            runOnUiThread {
                val completed = sessionTracker.onStateChanged(state)
                callStateView.text = "Call state: " + state.name
                if (completed != null) {
                    statusView.text = "Call session completed: " + completed.id.take(8)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "CallVault"
            textSize = 30f
        })

        statusView = TextView(this).apply {
            text = "CP03 call-state core ready"
            textSize = 18f
        }
        root.addView(statusView)

        callStateView = TextView(this).apply {
            text = "Call state: monitor off"
            textSize = 16f
        }
        root.addView(callStateView)

        root.addView(Button(this).apply {
            text = "Enable call-state monitor"
            setOnClickListener { requestOrStartCallMonitor() }
        })

        root.addView(Button(this).apply {
            text = "Start test recording"
            setOnClickListener { requestOrStartRecording() }
        })

        root.addView(Button(this).apply {
            text = "Stop recording"
            setOnClickListener { stopRecording() }
        })

        setContentView(root)
    }

    private fun requestOrStartCallMonitor() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
            return
        }
        startCallMonitor()
    }

    private fun startCallMonitor() {
        try {
            callMonitor.start()
            callStateView.text = "Call state: monitoring"
        } catch (error: Throwable) {
            callStateView.text = "Monitor failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun requestOrStartRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        startRecording()
    }

    private fun startRecording() {
        if (recorder.state == RecorderState.RECORDING) {
            statusView.text = "Already recording"
            return
        }

        val baseDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val output = File(baseDir, "recordings/test_" + System.currentTimeMillis() + ".m4a")

        try {
            recorder.start(output)
            statusView.text = "Recording: " + output.name
        } catch (error: Throwable) {
            statusView.text = "Start failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun stopRecording() {
        try {
            val file = recorder.stop()
            statusView.text =
                if (file != null) "Saved: " + file.absolutePath else "Recorder is idle"
        } catch (error: Throwable) {
            statusView.text = "Stop failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_RECORD_AUDIO -> {
                if (granted && pendingStart) startRecording()
                if (!granted) statusView.text = "Microphone permission is required"
                pendingStart = false
            }

            REQUEST_PHONE_STATE -> {
                if (granted) startCallMonitor()
                else callStateView.text = "Phone-state permission is required"
            }
        }
    }

    override fun onDestroy() {
        callMonitor.stop()
        if (recorder.state == RecorderState.RECORDING) {
            try {
                recorder.stop()
            } catch (_: Throwable) {
                recorder.cancel()
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 40
        private const val REQUEST_PHONE_STATE = 41
    }
}
