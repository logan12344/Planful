package com.production.planful.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.commons.extensions.applyColorFilter
import com.production.planful.commons.extensions.baseConfig
import com.production.planful.commons.extensions.getContrastColor
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.models.ChecklistItem
import kotlinx.android.synthetic.main.item_checklist.view.*

class ChecklistAdapter(
    private val items: ArrayList<ChecklistItem>) :
    RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val checkList = items[position]
        setupView(holder.itemView, checkList)
        if (position == 0) holder.itemView.checklist_item_delete.visibility = View.INVISIBLE
        else holder.itemView.checklist_item_delete.visibility = View.VISIBLE
    }

    private fun setupView(view: View, checkList: ChecklistItem) {
        view.apply {
            if (context.baseConfig.backgroundColor.getContrastColor() == Color.WHITE) {
                checklist_item.setTextColor(context.getColor(R.color.theme_dark_text_color))
            }
            checklist_item_delete.applyColorFilter(context.getProperTextColor())
            checklist_item.requestFocus()
            checklist_item.setText(checkList.name)

            checklist_item.setOnFocusChangeListener { v, hasFocus ->
                checkList.name = checklist_item.text.toString()
            }

            checklist_item_delete.setOnClickListener {
                items.remove(checkList)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount() = items.size
}