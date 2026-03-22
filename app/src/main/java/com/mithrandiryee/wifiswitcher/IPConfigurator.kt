package com.mithrandiryee.wifiswitcher

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.UnknownHostException

class IPConfigurator(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun applyStaticIP(profile: IPProfile): Boolean {
        if (!validateIPAddress(profile.ipAddress)) return false
        if (!validateGateway(profile.gateway)) return false

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applyStaticIPAndroid12Plus(profile)
            } else {
                applyStaticIPOlder(profile)
            }
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun applyStaticIPOlder(profile: IPProfile): Boolean {
        val currentNetworkId = wifiManager.connectionInfo.networkId
        if (currentNetworkId == -1) return false

        val configurations = wifiManager.configuredNetworks
        val currentConfig = configurations?.find { it.networkId == currentNetworkId } ?: return false

        try {
            val ipInt = InetAddressStringToInt(profile.ipAddress)
            val gatewayInt = InetAddressStringToInt(profile.gateway)
            val dns1Int = InetAddressStringToInt(profile.dns1)
            val dns2Int = InetAddressStringToInt(profile.dns2)
            val netmaskInt = InetAddressStringToInt(profile.subnetMask)

            val setIpConfigurationMethod = WifiConfiguration::class.java.getMethod(
                "setIpAssignment",
                String::class.java
            )
            setIpConfigurationMethod.invoke(currentConfig, "STATIC")

            currentConfig.ipAddress = android.net.LinkAddress(
                InetAddress.getByAddress(ipInt),
                calculatePrefixLength(profile.subnetMask)
            )
            currentConfig.gateway = InetAddress.getByAddress(gatewayInt)
            currentConfig.dnsServers.clear()
            currentConfig.dnsServers.add(InetAddress.getByAddress(dns1Int))
            currentConfig.dnsServers.add(InetAddress.getByAddress(dns2Int))
            currentConfig.networkPrefixLength = calculatePrefixLength(profile.subnetMask)

            wifiManager.updateNetwork(currentConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(currentNetworkId, true)
            wifiManager.reconnect()

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun applyStaticIPAndroid12Plus(profile: IPProfile): Boolean {
        return try {
            val staticConfig = android.net.StaticIpConfiguration.Builder()
                .setIpAddress(
                    android.net.LinkAddress(
                        InetAddress.getByName(profile.ipAddress),
                        calculatePrefixLength(profile.subnetMask)
                    )
                )
                .setGateway(InetAddress.getByName(profile.gateway))
                .addDnsServer(InetAddress.getByName(profile.dns1))
                .addDnsServer(InetAddress.getByName(profile.dns2))
                .build()

            val currentNetworkId = wifiManager.connectionInfo.networkId
            val configurations = wifiManager.configuredNetworks
            val currentConfig = configurations?.find { it.networkId == currentNetworkId } ?: return false

            val ipConfiguration = android.net.IpConfiguration.Builder()
                .setStaticIpConfiguration(staticConfig)
                .setIpAssignment(android.net.IpConfiguration.IpAssignment.STATIC)
                .build()

            currentConfig.setIpConfiguration(ipConfiguration)
            wifiManager.updateNetwork(currentConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(currentNetworkId, true)
            wifiManager.reconnect()

            true
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun applyDHCP(ssid: String): Boolean {
        return try {
            val currentNetworkId = wifiManager.connectionInfo.networkId
            if (currentNetworkId == -1) return false

            val configurations = wifiManager.configuredNetworks
            val currentConfig = configurations?.find { it.networkId == currentNetworkId } ?: return false

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val ipConfiguration = android.net.IpConfiguration.Builder()
                    .setIpAssignment(android.net.IpConfiguration.IpAssignment.DHCP)
                    .build()
                currentConfig.setIpConfiguration(ipConfiguration)
            } else {
                val setIpConfigurationMethod = WifiConfiguration::class.java.getMethod(
                    "setIpAssignment",
                    String::class.java
                )
                setIpConfigurationMethod.invoke(currentConfig, "DHCP")
            }

            wifiManager.updateNetwork(currentConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(currentNetworkId, true)
            wifiManager.reconnect()

            true
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun getCurrentIPConfiguration(): IPProfile? {
        return try {
            val wifiInfo = wifiManager.connectionInfo ?: return null
            val ssid = wifiInfo.ssid?.trim('"') ?: return null

            val dhcpInfo: DhcpInfo = wifiManager.dhcpInfo ?: return null

            val ipAddress = IntToInetAddressString(dhcpInfo.ipAddress)
            val gateway = IntToInetAddressString(dhcpInfo.gateway)
            val dns1 = IntToInetAddressString(dhcpInfo.dns1)
            val dns2 = IntToInetAddressString(dhcpInfo.dns2)
            val netmask = IntToInetAddressString(dhcpInfo.netmask)

            val currentNetworkId = wifiInfo.networkId
            val configurations = wifiManager.configuredNetworks
            val currentConfig = configurations?.find { it.networkId == currentNetworkId }

            val isDHCP = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                currentConfig?.ipConfiguration?.ipAssignment == android.net.IpConfiguration.IpAssignment.DHCP
            } else {
                true
            }

            IPProfile(
                id = "current",
                name = "Current Configuration",
                ssid = ssid,
                ipAddress = ipAddress,
                gateway = gateway,
                dns1 = dns1,
                dns2 = dns2,
                subnetMask = netmask,
                isDHCP = isDHCP
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getSubnetMask(prefixLength: Int): String {
        if (prefixLength < 0 || prefixLength > 32) return "255.255.255.0"
        val mask = (0xFFFFFFFF.toInt() shl (32 - prefixLength)) and 0xFFFFFFFF.toInt()
        return IntToInetAddressString(mask)
    }

    fun validateIPAddress(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false

        for (part in parts) {
            try {
                val num = part.toInt()
                if (num < 0 || num > 255) return false
            } catch (e: NumberFormatException) {
                return false
            }
        }
        return true
    }

    fun validateGateway(gateway: String): Boolean {
        if (!validateIPAddress(gateway)) return false

        val parts = gateway.split(".").map { it.toInt() }
        if (parts[0] == 0 && parts[1] == 0 && parts[2] == 0 && parts[3] == 0) return false
        if (parts[0] == 255 && parts[1] == 255 && parts[2] == 255 && parts[3] == 255) return false

        return true
    }

    private fun InetAddressStringToInt(address: String): ByteArray {
        return try {
            InetAddress.getByName(address).address
        } catch (e: UnknownHostException) {
            byteArrayOf(0, 0, 0, 0)
        }
    }

    private fun IntToInetAddressString(address: Int): String {
        return "${address and 0xFF}." +
               "${(address shr 8) and 0xFF}." +
               "${(address shr 16) and 0xFF}." +
               "${(address shr 24) and 0xFF}"
    }

    private fun calculatePrefixLength(subnetMask: String): Int {
        if (!validateIPAddress(subnetMask)) return 24

        val parts = subnetMask.split(".").map { it.toInt() }
        var prefixLength = 0

        for (part in parts) {
            var mask = part
            while (mask != 0) {
                if (mask and 1 == 1) prefixLength++
                mask = mask shr 1
            }
        }

        return prefixLength
    }
}
