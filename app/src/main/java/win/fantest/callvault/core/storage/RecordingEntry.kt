package win.fantest.callvault.core.storage

data class RecordingEntry(
    val id: String,
    val filePath: String,
    val createdAtEpochMs: Long,
    val durationMs: Long? = null,
    val phoneNumber: String? = null,
    val direction: String? = null,
    val simSubscriptionId: Int? = null,
    val displayName: String? = null,
    val note: String? = null,
    val favorite: Boolean = false,
    val trashedAtEpochMs: Long? = null
)
