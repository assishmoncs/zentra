package com.hsissa.zentra.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.hsissa.zentra.R
import com.hsissa.zentra.service.AppCategory

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    var category: AppCategory
)

class AppCategoryAdapter(
    private var apps: List<AppItem>,
    private val onCategoryClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val btnCategory: MaterialButton = view.findViewById(R.id.btnCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.appName
        val drawable = app.icon ?: loadCachedIcon(holder.itemView.context.packageManager, app.packageName)
        holder.ivIcon.setImageDrawable(drawable)
        holder.btnCategory.text = app.category.displayName
        holder.btnCategory.setOnClickListener { onCategoryClick(app) }
    }

    override fun getItemCount() = apps.size

    fun updateData(newApps: List<AppItem>) {
        val oldApps = apps.map { it.copy() }
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldApps.size
            override fun getNewListSize(): Int = newApps.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldApps[oldItemPosition].packageName == newApps[newItemPosition].packageName
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldApps[oldItemPosition].category == newApps[newItemPosition].category &&
                    oldApps[oldItemPosition].appName == newApps[newItemPosition].appName
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        apps = newApps
        diffResult.dispatchUpdatesTo(this)
    }

    private fun loadCachedIcon(pm: PackageManager, packageName: String): Drawable? {
        iconCache.get(packageName)?.let { return it }
        return try {
            pm.getApplicationIcon(packageName).also { iconCache.put(packageName, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    companion object {
        private val iconCache = LruCache<String, Drawable>(64)
    }
}
