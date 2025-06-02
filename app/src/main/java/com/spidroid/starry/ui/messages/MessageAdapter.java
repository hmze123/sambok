package com.spidroid.starry.ui.messages;

import android.content.Context;
import android.util.Log;
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
// import com.spidroid.starry.databinding.ItemMessageRecordingSentBinding; // إذا كنت تستخدمه
// import com.spidroid.starry.databinding.ItemMessageRecordingReceivedBinding; // إذا كنت تستخدمه
import com.spidroid.starry.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

  public static final int VIEW_TYPE_TEXT_SENT = 1;
  public static final int VIEW_TYPE_TEXT_RECEIVED = 2;
  public static final int VIEW_TYPE_IMAGE_SENT = 3;
  public static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
  public static final int VIEW_TYPE_VIDEO_SENT = 5;
  public static final int VIEW_TYPE_VIDEO_RECEIVED = 6;
  public static final int VIEW_TYPE_POLL = 7;
  public static final int VIEW_TYPE_FILE = 8;
  public static final int VIEW_TYPE_RECORDING_SENT = 9;
  public static final int VIEW_TYPE_RECORDING_RECEIVED = 10;

  private final String currentUserId;
  private RecyclerView recyclerView;
  private final MessageClickListener listener;
  private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
  private final Context context;

  public MessageAdapter(String currentUserId, Context context, MessageClickListener listener) {
    super(new MessageDiffCallback());
    this.currentUserId = currentUserId;
    this.context = context;
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
    if (message == null || message.getSenderId() == null || message.getType() == null) {
      Log.e("MessageAdapter", "Message or its critical fields are null at position: " + position + ". Returning default view type.");
      return VIEW_TYPE_TEXT_RECEIVED; // نوع افتراضي لتجنب التعطل
    }
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
      // case ChatMessage.TYPE_RECORDING:
      //   return isSent ? VIEW_TYPE_RECORDING_SENT : VIEW_TYPE_RECORDING_RECEIVED;
      default:
        Log.w("MessageAdapter", "Unknown message type: " + message.getType() + " at position: " + position);
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
      // case VIEW_TYPE_RECORDING_SENT:
      //   return new SentRecordingViewHolder(ItemMessageRecordingSentBinding.inflate(inflater, parent, false));
      // case VIEW_TYPE_RECORDING_RECEIVED:
      //   return new ReceivedRecordingViewHolder(ItemMessageRecordingReceivedBinding.inflate(inflater, parent, false));
      default:
        Log.e("MessageAdapter", "onCreateViewHolder received unknown viewType: " + viewType);
        return new SentTextViewHolder(ItemMessageTextSentBinding.inflate(inflater, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ChatMessage message = getItem(position);
    if (message == null) {
      Log.e("MessageAdapter", "Message is null in onBindViewHolder at position: " + position);
      holder.itemView.setVisibility(View.GONE);
      return;
    }
    holder.itemView.setVisibility(View.VISIBLE);

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
      // case VIEW_TYPE_RECORDING_SENT:
      //  ((SentRecordingViewHolder) holder).bind(message);
      //  break;
      // case VIEW_TYPE_RECORDING_RECEIVED:
      //  ((ReceivedRecordingViewHolder) holder).bind(message);
      //  break;
      default:
        Log.e("MessageAdapter", "onBindViewHolder encountered unknown viewType: " + holder.getItemViewType() + " for message: " + (message.getMessageId() != null ? message.getMessageId() : "ID_NULL"));
        holder.itemView.setVisibility(View.GONE);
        break;
    }
  }

  private static class MessageDiffCallback extends DiffUtil.ItemCallback<ChatMessage> {
    @Override
    public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
      if (oldItem.getMessageId() == null || newItem.getMessageId() == null) {
        return oldItem == newItem;
      }
      return oldItem.getMessageId().equals(newItem.getMessageId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
      return Objects.equals(oldItem.getContent(), newItem.getContent())
              && Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp())
              && oldItem.isEdited() == newItem.isEdited()
              && Objects.equals(oldItem.getType(), newItem.getType())
              && oldItem.isUploading() == newItem.isUploading()
              && Objects.equals(oldItem.getMediaUrl(), newItem.getMediaUrl())
              && Objects.equals(oldItem.getReactions(), newItem.getReactions());
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
      if (message == null) {
        Log.e("SentTextViewHolder", "Message object is null in bind.");
        if (binding.textContent != null) binding.textContent.setText("");
        // قد ترغب في إخفاء العنصر بالكامل إذا كانت الرسالة null
        // itemView.setVisibility(View.GONE);
        return;
      }
      // itemView.setVisibility(View.VISIBLE); // تأكد من أن العنصر ظاهر

      Log.d("SentTextViewHolder", "Binding message: " + message.getMessageId() + ", Content: [" + message.getContent() + "]");

      // ★★ إذا كنت تستخدم Data Binding لتعيين النص في XML, فهذا السطر ضروري ★★
      binding.setMessage(message);

      // إذا كنت لا تستخدم Data Binding لـ textContent أو تريد تعيينه برمجيًا أيضًا:
      // if (binding.textContent != null) {
      //     binding.textContent.setText(message.getContent() != null ? message.getContent() : "");
      // }


      if (binding.textTime != null) {
        binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      }
      if (binding.editedIndicator != null) {
        binding.editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
      }
      if (binding.statusIndicator != null) {
        if (message.getReadReceipts() != null && message.getReadReceipts().values().stream().anyMatch(b -> b != null && b)) {
          binding.statusIndicator.setImageResource(R.drawable.ic_read);
        } else {
          binding.statusIndicator.setImageResource(R.drawable.ic_sent);
        }
      }

      binding.getRoot().setOnLongClickListener( v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
          listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        }
        return true;
      });

      if (binding.replyPreview != null) {
        if (message.getReplyToId() != null && message.getReplyPreview() != null) {
          binding.replyPreview.setVisibility(View.VISIBLE);
          binding.replyPreview.setText(message.getReplyPreview()); // يتم تعيينه برمجيًا
          binding.replyPreview.setOnClickListener(v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
              listener.onReplyClick(getItem(getAdapterPosition()).getReplyToId());
            }
          });
        } else {
          binding.replyPreview.setVisibility(View.GONE);
        }
      }
    }
  }

  class ReceivedTextViewHolder extends BaseViewHolder {
    public final ItemMessageTextReceivedBinding binding;

    ReceivedTextViewHolder(ItemMessageTextReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    void bind(ChatMessage message) {
      if (message == null) {
        Log.e("ReceivedTextViewHolder", "Message object is null in bind.");
        if (binding.textContent != null) binding.textContent.setText("");
        if (binding.textSender != null) binding.textSender.setText("");
        return;
      }
      Log.d("ReceivedTextViewHolder", "Binding message: " + message.getMessageId() + ", Content: [" + message.getContent() + "], Sender: [" + message.getSenderName() + "]");

      // ★★ إذا كنت تستخدم Data Binding لتعيين النص في XML, فهذا السطر ضروري ★★
      binding.setMessage(message);

      // إذا كنت لا تستخدم Data Binding لـ textContent أو تريد تعيينه برمجيًا أيضًا:
      // if (binding.textContent != null) {
      //    binding.textContent.setText(message.getContent() != null ? message.getContent() : "");
      // }

      if (binding.textSender != null) {
        binding.textSender.setText(message.getSenderName() != null ? message.getSenderName() : (context != null ? context.getString(R.string.unknown_user_display_name) : "User"));
      }
      if (binding.textTime != null) {
        binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      }
      if (binding.editedIndicator != null) {
        binding.editedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
      }

      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
          listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        }
        return true;
      });

      if (binding.replyPreview != null) {
        if (message.getReplyToId() != null && message.getReplyPreview() != null) {
          binding.replyPreview.setVisibility(View.VISIBLE);
          binding.replyPreview.setText(message.getReplyPreview()); // يتم تعيينه برمجيًا
          binding.replyPreview.setOnClickListener(v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
              listener.onReplyClick(getItem(getAdapterPosition()).getReplyToId());
            }
          });
        } else {
          binding.replyPreview.setVisibility(View.GONE);
        }
      }

      if (context != null && binding.avatar != null) {
        if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) {
          Glide.with(context)
                  .load(message.getSenderAvatar())
                  .circleCrop()
                  .placeholder(R.drawable.ic_default_avatar)
                  .error(R.drawable.ic_default_avatar)
                  .into(binding.avatar);
        } else {
          binding.avatar.setImageResource(R.drawable.ic_default_avatar);
        }
      }
    }
  }

  class SentImageViewHolder extends BaseViewHolder {
    public final ItemMessageImageSentBinding binding;
    SentImageViewHolder(ItemMessageImageSentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      String imageUrl = message.getThumbnailUrl() != null ? message.getThumbnailUrl() : message.getMediaUrl();
      if (context != null && binding.imageContent != null) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
          Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_cover_placeholder).error(R.drawable.ic_cover_placeholder).transition(DrawableTransitionOptions.withCrossFade()).into(binding.imageContent);
        } else {
          binding.imageContent.setImageResource(R.drawable.ic_cover_placeholder);
        }
      }
      if (binding.textTime != null) binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.statusIndicator != null) {
        if (message.getReadReceipts() != null && message.getReadReceipts().values().stream().anyMatch(b -> b != null && b)) binding.statusIndicator.setImageResource(R.drawable.ic_read);
        else binding.statusIndicator.setImageResource(R.drawable.ic_sent);
      }
      if (binding.imageContent != null) binding.imageContent.setOnClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(getAdapterPosition()).getMediaUrl() != null) listener.onMediaClick(getItem(getAdapterPosition()).getMediaUrl(), getAdapterPosition());
      });
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
      if (binding.progressBar != null) binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);
    }
  }

  class ReceivedImageViewHolder extends BaseViewHolder {
    public final ItemMessageImageReceivedBinding binding;
    ReceivedImageViewHolder(ItemMessageImageReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      String imageUrl = message.getThumbnailUrl() != null ? message.getThumbnailUrl() : message.getMediaUrl();
      if (context != null && binding.imageContent != null) {
        if (imageUrl != null && !imageUrl.isEmpty()) Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_cover_placeholder).error(R.drawable.ic_cover_placeholder).transition(DrawableTransitionOptions.withCrossFade()).into(binding.imageContent);
        else binding.imageContent.setImageResource(R.drawable.ic_cover_placeholder);
      }
      if (binding.textTime != null) binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.imageContent != null) binding.imageContent.setOnClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(getAdapterPosition()).getMediaUrl() != null) listener.onMediaClick(getItem(getAdapterPosition()).getMediaUrl(), getAdapterPosition());
      });
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
      if (context != null && binding.avatar != null) {
        if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) Glide.with(context).load(message.getSenderAvatar()).circleCrop().placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar).into(binding.avatar);
        else binding.avatar.setImageResource(R.drawable.ic_default_avatar);
      }
    }
  }

  class SentVideoViewHolder extends BaseViewHolder {
    public final ItemMessageVideoSentBinding binding;
    SentVideoViewHolder(ItemMessageVideoSentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      if (context != null && binding.videoThumbnail != null ) {
        if (message.getThumbnailUrl() != null && !message.getThumbnailUrl().isEmpty()) Glide.with(context).load(message.getThumbnailUrl()).placeholder(R.drawable.ic_cover_placeholder).error(R.drawable.ic_cover_placeholder).into(binding.videoThumbnail);
        else binding.videoThumbnail.setImageResource(R.drawable.ic_cover_placeholder);
      }
      if (binding.textTime != null) binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.statusIndicator != null) {
        if (message.getReadReceipts() != null && message.getReadReceipts().values().stream().anyMatch(b -> b != null && b)) binding.statusIndicator.setImageResource(R.drawable.ic_read);
        else binding.statusIndicator.setImageResource(R.drawable.ic_sent);
      }
      if (binding.playButton != null) binding.playButton.setOnClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(getAdapterPosition()).getMediaUrl() != null) listener.onMediaClick(getItem(getAdapterPosition()).getMediaUrl(), getAdapterPosition());
      });
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
      if (binding.durationText != null) binding.durationText.setText(formatDuration(message.getVideoDuration()));
      if (binding.progressBar != null) binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);
    }
    private String formatDuration(long milliseconds) {
      if (milliseconds <= 0) return "0:00";
      long seconds = (milliseconds / 1000) % 60;
      long minutes = (milliseconds / (1000 * 60)) % 60;
      return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
    }
  }

  class ReceivedVideoViewHolder extends BaseViewHolder {
    public final ItemMessageVideoReceivedBinding binding;
    ReceivedVideoViewHolder(ItemMessageVideoReceivedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      if (context != null && binding.videoThumbnail != null ) {
        if (message.getThumbnailUrl() != null && !message.getThumbnailUrl().isEmpty()) Glide.with(context).load(message.getThumbnailUrl()).placeholder(R.drawable.ic_cover_placeholder).error(R.drawable.ic_cover_placeholder).into(binding.videoThumbnail);
        else binding.videoThumbnail.setImageResource(R.drawable.ic_cover_placeholder);
      }
      if (binding.textSender != null) binding.textSender.setText(message.getSenderName() != null ? message.getSenderName() : "User");
      if (binding.textTime != null) binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.durationText != null) binding.durationText.setText(formatDuration(message.getVideoDuration()));
      if (binding.playButton != null) binding.playButton.setOnClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(getAdapterPosition()).getMediaUrl() != null) listener.onMediaClick(getItem(getAdapterPosition()).getMediaUrl(), getAdapterPosition());
      });
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
      if (context != null && binding.avatar != null) {
        if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) Glide.with(context).load(message.getSenderAvatar()).circleCrop().placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar).into(binding.avatar);
        else binding.avatar.setImageResource(R.drawable.ic_default_avatar);
      }
    }
    private String formatDuration(long milliseconds) {
      if (milliseconds <= 0) return "0:00";
      long seconds = (milliseconds / 1000) % 60;
      long minutes = (milliseconds / (1000 * 60)) % 60;
      return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
    }
  }

  class PollViewHolder extends BaseViewHolder {
    public final ItemMessagePollBinding binding;
    PollViewHolder(ItemMessagePollBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      if (message.getPoll() == null) {
        Log.e("PollViewHolder", "Poll data is null for message: " + (message.getMessageId() != null ? message.getMessageId() : "ID_NULL"));
        itemView.setVisibility(View.GONE);
        return;
      }
      itemView.setVisibility(View.VISIBLE);

      if (binding.pollQuestion != null) binding.pollQuestion.setText(message.getPoll().getQuestion());
      setupPollOptions(message);

      if (binding.pollTotalVotes != null && itemView.getContext() != null) binding.pollTotalVotes.setText(itemView.getContext().getString(R.string.total_votes, message.getPoll().getTotalVotes()));
      if (binding.pollTime != null) binding.pollTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.pollExpired != null) binding.pollExpired.setVisibility(message.getPoll().isExpired() ? View.VISIBLE : View.GONE);
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
    }
    private void setupPollOptions(ChatMessage message) {
      if (message.getPoll() == null || message.getPoll().getOptions() == null) return;
      List<ChatMessage.PollOption> options = message.getPoll().getOptions();
      if (binding.option1Button != null) { binding.option1Button.setText(""); binding.option1Button.setVisibility(View.GONE); }
      if (binding.option1Progress != null) binding.option1Progress.setVisibility(View.GONE);
      if (binding.option2Button != null) { binding.option2Button.setText(""); binding.option2Button.setVisibility(View.GONE); }
      if (binding.option2Progress != null) binding.option2Progress.setVisibility(View.GONE);

      if (options.size() > 0 && options.get(0) != null && binding.option1Button != null) { binding.option1Button.setText(options.get(0).getText()); binding.option1Button.setVisibility(View.VISIBLE); }
      if (options.size() > 1 && options.get(1) != null && binding.option2Button != null) { binding.option2Button.setText(options.get(1).getText()); binding.option2Button.setVisibility(View.VISIBLE); }
      updatePollResults(message);
      if (message.getPoll().isVoted() || message.getPoll().isExpired()) disableVoting();
      else setVoteClickListeners(message.getMessageId());
    }
    private void updatePollResults(ChatMessage message) {
      if (message.getPoll() == null || message.getPoll().getOptions() == null) return;
      int total = message.getPoll().getTotalVotes();
      if (total > 0) {
        if (message.getPoll().getOptions().size() > 0 && message.getPoll().getOptions().get(0) != null && binding.option1Progress != null) updateOptionProgress(binding.option1Progress, message.getPoll().getOptions().get(0), total);
        if (message.getPoll().getOptions().size() > 1 && message.getPoll().getOptions().get(1) != null && binding.option2Progress != null) updateOptionProgress(binding.option2Progress, message.getPoll().getOptions().get(1), total);
      }
    }
    private void updateOptionProgress(ProgressBar progressBar, ChatMessage.PollOption option, int total) {
      if (option == null || total <= 0 || progressBar == null) { if (progressBar != null) progressBar.setVisibility(View.GONE); return; }
      int percentage = (int) ((option.getVotes() / (float) total) * 100);
      progressBar.setProgress(percentage);
      progressBar.setVisibility(View.VISIBLE);
    }
    private void disableVoting() {
      if (binding.option1Button != null) binding.option1Button.setEnabled(false);
      if (binding.option2Button != null) binding.option2Button.setEnabled(false);
    }
    private void setVoteClickListeners(String pollId) {
      if (binding.option1Button != null) binding.option1Button.setOnClickListener(v -> listener.onPollVote(pollId, 0));
      if (binding.option2Button != null) binding.option2Button.setOnClickListener(v -> listener.onPollVote(pollId, 1));
    }
  }

  class FileViewHolder extends BaseViewHolder {
    public final ItemMessageFileBinding binding;
    FileViewHolder(ItemMessageFileBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
    void bind(ChatMessage message) {
      if (binding.fileName != null) binding.fileName.setText(message.getFileName() != null ? message.getFileName() : "File");
      if (binding.fileSize != null) binding.fileSize.setText(formatFileSize(message.getFileSize()));
      if (binding.fileIcon != null) binding.fileIcon.setImageResource(getFileIcon(message.getFileType()));
      if (binding.textTime != null) binding.textTime.setText(timeFormat.format(message.getTimestamp() != null ? message.getTimestamp() : new Date()));
      if (binding.progressBar != null) binding.progressBar.setVisibility(message.isUploading() ? View.VISIBLE : View.GONE);
      if (binding.downloadButton != null) binding.downloadButton.setOnClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(getAdapterPosition()).getMediaUrl() != null) listener.onFileClick(getItem(getAdapterPosition()).getMediaUrl());
      });
      binding.getRoot().setOnLongClickListener(v -> {
        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition());
        return true;
      });
      if (binding.statusIndicator != null) {
        if (message.getReadReceipts() != null && message.getReadReceipts().values().stream().anyMatch(b -> b != null && b)) binding.statusIndicator.setImageResource(R.drawable.ic_read);
        else binding.statusIndicator.setImageResource(R.drawable.ic_sent);
        binding.statusIndicator.setVisibility(View.VISIBLE);
      }
    }
    private String formatFileSize(long size) {
      if (size <= 0) return "0 B";
      String[] units = {"B", "KB", "MB", "GB"};
      int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
      if (digitGroups < 0) digitGroups = 0;
      if (digitGroups >= units.length) digitGroups = units.length - 1;
      return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    private int getFileIcon(String mimeType) {
      if (mimeType == null || mimeType.isEmpty()) return R.drawable.ic_file_generic;
      String primaryType = mimeType.split("/")[0];
      switch (primaryType.toLowerCase()) {
        case "image": return R.drawable.ic_file_image;
        case "video": return R.drawable.ic_file_video;
        case "audio": return R.drawable.ic_file_audio;
        case "application": if (mimeType.equalsIgnoreCase("application/pdf")) return R.drawable.ic_file_pdf;
          return R.drawable.ic_file_generic;
        default: return R.drawable.ic_file_generic;
      }
    }
  }

  public void cleanup() {
    if (recyclerView == null || context == null) return;
    for (int i = 0; i < recyclerView.getChildCount(); i++) {
      View childView = recyclerView.getChildAt(i);
      if (childView == null) continue;
      RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(childView);
      if (holder == null) continue;

      if (holder instanceof SentImageViewHolder && ((SentImageViewHolder) holder).binding != null) {
        Glide.with(context).clear(((SentImageViewHolder) holder).binding.imageContent);
      } else if (holder instanceof ReceivedImageViewHolder && ((ReceivedImageViewHolder) holder).binding != null) {
        Glide.with(context).clear(((ReceivedImageViewHolder) holder).binding.imageContent);
      } else if (holder instanceof SentVideoViewHolder && ((SentVideoViewHolder) holder).binding != null) {
        Glide.with(context).clear(((SentVideoViewHolder) holder).binding.videoThumbnail);
      } else if (holder instanceof ReceivedVideoViewHolder && ((ReceivedVideoViewHolder) holder).binding != null) {
        Glide.with(context).clear(((ReceivedVideoViewHolder) holder).binding.videoThumbnail);
      }
    }
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
    super.onViewRecycled(holder);
    if (context == null) return;
    if (holder instanceof SentImageViewHolder && ((SentImageViewHolder) holder).binding != null) {
      Glide.with(context).clear(((SentImageViewHolder) holder).binding.imageContent);
    } else if (holder instanceof ReceivedImageViewHolder && ((ReceivedImageViewHolder) holder).binding != null) {
      Glide.with(context).clear(((ReceivedImageViewHolder) holder).binding.imageContent);
    } else if (holder instanceof SentVideoViewHolder && ((SentVideoViewHolder) holder).binding != null) {
      Glide.with(context).clear(((SentVideoViewHolder) holder).binding.videoThumbnail);
    } else if (holder instanceof ReceivedVideoViewHolder && ((ReceivedVideoViewHolder) holder).binding != null) {
      Glide.with(context).clear(((ReceivedVideoViewHolder) holder).binding.videoThumbnail);
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