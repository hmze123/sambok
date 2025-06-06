package com.spidroid.starry.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spidroid.starry.R

class RecentSearchAdapter(
    private val searchTerms: MutableList<String?>,
    private val listener: OnHistoryInteractionListener
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder?>() {
    interface OnHistoryInteractionListener {
        fun onTermClicked(term: String?)
        fun onRemoveClicked(term: String?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val term = searchTerms.get(position)
        holder.searchTerm.setText(term)
        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onTermClicked(
                term
            )
        })
        holder.removeButton.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onRemoveClicked(
                term
            )
        })
    }

    override fun getItemCount(): Int {
        return searchTerms.size
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var searchTerm: TextView
        var removeButton: ImageButton

        init {
            searchTerm = itemView.findViewById<TextView>(R.id.tv_search_term)
            removeButton = itemView.findViewById<ImageButton>(R.id.btn_remove_term)
        }
    }
}