package com.spidroid.starry.ui.messages;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.databinding.FragmentMessagesBinding;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.ui.messages.NewChatBottomSheetDialog;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesFragment extends Fragment {

  private FragmentMessagesBinding binding;
  private FirebaseAuth auth;
  private ListenerRegistration userListener;

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    binding = FragmentMessagesBinding.inflate(inflater, container, false);
    View root = binding.getRoot();
    auth = FirebaseAuth.getInstance();
    FloatingActionButton fab = binding.fabCreateNew;
    fab.setOnClickListener(v -> showNewChatDialog());
    return root;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupViewPager();
  }

  @Override
  public void onStart() {
    super.onStart();
    setupToolbar();
  }

  private void setupToolbar() {
    CircleImageView ivAvatar = binding.ivUserAvatar;
    FirebaseUser currentUser = auth.getCurrentUser();

    // Set default image first
    ivAvatar.setImageResource(R.drawable.ic_default_avatar);

    if (currentUser != null) {
      // Remove any existing listener to prevent duplicates
      if (userListener != null) userListener.remove();

      // Real-time listener to Firestore user document
      userListener =
          FirebaseFirestore.getInstance()
              .collection("users")
              .document(currentUser.getUid())
              .addSnapshotListener(
                  (documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists())
                      return;

                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null
                        && user.getProfileImageUrl() != null
                        && !user.getProfileImageUrl().isEmpty()) {
                      Glide.with(requireContext())
                          .load(user.getProfileImageUrl())
                          .placeholder(R.drawable.ic_default_avatar)
                          .error(R.drawable.ic_default_avatar)
                          .transition(DrawableTransitionOptions.withCrossFade())
                          .into(ivAvatar);
                    }
                  });
    }

    ivAvatar.setOnClickListener(
        v -> {
          if (currentUser != null) {
            Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
            profileIntent.putExtra("userId", currentUser.getUid());
            startActivity(profileIntent);
          } else {
            Toast.makeText(getContext(), "Please login to view profile", Toast.LENGTH_SHORT).show();
          }
        });
  }

  private void setupViewPager() {
    binding.viewPager.setAdapter(new ViewPagerAdapter(this));

    new TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
            (tab, position) -> {
              switch (position) {
                case 0:
                  tab.setText("Chats");
                  break;
                case 1:
                  tab.setText("Groups");
                  break;
                case 2:
                  tab.setText("Communities");
                  // Optional: Add icon or disable if needed
                  // tab.view.setEnabled(false);
                  break;
              }
            })
        .attach();
  }

  private class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(Fragment fragment) {
      super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      switch (position) {
        case 0:
          return new ChatsFragment();
        case 1:
          return new GroupsFragment();
        case 2:
          return new CommunitiesFragment();
        default:
          throw new IllegalArgumentException("Invalid position");
      }
    }

    @Override
    public int getItemCount() {
      return 3;
    }
  }

  private void showNewChatDialog() {
    NewChatBottomSheetDialog dialog = new NewChatBottomSheetDialog();
    dialog.show(getParentFragmentManager(), "NewChatBottomSheet");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (userListener != null) {
      userListener.remove(); // Clean up listener
      userListener = null;
    }
    binding = null;
  }
}
