// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/adapters/LinkPreviewAdapter.kt
package com.spidroid.starry.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemLinkPreviewBinding
import com.spidroid.starry.models.PostModel.LinkPreview

class LinkPreviewAdapter(
    private val listener: OnLinkPreviewListener?
) : ListAdapter<LinkPreview, LinkPreviewAdapter.LinkPreviewViewHolder>(DIFF_CALLBACK) {

    // واجهة للتفاعل مع النقرات على المعاينة أو زر الحذف
    interface OnLinkPreviewListener {
        fun onLinkPreviewClicked(preview: LinkPreview)
        fun onRemoveLinkClicked(preview: LinkPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkPreviewViewHolder {
        val binding = ItemLinkPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LinkPreviewViewHolder, position: Int) {
        val preview = getItem(position)
        if (preview != null) {
            holder.bind(preview)
        }
    }

    inner class LinkPreviewViewHolder(private val binding: ItemLinkPreviewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(preview: LinkPreview) {
            binding.tvLinkTitle.text = preview.title ?: "No Title Available"
            binding.tvLinkDescription.text = preview.description ?: ""
            binding.tvLinkDomain.text = try { // ✨ تم تغيير tvDomain إلى binding.tvLinkDomain
                preview.url?.let { Uri.parse(it).host } ?: "unknown.com"
            } catch (e: Exception) {
                "invalid.url"
            }

            if (!preview.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(preview.imageUrl)
                    .placeholder(R.drawable.ic_cover_placeholder)
                    .error(R.drawable.ic_cover_placeholder) // Show placeholder on error
                    .into(binding.ivLinkImage)
            } else {
                // Set a default image or hide it if no image URL is available
                binding.ivLinkImage.setImageResource(R.drawable.ic_cover_placeholder)
            }

            // تعيين مستمعي النقرات
            itemView.setOnClickListener {
                listener?.onLinkPreviewClicked(preview)
            }

            binding.btnRemoveLink.setOnClickListener {
                listener?.onRemoveLinkClicked(preview)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LinkPreview>() {
            override fun areItemsTheSame(oldItem: LinkPreview, newItem: LinkPreview): Boolean {
                // Assuming URL is a unique identifier for a preview
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: LinkPreview, newItem: LinkPreview): Boolean {
                return oldItem == newItem
            }
        }
    }
}