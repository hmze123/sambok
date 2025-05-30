package com.spidroid.starry.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.spidroid.starry.R;
import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    private List<String> searchTerms;
    private final OnHistoryInteractionListener listener;

    public interface OnHistoryInteractionListener {
        void onTermClicked(String term);
        void onRemoveClicked(String term);
    }

    public RecentSearchAdapter(List<String> terms, OnHistoryInteractionListener listener) {
        this.searchTerms = terms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String term = searchTerms.get(position);
        holder.searchTerm.setText(term);
        holder.itemView.setOnClickListener(v -> listener.onTermClicked(term));
        holder.removeButton.setOnClickListener(v -> listener.onRemoveClicked(term));
    }

    @Override
    public int getItemCount() {
        return searchTerms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView searchTerm;
        ImageButton removeButton;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            searchTerm = itemView.findViewById(R.id.tv_search_term);
            removeButton = itemView.findViewById(R.id.btn_remove_term);
        }
    }
}