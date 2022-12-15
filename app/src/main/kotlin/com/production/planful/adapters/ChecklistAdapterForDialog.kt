package com.production.planful.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.models.ChecklistItem

class ChecklistAdapterForDialog(private val items: ArrayList<ChecklistItem>, val rv: RecyclerView) :
    RecyclerView.Adapter<ChecklistAdapterForDialog.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvChecklistItemName: TextView = itemView.findViewById(R.id.tvChecklistItemName)
        var cbChecklistItemDone: CheckBox = itemView.findViewById(R.id.cbChecklistItemDone)
        var rootView: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist_for_dialog, parent, false)
        return ChecklistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]
        holder.tvChecklistItemName.text = item.name
        holder.cbChecklistItemDone.isChecked = item.checked

        holder.cbChecklistItemDone.setOnCheckedChangeListener { compoundButton, b ->
            item.checked = b
        }
    }

    override fun getItemCount() = items.size
}