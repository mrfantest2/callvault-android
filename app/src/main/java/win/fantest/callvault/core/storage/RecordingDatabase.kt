package win.fantest.callvault.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class RecordingDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE recordings (
                id TEXT PRIMARY KEY,
                file_path TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                duration_ms INTEGER,
                phone_number TEXT,
                direction TEXT,
                sim_subscription_id INTEGER,
                display_name TEXT,
                note TEXT,
                favorite INTEGER NOT NULL DEFAULT 0,
                trashed_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_recordings_created_at ON recordings(created_at DESC)")
        db.execSQL("CREATE INDEX idx_recordings_trashed_at ON recordings(trashed_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upsert(entry: RecordingEntry) {
        val values = ContentValues().apply {
            put("id", entry.id)
            put("file_path", entry.filePath)
            put("created_at", entry.createdAtEpochMs)
            entry.durationMs?.let { put("duration_ms", it) } ?: putNull("duration_ms")
            entry.phoneNumber?.let { put("phone_number", it) } ?: putNull("phone_number")
            entry.direction?.let { put("direction", it) } ?: putNull("direction")
            entry.simSubscriptionId?.let { put("sim_subscription_id", it) } ?: putNull("sim_subscription_id")
            entry.displayName?.let { put("display_name", it) } ?: putNull("display_name")
            entry.note?.let { put("note", it) } ?: putNull("note")
            put("favorite", if (entry.favorite) 1 else 0)
            entry.trashedAtEpochMs?.let { put("trashed_at", it) } ?: putNull("trashed_at")
        }
        writableDatabase.insertWithOnConflict(
            "recordings",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listActive(): List<RecordingEntry> =
        query("trashed_at IS NULL")

    fun listTrash(): List<RecordingEntry> =
        query("trashed_at IS NOT NULL")

    fun searchActive(search: String): List<RecordingEntry> {
        val normalized = search.trim()
        if (normalized.isEmpty()) return listActive()
        val pattern = "%" + normalized + "%"
        return query(
            "trashed_at IS NULL AND (display_name LIKE ? OR phone_number LIKE ? OR note LIKE ? OR file_path LIKE ?)",
            arrayOf(pattern, pattern, pattern, pattern)
        )
    }

    fun updateFavorite(id: String, favorite: Boolean) {
        val values = ContentValues().apply { put("favorite", if (favorite) 1 else 0) }
        writableDatabase.update("recordings", values, "id = ?", arrayOf(id))
    }

    fun updateNote(id: String, note: String?) {
        val values = ContentValues().apply {
            if (note.isNullOrBlank()) putNull("note") else put("note", note.trim())
        }
        writableDatabase.update("recordings", values, "id = ?", arrayOf(id))
    }

    fun updateDuration(id: String, durationMs: Long) {
        val values = ContentValues().apply { put("duration_ms", durationMs) }
        writableDatabase.update("recordings", values, "id = ?", arrayOf(id))
    }

    fun markTrashed(id: String, atEpochMs: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply { put("trashed_at", atEpochMs) }
        writableDatabase.update("recordings", values, "id = ?", arrayOf(id))
    }

    fun restore(id: String) {
        writableDatabase.execSQL(
            "UPDATE recordings SET trashed_at = NULL WHERE id = ?",
            arrayOf(id)
        )
    }

    fun deletePermanently(id: String) {
        writableDatabase.delete("recordings", "id = ?", arrayOf(id))
    }

    private fun query(
        where: String,
        selectionArgs: Array<String>? = null
    ): List<RecordingEntry> {
        val result = mutableListOf<RecordingEntry>()
        readableDatabase.query(
            "recordings",
            null,
            where,
            selectionArgs,
            null,
            null,
            "favorite DESC, created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += cursor.toEntry()
            }
        }
        return result
    }

    private fun Cursor.toEntry(): RecordingEntry {
        val durationIndex = getColumnIndexOrThrow("duration_ms")
        val phoneIndex = getColumnIndexOrThrow("phone_number")
        val directionIndex = getColumnIndexOrThrow("direction")
        val simIndex = getColumnIndexOrThrow("sim_subscription_id")
        val nameIndex = getColumnIndexOrThrow("display_name")
        val noteIndex = getColumnIndexOrThrow("note")
        val trashIndex = getColumnIndexOrThrow("trashed_at")

        return RecordingEntry(
            id = getString(getColumnIndexOrThrow("id")),
            filePath = getString(getColumnIndexOrThrow("file_path")),
            createdAtEpochMs = getLong(getColumnIndexOrThrow("created_at")),
            durationMs = longOrNull(durationIndex),
            phoneNumber = stringOrNull(phoneIndex),
            direction = stringOrNull(directionIndex),
            simSubscriptionId = intOrNull(simIndex),
            displayName = stringOrNull(nameIndex),
            note = stringOrNull(noteIndex),
            favorite = getInt(getColumnIndexOrThrow("favorite")) == 1,
            trashedAtEpochMs = longOrNull(trashIndex)
        )
    }

    private fun Cursor.longOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun Cursor.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private fun Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    companion object {
        private const val DATABASE_NAME = "callvault.db"
        private const val DATABASE_VERSION = 1
    }
}
