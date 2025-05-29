package com.spidroid.starry.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.PostModel.LinkPreview;
import java.util.List;

public class LinkPreviewAdapter extends RecyclerView.Adapter<LinkPreviewAdapter.LinkPreviewViewHolder> {

    private final List<LinkPreview> linkPreviews;

    public LinkPreviewAdapter(List<LinkPreview> linkPreviews) {
        this.linkPreviews = linkPreviews;
    }

    @NonNull
    @Override
    public LinkPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_link_preview, parent, false);
        return new LinkPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LinkPreviewViewHolder holder, int position) {
        LinkPreview preview = linkPreviews.get(position);
        
        holder.tvTitle.setText(preview.getTitle());
        holder.tvDescription.setText(preview.getDescription());
        holder.tvDomain.setText(Uri.parse(preview.getUrl()).getHost());

        // Load preview image
        if (preview.getImageUrl() != null && !preview.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(preview.getImageUrl())
                    .placeholder(R.drawable.ic_cover_placeholder)
                    .into(holder.ivImage);
        }
    }

    @Override
    public int getItemCount() {
        return linkPreviews.size();
    }

    static class LinkPreviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvDomain;

        LinkPreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivLinkImage);
            tvTitle = itemView.findViewById(R.id.tvLinkTitle);
            tvDescription = itemView.findViewById(R.id.tvLinkDescription);
            tvDomain = itemView.findViewById(R.id.tvLinkDomain);
        }
    }
}