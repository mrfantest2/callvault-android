package win.fantest.callvault.core.device

import android.os.Build

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int
) {
    val isValidatedSamsungS25Ultra: Boolean
        get() =
            manufacturer.equals("samsung", ignoreCase = true) &&
                model.equals("SM-S938B", ignoreCase = true) &&
                apiLevel == 36

    fun summary(): String =
        manufacturer + " " + model + " / API " + apiLevel

    companion object {
        fun current(): DeviceProfile =
            DeviceProfile(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                apiLevel = Build.VERSION.SDK_INT
            )
    }
}
