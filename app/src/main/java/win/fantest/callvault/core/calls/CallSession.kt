package win.fantest.callvault.core.calls

enum class CallState {
    IDLE,
    RINGING,
    OFFHOOK
}

enum class CallDirection {
    INCOMING,
    OUTGOING_OR_UNKNOWN,
    UNKNOWN
}

data class CallSession(
    val id: String,
    val direction: CallDirection,
    val startedAtEpochMs: Long,
    val connectedAtEpochMs: Long? = null,
    val endedAtEpochMs: Long? = null,
    val phoneNumber: String? = null,
    val simSubscriptionId: Int? = null
)
