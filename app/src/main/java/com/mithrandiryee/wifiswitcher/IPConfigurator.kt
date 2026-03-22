package com.mithrandiryee.wifiswitcher

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

class IPConfigurator(private val context: Context) {
    
    companion object {
        private const val TAG = "IPConfigurator"
    }
    
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    fun applyStaticIP(profile: IPProfile): Boolean {
        return try {
            Log.d(TAG, "Static IP config: ${profile.ipAddress}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            false
        }
    }
    
    fun applyDHCP(ssid: String): Boolean {
        return try {
            Log.d(TAG, "DHCP for: $ssid")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getCurrentIPConfiguration(): IPProfile? {
        return try {
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip != 0) {
                val ipStr = String.format("%d.%d.%d.%d",
                    ip and 0xff, ip shr 8 and 0xff,
                    ip shr 16 and 0xff, ip shr 24 and 0xff)
                IPProfile("current", "当前", "", ipStr, "", "", "", isDHCP = true)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    fun validateIPAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            parts.size == 4 && parts.all { it.toInt() in 0..255 }
        } catch (e: Exception) {
            false
        }
    }
    
    fun validateGateway(gateway: String) = validateIPAddress(gateway)
}
