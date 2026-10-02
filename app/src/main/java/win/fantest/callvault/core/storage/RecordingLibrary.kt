package win.fantest.callvault.core.storage

import android.content.Context
import win.fantest.callvault.core.calls.CallSession
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class RecordingLibrary(context: Context) {
    private val database = RecordingDatabase(context.applicationContext)

    fun register(
        file: File,
        session: CallSession? = null,
        durationMs: Long? = null
    ): RecordingEntry {
        val entry = RecordingEntry(
            id = UUID.randomUUID().toString(),
            filePath = file.absolutePath,
            createdAtEpochMs = System.currentTimeMillis(),
            durationMs = durationMs,
            phoneNumber = session?.phoneNumber,
            direction = session?.direction?.name,
            simSubscriptionId = session?.simSubscriptionId,
            displayName = file.nameWithoutExtension
        )
        database.upsert(entry)
        return entry
    }

    fun active(): List<RecordingEntry> = database.listActive()
    fun search(query: String): List<RecordingEntry> = database.searchActive(query)
    fun trash(): List<RecordingEntry> = database.listTrash()

    fun setFavorite(id: String, favorite: Boolean) =
        database.updateFavorite(id, favorite)

    fun setNote(id: String, note: String?) =
        database.updateNote(id, note)

    fun setDuration(id: String, durationMs: Long) =
        database.updateDuration(id, durationMs)

    fun moveToTrash(id: String) = database.markTrashed(id)
    fun restore(id: String) = database.restore(id)

    fun deletePermanently(entry: RecordingEntry) {
        File(entry.filePath).delete()
        database.deletePermanently(entry.id)
    }

    fun purgeExpiredTrash(retentionDays: Int, nowEpochMs: Long = System.currentTimeMillis()): Int {
        val cutoff = nowEpochMs - TimeUnit.DAYS.toMillis(retentionDays.coerceAtLeast(1).toLong())
        val expired = trash().filter { (it.trashedAtEpochMs ?: Long.MAX_VALUE) <= cutoff }
        expired.forEach(::deletePermanently)
        return expired.size
    }
}
