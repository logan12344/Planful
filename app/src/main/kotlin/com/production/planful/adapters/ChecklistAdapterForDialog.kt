package com.production.planful.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.commons.extensions.baseConfig
import com.production.planful.commons.extensions.getContrastColor
import com.production.planful.models.ChecklistItem

class ChecklistAdapterForDialog(
    private val context: Context,
    private val items: ArrayList<ChecklistItem>,
    val rv: RecyclerView) :
    RecyclerView.Adapter<ChecklistAdapterForDialog.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvChecklistItemName: TextView = itemView.findViewById(R.id.tvChecklistItemName)
        var cbChecklistItemDone: CheckBox = itemView.findViewById(R.id.cbChecklistItemDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist_for_dialog, parent, false)
        return ChecklistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]

        if (context.baseConfig.backgroundColor.getContrastColor() == Color.WHITE) {
            holder.tvChecklistItemName.setTextColor(context.getColor(R.color.theme_dark_text_color))
        }

        holder.tvChecklistItemName.text = item.name
        holder.cbChecklistItemDone.isChecked = item.checked

        holder.cbChecklistItemDone.setOnCheckedChangeListener { compoundButton, b ->
            item.checked = b
        }
    }

    override fun getItemCount() = items.size
}