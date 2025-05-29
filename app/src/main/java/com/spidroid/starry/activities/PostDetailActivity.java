package com.spidroid.starry.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.adapters.CommentAdapter;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.adapters.PostMediaAdapter;
import com.spidroid.starry.databinding.ActivityProfileBinding;
import com.spidroid.starry.models.CommentModel;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.PostInteractionHandler;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostDetailActivity extends AppCompatActivity
    implements PostAdapter.PostInteractionListener {

  public static final String EXTRA_POST = "post";
  private Uri selectedMediaUri;
  private static final int PICK_IMAGE_REQUEST = 100;
  private static final int MAX_CHARS = 500;
  private FirebaseUser currentUser;
  private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
  private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\bhttps?://[^\\s]+\\b");

  private PostModel post;
  private SwipeRefreshLayout swipeRefresh;
  private PostMediaAdapter mediaAdapter;

  private EditText postInput;
  private ImageButton addMedia;
  private TextView charCounter;
  private MaterialButton postButton;

  private CommentAdapter commentAdapter;

  private String currentUserDisplayName;
  private String currentUsername;
  private String currentUserAvatarUrl;

  private String parentCommentId;
  private String parentCommentAuthorId;
  private String parentCommentAuthorUsername;

  private PostInteractionHandler interactionHandler;

  public static void start(Context context, PostModel post) {
    Intent intent = new Intent(context, PostDetailActivity.class);
    intent.putExtra(EXTRA_POST, post);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post_detail);

    currentUser = FirebaseAuth.getInstance().getCurrentUser();

    post = getIntent().getParcelableExtra(EXTRA_POST);
    if (post == null) {
      finish();
      return;
    }

    View interactionView = findViewById(R.id.interaction_container);
    interactionHandler = new PostInteractionHandler(interactionView, this, this);
    interactionHandler.bind(post);

    initializeViews();
    setupToolbar();
    setupMediaRecyclerView();
    setupSwipeRefresh();
    bindPostData();
    setupInputSection();
    setupCommentsRecyclerView();
    loadComments();
  }

  private void initializeViews() {
    swipeRefresh = findViewById(R.id.swipeRefresh);
  }

  private void setupToolbar() {
    ImageView ivBack = findViewById(R.id.iv_back);
    ivBack.setOnClickListener(v -> finish());

    ImageButton btnMenu = findViewById(R.id.btnMenu);
    btnMenu.setOnClickListener(
        v -> {
          // Pass the post and the menu button as anchor
          onMenuClicked(post, v);
        });
  }

  private void setupMediaRecyclerView() {
    RecyclerView rvMedia = findViewById(R.id.rvMedia);
    rvMedia.setHasFixedSize(true);
    rvMedia.setNestedScrollingEnabled(false);

//    mediaAdapter = new PostMediaAdapter(new ArrayList<>(), new MediaInteractionListenerImpl());
    LinearLayoutManager layoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
    rvMedia.setLayoutManager(layoutManager);
    rvMedia.setAdapter(mediaAdapter);

    // Add this for better media scrolling
    new PagerSnapHelper().attachToRecyclerView(rvMedia);
    rvMedia.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (lm != null) {
              int activePosition = lm.findFirstVisibleItemPosition();
              updateMediaCounter(activePosition + 1); // +1 because positions start at 0
            }
          }
        });
  }

  private void updateMediaCounter(int currentPosition) {
    TextView tvMediaCounter = findViewById(R.id.tvMediaCounter);
    if (post.getMediaUrls().size() > 1) {
      tvMediaCounter.setText(currentPosition + "/" + post.getMediaUrls().size());
    }
  }

  private void setupActionButton() {
    MaterialButton btnAction = findViewById(R.id.btnAction);
    String currentUserId = FirebaseAuth.getInstance().getUid();

    if (currentUserId == null || currentUserId.equals(post.getAuthorId())) {
      // Hide button for anonymous users and post authors
      btnAction.setVisibility(View.GONE);
      return;
    }

    // Only show follow button for other users
    btnAction.setVisibility(View.VISIBLE);
    updateFollowButtonAppearance(post.isFollowing());

    btnAction.setOnClickListener(
        v -> {
          toggleFollowState();
          updateFollowButtonAppearance(!post.isFollowing());
        });
  }

  private void setupFollowButton() {
    MaterialButton btnAction = findViewById(R.id.btnAction);
    btnAction.setVisibility(View.VISIBLE);
    updateFollowButtonAppearance(post.isFollowing());

    btnAction.setOnClickListener(
        v -> {
          toggleFollowState();
          updateFollowButtonAppearance(!post.isFollowing());
        });
  }

  private void updateFollowButtonAppearance(boolean isFollowing) {
    MaterialButton btnAction = findViewById(R.id.btnAction);

    if (isFollowing) {
      btnAction.setText("Following");
      btnAction.setStrokeColorResource(android.R.color.transparent);
      btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
      btnAction.setTextColor(ContextCompat.getColor(this, R.color.white));
    } else {
      btnAction.setText("Follow");
      btnAction.setStrokeColorResource(R.color.text_secondary);
      btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
      btnAction.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    }
  }

  private void toggleFollowState() {
    String currentUserId = FirebaseAuth.getInstance().getUid();
    if (currentUserId == null) return;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference userRef = db.collection("users").document(currentUserId);
    DocumentReference authorRef = db.collection("users").document(post.getAuthorId());

    db.runTransaction(
            transaction -> {
              // Update current user's following list
              transaction.update(userRef, "following." + post.getAuthorId(), !post.isFollowing());

              // Update author's followers list
              transaction.update(authorRef, "followers." + currentUserId, !post.isFollowing());

              return null;
            })
        .addOnSuccessListener(
            aVoid -> {
              post.setFollowing(!post.isFollowing());
              updateFollowButtonAppearance(post.isFollowing());
            })
        .addOnFailureListener(
            e -> {
              Toast.makeText(this, "Failed to update follow status", Toast.LENGTH_SHORT).show();
            });
  }

  private void setupSwipeRefresh() {
    swipeRefresh.setOnRefreshListener(
        () -> {
          // Implement your refresh logic here
          swipeRefresh.setRefreshing(false);
        });
  }

  private void bindPostData() {
    // Author Info
    CircleImageView ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
    TextView tvAuthorName = findViewById(R.id.tvAuthorName);
    TextView tvUsername = findViewById(R.id.tvUsername);
    TextView tvTimestamp = findViewById(R.id.tvTimestamp);
    ImageView ivVerified = findViewById(R.id.ivVerified);

    Glide.with(this)
        .load(post.getAuthorAvatarUrl())
        .placeholder(R.drawable.ic_default_avatar)
        .into(ivAuthorAvatar);

    tvAuthorName.setText(post.getAuthorDisplayName());
    tvUsername.setText("@" + post.getAuthorUsername());
    Date createdAt = post.getCreatedAt() != null ? post.getCreatedAt() : new Date();
    tvTimestamp.setText(
        DateUtils.getRelativeTimeSpanString(
            createdAt.getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
    ivVerified.setVisibility(post.isAuthorVerified() ? View.VISIBLE : View.GONE);

    // Post Content
    TextView tvPostContent = findViewById(R.id.tvPostContent);
    applyTextStyling(tvPostContent, post.getContent());

    // Media
    updateMediaViews(post.getMediaUrls());

    // Link Preview
    setupLinkPreview(post.getLinkPreviews());

    // Engagement Metrics
    updateEngagementMetrics();
    setupButtonListeners();
    setupActionButton();
  }

  private void applyTextStyling(TextView textView, String content) {
    SpannableStringBuilder spannable = new SpannableStringBuilder(content);
    applyHashtagStyling(spannable);
    applyUrlStyling(spannable);
    textView.setText(spannable);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
  }

  private void applyHashtagStyling(SpannableStringBuilder spannable) {
    Matcher matcher = HASHTAG_PATTERN.matcher(spannable);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();

      spannable.setSpan(
          new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary)),
          start,
          end,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

      spannable.setSpan(
          new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
              onHashtagClicked(matcher.group(1));
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
          new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_color)),
          start,
          end,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

      final String url = spannable.subSequence(start, end).toString();
      spannable.setSpan(
          new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
              openWebPage(url);
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

  private void updateMediaViews(List<String> mediaUrls) {
    View mediaContainer = findViewById(R.id.mediaContainer);
    TextView tvMediaCounter = findViewById(R.id.tvMediaCounter);
    ImageView ivMediaTypeIndicator = findViewById(R.id.ivMediaTypeIndicator);

    if (mediaUrls == null || mediaUrls.isEmpty()) {
      mediaContainer.setVisibility(View.GONE);
      return;
    }

    mediaContainer.setVisibility(View.VISIBLE);
    mediaAdapter.updateUrls(mediaUrls);

    // Update media counter visibility
    tvMediaCounter.setVisibility(mediaUrls.size() > 1 ? View.VISIBLE : View.GONE);
    tvMediaCounter.setText("1/" + mediaUrls.size()); // Initialize with first position

    // Update video indicator
    boolean hasVideo =
        mediaUrls.stream()
            .anyMatch(url -> PostModel.VIDEO_EXTENSIONS.contains(PostModel.getFileExtension(url)));
    ivMediaTypeIndicator.setVisibility(hasVideo ? View.VISIBLE : View.GONE);

    // Force redraw of RecyclerView
    mediaAdapter.notifyDataSetChanged();
  }

  private void setupLinkPreview(List<PostModel.LinkPreview> linkPreviews) {
    View layoutLinkPreview = findViewById(R.id.layoutLinkPreview);
    if (linkPreviews.isEmpty()) {
      layoutLinkPreview.setVisibility(View.GONE);
      return;
    }

    PostModel.LinkPreview preview = linkPreviews.get(0);
    ImageView ivLinkImage = findViewById(R.id.ivLinkImage);
    TextView tvLinkTitle = findViewById(R.id.tvLinkTitle);
    TextView tvLinkDescription = findViewById(R.id.tvLinkDescription);
    TextView tvLinkDomain = findViewById(R.id.tvLinkDomain);

    Glide.with(this)
        .load(preview.getImageUrl())
        .placeholder(R.drawable.ic_cover_placeholder)
        .into(ivLinkImage);

    tvLinkTitle.setText(preview.getTitle());
    tvLinkDescription.setText(preview.getDescription());
    tvLinkDomain.setText(Uri.parse(preview.getUrl()).getHost());

    layoutLinkPreview.setOnClickListener(v -> openWebPage(preview.getUrl()));
  }

  private void updateEngagementMetrics() {
    TextView tvLikeCount = findViewById(R.id.tvLikeCount);
    TextView tvCommentCount = findViewById(R.id.tvCommentCount);
    TextView tvRepostCount = findViewById(R.id.tvRepostCount);
    TextView tvBookmarkCount = findViewById(R.id.tvBookmarkCount);

    tvLikeCount.setText(formatCount(post.getLikeCount()));
    tvCommentCount.setText(formatCount(post.getReplyCount()));
    tvRepostCount.setText(formatCount(post.getRepostCount()));
    tvBookmarkCount.setText(formatCount(post.getBookmarkCount()));

    updateButtonState(R.id.btnLike, post.isLiked(), R.drawable.ic_like_filled);
    updateButtonState(R.id.btnRepost, post.isReposted(), R.drawable.ic_repost_filled);
    updateButtonState(R.id.btnBookmark, post.isBookmarked(), R.drawable.ic_bookmark_filled);
  }

  private void updateButtonState(int buttonId, boolean isActive, int filledRes) {
    ImageButton button = findViewById(buttonId);
    button.setImageResource(isActive ? filledRes : getOutlineRes(filledRes));
    button.setColorFilter(
        ContextCompat.getColor(
            this, isActive ? getActiveColor(filledRes) : R.color.text_secondary));
  }

  private int getActiveColor(int filledRes) {
    if (filledRes == R.drawable.ic_like_filled) return R.color.red;
    if (filledRes == R.drawable.ic_repost_filled) return R.color.green;
    if (filledRes == R.drawable.ic_bookmark_filled) return R.color.yellow;
    return R.color.primary;
  }

  private int getOutlineRes(int filledRes) {
    if (filledRes == R.drawable.ic_like_filled) return R.drawable.ic_like_outline;
    if (filledRes == R.drawable.ic_repost_filled) return R.drawable.ic_repost_outline;
    if (filledRes == R.drawable.ic_bookmark_filled) return R.drawable.ic_bookmark_outline;
    return filledRes;
  }

  private void setupButtonListeners() {
    setupButton(R.id.btnLike, () -> toggleLike());
    setupButton(R.id.btnRepost, () -> toggleRepost());
    setupButton(R.id.btnBookmark, () -> toggleBookmark());
    findViewById(R.id.btnComment).setOnClickListener(v -> openComments());
  }

  private void setupButton(int buttonId, Runnable action) {
    findViewById(buttonId)
        .setOnClickListener(
            v -> {
              action.run();
              updateEngagementMetrics();
            });
  }

  @Override
  public void onMenuClicked(PostModel post, View anchor) {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
    View bottomSheetView =
        LayoutInflater.from(this).inflate(R.layout.bottom_sheet_post_options, null);
    bottomSheetDialog.setContentView(bottomSheetView);

    String currentUserId = FirebaseAuth.getInstance().getUid();
    boolean isAdmin = false; // Implement your admin check logic
    boolean isAuthor = post.getAuthorId().equals(currentUserId);
    boolean showAdminOptions = isAdmin || isAuthor;

    MaterialButton btnDelete = bottomSheetView.findViewById(R.id.option_delete);
    MaterialButton btnEdit = bottomSheetView.findViewById(R.id.option_edit);
    MaterialButton btnModerate = bottomSheetView.findViewById(R.id.option_moderate);

    btnDelete.setVisibility(showAdminOptions ? View.VISIBLE : View.GONE);
    btnEdit.setVisibility((showAdminOptions && isAuthor) ? View.VISIBLE : View.GONE);
    btnModerate.setVisibility((showAdminOptions && isAdmin) ? View.VISIBLE : View.GONE);

    // Set up click listeners
    bottomSheetView
        .findViewById(R.id.option_report)
        .setOnClickListener(
            v -> {
              onReportPost(post);
              bottomSheetDialog.dismiss();
            });

    bottomSheetView
        .findViewById(R.id.option_share)
        .setOnClickListener(
            v -> {
              onSharePost(post);
              bottomSheetDialog.dismiss();
            });

    bottomSheetView
        .findViewById(R.id.option_copy_link)
        .setOnClickListener(
            v -> {
              onCopyLink(post);
              bottomSheetDialog.dismiss();
            });

    bottomSheetView
        .findViewById(R.id.option_save)
        .setOnClickListener(
            v -> {
              onToggleBookmark(post);
              bottomSheetDialog.dismiss();
            });

    btnDelete.setOnClickListener(
        v -> {
          bottomSheetDialog.dismiss();
          onDeletePost(post);
        });

    btnEdit.setOnClickListener(
        v -> {
          bottomSheetDialog.dismiss();
          onEditPost(post);
        });

    btnModerate.setOnClickListener(
        v -> {
          bottomSheetDialog.dismiss();
          onModeratePost(post);
        });

    bottomSheetDialog.show();
  }

  @Override
  public void onSharePost(PostModel post) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent() + "\n" + generatePostLink(post));
    startActivity(Intent.createChooser(shareIntent, "Share Post"));
  }

  @Override
  public void onCopyLink(PostModel post) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("Post Link", generatePostLink(post));
    clipboard.setPrimaryClip(clip);
    Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onReportPost(PostModel post) {
    new AlertDialog.Builder(this)
        .setTitle("Report Post")
        .setMessage("Are you sure you want to report this post?")
        .setPositiveButton(
            "Report",
            (d, w) -> {
              // Implement report logic
              // viewModel.reportPost(post.getPostId());
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  @Override
  public void onDeletePost(PostModel post) {
    new AlertDialog.Builder(this)
        .setTitle("Delete Post")
        .setMessage("Are you sure you want to delete this post?")
        .setPositiveButton(
            "Delete",
            (d, w) -> {
              // Implement delete logic
              // viewModel.deletePost(post.getPostId());
              finish(); // Close activity after deletion
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  // Add this helper method
  private String generatePostLink(PostModel post) {
    return "https://your-app-domain.com/posts/" + post.getPostId();
  }

  private void setupInputSection() {
    // Set reply header visibility
    TextView replyHeader = findViewById(R.id.replyHeader);
    replyHeader.setVisibility(View.VISIBLE);
    replyHeader.setText(getString(R.string.replying_to, post.getAuthorUsername()));

    // Load current user avatar
    CircleImageView userImage = findViewById(R.id.userImage);
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(currentUser.getUid())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              UserModel user = documentSnapshot.toObject(UserModel.class);
              if (user != null && user.getProfileImageUrl() != null) {
                Glide.with(this)
                    .load(user.getProfileImageUrl()) // Use Firestore URL
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(userImage);
              } else {
                userImage.setImageResource(R.drawable.ic_default_avatar);
              }
            });

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(currentUser.getUid())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              UserModel user = documentSnapshot.toObject(UserModel.class);
              if (user != null) {
                currentUserDisplayName = user.getDisplayName();
                currentUsername = user.getUsername();
                currentUserAvatarUrl = user.getProfileImageUrl();
                // Update UI with userImage
              }
            });

    // Initialize views
    EditText postInput = findViewById(R.id.postInput);
    TextView charCounter = findViewById(R.id.charCounter);
    MaterialButton postButton = findViewById(R.id.postButton);
    View mediaPreview = findViewById(R.id.mediaPreview);
    ImageButton addMedia = findViewById(R.id.addMedia);

    // Text input listener
    postInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            int remaining = MAX_CHARS - s.length();
            charCounter.setText(String.valueOf(remaining));
            charCounter.setTextColor(
                ContextCompat.getColor(
                    PostDetailActivity.this, remaining < 0 ? R.color.red : R.color.text_secondary));
            updatePostButtonState();
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    // Media selection
    addMedia.setOnClickListener(v -> openMediaChooser());

    // Post button
    postButton.setOnClickListener(v -> postReply());
  }

  private void setupCommentsRecyclerView() {
    RecyclerView recyclerView = findViewById(R.id.recyclerView);
    commentAdapter =
        new CommentAdapter(
            new CommentInteractionListenerImpl(),
            this,
            post.getAuthorId(), // Post author ID
            post.getPostId() // Add post ID as fourth parameter
            );

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(commentAdapter);
  }

  private class CommentInteractionListenerImpl
      implements CommentAdapter.CommentInteractionListener {
    @Override
    public void onLikeClicked(CommentModel comment) {
      // Implement like functionality
      toggleCommentLike(comment);
    }

    @Override
    public void onReplyClicked(CommentModel comment) {
      // Handle reply to comment
      handleReplyToComment(comment);
    }

    @Override
    public void onShowRepliesClicked(CommentModel comment) {
      // Expand/collapse replies
      commentAdapter.toggleReplies(comment);
    }

    @Override
    public void onAuthorClicked(String userId) {
      // Open profile activity
      //      ProfileActivity.start(PostDetailActivity.this, userId);
    }
  }

  private void loadComments() {
    String currentUserId =
        FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
            : "";

    FirebaseFirestore.getInstance()
        .collection("posts")
        .document(post.getPostId())
        .collection("comments")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener(
            (value, error) -> {
              if (error != null) {
                Toast.makeText(this, "Error loading comments", Toast.LENGTH_SHORT).show();
                return;
              }

              List<CommentModel> comments = new ArrayList<>();
              for (QueryDocumentSnapshot doc : value) {
                CommentModel comment = doc.toObject(CommentModel.class);
                comment.setCommentId(doc.getId());
                comment.setLiked(comment.getLikes().containsKey(currentUserId));
                comments.add(comment);
              }

              commentAdapter.updateComments(comments);
            });
  }

  private void toggleCommentLike(CommentModel comment) {
    String currentUserId = FirebaseAuth.getInstance().getUid();
    if (currentUserId == null) return;

    DocumentReference commentRef =
        FirebaseFirestore.getInstance().collection("comments").document(comment.getCommentId());

    FirebaseFirestore.getInstance()
        .runTransaction(
            transaction -> {
              DocumentSnapshot snapshot = transaction.get(commentRef);
              boolean isLiked = snapshot.contains("likes." + currentUserId);

              Map<String, Object> updates = new HashMap<>();
              if (isLiked) {
                updates.put("likes." + currentUserId, FieldValue.delete());
                updates.put("likeCount", FieldValue.increment(-1));
              } else {
                updates.put("likes." + currentUserId, true);
                updates.put("likeCount", FieldValue.increment(1));
              }

              transaction.update(commentRef, updates);
              return null;
            });
  }

  private void handleReplyToComment(CommentModel comment) {
    TextView replyHeader = findViewById(R.id.replyHeader);
    replyHeader.setVisibility(View.VISIBLE);

    if (comment.isReplyToAuthor(post.getAuthorId())) {
      replyHeader.setText(getString(R.string.replying_to_author));
    } else {
      replyHeader.setText(getString(R.string.replying_to_user, comment.getAuthorUsername()));
    }

    // Store parent comment ID for reply threading
    // You'll need to modify your reply logic to use this

    parentCommentId = comment.getCommentId();
    parentCommentAuthorId = comment.getAuthorId();
    parentCommentAuthorUsername = comment.getAuthorUsername();
  }

  private void updatePostButtonState() {
    EditText postInput = findViewById(R.id.postInput);
    MaterialButton postButton = findViewById(R.id.postButton);

    boolean hasText = !postInput.getText().toString().trim().isEmpty();
    boolean hasMedia = selectedMediaUri != null;
    postButton.setEnabled(hasText || hasMedia);
  }

  private void openMediaChooser() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("image/*");
    startActivityForResult(intent, PICK_IMAGE_REQUEST);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
      selectedMediaUri = data.getData();
      showMediaPreview();
      updatePostButtonState();
    }
  }

  private void showMediaPreview() {
    View mediaPreview = findViewById(R.id.mediaPreview);
    ImageView previewImage = mediaPreview.findViewById(R.id.ivMedia);

    mediaPreview.setVisibility(View.VISIBLE);
    Glide.with(this).load(selectedMediaUri).into(previewImage);

    // Add remove button functionality
    ImageButton btnRemove = mediaPreview.findViewById(R.id.btnRemove);
    btnRemove.setOnClickListener(
        v -> {
          selectedMediaUri = null;
          mediaPreview.setVisibility(View.GONE);
          updatePostButtonState();
        });
  }

  private void postReply() {
    EditText postInput = findViewById(R.id.postInput);
    MaterialButton postButton = findViewById(R.id.postButton);
    String text = postInput.getText().toString().trim();

    if (text.isEmpty() && selectedMediaUri == null) {
      Toast.makeText(this, "Please enter text or add media", Toast.LENGTH_SHORT).show();
      return;
    }

    postButton.setEnabled(false);
    showProgress(true);

    if (selectedMediaUri != null) {
      uploadMediaAndPostReply(text);
    } else {
      saveReplyToFirestore(text, null);
    }
  }

  private void uploadMediaAndPostReply(String text) {
    StorageReference storageRef =
        FirebaseStorage.getInstance()
            .getReference()
            .child("replies/" + currentUser.getUid() + "/" + System.currentTimeMillis());

    storageRef
        .putFile(selectedMediaUri)
        .addOnSuccessListener(
            taskSnapshot -> {
              storageRef
                  .getDownloadUrl()
                  .addOnSuccessListener(
                      uri -> {
                        saveReplyToFirestore(text, uri.toString());
                      });
            })
        .addOnFailureListener(
            e -> {
              showProgress(false);
              Toast.makeText(this, "Media upload failed", Toast.LENGTH_SHORT).show();
            });
  }

  private void saveReplyToFirestore(String text, String mediaUrl) {
    Map<String, Object> reply = new HashMap<>();
    reply.put("content", text);
    reply.put("authorId", currentUser.getUid());
    reply.put("author_display_name", currentUserDisplayName);
    reply.put("author_username", currentUsername);
    reply.put("author_avatar_url", currentUserAvatarUrl);
    reply.put("parent_post_id", post.getPostId());
    reply.put("timestamp", FieldValue.serverTimestamp());
    reply.put("createdAt", new Date());

    if (mediaUrl != null) {
      reply.put("mediaUrls", Collections.singletonList(mediaUrl));
    }

    if (parentCommentId != null) {
      reply.put("parent_comment_id", parentCommentId);
      reply.put("parent_author_id", parentCommentAuthorId);
      reply.put("parent_author_username", parentCommentAuthorUsername);
    }

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    db.collection("posts")
        .document(post.getPostId())
        .collection("comments")
        .add(reply)
        .addOnSuccessListener(
            documentReference -> {
              db.collection("posts")
                  .document(post.getPostId())
                  .update("replyCount", FieldValue.increment(1));
              clearInput();
              Toast.makeText(this, "Reply posted", Toast.LENGTH_SHORT).show();
              post.setReplyCount(post.getReplyCount() + 1);
              updateEngagementMetrics(); // Refresh UI
              loadReplies();
            })
        .addOnFailureListener(
            e -> {
              showProgress(false);
              Toast.makeText(this, "Failed to post reply", Toast.LENGTH_SHORT).show();
            });
  }

  private void showProgress(boolean show) {
    ProgressBar progressBar = findViewById(R.id.progressBar);
    MaterialButton postButton = findViewById(R.id.postButton);

    if (progressBar != null) {
      progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    postButton.setVisibility(show ? View.GONE : View.VISIBLE);
  }

  private void clearInput() {
    EditText postInput = findViewById(R.id.postInput);
    View mediaPreview = findViewById(R.id.mediaPreview);

    postInput.setText("");
    mediaPreview.setVisibility(View.GONE);
    selectedMediaUri = null;
    updatePostButtonState();
    showProgress(false);
  }

  private void loadReplies() {
    // Refresh replies list
  }

  private void updateCharCounter(int currentLength) {
    charCounter.setText(String.valueOf(MAX_CHARS - currentLength));
    charCounter.setTextColor(
        ContextCompat.getColor(
            this, (MAX_CHARS - currentLength) < 0 ? R.color.red : R.color.text_secondary));
  }

  private void toggleLike() {
    post.toggleLike();
    // Update server here
  }

  private void toggleRepost() {
    post.toggleRepost();
    // Update server here
  }

  private void toggleBookmark() {
    post.toggleBookmark();
    // Update server here
  }

  private void openComments() {
    NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
  }

  private void showPostMenu(View anchor) {
    // Implement post menu
  }

  private void openWebPage(String url) {
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (Exception e) {
      Toast.makeText(this, "Couldn't open link", Toast.LENGTH_SHORT).show();
    }
  }

  private String formatCount(long count) {
    if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000f);
    if (count >= 1_000) return String.format("%.1fK", count / 1_000f);
    return String.valueOf(count);
  }

  private String getFormattedDateTime(Date date) {
    if (date == null) {
      return "Just now";
    }
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    return sdf.format(date);
  }

  @Override
  public void onModeratePost(PostModel post) {
    // Implement post moderation logic
    // Example:
    //    Intent intent = new Intent(this, ModerationActivity.class);
    //    intent.putExtra("post", post);
    //    startActivity(intent);
  }

  @Override
  public void onHashtagClicked(String hashtag) {
    // Implement hashtag search
  }

  @Override
  public void onLikeClicked(PostModel post) {
    /* Handled directly */
  }

  @Override
  public void onCommentClicked(PostModel post) {
    /* Handled directly */
  }

  @Override
  public void onRepostClicked(PostModel post) {
    // Implement repost functionality
  }

  @Override
  public void onBookmarkClicked(PostModel post) {
    // Implement bookmark functionality
  }

  @Override
  public void onPostLongClicked(PostModel post) {
    // Handle long click
  }

  @Override
  public void onMediaClicked(List<String> mediaUrls, int position) {
    // Handle media click
  }

  @Override
  public void onVideoPlayClicked(String videoUrl) {
    // Handle video play
  }

  @Override
  public void onEditPost(PostModel post) {
    // Implement post editing
    Intent intent = new Intent(this, ComposePostActivity.class);
    intent.putExtra("post", post);
    startActivity(intent);
  }

  @Override
  public void onToggleBookmark(PostModel post) {
    // Handle bookmark toggle
    toggleBookmark();
  }

  // Add these at the bottom of the class
  @Override
  public void onFollowClicked(UserModel user) {
    // Empty implementation or actual logic
  }

  @Override
  public void onUserClicked(UserModel user) {
    // Empty implementation or actual logic
  }

  @Override
  public void onShowOriginalClicked(PostModel post) {
    // Implement logic to show original content
  }

  @Override
  public void onTranslateClicked(PostModel post) {
    // Implement translation logic
  }

  @Override
  public void onSeeMoreClicked(PostModel post) {
    // Implement expand content logic
  }

  //  private class MediaInteractionListenerImpl implements
  // PostMediaAdapter.MediaInteractionListener {
  //    @Override
  //    public void onImageClicked(String imageUrl, int position) {
  //      // Open media viewer
  //    }
  //
  //    @Override
  //    public void onVideoClicked(String videoUrl) {
  //      // Play video
  //    }
  //  }

  @Override
  public void onLayoutClicked(PostModel post) {
    // Already viewing post details, no action needed
    // Could optionally refresh if needed
  }
}
