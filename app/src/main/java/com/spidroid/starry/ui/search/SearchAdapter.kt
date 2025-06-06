package com.spidroid.starry.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spidroid.starry.ui.search.SearchAdapter.SearchViewHolder

class SearchAdapter(private var results: MutableList<String?>) :
    RecyclerView.Adapter<SearchViewHolder?>() {
    fun updateData(newResults: MutableList<String?>) {
        this.results = newResults
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.title.setText(results.get(position))
    }

    override fun getItemCount(): Int {
        return results.size
    }

    internal class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView

        init {
            title = itemView.findViewById<TextView>(android.R.id.text1)
        }
    }
}