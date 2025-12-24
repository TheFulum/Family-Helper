package com.example.familyhelper.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.User;

import java.util.List;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(User user);
    }

    public FamilyMemberAdapter(List<User> memberList, OnMemberClickListener listener) {
        this.memberList = memberList;
        this.listener = listener;
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
        ImageView ivProfileImage;
        TextView tvName, tvUsername;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivMemberProfileImage);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvUsername = itemView.findViewById(R.id.tvMemberUsername);
        }

        void bind(User user) {
            tvName.setText(user.getName() != null ? user.getName() : "Не указано");
            tvUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));

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

            itemView.setOnClickListener(v -> listener.onMemberClick(user));
        }
    }
}