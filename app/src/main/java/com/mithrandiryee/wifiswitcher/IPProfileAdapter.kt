package com.mithrandiryee.wifiswitcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mithrandiryee.wifiswitcher.databinding.ItemIpProfileBinding

class IPProfileAdapter(
    private val listener: OnProfileClickListener
) : ListAdapter<IPProfile, IPProfileAdapter.ViewHolder>(IPProfileDiffCallback()) {
    
    private var activeProfileId: String? = null
    private var currentSSID: String? = null
    
    interface OnProfileClickListener {
        fun onProfileClick(profile: IPProfile)
        fun onProfileLongClick(profile: IPProfile)
        fun onSwitchClick(profile: IPProfile)
    }
    
    fun setActiveProfile(id: String?, ssid: String?) {
        activeProfileId = id
        currentSSID = ssid
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIpProfileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemIpProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(profile: IPProfile) {
            binding.tvProfileName.text = profile.name
            binding.tvSsid.text = "SSID: ${profile.ssid}"
            
            if (profile.isDHCP) {
                binding.tvIpAddress.text = "IP: DHCP自动获取"
                binding.chipDhcpStatic.text = "DHCP"
                binding.chipDhcpStatic.setChipBackgroundColorResource(R.color.chip_dhcp)
                binding.chipDhcpStatic.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.chip_dhcp_text)
                )
            } else {
                binding.tvIpAddress.text = "IP: ${profile.ipAddress}"
                binding.chipDhcpStatic.text = "静态IP"
                binding.chipDhcpStatic.setChipBackgroundColorResource(R.color.chip_static)
                binding.chipDhcpStatic.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.chip_static_text)
                )
            }
            
            val isActive = profile.id == activeProfileId && profile.ssid == currentSSID
            binding.cardIpProfile.isChecked = isActive
            
            if (isActive) {
                binding.cardIpProfile.strokeColor = ContextCompat.getColor(
                    binding.root.context, R.color.active_profile_stroke
                )
                binding.cardIpProfile.strokeWidth = binding.root.context.resources
                    .getDimensionPixelSize(R.dimen.active_profile_stroke_width)
            } else {
                binding.cardIpProfile.strokeWidth = 0
            }
            
            binding.root.setOnClickListener {
                listener.onProfileClick(profile)
            }
            
            binding.root.setOnLongClickListener {
                listener.onProfileLongClick(profile)
                true
            }
            
            binding.btnSwitch.setOnClickListener {
                listener.onSwitchClick(profile)
            }
            
            binding.btnMore.setOnClickListener {
                listener.onProfileLongClick(profile)
            }
        }
    }
    
    class IPProfileDiffCallback : DiffUtil.ItemCallback<IPProfile>() {
        override fun areItemsTheSame(oldItem: IPProfile, newItem: IPProfile): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: IPProfile, newItem: IPProfile): Boolean {
            return oldItem == newItem
        }
    }
}
