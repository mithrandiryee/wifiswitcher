package com.mithrandiryee.wifiswitcher

import android.os.Bundle
import android.view.View
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
        setupDhcpToggle()
        setupSaveButton()
        loadProfileIfEditing()
    }

    private fun initManagers() {
        wifiController = WifiController(this)
        storageManager = ProfileStorageManager(this)
    }

    private fun setupDhcpToggle() {
        binding.switchDhcp.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutStaticIp.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (validateAndSave()) {
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadProfileIfEditing() {
        editingProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        isEditMode = editingProfileId != null

        if (isEditMode) {
            val profiles = storageManager.loadProfiles()
            val profile = profiles.find { it.id == editingProfileId }
            profile?.let { fillForm(it) }
        }
    }

    private fun fillForm(profile: IPProfile) {
        binding.etProfileName.setText(profile.name)
        binding.etSsid.setText(profile.ssid)
        binding.switchDhcp.isChecked = profile.isDHCP

        if (!profile.isDHCP) {
            binding.etIpAddress.setText(profile.ipAddress)
            binding.etGateway.setText(profile.gateway)
            binding.etDns1.setText(profile.dns1)
            binding.etDns2.setText(profile.dns2)
        }
    }

    private fun validateAndSave(): Boolean {
        val name = binding.etProfileName.text.toString().trim()
        val ssid = binding.etSsid.text.toString().trim()
        val isDhcp = binding.switchDhcp.isChecked

        if (name.isEmpty()) {
            binding.etProfileName.error = "请输入配置名称"
            return false
        }

        if (ssid.isEmpty()) {
            binding.etSsid.error = "请输入WiFi名称"
            return false
        }

        val profile = if (isDhcp) {
            IPProfile(
                id = editingProfileId ?: UUID.randomUUID().toString(),
                name = name,
                ssid = ssid,
                ipAddress = "",
                gateway = "",
                dns1 = "",
                dns2 = "",
                isDHCP = true
            )
        } else {
            val ipAddress = binding.etIpAddress.text.toString().trim()
            val gateway = binding.etGateway.text.toString().trim()
            val dns1 = binding.etDns1.text.toString().trim()

            if (ipAddress.isEmpty()) {
                binding.etIpAddress.error = "请输入IP地址"
                return false
            }

            if (gateway.isEmpty()) {
                binding.etGateway.error = "请输入网关"
                return false
            }

            IPProfile(
                id = editingProfileId ?: UUID.randomUUID().toString(),
                name = name,
                ssid = ssid,
                ipAddress = ipAddress,
                gateway = gateway,
                dns1 = dns1,
                dns2 = binding.etDns2.text.toString().trim(),
                isDHCP = false
            )
        }

        val profiles = storageManager.loadProfiles().toMutableList()
        if (isEditMode) {
            val index = profiles.indexOfFirst { it.id == editingProfileId }
            if (index >= 0) profiles[index] = profile
        } else {
            profiles.add(profile)
        }
        storageManager.saveProfiles(profiles)

        return true
    }
}
