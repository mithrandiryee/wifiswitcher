package com.mithrandiryee.wifiswitcher

import java.util.regex.Pattern

/**
 * IP配置文件数据模型
 * 存储WiFi网络的静态IP配置信息
 */
data class IPProfile(
    val id: String,
    val name: String,
    val ssid: String,
    val ipAddress: String,
    val gateway: String,
    val dns1: String,
    val dns2: String,
    val subnetMask: String = "255.255.255.0",
    val isDHCP: Boolean = false
) {
    companion object {
        private val IP_ADDRESS_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
    }
    
    /**
     * 验证IP地址格式是否有效
     */
    fun isValidIPAddress(): Boolean {
        return ipAddress.isEmpty() || IP_ADDRESS_PATTERN.matcher(ipAddress).matches()
    }
    
    /**
     * 验证网关地址格式是否有效
     */
    fun isValidGateway(): Boolean {
        return gateway.isEmpty() || IP_ADDRESS_PATTERN.matcher(gateway).matches()
    }
    
    /**
     * 验证DNS地址格式是否有效
     */
    fun isValidDNS(): Boolean {
        return (dns1.isEmpty() || IP_ADDRESS_PATTERN.matcher(dns1).matches()) &&
               (dns2.isEmpty() || IP_ADDRESS_PATTERN.matcher(dns2).matches())
    }
    
    /**
     * 验证子网掩码格式是否有效
     */
    fun isValidSubnetMask(): Boolean {
        return IP_ADDRESS_PATTERN.matcher(subnetMask).matches()
    }
    
    /**
     * 验证整个配置是否有效
     */
    fun isValid(): Boolean {
        return if (isDHCP) {
            ssid.isNotEmpty()
        } else {
            ssid.isNotEmpty() &&
            isValidIPAddress() &&
            isValidGateway() &&
            isValidDNS() &&
            isValidSubnetMask()
        }
    }
    
    /**
     * 创建DHCP配置
     */
    fun toDHCP(): IPProfile {
        return copy(
            isDHCP = true,
            ipAddress = "",
            gateway = "",
            dns1 = "",
            dns2 = ""
        )
    }
}
