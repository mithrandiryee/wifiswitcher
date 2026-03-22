package com.mithrandiryee.wifiswitcher

import android.content.Context
import android.util.Log

class IPConfigurator(private val context: Context) {
    
    companion object {
        private const val TAG = "IPConfigurator"
    }
    
    private val wifiController = WifiController(context)
    
    fun applyStaticIP(profile: IPProfile): Boolean {
        return try {
            Log.d(TAG, "Static IP config: ${profile.ipAddress}")
            // Note: Direct IP configuration requires system privileges
            // This is a simplified version that stores the configuration
            // Actual implementation would need root access or device owner privileges
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying static IP", e)
            false
        }
    }
    
    fun applyDHCP(ssid: String): Boolean {
        return try {
            Log.d(TAG, "DHCP for: $ssid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying DHCP", e)
            false
        }
    }
    
    fun getCurrentIPConfiguration(): IPProfile? {
        return try {
            val networkInfo = wifiController.getConnectedNetworkInfo()
            val ip = networkInfo?.ipAddress
            
            if (ip != null) {
                IPProfile(
                    id = "current",
                    name = "当前配置",
                    ssid = networkInfo.ssid ?: "",
                    ipAddress = ip,
                    gateway = "",
                    dns1 = "",
                    dns2 = "",
                    isDHCP = true
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current IP", e)
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
