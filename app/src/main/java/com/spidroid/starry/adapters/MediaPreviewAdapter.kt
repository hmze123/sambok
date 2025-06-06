package com.spidroid.starry.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemMediaPreviewBinding
import com.spidroid.starry.models.PostModel

/**
 * محول مُحسَّن لعرض معاينة الوسائط (صور أو فيديوهات).
 * يمكن استخدامه في شاشة إنشاء منشور أو في أي مكان آخر يتطلب عرض شبكة من الوسائط.
 */
class MediaPreviewAdapter(
    private val listener: OnMediaInteraction?
) : ListAdapter<MediaPreviewAdapter.MediaItem, MediaPreviewAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    // واجهة للتفاعل مع الأحداث مثل الحذف أو النقر.
    interface OnMediaInteraction {
        fun onMediaRemoved(item: MediaItem, position: Int)
        fun onMediaClicked(item: MediaItem, position: Int)
    }

    // Sealed class لتمثيل أنواع الوسائط المختلفة بطريقة آمنة.
    sealed class MediaItem {
        abstract val id: String
        data class New(val uri: Uri) : MediaItem() {
            override val id: String get() = uri.toString()
        }
        data class Existing(val url: String) : MediaItem() {
            override val id: String get() = url
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaPreviewBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // تعيين مستمعي النقرات مرة واحدة فقط عند إنشاء الـ ViewHolder
            binding.btnRemove.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener?.onMediaRemoved(getItem(bindingAdapterPosition), bindingAdapterPosition)
                }
            }
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener?.onMediaClicked(getItem(bindingAdapterPosition), bindingAdapterPosition)
                }
            }
        }

        fun bind(item: MediaItem) {
            // تحديد ما إذا كان يجب إظهار زر الحذف (يمكن التحكم فيه من الخارج إذا لزم الأمر)
            binding.btnRemove.visibility = View.VISIBLE // افترض أنه ظاهر دائماً في هذه الحالة

            // استخدام Glide لتحميل الصورة سواء كانت من Uri أو String Url
            val loadable = when (item) {
                is MediaItem.New -> item.uri
                is MediaItem.Existing -> item.url
            }

            // يمكنك إضافة منطق هنا لإظهار أيقونة تشغيل الفيديو إذا كان العنصر فيديو
            // val isVideo = (loadable as? String)?.let { isVideoUrl(it) } ?: false
            // binding.videoIcon.visibility = if(isVideo) View.VISIBLE else View.GONE

            Glide.with(itemView.context)
                .load(loadable)
                .placeholder(R.drawable.ic_cover_placeholder)
                .centerCrop()
                .into(binding.ivMedia)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}