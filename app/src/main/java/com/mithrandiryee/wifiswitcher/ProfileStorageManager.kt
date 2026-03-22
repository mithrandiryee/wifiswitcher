package com.mithrandiryee.wifiswitcher

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * IP配置文件存储管理器
 * 使用SharedPreferences + Gson进行JSON序列化存储
 */
class ProfileStorageManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "wifi_ip_profiles"
        private const val KEY_PROFILES = "profiles"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * 保存所有配置文件
     * @param profiles 配置文件列表
     */
    fun saveProfiles(profiles: List<IPProfile>) {
        val json = gson.toJson(profiles)
        sharedPreferences.edit()
            .putString(KEY_PROFILES, json)
            .apply()
    }
    
    /**
     * 加载所有配置文件
     * @return 配置文件列表，如果没有则返回空列表
     */
    fun loadProfiles(): List<IPProfile> {
        val json = sharedPreferences.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<IPProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 添加新配置文件
     * @param profile 要添加的配置文件
     */
    fun addProfile(profile: IPProfile) {
        val profiles = loadProfiles().toMutableList()
        // 检查是否已存在相同ID的配置
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex != -1) {
            profiles[existingIndex] = profile
        } else {
            profiles.add(profile)
        }
        saveProfiles(profiles)
    }
    
    /**
     * 删除指定配置文件
     * @param id 要删除的配置文件ID
     */
    fun deleteProfile(id: String) {
        val profiles = loadProfiles().toMutableList()
        profiles.removeAll { it.id == id }
        saveProfiles(profiles)
    }
    
    /**
     * 更新配置文件
     * @param profile 要更新的配置文件（根据ID匹配）
     */
    fun updateProfile(profile: IPProfile) {
        val profiles = loadProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            profiles[index] = profile
            saveProfiles(profiles)
        }
    }
    
    /**
     * 根据ID获取配置文件
     * @param id 配置文件ID
     * @return 配置文件，不存在则返回null
     */
    fun getProfileById(id: String): IPProfile? {
        return loadProfiles().find { it.id == id }
    }
    
    /**
     * 根据SSID获取配置文件
     * @param ssid WiFi SSID
     * @return 匹配的配置文件列表
     */
    fun getProfilesBySSID(ssid: String): List<IPProfile> {
        return loadProfiles().filter { it.ssid == ssid }
    }
    
    /**
     * 清除所有存储的配置文件
     */
    fun clearAll() {
        sharedPreferences.edit()
            .remove(KEY_PROFILES)
            .apply()
    }
}