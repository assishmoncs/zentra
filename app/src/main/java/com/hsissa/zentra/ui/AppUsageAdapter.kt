package com.hsissa.zentra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hsissa.zentra.R
import com.hsissa.zentra.service.AppUsageInfo

class AppUsageAdapter(private var items: List<AppUsageInfo>) :
    RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvUsage: TextView = view.findViewById(R.id.tvAppUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.appName
        holder.tvUsage.text = item.formattedTime
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<AppUsageInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}
