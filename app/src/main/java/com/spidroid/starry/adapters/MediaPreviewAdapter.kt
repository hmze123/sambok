package com.spidroid.starry.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.databinding.ItemMediaPreviewBinding

// Sealed class لتمثيل نوع عنصر الوسائط، سواء كان جديدًا (URI) أو موجودًا (URL)
sealed class MediaPreviewItem {
    abstract val id: String
    data class New(val uri: Uri) : MediaPreviewItem() {
        override val id: String get() = uri.toString()
    }
    data class Existing(val url: String) : MediaPreviewItem() {
        override val id: String get() = url
    }
}

class MediaPreviewAdapter(
    private val listener: MediaRemoveListener
) : ListAdapter<MediaPreviewItem, MediaPreviewAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    // واجهة للاستماع إلى حدث حذف عنصر
    interface MediaRemoveListener {
        fun onMediaRemoved(item: MediaPreviewItem, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class MediaViewHolder(private val binding: ItemMediaPreviewBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRemove.setOnClickListener {
                // التأكد من أن الموضع صالح قبل استخدامه
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onMediaRemoved(getItem(bindingAdapterPosition), bindingAdapterPosition)
                }
            }
        }

        fun bind(item: MediaPreviewItem) {
            val context: Context = itemView.context
            // استخدام Glide لتحميل الصورة سواء كانت من Uri أو String Url
            val loadable = when (item) {
                is MediaPreviewItem.New -> item.uri
                is MediaPreviewItem.Existing -> item.url
            }

            Glide.with(context)
                .load(loadable)
                .centerCrop()
                .into(binding.ivMedia)
        }
    }

    companion object {
        // DiffUtil Callback لتحسين أداء RecyclerView
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaPreviewItem>() {
            override fun areItemsTheSame(oldItem: MediaPreviewItem, newItem: MediaPreviewItem): Boolean {
                // استخدام ID فريد لكل عنصر للمقارنة
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MediaPreviewItem, newItem: MediaPreviewItem): Boolean {
                // بما أن العناصر غير قابلة للتغيير، فإنها تكون متطابقة إذا كانت IDs متطابقة
                return oldItem == newItem
            }
        }
    }
}
