package win.fantest.callvault.core.rules

data class RecordingRules(
    val enabled: Boolean = false,
    val recordIncoming: Boolean = true,
    val recordOutgoingOrUnknown: Boolean = true,
    val minimumDurationMs: Long = 0L
)
