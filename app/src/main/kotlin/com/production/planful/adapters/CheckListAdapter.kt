package com.production.planful.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.production.planful.R
import com.production.planful.commons.extensions.applyColorFilter
import com.production.planful.commons.extensions.getProperPrimaryColor
import com.production.planful.models.ChecklistItem
import kotlinx.android.synthetic.main.activity_event.*

class ChecklistAdapter(val context: Context, val rv: RecyclerView, private val items: ArrayList<ChecklistItem>) :
    RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    inner class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var checklistText: TextInputLayout = itemView.findViewById(R.id.checklist_item_layout)
        var checklistDelete: ImageView = itemView.findViewById(R.id.checklist_item_delete)
        var rootView: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = items[position]

        holder.checklistDelete.applyColorFilter(context.getProperPrimaryColor())
        
        holder.checklistText.editText?.setText(item.name)
        holder.checklistText.editText?.doAfterTextChanged {
            items[position].name = it.toString()
        }

        if (position == 0) {
            holder.checklistDelete.visibility = View.INVISIBLE
        } else {
            holder.checklistDelete.visibility = View.VISIBLE
        }

        if (position == (itemCount - 1)) {
            holder.checklistText.editText?.requestFocus()
        }

        holder.checklistDelete.setOnClickListener {
            rv.post {
                items.removeAt(position)
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount() = items.size
}