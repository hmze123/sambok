package com.spidroid.starry.ui.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ItemMessageFileBinding;
import com.spidroid.starry.databinding.ItemMessageImageReceivedBinding;
import com.spidroid.starry.databinding.ItemMessageImageSentBinding;
import com.spidroid.starry.databinding.ItemMessagePollBinding;
import com.spidroid.starry.databinding.ItemMessageTextReceivedBinding;
import com.spidroid.starry.databinding.ItemMessageTextSentBinding;
import com.spidroid.starry.databinding.ItemMessageVideoReceivedBinding;
import com.spidroid.starry.databinding.ItemMessageVideoSentBinding;
import com.spidroid.starry.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_TEXT_SENT = 1;
  private static final int VIEW_TYPE_TEXT_RECEIVED = 2;
  private static final int VIEW_TYPE_IMAGE_SENT = 3;
  private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
  private static final int VIEW_TYPE_VIDEO_SENT = 5;
  private static final int VIEW_TYPE_VIDEO_RECEIVED = 6;
  private static final int VIEW_TYPE_POLL = 7;
  private static final int VIEW_TYPE_FILE = 8;
  private static final int VIEW_TYPE_RECORDING_SENT = 9;
  private static final int VIEW_TYPE_RECORDING_RECEIVED = 10;

  private final String currentUserId;
  private RecyclerView recyclerView;
  private final MessageClickListener listener;
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

  public MessageAdapter(String currentUserId, MessageClickListener listener) {
    super(new MessageDiffCallback());
    this.currentUserId = currentUserId;
    this.listener = listener;
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    this.recyclerView = recyclerView;
  }

  @Override
  public int getItemViewType(int position) {
    ChatMessage message = getItem(position);
    boolean isSent = message.getSenderId().equals(currentUserId);

    switch (message.getType()) {
      case ChatMessage.TYPE_TEXT:
        return isSent ? VIEW_TYPE_TEXT_SENT : VIEW_TYPE_TEXT_RECEIVED;
      case ChatMessage.TYPE_IMAGE:
      case ChatMessage.TYPE_GIF:
        return isSent ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_IMAGE_RECEIVED;
      case ChatMessage.TYPE_VIDEO:
        return isSent ? VIEW_TYPE_VIDEO_SENT : VIEW_TYPE_VIDEO_RECEIVED;
      case ChatMessage.TYPE_POLL:
        return VIEW_TYPE_POLL;
      case ChatMessage.TYPE_FILE:
        return VIEW_TYPE_FILE;
      default:
        return isSent ? VIEW_TYPE_TEXT_SENT : VIEW_TYPE_TEXT_RECEIVED;
    }
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());

    switch (viewType) {
      case VIEW_TYPE_TEXT_SENT:
        return new SentTextViewHolder(ItemMessageTextSentBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_TEXT_RECEIVED:
        return new ReceivedTextViewHolder(
            ItemMessageTextReceivedBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_IMAGE_SENT:
        return new SentImageViewHolder(
            ItemMessageImageSentBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_IMAGE_RECEIVED:
        return new ReceivedImageViewHolder(
            ItemMessageImageReceivedBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_VIDEO_SENT:
        return new SentVideoViewHolder(
            ItemMessageVideoSentBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_VIDEO_RECEIVED:
        return new ReceivedVideoViewHolder(
            ItemMessageVideoReceivedBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_POLL:
        return new PollViewHolder(ItemMessagePollBinding.inflate(inflater, parent, false));
      case VIEW_TYPE_FILE:
        return new FileViewHolder(ItemMessageFileBinding.inflate(inflater, parent, false));
      default:
        return new SentTextViewHolder(ItemMessageTextSentBinding.inflate(inflater, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ChatMessage message = getItem(position);

    switch (holder.getItemViewType()) {
      case VIEW_TYPE_TEXT_SENT:
        ((SentTextViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_TEXT_RECEIVED:
        ((ReceivedTextViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_IMAGE_SENT:
        ((SentImageViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_IMAGE_RECEIVED:
        ((ReceivedImageViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_VIDEO_SENT:
        ((SentVideoViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_VIDEO_RECEIVED:
        ((ReceivedVideoViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_POLL:
        ((PollViewHolder) holder).bind(message);
        break;
      case VIEW_TYPE_FILE:
        ((FileViewHolder) holder).bind(message);
        break;
    }
  }

  private static class MessageDiffCallback extends DiffUtil.ItemCallback<ChatMessage> {
    @Override
    public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
      return oldItem.getMessageId().equals(newItem.getMessageId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
      // Check ALL relevant fields
      return oldItem.getContent().equals(newItem.getContent())
          && oldItem.getTimestamp().equals(newItem.getTimestamp())
          && oldItem.isEdited() == newItem.isEdited()
          && oldItem.getType().equals(newItem.getType());
    }
  }

  abstract class BaseViewHolder extends RecyclerView.ViewHolder {
    BaseViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    abstract void bind(ChatMessage message);
  }

  class SentTextViewHolder extends BaseViewHolder {
    public final ItemMessageTextSentBinding binding;

    SentTextViewHolder(ItemMessageTextSentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      binding.textContent.setText(message.getContent());
      binding.textTime.setText(timeFormat.format(message.getTimestamp()));

      binding.editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

      if (message.getReadReceipts().values().stream().anyMatch(b -> b)) {
        binding.statusIndicator.setImageResource(R.drawable.ic_read);
      } else if (message.getTimestamp() != null) {
        binding.statusIndicator.setImageResource(R.drawable.ic_sent);
      }

      binding
          .getRoot()
          .setOnLongClickListener(
              v -> {
                listener.onMessageLongClick(message, getAdapterPosition());
                return true;
              });

      if (message.getReplyToId() != null) {
        binding.replyPreview.setVisibility(View.VISIBLE);
        binding.replyPreview.setText(message.getReplyPreview());
        binding.replyPreview.setOnClickListener(v -> listener.onReplyClick(message.getReplyToId()));
      } else {
        binding.replyPreview.setVisibility(View.GONE);
      }
      binding.editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
      binding.textTime.setText(formatTime(message.getTimestamp(), message.isEdited()));
    }

    private String formatTime(Date timestamp, boolean edited) {
      String time = timeFormat.format(timestamp);
      return edited ? time + " (edited)" : time;
    }
  }

  class ReceivedTextViewHolder extends BaseViewHolder {
    public final ItemMessageTextReceivedBinding binding;

    ReceivedTextViewHolder(ItemMessageTextReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      binding.textContent.setText(message.getContent());
      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.textSender.setText(message.getSenderName());
      binding.editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

      binding
          .getRoot()
          .setOnLongClickListener(
              v -> {
                listener.onMessageLongClick(message, getAdapterPosition());
                return true;
              });

      if (message.getReplyToId() != null) {
        binding.replyPreview.setVisibility(View.VISIBLE);
        binding.replyPreview.setText(message.getReplyPreview());
        binding.replyPreview.setOnClickListener(v -> listener.onReplyClick(message.getReplyToId()));
      } else {
        binding.replyPreview.setVisibility(View.GONE);
      }

      Glide.with(itemView)
          .load(message.getSenderAvatar())
          .circleCrop()
          .placeholder(R.drawable.ic_default_avatar)
          .into(binding.avatar);
    }
  }

  class SentImageViewHolder extends BaseViewHolder {
    public final ItemMessageImageSentBinding binding;

    SentImageViewHolder(ItemMessageImageSentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      String imageUrl =
          message.getThumbnailUrl() != null ? message.getThumbnailUrl() : message.getMediaUrl();

      Glide.with(itemView)
          .load(imageUrl)
          .placeholder(R.drawable.ic_cover_placeholder)
          .error(R.drawable.ic_cover_placeholder)
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(binding.imageContent);

      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.statusIndicator.setImageResource(
          message.isRead(currentUserId) ? R.drawable.ic_read : R.drawable.ic_sent);

      binding.imageContent.setOnClickListener(
          v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
              listener.onMediaClick(message.getMediaUrl(), getAdapterPosition());
            }
          });

      binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);
    }
  }

  class ReceivedImageViewHolder extends BaseViewHolder {
    public final ItemMessageImageReceivedBinding binding;

    ReceivedImageViewHolder(ItemMessageImageReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      String imageUrl =
          message.getThumbnailUrl() != null ? message.getThumbnailUrl() : message.getMediaUrl();

      Glide.with(itemView)
          .load(imageUrl)
          .placeholder(R.drawable.ic_cover_placeholder)
          .error(R.drawable.ic_cover_placeholder)
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(binding.imageContent);

      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.imageContent.setOnClickListener(
          v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
              listener.onMediaClick(message.getMediaUrl(), getAdapterPosition());
            }
          });

      Glide.with(itemView)
          .load(message.getSenderAvatar())
          .circleCrop()
          .placeholder(R.drawable.ic_default_avatar)
          .into(binding.avatar);
    }
  }

  class SentVideoViewHolder extends BaseViewHolder {
    public final ItemMessageVideoSentBinding binding;

    SentVideoViewHolder(ItemMessageVideoSentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      Glide.with(itemView)
          .load(message.getThumbnailUrl())
          .placeholder(R.drawable.ic_cover_placeholder)
          .error(R.drawable.ic_cover_placeholder)
          .into(binding.videoThumbnail);

      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.statusIndicator.setImageResource(
          message.isRead(currentUserId) ? R.drawable.ic_read : R.drawable.ic_sent);

      binding.playButton.setOnClickListener(
          v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
              listener.onMediaClick(message.getMediaUrl(), getAdapterPosition());
            }
          });

      binding.durationText.setText(formatDuration(message.getVideoDuration()));
      binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);
    }

    private String formatDuration(long milliseconds) {
      long seconds = milliseconds / 1000;
      return String.format(Locale.getDefault(), "%02d:%02d", (seconds % 3600) / 60, seconds % 60);
    }
  }

  class ReceivedVideoViewHolder extends BaseViewHolder {
    public final ItemMessageVideoReceivedBinding binding;

    ReceivedVideoViewHolder(ItemMessageVideoReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      Glide.with(itemView)
          .load(message.getThumbnailUrl())
          .placeholder(R.drawable.ic_cover_placeholder)
          .error(R.drawable.ic_cover_placeholder)
          .into(binding.videoThumbnail);

      binding.textSender.setText(message.getSenderName());
      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.durationText.setText(formatDuration(message.getVideoDuration()));

      binding.playButton.setOnClickListener(
          v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
              listener.onMediaClick(message.getMediaUrl(), getAdapterPosition());
            }
          });

      Glide.with(itemView)
          .load(message.getSenderAvatar())
          .circleCrop()
          .placeholder(R.drawable.ic_default_avatar)
          .into(binding.avatar);
    }

    private String formatDuration(long milliseconds) {
      long seconds = milliseconds / 1000;
      return String.format(Locale.getDefault(), "%02d:%02d", (seconds % 3600) / 60, seconds % 60);
    }
  }

  class PollViewHolder extends BaseViewHolder {
    public final ItemMessagePollBinding binding;

    PollViewHolder(ItemMessagePollBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      binding.pollQuestion.setText(message.getContent());
      setupPollOptions(message);
      binding.pollTotalVotes.setText(
          itemView.getContext().getString(R.string.total_votes, message.getPoll().getTotalVotes()));
      binding.pollTime.setText(timeFormat.format(message.getTimestamp()));

      if (message.getPoll().isExpired()) {
        binding.pollExpired.setVisibility(View.VISIBLE);
      }
    }

    private void setupPollOptions(ChatMessage message) {
      List<ChatMessage.PollOption> options = message.getPoll().getOptions();
      if (options.size() > 0) {
        binding.option1Button.setText(options.get(0).getText());
      }
      if (options.size() > 1) {
        binding.option2Button.setText(options.get(1).getText());
      }
      updatePollResults(message);

      if (message.getPoll().isVoted() || message.getPoll().isExpired()) {
        disableVoting();
      } else {
        setVoteClickListeners(message.getMessageId());
      }
    }

    private void updatePollResults(ChatMessage message) {
      int total = message.getPoll().getTotalVotes();
      if (total > 0) {
        updateOptionProgress(binding.option1Progress, message.getPoll().getOptions().get(0), total);
        if (message.getPoll().getOptions().size() > 1) {
          updateOptionProgress(
              binding.option2Progress, message.getPoll().getOptions().get(1), total);
        }
      }
    }

    private void updateOptionProgress(
        ProgressBar progressBar, ChatMessage.PollOption option, int total) {
      int percentage = (int) ((option.getVotes() / (float) total) * 100);
      progressBar.setProgress(percentage);
      progressBar.setVisibility(View.VISIBLE);
    }

    private void disableVoting() {
      binding.option1Button.setEnabled(false);
      binding.option2Button.setEnabled(false);
    }

    private void setVoteClickListeners(String pollId) {
      binding.option1Button.setOnClickListener(v -> listener.onPollVote(pollId, 0));
      binding.option2Button.setOnClickListener(v -> listener.onPollVote(pollId, 1));
    }
  }

  class FileViewHolder extends BaseViewHolder {
    public final ItemMessageFileBinding binding;

    FileViewHolder(ItemMessageFileBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      binding.fileName.setText(message.getFileName());
      binding.fileSize.setText(formatFileSize(message.getFileSize()));
      binding.fileIcon.setImageResource(getFileIcon(message.getFileType()));
      binding.textTime.setText(timeFormat.format(message.getTimestamp()));
      binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);

      binding.downloadButton.setOnClickListener(
          v -> listener.onFileClick(message.getMediaUrl())); // Using mediaUrl for file URL

      binding.statusIndicator.setImageResource(
          message.isRead(currentUserId) ? R.drawable.ic_read : R.drawable.ic_sent);
    }

    private String formatFileSize(long size) {
      if (size <= 0) return "0 B";
      String[] units = {"B", "KB", "MB", "GB"};
      int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
      return String.format(
          Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private int getFileIcon(String mimeType) {
      if (mimeType == null) return R.drawable.ic_file_generic;

      String primaryType = mimeType.split("/")[0];
      switch (primaryType) {
        case "image":
          return R.drawable.ic_file_image;
        case "video":
          return R.drawable.ic_file_video;
        case "audio":
          return R.drawable.ic_file_audio;
        case "application":
          return mimeType.equals("application/pdf")
              ? R.drawable.ic_file_pdf
              : R.drawable.ic_file_generic;
        default:
          return R.drawable.ic_file_generic;
      }
    }
  }

  public void cleanup() {
    for (RecyclerView.ViewHolder holder : getRecyclerViewHolders()) {
      if (holder instanceof SentImageViewHolder) {
        Glide.with(holder.itemView).clear(((SentImageViewHolder) holder).binding.imageContent);
      }
      if (holder instanceof ReceivedImageViewHolder) {
        Glide.with(holder.itemView).clear(((ReceivedImageViewHolder) holder).binding.imageContent);
      }
      if (holder instanceof SentVideoViewHolder) {
        Glide.with(holder.itemView).clear(((SentVideoViewHolder) holder).binding.videoThumbnail);
      }
      if (holder instanceof ReceivedVideoViewHolder) {
        Glide.with(holder.itemView)
            .clear(((ReceivedVideoViewHolder) holder).binding.videoThumbnail);
      }
    }
  }

  // Method to get all active view holders
  private List<RecyclerView.ViewHolder> getRecyclerViewHolders() {
    List<RecyclerView.ViewHolder> holders = new ArrayList<>();
    if (recyclerView == null) return holders;

    for (int i = 0; i < getItemCount(); i++) {
      RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
      if (holder != null) {
        holders.add(holder);
      }
    }
    return holders;
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
    super.onViewRecycled(holder);
    if (holder instanceof SentImageViewHolder) {
      Glide.with(holder.itemView).clear(((SentImageViewHolder) holder).binding.imageContent);
    }
    if (holder instanceof ReceivedImageViewHolder) {
      Glide.with(holder.itemView).clear(((ReceivedImageViewHolder) holder).binding.imageContent);
    }
    if (holder instanceof SentVideoViewHolder) {
      Glide.with(holder.itemView).clear(((SentVideoViewHolder) holder).binding.videoThumbnail);
    }
    if (holder instanceof ReceivedVideoViewHolder) {
      Glide.with(holder.itemView).clear(((ReceivedVideoViewHolder) holder).binding.videoThumbnail);
    }
  }

  public interface MessageClickListener {
    void onMessageLongClick(ChatMessage message, int position);

    void onMediaClick(String mediaUrl, int position);

    void onReplyClick(String messageId);

    void onPollVote(String pollId, int option);

    void onFileClick(String fileUrl);
  }
}
