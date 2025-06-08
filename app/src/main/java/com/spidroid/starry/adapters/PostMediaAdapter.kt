package com.spidroid.starry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostMediaBinding
import com.spidroid.starry.models.PostModel

class PostMediaAdapter(
    private val listener: PostInteractionListener
) : ListAdapter<String, PostMediaAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemPostMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaUrl = getItem(position)
        if (mediaUrl != null) {
            holder.bind(mediaUrl)
        }
    }

    inner class MediaViewHolder(private val binding: ItemPostMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mediaUrl: String) {
            val isVideo = isVideoUrl(mediaUrl)
            binding.ivPlayButton.visibility = if (isVideo) View.VISIBLE else View.GONE
            binding.videoOverlay.visibility = if (isVideo) View.VISIBLE else View.GONE
            ViewCompat.setTransitionName(binding.ivMedia, mediaUrl)

            Glide.with(itemView.context)
                .load(mediaUrl)
                .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_cover_placeholder))
                .into(binding.ivMedia)

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    if (isVideo) {
                        listener.onVideoPlayClicked(mediaUrl)
                    } else {
                        listener.onMediaClicked(currentList.toMutableList(), bindingAdapterPosition, binding.ivMedia)
                    }
                }
            }
        }
        private fun isVideoUrl(url: String): Boolean {
            return PostModel.VIDEO_EXTENSIONS.any { url.lowercase().endsWith(".$it") }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        }
    }
}