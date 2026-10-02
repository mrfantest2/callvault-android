package win.fantest.callvault.core.rules

import android.content.Context

class RecordingRulesRepository(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("recording_rules", Context.MODE_PRIVATE)

    fun load(): RecordingRules =
        RecordingRules(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            recordIncoming = prefs.getBoolean(KEY_INCOMING, true),
            recordOutgoingOrUnknown = prefs.getBoolean(KEY_OUTGOING, true),
            minimumDurationMs = prefs.getLong(KEY_MIN_DURATION, 0L)
        )

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun save(rules: RecordingRules) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, rules.enabled)
            .putBoolean(KEY_INCOMING, rules.recordIncoming)
            .putBoolean(KEY_OUTGOING, rules.recordOutgoingOrUnknown)
            .putLong(KEY_MIN_DURATION, rules.minimumDurationMs)
            .apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_INCOMING = "incoming"
        private const val KEY_OUTGOING = "outgoing"
        private const val KEY_MIN_DURATION = "min_duration"
    }
}
