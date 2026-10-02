package win.fantest.callvault.core.recorder

import java.io.File

enum class RecorderState {
    IDLE,
    RECORDING
}

interface RecorderEngine {
    val state: RecorderState
    fun start(outputFile: File)
    fun stop(): File?
    fun cancel()
}
