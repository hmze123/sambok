package com.spidroid.starry.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.spidroid.starry.R;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<String> results;

    public SearchAdapter(List<String> results) {
        this.results = results;
    }

    public void updateData(List<String> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                     .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        holder.title.setText(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        public SearchViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
        }
    }
}