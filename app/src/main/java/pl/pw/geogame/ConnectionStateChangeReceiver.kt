package pl.pw.geogame

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log

class ConnectionStateChangeReceiver : BroadcastReceiver () {

    var isGpsEnabled = false
    var isBluetoothEnabled = false
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        when (intent.action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                Log.d("ConnectionState", "GPS status changed: ${if (isGpsEnabled) "Enabled" else "Disabled"}")
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                isBluetoothEnabled = bluetoothState == BluetoothAdapter.STATE_ON
                Log.d("ConnectionState", "Bluetooth enabled: ${if (isBluetoothEnabled) "Enabled" else "Disabled"}")
            }
        }
    }
}
