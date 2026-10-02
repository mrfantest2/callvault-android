package win.fantest.callvault.core.metadata

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class ContactMatch(
    val displayName: String,
    val lookupKey: String?
)

class ContactResolver(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun resolve(phoneNumber: String): ContactMatch? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        return try {
            resolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.LOOKUP_KEY
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val name = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                )
                val lookupIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY)
                ContactMatch(
                    displayName = name,
                    lookupKey = if (cursor.isNull(lookupIndex)) null else cursor.getString(lookupIndex)
                )
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
