package com.example.familyhelper.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.User;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private OnMemberClickListener listener;
    private String creatorId;
    private String currentUserId;
    private boolean isCurrentUserCreator;

    public interface OnMemberClickListener {
        void onMemberClick(User user);
        void onMemberOptionsClick(User user);
    }

    public FamilyMemberAdapter(List<User> memberList, String creatorId, String currentUserId, OnMemberClickListener listener) {
        this.memberList = memberList;
        this.creatorId = creatorId;
        this.currentUserId = currentUserId;
        this.isCurrentUserCreator = currentUserId.equals(creatorId);
        this.listener = listener;
    }

    public void updateMembers(List<User> newMembers) {
        this.memberList = newMembers;
        sortMembers();
        notifyDataSetChanged();
    }

    private void sortMembers() {
        Collections.sort(memberList, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                if (u1.getUserId().equals(creatorId)) return -1;
                if (u2.getUserId().equals(creatorId)) return 1;

                return Long.compare(u1.getJoinedFamilyAt(), u2.getJoinedFamilyAt());
            }
        });
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = memberList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfileImage, ivCreatorBadge;
        TextView tvName, tvUsername, tvJoinedDate;
        ImageButton btnMemberOptions;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivMemberProfileImage);
            ivCreatorBadge = itemView.findViewById(R.id.ivCreatorBadge);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvUsername = itemView.findViewById(R.id.tvMemberUsername);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            btnMemberOptions = itemView.findViewById(R.id.btnMemberOptions);
        }

        void bind(User user) {
            tvName.setText(user.getName() != null ? user.getName() : "Не указано");
            tvUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));

            if (user.getUserId().equals(creatorId)) {
                ivCreatorBadge.setVisibility(View.VISIBLE);
            } else {
                ivCreatorBadge.setVisibility(View.GONE);
            }

            if (user.getJoinedFamilyAt() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                String joinedDate = dateFormat.format(new Date(user.getJoinedFamilyAt()));
                tvJoinedDate.setText("В семье с " + joinedDate);
                tvJoinedDate.setVisibility(View.VISIBLE);
            } else {
                tvJoinedDate.setVisibility(View.GONE);
            }

            if (isCurrentUserCreator && !user.getUserId().equals(currentUserId)) {
                btnMemberOptions.setVisibility(View.VISIBLE);
                btnMemberOptions.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMemberOptionsClick(user);
                    }
                });
            } else {
                btnMemberOptions.setVisibility(View.GONE);
            }

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.username_icon)
                        .error(R.drawable.username_icon)
                        .circleCrop()
                        .into(ivProfileImage);
            } else {
                ivProfileImage.setImageResource(R.drawable.username_icon);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(user);
                }
            });
        }
    }
}