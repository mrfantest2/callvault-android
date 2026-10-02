package win.fantest.callvault

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import win.fantest.callvault.core.recorder.MicrophoneRecorderEngine
import win.fantest.callvault.core.recorder.RecorderState
import java.io.File

class MainActivity : Activity() {
    private val recorder by lazy { MicrophoneRecorderEngine(this) }
    private lateinit var statusView: TextView
    private var pendingStart = false

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
            text = "CP02 recorder core ready"
            textSize = 18f
        }
        root.addView(statusView)

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
        if (requestCode == REQUEST_RECORD_AUDIO) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            if (granted && pendingStart) startRecording()
            if (!granted) statusView.text = "Microphone permission is required"
            pendingStart = false
        }
    }

    override fun onDestroy() {
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
    }
}
