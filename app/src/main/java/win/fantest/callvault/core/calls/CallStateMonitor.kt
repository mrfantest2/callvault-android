package win.fantest.callvault.core.calls

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

class CallStateMonitor(
    context: Context,
    private val onStateChanged: (CallState) -> Unit
) {
    private val appContext = context.applicationContext
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var modernCallback: TelephonyCallback? = null
    private var legacyListener: PhoneStateListener? = null

    fun start() {
        stop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    dispatch(state)
                }
            }
            modernCallback = callback
            telephonyManager.registerTelephonyCallback(appContext.mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    dispatch(state)
                }
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
            modernCallback = null
        } else {
            legacyListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            legacyListener = null
        }
    }

    private fun dispatch(rawState: Int) {
        val state = when (rawState) {
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
            else -> CallState.IDLE
        }
        onStateChanged(state)
    }
}
