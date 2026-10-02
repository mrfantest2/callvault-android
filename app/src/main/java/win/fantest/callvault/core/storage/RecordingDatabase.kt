package win.fantest.callvault.core.storage

import android.content.ContentValues
import android.content.Context
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

    private fun query(where: String): List<RecordingEntry> {
        val result = mutableListOf<RecordingEntry>()
        readableDatabase.query(
            "recordings",
            null,
            where,
            null,
            null,
            null,
            "created_at DESC"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val pathIndex = cursor.getColumnIndexOrThrow("file_path")
            val createdIndex = cursor.getColumnIndexOrThrow("created_at")
            val durationIndex = cursor.getColumnIndexOrThrow("duration_ms")
            val phoneIndex = cursor.getColumnIndexOrThrow("phone_number")
            val directionIndex = cursor.getColumnIndexOrThrow("direction")
            val simIndex = cursor.getColumnIndexOrThrow("sim_subscription_id")
            val nameIndex = cursor.getColumnIndexOrThrow("display_name")
            val noteIndex = cursor.getColumnIndexOrThrow("note")
            val favoriteIndex = cursor.getColumnIndexOrThrow("favorite")
            val trashIndex = cursor.getColumnIndexOrThrow("trashed_at")

            while (cursor.moveToNext()) {
                result += RecordingEntry(
                    id = cursor.getString(idIndex),
                    filePath = cursor.getString(pathIndex),
                    createdAtEpochMs = cursor.getLong(createdIndex),
                    durationMs = cursor.longOrNull(durationIndex),
                    phoneNumber = cursor.stringOrNull(phoneIndex),
                    direction = cursor.stringOrNull(directionIndex),
                    simSubscriptionId = cursor.intOrNull(simIndex),
                    displayName = cursor.stringOrNull(nameIndex),
                    note = cursor.stringOrNull(noteIndex),
                    favorite = cursor.getInt(favoriteIndex) == 1,
                    trashedAtEpochMs = cursor.longOrNull(trashIndex)
                )
            }
        }
        return result
    }

    private fun android.database.Cursor.longOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun android.database.Cursor.intOrNull(index: Int): Int? =
        if (isNull(index)) null else getInt(index)

    private fun android.database.Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    companion object {
        private const val DATABASE_NAME = "callvault.db"
        private const val DATABASE_VERSION = 1
    }
}
