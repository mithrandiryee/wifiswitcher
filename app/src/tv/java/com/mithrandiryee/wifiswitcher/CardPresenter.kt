package com.mithrandiryee.wifiswitcher

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class CardPresenter : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }
    
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        
        when (item) {
            is IPProfile -> {
                cardView.titleText = item.name
                cardView.contentText = "${item.ssid}\n${item.ipAddress}"
                cardView.setMainImageDimensions(313, 176)
            }
        }
    }
    
    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // 清理资源
    }
}
