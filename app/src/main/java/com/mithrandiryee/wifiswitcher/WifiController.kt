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
            Log.w(TAG, "No required permissions")
            return null
        }

        return try {
            // 方法1: 使用 ConnectivityManager (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSSIDFromConnectivityManager()
            } else {
                // 方法2: 使用 WifiManager.connectionInfo (Android 10以下)
                getSSIDFromWifiManager()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SSID", e)
            null
        }
    }

    private fun getSSIDFromConnectivityManager(): String? {
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        // 检查是否是WiFi连接
        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        // Android 13+ 可以从 WifiInfo 获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
            val ssid = wifiInfo?.ssid
            if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
                return ssid.trim('"')
            }
        }

        // 备用方法: 尝试从 WifiManager 获取
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo?.ssid
        if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
            return ssid.trim('"')
        }

        return null
    }

    @Suppress("DEPRECATION")
    private fun getSSIDFromWifiManager(): String? {
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ssid = wifiInfo.ssid

        return if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
            ssid.trim('"')
        } else {
            null
        }
    }

    fun getConnectedNetworkInfo(): ConnectedNetworkInfo? {
        if (!hasRequiredPermissions()) {
            return null
        }

        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

            // 检查是否是WiFi连接
            val isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (!isWifi) {
                return ConnectedNetworkInfo(
                    ssid = null,
                    ipAddress = null,
                    isConnected = false
                )
            }

            // 获取SSID
            val ssid = getConnectedSSID()

            // 获取IP地址
            val ipAddress = getIPAddress()

            ConnectedNetworkInfo(
                ssid = ssid,
                ipAddress = ipAddress,
                isConnected = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network info", e)
            null
        }
    }

    fun getIPAddress(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getIPAddressFromConnectivityManager()
            } else {
                getIPAddressFromWifiManager()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            null
        }
    }

    private fun getIPAddressFromConnectivityManager(): String? {
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null

        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            // 只返回IPv4地址
            if (address is java.net.Inet4Address) {
                return address.hostAddress
            }
        }

        // 如果没有IPv4,返回IPv6
        for (linkAddress in linkProperties.linkAddresses) {
            val address = linkAddress.address
            if (address is java.net.Inet6Address) {
                return address.hostAddress
            }
        }

        return null
    }

    @Suppress("DEPRECATION")
    private fun getIPAddressFromWifiManager(): String? {
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ip = wifiInfo.ipAddress

        if (ip == 0) return null

        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
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
        if (!hasRequiredPermissions()) {
            return null
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                capabilities?.transportInfo as? WifiInfo
            } else {
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val wifiPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermission && wifiPermission && ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            locationPermission && wifiPermission
        }
    }

    data class ConnectedNetworkInfo(
        val ssid: String?,
        val ipAddress: String?,
        val isConnected: Boolean
    )
}
