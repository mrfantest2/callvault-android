package win.fantest.callvault.core.calls

import java.util.UUID

class CallSessionTracker {
    private var current: CallSession? = null
    private var previousState: CallState = CallState.IDLE

    fun onStateChanged(state: CallState, now: Long = System.currentTimeMillis()): CallSession? {
        when (state) {
            CallState.RINGING -> {
                if (current == null) {
                    current = CallSession(
                        id = UUID.randomUUID().toString(),
                        direction = CallDirection.INCOMING,
                        startedAtEpochMs = now
                    )
                }
            }

            CallState.OFFHOOK -> {
                if (current == null) {
                    current = CallSession(
                        id = UUID.randomUUID().toString(),
                        direction = CallDirection.OUTGOING_OR_UNKNOWN,
                        startedAtEpochMs = now,
                        connectedAtEpochMs = now
                    )
                } else if (current?.connectedAtEpochMs == null) {
                    current = current?.copy(connectedAtEpochMs = now)
                }
            }

            CallState.IDLE -> {
                if (previousState != CallState.IDLE && current != null) {
                    val completed = current?.copy(endedAtEpochMs = now)
                    current = null
                    previousState = state
                    return completed
                }
            }
        }

        previousState = state
        return null
    }

    fun activeSession(): CallSession? = current
}
