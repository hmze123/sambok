package com.spidroid.starry.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.spidroid.starry.R;

import java.util.List;

public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {
  private List<Uri> mediaUris;
  private List<String> existingMediaUrls;
  private MediaRemoveListener listener;

  public interface MediaRemoveListener {
    void onMediaRemoved(int position);
  }

  public MediaPreviewAdapter(
      List<Uri> mediaUris, List<String> existingMediaUrls, MediaRemoveListener listener) {
    this.mediaUris = mediaUris;
    this.existingMediaUrls = existingMediaUrls;
    this.listener = listener;
  }

  @Override
  public int getItemCount() {
    return mediaUris.size() + existingMediaUrls.size();
  }

  @NonNull
  @Override
  public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_media_preview, parent, false);
    return new MediaViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
    if (position < existingMediaUrls.size()) {
      // Load existing media URL
      Glide.with(holder.itemView).load(existingMediaUrls.get(position)).into(holder.ivMedia);
    } else {
      // Load new media URI
      int uriPosition = position - existingMediaUrls.size();
      Glide.with(holder.itemView).load(mediaUris.get(uriPosition)).into(holder.ivMedia);
    }

    holder.btnRemove.setOnClickListener(v -> listener.onMediaRemoved(position));
  }

  static class MediaViewHolder extends RecyclerView.ViewHolder {
    ImageView ivMedia;
    ImageButton btnRemove;

    MediaViewHolder(@NonNull View itemView) {
      super(itemView);
      ivMedia = itemView.findViewById(R.id.ivMedia);
      btnRemove = itemView.findViewById(R.id.btnRemove);
    }
  }
}
