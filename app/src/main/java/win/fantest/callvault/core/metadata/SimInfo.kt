package win.fantest.callvault.core.metadata

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val carrierName: String?,
    val displayName: String?,
    val countryIso: String?
)
