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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FamilyInviteAdapter extends RecyclerView.Adapter<FamilyInviteAdapter.InviteViewHolder> {

    private List<FamilyInvite> inviteList;
    private OnInviteCancelListener listener;
    private FirebaseFirestore db;

    public interface OnInviteCancelListener {
        void onCancelClick(FamilyInvite invite);
    }

    public FamilyInviteAdapter(List<FamilyInvite> inviteList, OnInviteCancelListener listener) {
        this.inviteList = inviteList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_invite, parent, false);
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
        TextView tvInvitedUser, tvInviteDate;
        Button btnCancelInvite;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInvitedUser = itemView.findViewById(R.id.tvInvitedUser);
            tvInviteDate = itemView.findViewById(R.id.tvInviteDate);
            btnCancelInvite = itemView.findViewById(R.id.btnCancelInvite);
        }

        void bind(FamilyInvite invite) {
            db.collection("users").document(invite.getToUserId()).get()
                    .addOnSuccessListener(doc -> {
                        String username = doc.getString("username");
                        String name = doc.getString("name");

                        String display = name != null ? name : "Пользователь";
                        if (username != null) {
                            display += " (@" + username + ")";
                        }
                        tvInvitedUser.setText(display);
                    });

            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(invite.getTimestamp());
            tvInviteDate.setText("Отправлено: " + date);

            btnCancelInvite.setOnClickListener(v -> listener.onCancelClick(invite));
        }
    }
}