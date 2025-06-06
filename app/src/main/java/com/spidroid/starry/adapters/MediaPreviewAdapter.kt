package com.spidroid.starry.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R

class MediaPreviewAdapter(
    private val mediaUris: MutableList<Uri?>,
    private val existingMediaUrls: MutableList<String?>,
    private val listener: MediaRemoveListener
) : RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder?>() {
    interface MediaRemoveListener {
        fun onMediaRemoved(position: Int)
    }

    override fun getItemCount(): Int {
        return mediaUris.size + existingMediaUrls.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view =
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media_preview, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        if (position < existingMediaUrls.size) {
            // Load existing media URL
            Glide.with(holder.itemView).load(existingMediaUrls.get(position)).into(holder.ivMedia)
        } else {
            // Load new media URI
            val uriPosition = position - existingMediaUrls.size
            Glide.with(holder.itemView).load(mediaUris.get(uriPosition)).into(holder.ivMedia)
        }

        holder.btnRemove.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onMediaRemoved(
                position
            )
        })
    }

    internal class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivMedia: ImageView
        var btnRemove: ImageButton

        init {
            ivMedia = itemView.findViewById<ImageView>(R.id.ivMedia)
            btnRemove = itemView.findViewById<ImageButton>(R.id.btnRemove)
        }
    }
}
