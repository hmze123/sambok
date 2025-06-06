package com.spidroid.starry.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.adapters.LinkPreviewAdapter.LinkPreviewViewHolder
import com.spidroid.starry.models.PostModel.LinkPreview

class LinkPreviewAdapter(private val linkPreviews: MutableList<LinkPreview>) :
    RecyclerView.Adapter<LinkPreviewViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkPreviewViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_link_preview, parent, false)
        return LinkPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkPreviewViewHolder, position: Int) {
        val preview = linkPreviews.get(position)

        holder.tvTitle.setText(preview.getTitle())
        holder.tvDescription.setText(preview.getDescription())
        holder.tvDomain.setText(Uri.parse(preview.getUrl()).getHost())

        // Load preview image
        if (preview.getImageUrl() != null && !preview.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(preview.getImageUrl())
                .placeholder(R.drawable.ic_cover_placeholder)
                .into(holder.ivImage)
        }
    }

    override fun getItemCount(): Int {
        return linkPreviews.size
    }

    internal class LinkPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivImage: ImageView
        var tvTitle: TextView
        var tvDescription: TextView
        var tvDomain: TextView

        init {
            ivImage = itemView.findViewById<ImageView>(R.id.ivLinkImage)
            tvTitle = itemView.findViewById<TextView>(R.id.tvLinkTitle)
            tvDescription = itemView.findViewById<TextView>(R.id.tvLinkDescription)
            tvDomain = itemView.findViewById<TextView>(R.id.tvLinkDomain)
        }
    }
}