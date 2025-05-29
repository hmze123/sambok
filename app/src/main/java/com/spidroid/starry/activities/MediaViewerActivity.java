package com.spidroid.starry.activities;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.spidroid.starry.R;
import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;

public class MediaViewerActivity extends AppCompatActivity {

  private ViewPager2 viewPager;
  private ArrayList<String> mediaUrls;
  private int currentPosition;
  private ImageButton btnMenu;
  private ProgressBar loadingProgressBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_viewer);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    // Initialize data
    Intent intent = getIntent();
    mediaUrls = intent.getStringArrayListExtra("media_urls");
    currentPosition = intent.getIntExtra("position", 0);

    if (mediaUrls == null || mediaUrls.isEmpty()) {
      Toast.makeText(this, "No media to display", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    initializeViews();
    setupViewPager();
    setupButtonListeners();
  }

  private void initializeViews() {
    viewPager = findViewById(R.id.viewPager);
    btnMenu = findViewById(R.id.btnMenu);
    findViewById(R.id.btnClose).setOnClickListener(v -> supportFinishAfterTransition());
    findViewById(R.id.btnShare).setOnClickListener(v -> shareCurrentMedia());
  }

  private void setupViewPager() {
    MediaPagerAdapter adapter = new MediaPagerAdapter();
    viewPager.setAdapter(adapter);
    viewPager.setCurrentItem(currentPosition, false);
    viewPager.setOffscreenPageLimit(1);

    viewPager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            currentPosition = position;
          }
        });
  }

  private void setupButtonListeners() {
    btnMenu.setOnClickListener(v -> showMenu());
  }

  private class MediaPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_VIDEO = 2;

    @Override
    public int getItemViewType(int position) {
      return isVideo(mediaUrls.get(position)) ? TYPE_VIDEO : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      if (viewType == TYPE_VIDEO) {
        return new VideoViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.media_item_video, parent, false));
      }
      return new ImageViewHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.media_item_image, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      String url = mediaUrls.get(position);
      if (holder instanceof VideoViewHolder) {
        ((VideoViewHolder) holder).bind(url);
      } else if (holder instanceof ImageViewHolder) {
        ((ImageViewHolder) holder).bind(url);
      }
    }

    @Override
    public int getItemCount() {
      return mediaUrls.size();
    }

    private boolean isVideo(String url) {
      try {
        Uri uri = Uri.parse(url);
        String mimeType = URLConnection.guessContentTypeFromName(uri.getLastPathSegment());
        return mimeType != null && mimeType.startsWith("video/");
      } catch (Exception e) {
        return url.matches(".*\\.(mp4|mov|mkv|webm|3gp|avi|m3u8)(\\?.*)?$");
      }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
      private final PhotoView photoView;

      ImageViewHolder(View itemView) {
        super(itemView);
        photoView = itemView.findViewById(R.id.photoView);
        photoView.setOnClickListener(v -> toggleControls());
      }

      void bind(String url) {
        Glide.with(itemView)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_cover_placeholder)
            .into(photoView);
      }
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
      private final PlayerView playerView;
      private ExoPlayer player;
      private final ProgressBar loadingProgressBar;

      VideoViewHolder(View itemView) {
        super(itemView);
        playerView = itemView.findViewById(R.id.playerView);
        loadingProgressBar = itemView.findViewById(R.id.loadingProgressBar);

        playerView.setControllerVisibilityListener(
    (PlayerView.ControllerVisibilityListener) visibility -> {
      if (visibility == View.VISIBLE) showControls();
      else hideControls();
    });
      }

      void bind(String url) {
        initializePlayer();
        loadingProgressBar.setVisibility(View.VISIBLE);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(
            new Player.Listener() {
              @Override
              public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                  loadingProgressBar.setVisibility(View.GONE);
                } else if (state == Player.STATE_BUFFERING) {
                  loadingProgressBar.setVisibility(View.VISIBLE);
                }
              }

              @Override
              public void onPlayerError(@NonNull PlaybackException error) {
                loadingProgressBar.setVisibility(View.GONE);
                Toast.makeText(itemView.getContext(), "Error playing video", Toast.LENGTH_SHORT)
                    .show();
              }
            });

        player.play();
      }

      private void initializePlayer() {
        if (player == null) {
          player = new ExoPlayer.Builder(itemView.getContext()).build();
          player.setRepeatMode(Player.REPEAT_MODE_ONE);
          playerView.setPlayer(player);
        }
      }

      void releasePlayer() {
        if (player != null) {
          player.stop();
          player.release();
          player = null;
        }
      }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
      super.onViewRecycled(holder);
      if (holder instanceof VideoViewHolder) {
        ((VideoViewHolder) holder).releasePlayer();
      }
    }
  }

  private void toggleControls() {
    View controls = findViewById(R.id.topControls);
    if (controls != null) {
      controls.setVisibility(controls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
  }

  private void showControls() {
    View controls = findViewById(R.id.topControls);
    if (controls != null) {
      controls.setVisibility(View.VISIBLE);
    }
  }

  private void hideControls() {
    View controls = findViewById(R.id.topControls);
    if (controls != null) {
      controls.setVisibility(View.GONE);
    }
  }

  private void shareCurrentMedia() {
    if (currentPosition < 0 || currentPosition >= mediaUrls.size()) return;

    String url = mediaUrls.get(currentPosition);
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType(getMimeType(url));
    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
    startActivity(Intent.createChooser(shareIntent, "Share via"));
  }

  private void downloadMedia(String url) {
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    String fileName = "Starry_" + System.currentTimeMillis() + getFileExtension(url);

    request.setDestinationUri(Uri.fromFile(new File(dir, fileName)));
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    if (dm != null) dm.enqueue(request);
  }

  private String getFileExtension(String url) {
    String ext = MimeTypeMap.getFileExtensionFromUrl(url);
    return ext != null && !ext.isEmpty() ? "." + ext : ".mp4";
  }

  private String getMimeType(String url) {
    String type =
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
    return type != null ? type : "image/*";
  }

  private void showMenu() {
    PopupMenu popup = new PopupMenu(this, btnMenu);
    popup.getMenuInflater().inflate(R.menu.menu_media_download, popup.getMenu());
    popup.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.id.action_download) {
            downloadMedia(mediaUrls.get(currentPosition));
            return true;
          }
          return false;
        });
    popup.show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (viewPager != null) {
      RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
      for (int i = 0; i < recyclerView.getChildCount(); i++) {
        RecyclerView.ViewHolder holder =
            recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
        if (holder instanceof MediaPagerAdapter.VideoViewHolder) {
          ((MediaPagerAdapter.VideoViewHolder) holder).releasePlayer();
        }
      }
    }
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }

  public static void launch(Activity activity, ArrayList<String> urls, int pos, View sharedView) {
    Intent intent = new Intent(activity, MediaViewerActivity.class);
    intent.putStringArrayListExtra("media_urls", urls);
    intent.putExtra("position", pos);

    if (sharedView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ActivityOptionsCompat options =
          ActivityOptionsCompat.makeSceneTransitionAnimation(
              activity, sharedView, "media_transition");
      ActivityCompat.startActivity(activity, intent, options.toBundle());
    } else {
      activity.startActivity(intent);
      activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
  }

  public static void launchWithoutTransition(
      Activity activity, ArrayList<String> mediaUrls, int position) {
    Intent intent = new Intent(activity, MediaViewerActivity.class);
    intent.putStringArrayListExtra("media_urls", mediaUrls);
    intent.putExtra("position", Math.max(0, Math.min(position, mediaUrls.size() - 1)));
    activity.startActivity(intent);
    activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }
}
