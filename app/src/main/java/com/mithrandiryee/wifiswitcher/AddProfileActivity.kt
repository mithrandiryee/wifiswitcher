package com.mithrandiryee.wifiswitcher

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mithrandiryee.wifiswitcher.databinding.ActivityAddProfileBinding
import java.util.UUID

class AddProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProfileBinding
    private lateinit var wifiController: WifiController
    private lateinit var storageManager: ProfileStorageManager

    private var editingProfileId: String? = null
    private var isEditMode = false

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initManagers()
        setupToolbar()
        setupDhcpToggle()
        setupSsidDropdown()
        setupSaveButton()
        loadProfileIfEditing()
    }

    private fun initManagers() {
        wifiController = WifiController(this)
        storageManager = ProfileStorageManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        isEditMode = intent.hasExtra(EXTRA_PROFILE_ID)
        supportActionBar?.title = if (isEditMode) "编辑配置" else "添加配置"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupDhcpToggle() {
        binding.switchDhcp.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutStaticIp.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
    }

    private fun setupSsidDropdown() {
        val availableNetworks = wifiController.getAvailableNetworks()
        val currentSsid = wifiController.getConnectedSSID()

        val ssids = mutableListOf<String>()
        if (currentSsid != null) {
            ssids.add(currentSsid)
        }
        availableNetworks.forEach { ssid ->
            if (!ssids.contains(ssid)) {
                ssids.add(ssid)
            }
        }

        if (ssids.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ssids)
            binding.actvSsid.setAdapter(adapter)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileIfEditing() {
        editingProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        if (editingProfileId != null) {
            isEditMode = true
            val profile = storageManager.getProfileById(editingProfileId!!)
            profile?.let { populateForm(it) }
        }
    }

    private fun populateForm(profile: IPProfile) {
        binding.etProfileName.setText(profile.name)
        binding.actvSsid.setText(profile.ssid)
        binding.switchDhcp.isChecked = profile.isDHCP

        if (!profile.isDHCP) {
            binding.etIpAddress.setText(profile.ipAddress)
            binding.etSubnetMask.setText(profile.subnetMask)
            binding.etGateway.setText(profile.gateway)
            binding.etDns1.setText(profile.dns1)
            binding.etDns2.setText(profile.dns2)
        }
    }

    private fun saveProfile() {
        val name = binding.etProfileName.text.toString().trim()
        val ssid = binding.actvSsid.text.toString().trim()
        val isDHCP = binding.switchDhcp.isChecked

        if (!validateInputs(name, ssid, isDHCP)) {
            return
        }

        val profile = if (isDHCP) {
            IPProfile(
                id = editingProfileId ?: UUID.randomUUID().toString(),
                name = name,
                ssid = ssid,
                ipAddress = "",
                gateway = "",
                dns1 = "",
                dns2 = "",
                subnetMask = "255.255.255.0",
                isDHCP = true
            )
        } else {
            IPProfile(
                id = editingProfileId ?: UUID.randomUUID().toString(),
                name = name,
                ssid = ssid,
                ipAddress = binding.etIpAddress.text.toString().trim(),
                gateway = binding.etGateway.text.toString().trim(),
                dns1 = binding.etDns1.text.toString().trim(),
                dns2 = binding.etDns2.text.toString().trim(),
                subnetMask = binding.etSubnetMask.text.toString().trim().ifEmpty { "255.255.255.0" },
                isDHCP = false
            )
        }

        storageManager.addProfile(profile)

        val message = if (isEditMode) "配置已更新" else "配置已保存"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun validateInputs(name: String, ssid: String, isDHCP: Boolean): Boolean {
        if (name.isEmpty()) {
            binding.etProfileName.error = "请输入配置名称"
            return false
        }

        if (ssid.isEmpty()) {
            binding.actvSsid.error = "请输入或选择WiFi网络"
            return false
        }

        if (!isDHCP) {
            val ipAddress = binding.etIpAddress.text.toString().trim()
            val gateway = binding.etGateway.text.toString().trim()
            val dns1 = binding.etDns1.text.toString().trim()

            if (ipAddress.isEmpty()) {
                binding.etIpAddress.error = "请输入IP地址"
                return false
            }

            if (!isValidIPAddress(ipAddress)) {
                binding.etIpAddress.error = "IP地址格式无效"
                return false
            }

            if (gateway.isEmpty()) {
                binding.etGateway.error = "请输入网关地址"
                return false
            }

            if (!isValidIPAddress(gateway)) {
                binding.etGateway.error = "网关地址格式无效"
                return false
            }

            if (dns1.isEmpty()) {
                binding.etDns1.error = "请输入DNS服务器地址"
                return false
            }

            if (!isValidIPAddress(dns1)) {
                binding.etDns1.error = "DNS地址格式无效"
                return false
            }

            val dns2 = binding.etDns2.text.toString().trim()
            if (dns2.isNotEmpty() && !isValidIPAddress(dns2)) {
                binding.etDns2.error = "DNS地址格式无效"
                return false
            }

            val subnetMask = binding.etSubnetMask.text.toString().trim()
            if (subnetMask.isNotEmpty() && !isValidIPAddress(subnetMask)) {
                binding.etSubnetMask.error = "子网掩码格式无效"
                return false
            }
        }

        return true
    }

    private fun isValidIPAddress(ip: String): Boolean {
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
}
