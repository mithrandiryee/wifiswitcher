package com.mithrandiryee.wifiswitcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

class NetworkStateReceiver(private val listener: NetworkStateListener) : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }

    interface NetworkStateListener {
        fun onWifiStateChanged(state: Int)
        fun onNetworkConnected(ssid: String?, ipAddress: String?)
        fun onNetworkDisconnected()
        fun onIPChanged(newIP: String?)
        fun onSignalStrengthChanged(level: Int)
    }

    private var lastSSID: String? = null
    private var lastIP: String? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                handleWifiStateChanged(intent)
            }
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                handleNetworkStateChanged(context, intent)
            }
            WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION -> {
                handleSupplicantConnectionChanged(intent)
            }
            WifiManager.RSSI_CHANGED_ACTION -> {
                handleSignalStrengthChanged(context, intent)
            }
            android.net.ConnectivityManager.CONNECTIVITY_ACTION -> {
                handleConnectivityChanged(context, intent)
            }
        }
    }

    private fun handleWifiStateChanged(intent: Intent) {
        val wifiState = intent.getIntExtra(
            WifiManager.EXTRA_WIFI_STATE,
            WifiManager.WIFI_STATE_UNKNOWN
        )

        when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                Log.d(TAG, "WiFi enabled")
            }
            WifiManager.WIFI_STATE_DISABLED -> {
                Log.d(TAG, "WiFi disabled")
                lastSSID = null
                lastIP = null
                listener.onNetworkDisconnected()
            }
            WifiManager.WIFI_STATE_ENABLING -> {
                Log.d(TAG, "WiFi enabling")
            }
            WifiManager.WIFI_STATE_DISABLING -> {
                Log.d(TAG, "WiFi disabling")
            }
        }

        listener.onWifiStateChanged(wifiState)
    }

    @Suppress("DEPRECATION")
    private fun handleNetworkStateChanged(context: Context, intent: Intent) {
        val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
        }

        val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO, WifiInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO)
        }

        if (networkInfo != null) {
            when (networkInfo.state) {
                NetworkInfo.State.CONNECTED -> {
                    val ssid = wifiInfo?.ssid?.trim('"')
                    val ipAddress = getIPAddress(context)

                    if (ssid != lastSSID || ipAddress != lastIP) {
                        lastSSID = ssid
                        lastIP = ipAddress
                        listener.onNetworkConnected(ssid, ipAddress)
                        Log.d(TAG, "Network connected: SSID=$ssid, IP=$ipAddress")
                    }
                }
                NetworkInfo.State.DISCONNECTED -> {
                    lastSSID = null
                    lastIP = null
                    listener.onNetworkDisconnected()
                    Log.d(TAG, "Network disconnected")
                }
                NetworkInfo.State.CONNECTING -> {
                    Log.d(TAG, "Network connecting")
                }
                NetworkInfo.State.DISCONNECTING -> {
                    Log.d(TAG, "Network disconnecting")
                }
                else -> {
                    Log.d(TAG, "Network state: ${networkInfo.state}")
                }
            }
        }
    }

    private fun handleSupplicantConnectionChanged(intent: Intent) {
        val connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)
        Log.d(TAG, "Supplicant connected: $connected")

        if (!connected) {
            lastSSID = null
            lastIP = null
            listener.onNetworkDisconnected()
        }
    }

    private fun handleSignalStrengthChanged(context: Context, intent: Intent) {
        val newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -1)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val level = WifiManager.calculateSignalLevel(newRssi, 5)
        listener.onSignalStrengthChanged(level)
    }

    private fun handleConnectivityChanged(context: Context, intent: Intent) {
        val noConnectivity = intent.getBooleanExtra(
            android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY,
            false
        )

        if (noConnectivity) {
            listener.onNetworkDisconnected()
        } else {
            val currentIP = getIPAddress(context)
            if (currentIP != lastIP) {
                lastIP = currentIP
                listener.onIPChanged(currentIP)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getIPAddress(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo?.ipAddress ?: return null

            if (ipAddress == 0) return null

            val ip = (ipAddress and 0xFF).toString() + "." +
                    (ipAddress shr 8 and 0xFF).toString() + "." +
                    (ipAddress shr 16 and 0xFF).toString() + "." +
                    (ipAddress shr 24 and 0xFF).toString()

            if (ip == "0.0.0.0") null else ip
        } catch (e: Exception) {
            null
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }

        Log.d(TAG, "NetworkStateReceiver registered")
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Log.d(TAG, "NetworkStateReceiver unregistered")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered")
        }
    }

    fun getCurrentState(context: Context): NetworkState {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiEnabled = wifiManager.isWifiEnabled
        val ssid = wifiManager.connectionInfo?.ssid?.trim('"')?.takeIf { it != "<unknown ssid>" }
        val ipAddress = getIPAddress(context)
        val level = if (wifiManager.connectionInfo != null) {
            WifiManager.calculateSignalLevel(wifiManager.connectionInfo.rssi, 5)
        } else {
            0
        }

        return NetworkState(
            isWifiEnabled = isWifiEnabled,
            isConnected = ssid != null,
            ssid = ssid,
            ipAddress = ipAddress,
            signalLevel = level
        )
    }

    data class NetworkState(
        val isWifiEnabled: Boolean,
        val isConnected: Boolean,
        val ssid: String?,
        val ipAddress: String?,
        val signalLevel: Int
    )
}
