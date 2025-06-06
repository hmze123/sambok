package com.spidroid.starry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.spidroid.starry.R

class PostMediaAdapter(
    imageUrls: MutableList<String?>,
    private val listener: ImageInteractionListener
) : RecyclerView.Adapter<PostMediaAdapter.MediaViewHolder?>() {
    private var imageUrls: MutableList<String?>

    init {
        this.imageUrls = ArrayList<String?>(imageUrls)
    }

    fun getImageUrls(): MutableList<String?> {
        return ArrayList<String?>(imageUrls)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view =
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_media, parent, false) // Keep original layout name
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val imageUrl = imageUrls.get(position)

        Glide.with(holder.itemView)
            .load(imageUrl)
            .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_cover_placeholder))
            .into(holder.ivMedia)

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onImageClicked(
                imageUrl,
                position
            )
        })
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    fun updateUrls(newUrls: MutableList<String?>) {
        val oldList = this.imageUrls
        this.imageUrls = ArrayList<String?>(newUrls)

        val result =
            DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return oldList.size
                    }

                    override fun getNewListSize(): Int {
                        return imageUrls.size
                    }

                    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                        return oldList.get(oldPos) == imageUrls.get(newPos)
                    }

                    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                        return oldList.get(oldPos) == imageUrls.get(newPos)
                    }
                })
        result.dispatchUpdatesTo(this)
    }

    fun clear() {
        val size = imageUrls.size
        imageUrls.clear()
        notifyItemRangeRemoved(0, size)
    }

    internal class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivMedia: ImageView

        init {
            ivMedia = itemView.findViewById<ImageView>(R.id.ivMedia) // Keep original ID
        }
    }

    interface ImageInteractionListener {
        fun onImageClicked(imageUrl: String?, position: Int)
    }
}
