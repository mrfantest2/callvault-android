package win.fantest.callvault.core.backup

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import win.fantest.callvault.core.crypto.PortableBackupCrypto
import win.fantest.callvault.core.storage.RecordingLibrary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EncryptedBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val library = RecordingLibrary(appContext)
    private val crypto = PortableBackupCrypto()

    fun createPortableBackup(passphrase: CharArray): BackupResult {
        require(passphrase.size >= 6) { "Backup passphrase must be at least 6 characters" }

        val root =
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: appContext.filesDir
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(root, "backups/callvault_$stamp").apply { mkdirs() }
        val audioDir = File(backupDir, "audio").apply { mkdirs() }

        val entries = (library.active() + library.trash()).distinctBy { it.id }
        val manifestItems = JSONArray()
        var encrypted = 0
        var skipped = 0

        entries.forEach { entry ->
            val source = File(entry.filePath)
            if (!source.isFile) {
                skipped++
                return@forEach
            }

            val destination = File(audioDir, entry.id + ".cvbackup")
            crypto.encrypt(source, destination, passphrase)

            manifestItems.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("backup_file", "audio/" + destination.name)
                    .put("original_name", source.name)
                    .put("created_at", entry.createdAtEpochMs)
                    .put("duration_ms", entry.durationMs ?: JSONObject.NULL)
                    .put("phone_number", entry.phoneNumber ?: JSONObject.NULL)
                    .put("direction", entry.direction ?: JSONObject.NULL)
                    .put("sim_subscription_id", entry.simSubscriptionId ?: JSONObject.NULL)
                    .put("display_name", entry.displayName ?: JSONObject.NULL)
                    .put("note", entry.note ?: JSONObject.NULL)
                    .put("favorite", entry.favorite)
                    .put("trashed_at", entry.trashedAtEpochMs ?: JSONObject.NULL)
            )
            encrypted++
        }

        val manifest = JSONObject()
            .put("format", "CallVault Portable Backup")
            .put("version", 1)
            .put("created_at", System.currentTimeMillis())
            .put("encryption", "AES-256-GCM + PBKDF2-HMAC-SHA256")
            .put("entries", manifestItems)

        File(backupDir, "manifest.json").writeText(manifest.toString(2))

        return BackupResult(
            directory = backupDir.absolutePath,
            encryptedFiles = encrypted,
            skippedFiles = skipped
        )
    }
}
