package com.mithrandiryee.wifiswitcher

import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*

class TVBrowseFragment : BrowseSupportFragment(), OnItemViewClickedListener {
    
    private lateinit var storageManager: ProfileStorageManager
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageManager = ProfileStorageManager(requireContext())
        
        setupUIElements()
        loadRows()
        setupEventListeners()
    }
    
    private fun setupUIElements() {
        title = "WiFi IP切换器"
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = resources.getColor(R.color.primary, null)
    }
    
    private fun loadRows() {
        // WiFi状态行
        val wifiHeader = HeaderItem(0, "当前状态")
        val wifiAdapter = ArrayObjectAdapter(CardPresenter())
        // 添加状态卡片
        
        // IP配置列表行
        val profilesHeader = HeaderItem(1, "IP配置")
        val profilesAdapter = ArrayObjectAdapter(CardPresenter())
        
        val profiles = storageManager.loadProfiles()
        profiles.forEach { profile ->
            profilesAdapter.add(profile)
        }
        
        rowsAdapter.add(ListRow(wifiHeader, wifiAdapter))
        rowsAdapter.add(ListRow(profilesHeader, profilesAdapter))
        
        adapter = rowsAdapter
    }
    
    private fun setupEventListeners() {
        onItemViewClickedListener = this
    }
    
    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (item is IPProfile) {
            applyProfile(item)
        }
    }
    
    private fun applyProfile(profile: IPProfile) {
        val ipConfigurator = IPConfigurator(requireContext())
        val success = if (profile.isDHCP) {
            ipConfigurator.applyDHCP(profile.ssid)
        } else {
            ipConfigurator.applyStaticIP(profile)
        }
        
        Toast.makeText(
            requireContext(),
            if (success) "已切换到 ${profile.name}" else "切换失败",
            Toast.LENGTH_SHORT
        ).show()
    }
}