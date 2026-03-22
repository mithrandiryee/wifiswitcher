package com.mithrandiryee.wifiswitcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

class WifiController(private val context: Context) {

    companion object {
        private const val TAG = "WifiController"
    }

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val connectivityManager: ConnectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun enableWifi(): Boolean {
        if (!checkBasicPermissions()) {
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
        if (!checkBasicPermissions()) {
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
        Log.d(TAG, "getConnectedSSID called")
        
        return try {
            // 优先使用 WifiManager.connectionInfo (更可靠)
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            Log.d(TAG, "wifiInfo: $wifiInfo")
            
            if (wifiInfo != null) {
                val ssid = wifiInfo.ssid
                Log.d(TAG, "raw ssid: $ssid")
                
                if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x" && ssid.isNotEmpty()) {
                    val cleanSsid = ssid.trim('"')
                    Log.d(TAG, "clean ssid: $cleanSsid")
                    return cleanSsid
                }
            }
            
            // 备用: 使用 ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSSIDFromConnectivityManager()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SSID", e)
            null
        }
    }

    private fun getSSIDFromConnectivityManager(): String? {
        Log.d(TAG, "getSSIDFromConnectivityManager called")
        
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                val ssid = wifiInfo?.ssid
                if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
                    return ssid.trim('"')
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transportInfo", e)
            }
        }

        return null
    }

    fun getConnectedNetworkInfo(): ConnectedNetworkInfo? {
        Log.d(TAG, "getConnectedNetworkInfo called")
        
        return try {
            val ssid = getConnectedSSID()
            Log.d(TAG, "ssid: $ssid")
            
            val ipAddress = getIPAddress()
            Log.d(TAG, "ipAddress: $ipAddress")

            val isWifi = ssid != null || isWifiConnected()
            Log.d(TAG, "isWifi: $isWifi")

            ConnectedNetworkInfo(
                ssid = ssid,
                ipAddress = ipAddress,
                isConnected = isWifi
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network info", e)
            null
        }
    }

    private fun isWifiConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo?.type == android.net.ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getIPAddress(): String? {
        Log.d(TAG, "getIPAddress called")
        return try {
            // 方法1: 从 WifiManager 获取 (兼容性更好)
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            val ip = wifiInfo?.ipAddress
            Log.d(TAG, "WifiManager ip: $ip")
            
            if (ip != null && ip != 0) {
                val ipStr = String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
                Log.d(TAG, "IP from WifiManager: $ipStr")
                return ipStr
            }
            
            // 方法2: 从 ConnectivityManager 获取
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getIPAddressFromConnectivityManager()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            null
        }
    }

    private fun getIPAddressFromConnectivityManager(): String? {
        Log.d(TAG, "getIPAddressFromConnectivityManager called")
        
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                return address.hostAddress
            }
        }

        return null
    }

    fun getAvailableNetworks(): List<String> {
        if (!checkBasicPermissions()) {
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
        if (!checkBasicPermissions()) {
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
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $ssid")
            }
            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable: $ssid")
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback)
        return true
    }

    @Suppress("DEPRECATION")
    private fun connectWithWifiConfiguration(ssid: String, password: String?): Boolean {
        val wifiConfig = android.net.wifi.WifiConfiguration().apply {
            this.SSID = "\"$ssid\""
            if (password != null) {
                this.preSharedKey = "\"$password\""
                this.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
            } else {
                this.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
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
        return try {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo
        } catch (e: Exception) {
            null
        }
    }

    private fun checkBasicPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasRequiredPermissions(): Boolean {
        val wifiState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val location = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearby = ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            wifiState && (location || nearby)
        } else {
            wifiState
        }
    }

    data class ConnectedNetworkInfo(
        val ssid: String?,
        val ipAddress: String?,
        val isConnected: Boolean
    )
}
