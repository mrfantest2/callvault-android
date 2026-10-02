package win.fantest.callvault.core.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class MicrophoneRecorderEngine(
    private val context: Context
) : RecorderEngine {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    override var state: RecorderState = RecorderState.IDLE
        private set

    override fun start(outputFile: File) {
        check(state == RecorderState.IDLE) { "Recorder is already active" }
        outputFile.parentFile?.mkdirs()

        val mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(128_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(outputFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            currentFile = outputFile
            state = RecorderState.RECORDING
        } catch (error: Throwable) {
            mediaRecorder.release()
            throw error
        }
    }

    override fun stop(): File? {
        val activeRecorder = recorder ?: return null
        val completedFile = currentFile
        try {
            activeRecorder.stop()
            return completedFile
        } finally {
            activeRecorder.release()
            recorder = null
            currentFile = null
            state = RecorderState.IDLE
        }
    }

    override fun cancel() {
        val file = currentFile
        try {
            stop()
        } catch (_: Throwable) {
            recorder?.release()
            recorder = null
            currentFile = null
            state = RecorderState.IDLE
        }
        file?.delete()
    }
}
