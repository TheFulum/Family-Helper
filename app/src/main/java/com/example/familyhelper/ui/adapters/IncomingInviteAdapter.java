package com.example.familyhelper.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familyhelper.R;
import com.example.familyhelper.data.models.FamilyInvite;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncomingInviteAdapter extends RecyclerView.Adapter<IncomingInviteAdapter.InviteViewHolder> {

    private List<FamilyInvite> inviteList;
    private OnInviteActionListener listener;

    public interface OnInviteActionListener {
        void onAccept(FamilyInvite invite);
        void onDecline(FamilyInvite invite);
    }

    public IncomingInviteAdapter(List<FamilyInvite> inviteList, OnInviteActionListener listener) {
        this.inviteList = inviteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incoming_invite, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        FamilyInvite invite = inviteList.get(position);
        holder.bind(invite);
    }

    @Override
    public int getItemCount() {
        return inviteList.size();
    }

    class InviteViewHolder extends RecyclerView.ViewHolder {
        TextView tvFamilyName, tvFromUser, tvInviteDate;
        Button btnAccept, btnDecline;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFamilyName = itemView.findViewById(R.id.tvFamilyName);
            tvFromUser = itemView.findViewById(R.id.tvFromUser);
            tvInviteDate = itemView.findViewById(R.id.tvInviteDate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        void bind(FamilyInvite invite) {
            tvFamilyName.setText("Семья: " + invite.getFamilyName());
            tvFromUser.setText("От: @" + invite.getFromUsername());

            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(invite.getTimestamp());
            tvInviteDate.setText(date);

            btnAccept.setOnClickListener(v -> listener.onAccept(invite));
            btnDecline.setOnClickListener(v -> listener.onDecline(invite));
        }
    }
}