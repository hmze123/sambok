package com.spidroid.starry.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spidroid.starry.R

class RecentSearchAdapter(
    private val listener: OnHistoryInteractionListener
) : ListAdapter<String, RecentSearchAdapter.ViewHolder>(DIFF_CALLBACK) {

    interface OnHistoryInteractionListener {
        fun onTermClicked(term: String) // تم جعلها غير قابلة للـ null
        fun onRemoveClicked(term: String) // تم جعلها غير قابلة للـ null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val term = getItem(position)
        // لا داعي للتحقق من term != null لأن ListAdapter يضمن ذلك
        holder.bind(term, listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val searchTerm: TextView = itemView.findViewById(R.id.tv_search_term)
        private val removeButton: ImageButton = itemView.findViewById(R.id.btn_remove_term)

        fun bind(term: String, listener: OnHistoryInteractionListener) {
            searchTerm.text = term
            itemView.setOnClickListener {
                listener.onTermClicked(term)
            }
            removeButton.setOnClickListener {
                listener.onRemoveClicked(term)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}