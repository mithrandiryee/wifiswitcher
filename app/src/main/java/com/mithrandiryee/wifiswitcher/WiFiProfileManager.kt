package com.mithrandiryee.wifiswitcher

import android.content.Context
import android.util.Log

/**
 * WiFi配置管理核心类
 * 整合ProfileStorageManager和IPConfigurator，提供统一的配置管理接口
 */
class WiFiProfileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WiFiProfileManager"
    }
    
    private val profileStorageManager = ProfileStorageManager(context)
    private val ipConfigurator = IPConfigurator(context)
    private val wifiController = WifiController(context)
    
    /**
     * 应用静态IP配置到当前连接的WiFi网络
     * @param profile IP配置文件
     * @return 是否成功应用配置
     */
    fun applyStaticIP(profile: IPProfile): Boolean {
        if (!profile.isValid()) {
            Log.e(TAG, "配置文件无效")
            return false
        }
        
        if (!wifiController.hasRequiredPermissions()) {
            Log.e(TAG, "缺少必要权限")
            return false
        }
        
        return try {
            val success = ipConfigurator.applyStaticIP(profile)
            if (success) {
                Log.d(TAG, "成功应用静态IP配置: ${profile.name}")
            } else {
                Log.e(TAG, "应用静态IP配置失败")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "应用静态IP配置异常", e)
            false
        }
    }
    
    /**
     * 应用DHCP配置到当前连接的WiFi网络
     * @param ssid WiFi SSID
     * @return 是否成功应用配置
     */
    fun applyDHCP(ssid: String): Boolean {
        if (ssid.isEmpty()) {
            Log.e(TAG, "SSID不能为空")
            return false
        }
        
        if (!wifiController.hasRequiredPermissions()) {
            Log.e(TAG, "缺少必要权限")
            return false
        }
        
        return try {
            val success = ipConfigurator.applyDHCP(ssid)
            if (success) {
                Log.d(TAG, "成功应用DHCP配置: $ssid")
            } else {
                Log.e(TAG, "应用DHCP配置失败")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "应用DHCP配置异常", e)
            false
        }
    }
    
    /**
     * 获取当前WiFi网络的IP配置
     * @param ssid WiFi SSID（当前实现中未使用，保留参数兼容性）
     * @return 当前IP配置，无法获取时返回null
     */
    fun getCurrentIPConfig(ssid: String): IPProfile? {
        if (!wifiController.hasRequiredPermissions()) {
            Log.e(TAG, "缺少必要权限")
            return null
        }
        
        return try {
            ipConfigurator.getCurrentIPConfiguration()
        } catch (e: Exception) {
            Log.e(TAG, "获取当前IP配置异常", e)
            null
        }
    }
    
    /**
     * 保存IP配置文件
     * @param profile 要保存的配置文件
     */
    fun saveProfile(profile: IPProfile) {
        try {
            profileStorageManager.addProfile(profile)
            Log.d(TAG, "保存配置文件: ${profile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "保存配置文件异常", e)
        }
    }
    
    /**
     * 删除IP配置文件
     * @param id 要删除的配置文件ID
     */
    fun deleteProfile(id: String) {
        try {
            profileStorageManager.deleteProfile(id)
            Log.d(TAG, "删除配置文件: $id")
        } catch (e: Exception) {
            Log.e(TAG, "删除配置文件异常", e)
        }
    }
    
    /**
     * 更新IP配置文件
     * @param profile 要更新的配置文件
     */
    fun updateProfile(profile: IPProfile) {
        try {
            profileStorageManager.updateProfile(profile)
            Log.d(TAG, "更新配置文件: ${profile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "更新配置文件异常", e)
        }
    }
    
    /**
     * 获取所有保存的配置文件
     * @return 配置文件列表
     */
    fun getAllProfiles(): List<IPProfile> {
        return try {
            profileStorageManager.loadProfiles()
        } catch (e: Exception) {
            Log.e(TAG, "获取配置文件列表异常", e)
            emptyList()
        }
    }
    
    /**
     * 根据ID获取配置文件
     * @param id 配置文件ID
     * @return 配置文件，不存在则返回null
     */
    fun getProfileById(id: String): IPProfile? {
        return try {
            profileStorageManager.getProfileById(id)
        } catch (e: Exception) {
            Log.e(TAG, "获取配置文件异常", e)
            null
        }
    }
    
    /**
     * 根据SSID获取配置文件
     * @param ssid WiFi SSID
     * @return 匹配的配置文件列表
     */
    fun getProfilesBySSID(ssid: String): List<IPProfile> {
        return try {
            profileStorageManager.getProfilesBySSID(ssid)
        } catch (e: Exception) {
            Log.e(TAG, "根据SSID获取配置文件异常", e)
            emptyList()
        }
    }
    
    /**
     * 检查WiFi是否已连接
     * @return 是否已连接到WiFi
     */
    fun isWiFiConnected(): Boolean {
        return try {
            wifiController.getConnectedSSID() != null
        } catch (e: Exception) {
            Log.e(TAG, "检查WiFi连接状态异常", e)
            false
        }
    }
    
    /**
     * 获取当前连接的WiFi SSID
     * @return SSID，无法获取时返回null
     */
    fun getCurrentSSID(): String? {
        return try {
            wifiController.getConnectedSSID()
        } catch (e: Exception) {
            Log.e(TAG, "获取当前SSID异常", e)
            null
        }
    }
    
    /**
     * 创建新的配置文件（基于当前配置）
     * @param name 配置名称
     * @return 新的配置文件，如果无法获取当前配置则返回null
     */
    fun createProfileFromCurrent(name: String): IPProfile? {
        val currentConfig = getCurrentIPConfig("") ?: return null
        val ssid = getCurrentSSID() ?: return null
        
        return IPProfile(
            id = System.currentTimeMillis().toString(),
            name = name,
            ssid = ssid,
            ipAddress = currentConfig.ipAddress,
            gateway = currentConfig.gateway,
            dns1 = currentConfig.dns1,
            dns2 = currentConfig.dns2,
            subnetMask = currentConfig.subnetMask,
            isDHCP = currentConfig.isDHCP
        )
    }
}