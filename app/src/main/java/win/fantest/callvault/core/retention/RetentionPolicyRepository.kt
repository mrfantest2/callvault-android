package win.fantest.callvault.core.retention

import android.content.Context

class RetentionPolicyRepository(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("retention_policy", Context.MODE_PRIVATE)

    fun trashRetentionDays(): Int =
        prefs.getInt(KEY_TRASH_DAYS, DEFAULT_TRASH_DAYS).coerceIn(1, 3650)

    fun setTrashRetentionDays(days: Int) {
        prefs.edit()
            .putInt(KEY_TRASH_DAYS, days.coerceIn(1, 3650))
            .apply()
    }

    companion object {
        private const val KEY_TRASH_DAYS = "trash_days"
        private const val DEFAULT_TRASH_DAYS = 30
    }
}
