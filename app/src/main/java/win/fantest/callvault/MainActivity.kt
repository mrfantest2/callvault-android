package win.fantest.callvault

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "CallVault"
            textSize = 30f
        })
        root.addView(TextView(this).apply {
            text = "CP01 foundation ready"
            textSize = 18f
        })
        setContentView(root)
    }
}
