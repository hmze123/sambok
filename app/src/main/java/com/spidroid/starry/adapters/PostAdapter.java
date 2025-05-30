package com.spidroid.starry.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.InAppBrowserActivity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.PostInteractionHandler;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import com.spidroid.starry.adapters.BasePostViewHolder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import androidx.constraintlayout.widget.ConstraintLayout;


public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  List<Object> items = new ArrayList<>();

  private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
  private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\bhttps?://[^\\s]+\\b");

  private static final int TYPE_POST = 0;
  private static final int TYPE_SUGGESTION = 1;

  private final Context context;
  private final PostInteractionListener listener;

  private static final DiffUtil.ItemCallback<PostModel> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<PostModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull PostModel oldItem, @NonNull PostModel newItem) {
              return oldItem.getPostId().equals(newItem.getPostId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull PostModel oldItem, @NonNull PostModel newItem) {
              return oldItem.equals(newItem);
            }

            @Nullable
            @Override
            public Object getChangePayload(@NonNull PostModel oldItem, @NonNull PostModel newItem) {
              Bundle payload = new Bundle();
              if (oldItem.isLiked() != newItem.isLiked()) payload.putBoolean("liked", newItem.isLiked());
              if (oldItem.getLikeCount() != newItem.getLikeCount())
                payload.putLong("likeCount", newItem.getLikeCount());
              if (oldItem.isReposted() != newItem.isReposted())
                payload.putBoolean("reposted", newItem.isReposted());
              if (oldItem.getRepostCount() != newItem.getRepostCount())
                payload.putLong("repostCount", newItem.getRepostCount());
              if (oldItem.isBookmarked() != newItem.isBookmarked())
                payload.putBoolean("bookmarked", newItem.isBookmarked());
              if (oldItem.getBookmarkCount() != newItem.getBookmarkCount())
                payload.putLong("bookmarkCount", newItem.getBookmarkCount());
              if (oldItem.getReplyCount() != newItem.getReplyCount())
                payload.putLong("replyCount", newItem.getReplyCount());
              return payload.isEmpty() ? null : payload;
            }
          };

  public PostAdapter(Context context, PostInteractionListener listener) {
    this.context = context;
    this.listener = listener;
  }

  public List<Object> getItems() {
    return items;
  }

  public void submitCombinedList(List<Object> newItems) {
    this.items = newItems;
    notifyDataSetChanged();
  }

  @Override
  public int getItemViewType(int position) {
    Object item = items.get(position);
    if (item instanceof PostModel) {
      return TYPE_POST;
    }
    return TYPE_SUGGESTION;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_POST) {
      return new PostViewHolder(inflater.inflate(R.layout.item_post, parent, false), listener, context);
    }
    return new UserSuggestionViewHolder(inflater.inflate(R.layout.item_user_suggestion, parent, false));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    Object item = items.get(position);
    if (holder instanceof PostViewHolder && item instanceof PostModel) {
      ((PostViewHolder) holder).bindCommon((PostModel) item);
    } else if (holder instanceof UserSuggestionViewHolder && item instanceof UserModel) {
      ((UserSuggestionViewHolder) holder).bind((UserModel) item, listener);
    }
  }

  public static class PostViewHolder extends BasePostViewHolder {
    private final CircleImageView ivAuthorAvatar;
    private final TextView tvAuthorName, tvUsername, tvTimestamp, tvPostContent;
    private final TextView tvLikeCount, tvCommentCount, tvRepostCount, tvBookmarkCount;
    private final ImageButton btnLike, btnComment, btnRepost, btnBookmark, btnMenu;
    private final RecyclerView rvMedia;
    private final TextView tvMediaCounter;
    private final FrameLayout mediaContainer;
    private final RelativeLayout videoLayout;
    private final ImageView ivVideoThumbnail;
    private final ImageButton btnPlay;
    private final TextView tvVideoDuration;
    private final ConstraintLayout postLayout;
    private final View layoutLinkPreview;
    private final ImageView ivLinkImage;
    private final TextView tvLinkTitle, tvLinkDescription, tvLinkDomain;
    private PostMediaAdapter mediaAdapter;
    private final SnapHelper snapHelper = new PagerSnapHelper();


    PostViewHolder(@NonNull View itemView, PostInteractionListener listener, Context context) {
      super(itemView, listener, context);

      ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
      tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
      tvUsername = itemView.findViewById(R.id.tvUsername);
      tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
      tvPostContent = itemView.findViewById(R.id.tvPostContent);
      postLayout = itemView.findViewById(R.id.postLayout);

      mediaContainer = itemView.findViewById(R.id.mediaContainer);
      rvMedia = itemView.findViewById(R.id.rvMedia);
      videoLayout = itemView.findViewById(R.id.videoLayout);
      ivVideoThumbnail = itemView.findViewById(R.id.ivVideoThumbnail);
      btnPlay = itemView.findViewById(R.id.btnPlay);
      tvVideoDuration = itemView.findViewById(R.id.tvVideoDuration);
      tvMediaCounter = itemView.findViewById(R.id.tvMediaCounter);

      layoutLinkPreview = itemView.findViewById(R.id.layoutLinkPreview);
      ivLinkImage = layoutLinkPreview.findViewById(R.id.ivLinkImage);
      tvLinkTitle = layoutLinkPreview.findViewById(R.id.tvLinkTitle);
      tvLinkDescription = layoutLinkPreview.findViewById(R.id.tvLinkDescription);
      tvLinkDomain = layoutLinkPreview.findViewById(R.id.tvLinkDomain);

      btnLike = itemView.findViewById(R.id.btnLike);
      btnComment = itemView.findViewById(R.id.btnComment);
      btnRepost = itemView.findViewById(R.id.btnRepost);
      btnBookmark = itemView.findViewById(R.id.btnBookmark);
      btnMenu = itemView.findViewById(R.id.btnMenu);
      tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
      tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
      tvRepostCount = itemView.findViewById(R.id.tvRepostCount);
      tvBookmarkCount = itemView.findViewById(R.id.tvBookmarkCount);

      mediaAdapter =
              new PostMediaAdapter(
                      Collections.emptyList(),
                      new PostMediaAdapter.ImageInteractionListener() {
                        @Override
                        public void onImageClicked(String imageUrl, int position) {
                          if (listener != null) {
                            List<String> items = mediaAdapter.getImageUrls();
                            listener.onMediaClicked(new ArrayList<>(items), position);
                          }
                        }
                      });

      LinearLayoutManager layoutManager =
              new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
      rvMedia.setLayoutManager(layoutManager);
      rvMedia.setAdapter(mediaAdapter);
      snapHelper.attachToRecyclerView(rvMedia);
    }

    @Override
    public void bindCommon(PostModel post) {
      tvAuthorName.setText(post.getAuthorDisplayName());
      tvUsername.setText("@" + post.getAuthorUsername());
      tvTimestamp.setText(getRelativeTime(post.getCreatedAt()));

      Glide.with(itemView.getContext())
              .load(post.getAuthorAvatarUrl())
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .placeholder(R.drawable.ic_default_avatar)
              .error(R.drawable.ic_default_avatar)
              .into(ivAuthorAvatar);

      itemView.findViewById(R.id.ivVerified).setVisibility(post.isAuthorVerified() ? View.VISIBLE : View.GONE);
      applyTextStyling(tvPostContent, post.getContent(), post);

      // الحصول على قائمة الوسائط مرة واحدة والتحقق منها هنا
      List<String> mediaUrls = post.getMediaUrls();

      if (post.isImagePost() && mediaUrls != null && !mediaUrls.isEmpty()) {
        mediaContainer.setVisibility(View.VISIBLE);
        rvMedia.setVisibility(View.VISIBLE);
        videoLayout.setVisibility(View.GONE);
        updateMedia(mediaUrls);
      } else if (post.isVideoPost() && mediaUrls != null && !mediaUrls.isEmpty()) {
        mediaContainer.setVisibility(View.VISIBLE);
        rvMedia.setVisibility(View.GONE);
        videoLayout.setVisibility(View.VISIBLE);
        setupVideoPost(post);
      } else {
        mediaContainer.setVisibility(View.GONE);
      }

      tvLikeCount.setText(formatCount(post.getLikeCount()));
      tvCommentCount.setText(formatCount(post.getReplyCount()));
      tvRepostCount.setText(formatCount(post.getRepostCount()));
      tvBookmarkCount.setText(formatCount(post.getBookmarkCount()));

      updateButtonState(btnLike, post.isLiked(), R.drawable.ic_like_filled);
      updateButtonState(btnRepost, post.isReposted(), R.drawable.ic_repost_filled);
      updateButtonState(btnBookmark, post.isBookmarked(), R.drawable.ic_bookmark_filled);

      setupLinkPreview(post.getLinkPreviews());
      setupClickListeners(post);
      interactionHandler.bind(post);
    }

    void updatePartial(@NonNull List<Object> payloads) {
      for (Object payload : payloads) {
        if (payload instanceof Bundle) {
          interactionHandler.handlePayload((Bundle) payload);
        }
      }
    }

    private void clearGlideLoadsSafely(ImageView imageView) {
      Context context = itemView.getContext();
      if (isValidContextForGlide(context)) {
        Glide.with(context).clear(imageView);
      }
    }

    private boolean isValidContextForGlide(Context context) {
      if (context instanceof Activity) {
        Activity activity = (Activity) context;
        return !activity.isDestroyed() && !activity.isFinishing();
      }
      return context != null;
    }

    private void applyTextStyling(TextView textView, String content, PostModel post) {
      if (post.getCachedSpannable() == null) {
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
        String displayContent = post.isTranslated() ? post.getTranslatedContent() : post.getContent();

        if (!post.isExpanded() && displayContent.length() > 300) {
          String truncated = displayContent.substring(0, 300) + "... ";
          spannableBuilder.append(truncated);
          applyHashtagStyling(spannableBuilder);
          applyUrlStyling(spannableBuilder);

          SpannableString seeMore = new SpannableString(context.getString(R.string.see_more));
          ClickableSpan seeMoreSpan =
                  new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                      if (listener != null) listener.onSeeMoreClicked(post);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                      ds.setColor(ContextCompat.getColor(context, R.color.primary));
                      ds.setUnderlineText(false);
                    }
                  };
          seeMore.setSpan(seeMoreSpan, 0, seeMore.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          spannableBuilder.append(seeMore);
        } else {
          spannableBuilder.append(displayContent);
          applyHashtagStyling(spannableBuilder);
          applyUrlStyling(spannableBuilder);
        }

        SpannableString translateSpanText;
        if (!post.isTranslated()) {
          translateSpanText = new SpannableString(" • " + context.getString(R.string.translate));
          ClickableSpan translateSpan =
                  new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                      if (listener != null) listener.onTranslateClicked(post);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                      ds.setColor(ContextCompat.getColor(context, R.color.primary));
                      ds.setUnderlineText(false);
                    }
                  };
          translateSpanText.setSpan(
                  translateSpan, 3, translateSpanText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
          translateSpanText = new SpannableString(" • " + context.getString(R.string.show_original));
          ClickableSpan originalSpan =
                  new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                      if (listener != null) listener.onShowOriginalClicked(post);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                      ds.setColor(ContextCompat.getColor(context, R.color.primary));
                      ds.setUnderlineText(false);
                    }
                  };
          translateSpanText.setSpan(
                  originalSpan, 3, translateSpanText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        spannableBuilder.append(translateSpanText);

        post.setCachedSpannable((SpannableStringBuilder) spannableBuilder);
      }

      textView.setText(new SpannableString(post.getCachedSpannable()));
      textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void applyHashtagStyling(SpannableStringBuilder spannable) {
      Matcher matcher = HASHTAG_PATTERN.matcher(spannable);
      while (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();

        spannable.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(
                new ClickableSpan() {
                  @Override
                  public void onClick(@NonNull View widget) {
                    if (listener != null) {
                      listener.onHashtagClicked(matcher.group(1));
                    }
                  }

                  @Override
                  public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                  }
                },
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }

    private void applyUrlStyling(SpannableStringBuilder spannable) {
      Matcher matcher = URL_PATTERN.matcher(spannable);
      while (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();

        spannable.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.link_color)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final String url = spannable.subSequence(start, end).toString();
        spannable.setSpan(
                new ClickableSpan() {
                  @Override
                  public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, InAppBrowserActivity.class);
                    intent.putExtra(InAppBrowserActivity.EXTRA_URL, url);

                    ActivityOptions options =
                            ActivityOptions.makeCustomAnimation(
                                    context, android.R.anim.fade_in, android.R.anim.fade_out);
                    context.startActivity(intent, options.toBundle());
                  }

                  @Override
                  public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                  }
                },
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }

    private void setupVideoPost(PostModel post) {
      String videoUrl = post.getFirstMediaUrl();
      Glide.with(context)
              .load(videoUrl)
              .placeholder(R.drawable.ic_video_placeholder)
              .into(ivVideoThumbnail);

      if (post.getVideoDuration() > 0) {
        tvVideoDuration.setText(formatDuration(post.getVideoDuration()));
        tvVideoDuration.setVisibility(View.VISIBLE);
      } else {
        tvVideoDuration.setVisibility(View.GONE);
      }

      btnPlay.setOnClickListener(
              v -> {
                if (listener != null) listener.onVideoPlayClicked(videoUrl);
              });
    }

    private String formatDuration(long milliseconds) {
      long seconds = milliseconds / 1000;
      return String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60);
    }

    private void updateMedia(List<String> mediaUrls) {
      List<String> validUrls =
              mediaUrls != null
                      ? mediaUrls.stream()
                      .filter(url -> url != null && !url.isEmpty())
                      .collect(Collectors.toList())
                      : Collections.emptyList();

      if (!validUrls.isEmpty()) {
        mediaAdapter.updateUrls(validUrls);
        tvMediaCounter.setVisibility(validUrls.size() > 1 ? View.VISIBLE : View.GONE);

        rvMedia.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                  @Override
                  public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager =
                            (LinearLayoutManager) recyclerView.getLayoutManager();
                    int currentPosition = layoutManager.findFirstVisibleItemPosition() + 1;
                    tvMediaCounter.setText(currentPosition + "/" + validUrls.size());
                  }
                });

        tvMediaCounter.setText("1/" + validUrls.size());
      }
    }

    private void setupLinkPreview(List<PostModel.LinkPreview> linkPreviews) {
      if (linkPreviews == null || linkPreviews.isEmpty()) {
        layoutLinkPreview.setVisibility(View.GONE);
        return;
      }

      PostModel.LinkPreview firstPreview = linkPreviews.get(0);
      if (firstPreview == null || firstPreview.getUrl() == null) {
        layoutLinkPreview.setVisibility(View.GONE);
        return;
      }

      layoutLinkPreview.setVisibility(View.VISIBLE);
      tvLinkTitle.setText(
              firstPreview.getTitle() != null ? firstPreview.getTitle() : "Link Preview");

      if (firstPreview.getDescription() != null && !firstPreview.getDescription().isEmpty()) {
        tvLinkDescription.setText(firstPreview.getDescription());
        tvLinkDescription.setVisibility(View.VISIBLE);
      } else {
        tvLinkDescription.setVisibility(View.GONE);
      }

      String domain = null;
      try {
        Uri uri = Uri.parse(firstPreview.getUrl());
        if (uri != null) {
          domain = uri.getHost();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      tvLinkDomain.setText(domain != null ? domain : "");

      if (firstPreview.getImageUrl() != null) {
        Glide.with(context)
                .load(firstPreview.getImageUrl())
                .placeholder(R.drawable.ic_cover_placeholder)
                .into(ivLinkImage);
      } else {
        Glide.with(context).load(R.drawable.ic_cover_placeholder).into(ivLinkImage);
      }

      layoutLinkPreview.setOnClickListener(
              v -> {
                String url = null;
                try {
                  url = firstPreview.getUrl();

                  if (url == null || url.isEmpty()) {
                    Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show();
                    return;
                  }

                  Intent intent = new Intent(context, InAppBrowserActivity.class);
                  intent.putExtra(InAppBrowserActivity.EXTRA_URL, url);

                  ActivityOptions options =
                          ActivityOptions.makeCustomAnimation(
                                  context, android.R.anim.fade_in, android.R.anim.fade_out);
                  context.startActivity(intent, options.toBundle());

                } catch (ActivityNotFoundException e) {
                  if (url != null) {
                    Intent fallbackIntent =
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);

                    try {
                      context.startActivity(fallbackIntent);
                    } catch (ActivityNotFoundException ex) {
                      Toast.makeText(context, "No browser installed", Toast.LENGTH_SHORT).show();
                    }
                  } else {
                    Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show();
                  }
                } catch (Exception e) {
                  Toast.makeText(context, "Error opening link", Toast.LENGTH_SHORT).show();
                }
              });
    }

    private void setupClickListeners(PostModel post) {
      ivAuthorAvatar.setOnClickListener(
              v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", post.getAuthorId());
                context.startActivity(intent);
              });

      postLayout.setOnClickListener(
              v -> {
                if (listener != null) listener.onLayoutClicked(post);
              });

      btnLike.setOnClickListener(
              v -> {
                post.toggleLike();
                updateButtonState(btnLike, post.isLiked(), R.drawable.ic_like_filled);
                tvLikeCount.setText(formatCount(post.getLikeCount()));
                if (listener != null) listener.onLikeClicked(post);
              });

      btnRepost.setOnClickListener(
              v -> {
                post.toggleRepost();
                updateButtonState(btnRepost, post.isReposted(), R.drawable.ic_repost_filled);
                tvRepostCount.setText(formatCount(post.getRepostCount()));
                if (listener != null) listener.onRepostClicked(post);
              });

      btnBookmark.setOnClickListener(
              v -> {
                post.toggleBookmark();
                updateButtonState(btnBookmark, post.isBookmarked(), R.drawable.ic_bookmark_filled);
                tvBookmarkCount.setText(formatCount(post.getBookmarkCount()));
                if (listener != null) listener.onBookmarkClicked(post);
              });

      btnComment.setOnClickListener(
              v -> {
                if (listener != null) listener.onCommentClicked(post);
              });

      btnMenu.setOnClickListener(
              v -> {
                if (listener != null) listener.onMenuClicked(post, v);
              });

      itemView.setOnLongClickListener(
              v -> {
                if (listener != null) listener.onPostLongClicked(post);
                return true;
              });
    }

    private void updateButtonState(ImageButton button, boolean isActive, int filledRes) {
      button.setImageResource(isActive ? filledRes : getOutlineRes(filledRes));
      int colorRes;
      if (isActive) {
        if (filledRes == R.drawable.ic_like_filled) {
          colorRes = R.color.red;
        } else if (filledRes == R.drawable.ic_repost_filled) {
          colorRes = R.color.green;
        } else if (filledRes == R.drawable.ic_bookmark_filled) {
          colorRes = R.color.yellow;
        } else {
          colorRes = R.color.primary;
        }
      } else {
        colorRes = R.color.text_secondary;
      }
      button.setColorFilter(ContextCompat.getColor(context, colorRes));
    }

    private int getOutlineRes(int filledRes) {
      if (filledRes == R.drawable.ic_like_filled) {
        return R.drawable.ic_like_outline;
      } else if (filledRes == R.drawable.ic_repost_filled) {
        return R.drawable.ic_repost_outline;
      } else if (filledRes == R.drawable.ic_bookmark_filled) {
        return R.drawable.ic_bookmark_outline;
      }
      return filledRes;
    }

    private String formatCount(long count) {
      if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000f);
      if (count >= 1_000) return String.format("%.1fK", count / 1_000f);
      return String.valueOf(count);
    }

    private String getRelativeTime(Date date) {
      long now = System.currentTimeMillis();
      return DateUtils.getRelativeTimeSpanString(
                      date.getTime(), now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
              .toString();
    }
  }

  static class UserSuggestionViewHolder extends RecyclerView.ViewHolder {
    private final CircleImageView profileImage;
    private final TextView username, displayName;
    private final ImageButton followButton;

    UserSuggestionViewHolder(@NonNull View itemView) {
      super(itemView);
      profileImage = itemView.findViewById(R.id.profileImage);
      username = itemView.findViewById(R.id.username);
      displayName = itemView.findViewById(R.id.displayName);
      followButton = itemView.findViewById(R.id.followButton);
    }

    void bind(UserModel user, PostInteractionListener listener) {
      username.setText("@" + user.getUsername());
      displayName.setText(user.getDisplayName());

      Glide.with(itemView)
              .load(user.getProfileImageUrl())
              .placeholder(R.drawable.ic_default_avatar)
              .into(profileImage);

      followButton.setOnClickListener(v -> listener.onFollowClicked(user));
      itemView.setOnClickListener(v -> listener.onUserClicked(user));
    }
  }

  public interface PostInteractionListener {
    void onHashtagClicked(String hashtag);

    void onLikeClicked(PostModel post);

    void onCommentClicked(PostModel post);

    void onRepostClicked(PostModel post);

    void onBookmarkClicked(PostModel post);

    void onMenuClicked(PostModel post, View anchor);

    void onDeletePost(PostModel post);

    void onEditPost(PostModel post);

    void onModeratePost(PostModel post);

    void onPostLongClicked(PostModel post);

    void onMediaClicked(List<String> mediaUrls, int position);

    void onVideoPlayClicked(String videoUrl);

    void onSharePost(PostModel post);

    void onCopyLink(PostModel post);

    void onReportPost(PostModel post);

    void onToggleBookmark(PostModel post);

    void onLayoutClicked(PostModel post);

    void onSeeMoreClicked(PostModel post);

    void onTranslateClicked(PostModel post);

    void onShowOriginalClicked(PostModel post);

    void onFollowClicked(UserModel user);

    void onUserClicked(UserModel user);
  }
}