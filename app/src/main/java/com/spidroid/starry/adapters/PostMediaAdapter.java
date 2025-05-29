package com.spidroid.starry.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.spidroid.starry.R;
import java.util.ArrayList;
import java.util.List;

public class PostMediaAdapter extends RecyclerView.Adapter<PostMediaAdapter.MediaViewHolder> {

  private List<String> imageUrls;
  private final ImageInteractionListener listener;

  public PostMediaAdapter(List<String> imageUrls, ImageInteractionListener listener) {
    this.imageUrls = new ArrayList<>(imageUrls);
    this.listener = listener;
  }

  public List<String> getImageUrls() {
    return new ArrayList<>(imageUrls);
  }

  @NonNull
  @Override
  public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_post_media, parent, false); // Keep original layout name
    return new MediaViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
    String imageUrl = imageUrls.get(position);

    Glide.with(holder.itemView)
        .load(imageUrl)
        .apply(new RequestOptions().centerCrop().placeholder(R.drawable.ic_cover_placeholder))
        .into(holder.ivMedia);

    holder.itemView.setOnClickListener(v -> listener.onImageClicked(imageUrl, position));
  }

  @Override
  public int getItemCount() {
    return imageUrls.size();
  }

  public void updateUrls(List<String> newUrls) {
    List<String> oldList = this.imageUrls;
    this.imageUrls = new ArrayList<>(newUrls);

    DiffUtil.DiffResult result =
        DiffUtil.calculateDiff(
            new DiffUtil.Callback() {
              @Override
              public int getOldListSize() {
                return oldList.size();
              }

              @Override
              public int getNewListSize() {
                return imageUrls.size();
              }

              @Override
              public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).equals(imageUrls.get(newPos));
              }

              @Override
              public boolean areContentsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).equals(imageUrls.get(newPos));
              }
            });
    result.dispatchUpdatesTo(this);
  }

  public void clear() {
    int size = imageUrls.size();
    imageUrls.clear();
    notifyItemRangeRemoved(0, size);
  }

  static class MediaViewHolder extends RecyclerView.ViewHolder {
    ImageView ivMedia;

    MediaViewHolder(@NonNull View itemView) {
      super(itemView);
      ivMedia = itemView.findViewById(R.id.ivMedia); // Keep original ID
    }
  }

  public interface ImageInteractionListener {
    void onImageClicked(String imageUrl, int position);
  }
}
