package com.hsissa.zentra.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.hsissa.zentra.R
import com.hsissa.zentra.service.AppCategory

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    var category: AppCategory
)

class AppCategoryAdapter(
    private var apps: List<AppItem>,
    private val onCategoryClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
        val btnCategory: MaterialButton = view.findViewById(R.id.btnCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.appName
        holder.tvPackage.text = app.packageName
        holder.ivIcon.setImageDrawable(app.icon)
        holder.btnCategory.text = app.category.name
        holder.btnCategory.setOnClickListener { onCategoryClick(app) }
    }

    override fun getItemCount() = apps.size

    fun updateData(newApps: List<AppItem>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
