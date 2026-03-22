package com.mithrandiryee.wifiswitcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class WifiController(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun enableWifi(): Boolean {
        if (!hasRequiredPermissions()) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = android.content.Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = true
            true
        }
    }

    fun disableWifi(): Boolean {
        if (!hasRequiredPermissions()) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            false
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = false
            true
        }
    }

    fun getConnectedSSID(): String? {
        if (!hasRequiredPermissions()) {
            return null
        }
        return try {
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid
            if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
                ssid.trim('"')
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getAvailableNetworks(): List<String> {
        if (!hasRequiredPermissions()) {
            return emptyList()
        }
        return try {
            val scanResults = wifiManager.scanResults
            scanResults.map { it.SSID }.distinct().filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun connectToNetwork(ssid: String, password: String?): Boolean {
        if (!hasRequiredPermissions()) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectWithNetworkSuggestion(ssid, password)
            } else {
                connectWithWifiConfiguration(ssid, password)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun connectWithNetworkSuggestion(ssid: String, password: String?): Boolean {
        val specifierBuilder = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)

        if (password != null) {
            specifierBuilder.setWpa2Passphrase(password)
        }

        val specifier = specifierBuilder.build()
        val networkRequest = android.net.NetworkRequest.Builder()
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
            }
            override fun onUnavailable() {
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback)
        return true
    }

    @Suppress("DEPRECATION")
    private fun connectWithWifiConfiguration(ssid: String, password: String?): Boolean {
        val wifiConfig = WifiConfiguration().apply {
            this.SSID = "\"$ssid\""
            if (password != null) {
                this.preSharedKey = "\"$password\""
                this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            } else {
                this.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }

        val networkId = wifiManager.addNetwork(wifiConfig)
        if (networkId == -1) {
            return false
        }

        wifiManager.disconnect()
        val success = wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
        return success
    }

    fun getCurrentWifiInfo(): WifiInfo? {
        if (!hasRequiredPermissions()) {
            return null
        }
        return try {
            wifiManager.connectionInfo
        } catch (e: Exception) {
            null
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermission && ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            locationPermission
        }
    }
}
