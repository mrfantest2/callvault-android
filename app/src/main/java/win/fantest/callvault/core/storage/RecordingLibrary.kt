package win.fantest.callvault.core.storage

import android.content.Context
import java.io.File
import java.util.UUID

class RecordingLibrary(context: Context) {
    private val database = RecordingDatabase(context.applicationContext)

    fun register(file: File): RecordingEntry {
        val entry = RecordingEntry(
            id = UUID.randomUUID().toString(),
            filePath = file.absolutePath,
            createdAtEpochMs = System.currentTimeMillis(),
            displayName = file.nameWithoutExtension
        )
        database.upsert(entry)
        return entry
    }

    fun active(): List<RecordingEntry> = database.listActive()

    fun trash(): List<RecordingEntry> = database.listTrash()

    fun moveToTrash(id: String) = database.markTrashed(id)

    fun restore(id: String) = database.restore(id)

    fun deletePermanently(entry: RecordingEntry) {
        File(entry.filePath).delete()
        database.deletePermanently(entry.id)
    }
}
