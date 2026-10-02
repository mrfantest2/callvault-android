package win.fantest.callvault.core.metadata

import android.content.Context
import android.telephony.SubscriptionManager

class SimInventory(context: Context) {
    private val appContext = context.applicationContext
    private val subscriptionManager =
        appContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    fun activeSubscriptions(): List<SimInfo> {
        return try {
            subscriptionManager.activeSubscriptionInfoList.orEmpty()
                .map { info ->
                    SimInfo(
                        subscriptionId = info.subscriptionId,
                        slotIndex = info.simSlotIndex,
                        carrierName = info.carrierName?.toString(),
                        displayName = info.displayName?.toString(),
                        countryIso = info.countryIso
                    )
                }
                .sortedBy { it.slotIndex }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
