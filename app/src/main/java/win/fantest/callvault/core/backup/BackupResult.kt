package win.fantest.callvault.core.backup

data class BackupResult(
    val directory: String,
    val encryptedFiles: Int,
    val skippedFiles: Int
)
