package com.mithrandiryee.wifiswitcher

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*

class TVMainActivity : FragmentActivity() {
    
    private lateinit var browseFragment: BrowseSupportFragment
    private lateinit var storageManager: ProfileStorageManager
    private lateinit var ipConfigurator: IPConfigurator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)
        
        storageManager = ProfileStorageManager(this)
        ipConfigurator = IPConfigurator(this)
        
        setupBrowseFragment()
    }
    
    private fun setupBrowseFragment() {
        browseFragment = TVBrowseFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.tv_container, browseFragment)
            .commit()
    }
}