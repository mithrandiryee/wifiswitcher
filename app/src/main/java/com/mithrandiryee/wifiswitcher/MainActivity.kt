package com.mithrandiryee.wifiswitcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mithrandiryee.wifiswitcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),
    IPProfileAdapter.OnProfileClickListener,
    NetworkStateReceiver.NetworkStateListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: IPProfileAdapter
    private lateinit var wifiController: WifiController
    private lateinit var ipConfigurator: IPConfigurator
    private lateinit var storageManager: ProfileStorageManager
    private lateinit var networkReceiver: NetworkStateReceiver

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initManagers()
        setupRecyclerView()
        setupClickListeners()
        checkPermissions()
        updateCurrentStatus()
        loadProfiles()
    }

    override fun onResume() {
        super.onResume()
        networkReceiver.register(this)
        loadProfiles()
        updateCurrentStatus()
    }

    override fun onPause() {
        super.onPause()
        networkReceiver.unregister(this)
    }

    private fun initManagers() {
        wifiController = WifiController(this)
        ipConfigurator = IPConfigurator(this)
        storageManager = ProfileStorageManager(this)
        networkReceiver = NetworkStateReceiver(this)
    }

    private fun setupRecyclerView() {
        adapter = IPProfileAdapter(this)
        binding.rvIpProfiles.layoutManager = LinearLayoutManager(this)
        binding.rvIpProfiles.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabAddProfile.setOnClickListener {
            val intent = Intent(this, AddProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                updateCurrentStatus()
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要WiFi权限才能正常使用", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateCurrentStatus() {
        val ssid = wifiController.getConnectedSSID()
        val ipAddress = ipConfigurator.getCurrentIPConfiguration()?.ipAddress

        binding.tvWifiSsid.text = ssid ?: getString(R.string.no_connection)
        binding.tvIpAddress.text = ipAddress ?: getString(R.string.ip_not_available)
    }

    private fun loadProfiles() {
        val profiles = storageManager.loadProfiles()
        adapter.submitList(profiles)
    }

    override fun onProfileClick(profile: IPProfile) {
        switchToProfile(profile)
    }

    override fun onProfileLongClick(profile: IPProfile) {
        showPopupMenu(profile)
    }

    override fun onSwitchClick(profile: IPProfile) {
        switchToProfile(profile)
    }

    private fun showPopupMenu(profile: IPProfile) {
        val anchorView = binding.rvIpProfiles.findViewHolderForAdapterPosition(
            adapter.currentList.indexOf(profile)
        )?.itemView ?: binding.root

        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menu.add(0, 1, 0, "编辑")
        popupMenu.menu.add(0, 2, 1, "删除")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    editProfile(profile)
                    true
                }
                2 -> {
                    confirmDeleteProfile(profile)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun editProfile(profile: IPProfile) {
        val intent = Intent(this, AddProfileActivity::class.java)
        intent.putExtra(EXTRA_PROFILE_ID, profile.id)
        startActivity(intent)
    }

    private fun confirmDeleteProfile(profile: IPProfile) {
        AlertDialog.Builder(this)
            .setTitle("删除配置")
            .setMessage("确定要删除配置 \"${profile.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                storageManager.deleteProfile(profile.id)
                loadProfiles()
                Toast.makeText(this, "配置已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun switchToProfile(profile: IPProfile) {
        val currentSsid = wifiController.getConnectedSSID()

        if (currentSsid == null) {
            Toast.makeText(this, "请先连接WiFi", Toast.LENGTH_SHORT).show()
            return
        }

        if (profile.ssid != currentSsid) {
            AlertDialog.Builder(this)
                .setTitle("SSID不匹配")
                .setMessage("当前连接的WiFi ($currentSsid) 与配置的SSID (${profile.ssid}) 不同，是否继续？")
                .setPositiveButton("继续") { _, _ ->
                    applyProfile(profile)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            applyProfile(profile)
        }
    }

    private fun applyProfile(profile: IPProfile) {
        val success = if (profile.isDHCP) {
            ipConfigurator.applyDHCP(profile.ssid)
        } else {
            ipConfigurator.applyStaticIP(profile)
        }

        if (success) {
            Toast.makeText(this, "正在应用配置: ${profile.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "应用配置失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onWifiStateChanged(state: Int) {
        updateCurrentStatus()
    }

    override fun onNetworkConnected(ssid: String?, ipAddress: String?) {
        binding.tvWifiSsid.text = ssid ?: getString(R.string.no_connection)
        binding.tvIpAddress.text = ipAddress ?: getString(R.string.ip_not_available)
    }

    override fun onNetworkDisconnected() {
        binding.tvWifiSsid.text = getString(R.string.no_connection)
        binding.tvIpAddress.text = getString(R.string.ip_not_available)
    }

    override fun onIPChanged(newIP: String?) {
        binding.tvIpAddress.text = newIP ?: getString(R.string.ip_not_available)
    }

    override fun onSignalStrengthChanged(level: Int) {
    }
}
