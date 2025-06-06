package com.spidroid.starry.adapters

// ★ إضافة استيراد FirebaseAuth
import android.content.Context
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.databinding.ItemUserSuggestionBinding
import java.util.Map
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

class PostAdapter(private val context: Context?, private val listener: PostInteractionListener?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private var items: MutableList<Any?>? = ArrayList<Any?>()
    private val currentUserId: String? = null // ★ إضافة حقل لتخزين معرّف المستخدم الحالي

    // ★★★ تم تعديل المُنشئ ليقوم بتهيئة currentUserId ★★★
    init {
        // تهيئة currentUserId عند إنشاء الـ Adapter
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid()
        } else {
            this.currentUserId = "" // أو قيمة افتراضية مناسبة إذا لم يكن هناك مستخدم
            Log.w(PostAdapter.Companion.TAG, "Current user is null when creating PostAdapter.")
        }
    }

    fun submitCombinedList(newItems: MutableList<Any?>?) {
        this.items = if (newItems != null) newItems else ArrayList<Any?>()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.getContext())
        if (viewType == PostAdapter.Companion.TYPE_POST) {
            val binding = ItemPostBinding.inflate(inflater, parent, false)
            // ★ تمرير currentUserId إلى مُنشئ PostViewHolder
            return PostViewHolder(binding, listener, context, currentUserId)
        } else { // TYPE_SUGGESTION
            val binding = ItemUserSuggestionBinding.inflate(inflater, parent, false)
            return UserSuggestionViewHolder(binding, listener)
            // ملاحظة: UserSuggestionViewHolder لا يستخدم PostInteractionHandler مباشرة بنفس الطريقة
            // لذا قد لا يحتاج إلى currentUserId في مُنشئه إلا إذا كنت ستضيف تفاعلات مشابهة له
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items!!.get(position)
        if (holder.getItemViewType() == PostAdapter.Companion.TYPE_POST) {
            (holder as PostViewHolder).bindCommon(item as PostModel?)
        } else if (holder.getItemViewType() == PostAdapter.Companion.TYPE_SUGGESTION) {
            (holder as UserSuggestionViewHolder).bind(item as UserModel?)
        }
    }

    val itemCount: Int
        get() = if (items != null) items!!.size else 0

    override fun getItemViewType(position: Int): Int {
        if (items!!.get(position) is PostModel) {
            return PostAdapter.Companion.TYPE_POST
        } else if (items!!.get(position) is UserModel) {
            return PostAdapter.Companion.TYPE_SUGGESTION
        }
        return -1
    }

    class PostViewHolder internal constructor(
        private val binding: ItemPostBinding,
        listener: PostInteractionListener?,
        context: Context?,
        currentUserId: String?
    ) : BasePostViewHolder(
        binding, listener, context, currentUserId
    ) {
        private val reactionsDisplayContainer: LinearLayout?

        // ★★★ تم تعديل مُنشئ PostViewHolder ليقبل currentUserId ويمرره إلى super ★★★
        init {
            this.reactionsDisplayContainer = binding.reactionsDisplayContainer
            // PostInteractionHandler يتم تهيئته الآن في BasePostViewHolder
        }

        override fun bindCommon(post: PostModel?) {
            if (post == null) {
                Log.e(PostAdapter.Companion.TAG, "PostModel is null in bindCommon. Hiding item.")
                itemView.setVisibility(View.GONE)
                itemView.setLayoutParams(RecyclerView.LayoutParams(0, 0))
                return
            }
            if (TextUtils.isEmpty(post.getAuthorDisplayName()) && TextUtils.isEmpty(post.getAuthorUsername())) {
                binding.tvAuthorName.setText("Unknown User")
            } else {
                binding.tvAuthorName.setText(if (post.getAuthorDisplayName() != null) post.getAuthorDisplayName() else post.getAuthorUsername())
            }

            if (TextUtils.isEmpty(post.getContent()) && (post.getMediaUrls() == null || post.getMediaUrls()
                    .isEmpty()) && (post.getLinkPreviews() == null || post.getLinkPreviews()
                    .isEmpty())
            ) {
                itemView.setVisibility(View.GONE)
                itemView.setLayoutParams(RecyclerView.LayoutParams(0, 0))
                return
            }

            itemView.setVisibility(View.VISIBLE)
            itemView.setLayoutParams(
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            binding.tvUsername.setText("@" + (if (post.getAuthorUsername() != null) post.getAuthorUsername() else "unknown"))
            binding.tvPostContent.setText(if (post.getContent() != null) post.getContent() else "")

            val avatarUrl: String? = post.getAuthorAvatarUrl()
            if (avatarUrl != null && !avatarUrl.isEmpty() && context != null) {
                Glide.with(context).load(avatarUrl).placeholder(R.drawable.ic_default_avatar)
                    .into(binding.ivAuthorAvatar)
            } else if (context != null) {
                binding.ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            if (interactionHandler != null) {
                interactionHandler.bind(post)
            }
            updateReactionsDisplay(post.getReactions())
        }

        private fun updateReactionsDisplay(reactionsMap: MutableMap<String?, String?>?) {
            if (reactionsDisplayContainer == null || context == null) {
                return
            }
            reactionsDisplayContainer.removeAllViews()

            if (reactionsMap == null || reactionsMap.isEmpty()) {
                reactionsDisplayContainer.setVisibility(View.GONE)
                return
            }

            val emojiCounts: MutableMap<String?, Int?> = HashMap<String?, Int?>()
            for (emoji in reactionsMap.values) {
                emojiCounts.put(emoji, emojiCounts.getOrDefault(emoji, 0)!! + 1)
            }

            val sortedEmojiCounts: MutableMap<String?, Int?> = emojiCounts.entries.stream()
                .sorted(Map.Entry.comparingByValue<String?, Int?>(Comparator.reverseOrder<Int?>()))
                .collect(
                    Collectors.toMap(
                        Function { Map.Entry.key },
                        Function { Map.Entry.value },
                        BinaryOperator { e1: Int?, e2: Int? -> e1 },
                        Supplier { LinkedHashMap() }
                    ))

            var reactionsToShow = 0
            for (entry in sortedEmojiCounts.entries) {
                if (reactionsToShow >= 3) break

                val emoji = entry.key
                val count: Int = entry.value!!

                if (count > 0) {
                    val emojiView: TextView = TextView(context)
                    emojiView.setText(emoji)
                    emojiView.setTextSize(16f)
                    emojiView.setPadding(0, 0, 4, 0)
                    reactionsDisplayContainer.addView(emojiView)
                    reactionsToShow++
                }
            }

            if (!reactionsMap.isEmpty()) {
                val totalReactionsView: TextView = TextView(context)
                totalReactionsView.setText(reactionsMap.size.toString())
                totalReactionsView.setTextSize(14f)
                if (context != null) { // Check context before accessing resources
                    totalReactionsView.setTextColor(
                        context.getResources().getColor(R.color.text_secondary, null)
                    )
                }
                val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMarginStart(8)
                totalReactionsView.setLayoutParams(params)
                reactionsDisplayContainer.addView(totalReactionsView)
            }

            if (reactionsDisplayContainer.getChildCount() > 0) {
                reactionsDisplayContainer.setVisibility(View.VISIBLE)
            } else {
                reactionsDisplayContainer.setVisibility(View.GONE)
            }
        }
    }

    internal class UserSuggestionViewHolder(
        private val binding: ItemUserSuggestionBinding,
        private val listener: PostInteractionListener?
    ) : RecyclerView.ViewHolder(
        binding.getRoot()
    ) {
        private val context: Context?

        init {
            this.context = itemView.getContext()
        }

        fun bind(user: UserModel?) {
            if (user == null) return

            binding.username.setText("@" + (if (user.getUsername() != null) user.getUsername() else ""))
            binding.displayName.setText(if (user.getDisplayName() != null) user.getDisplayName() else "")

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl()
                    .isEmpty() && context != null
            ) {
                Glide.with(context).load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar).into(binding.profileImage)
            } else if (context != null) {
                binding.profileImage.setImageResource(R.drawable.ic_default_avatar)
            }

            binding.followButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) listener.onFollowClicked(user)
            })
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) listener.onUserClicked(user)
            })
        }
    }

    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_SUGGESTION = 1
        private const val TAG = "PostAdapter"
    }
}