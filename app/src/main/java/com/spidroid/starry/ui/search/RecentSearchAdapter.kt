// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/search/RecentSearchAdapter.kt
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
        fun onTermClicked(term: String?)
        fun onRemoveClicked(term: String?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val term = getItem(position)
        holder.searchTerm.text = term
        holder.itemView.setOnClickListener { v: View? ->
            listener.onTermClicked(
                term
            )
        }
        holder.removeButton.setOnClickListener { v: View? ->
            listener.onRemoveClicked(
                term
            )
        }
    }

    // getItemCount لم يعد بحاجة إلى تجاوز صريح لأنه يتم توفيره بواسطة ListAdapter

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { // ✨ تم تغيير 'internal' إلى 'class' (الذي يعني ضمنًا 'public')
        var searchTerm: TextView
        var removeButton: ImageButton

        init {
            searchTerm = itemView.findViewById(R.id.tv_search_term)
            removeButton = itemView.findViewById(R.id.btn_remove_term)
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<String> =
            object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }
            }
    }
}