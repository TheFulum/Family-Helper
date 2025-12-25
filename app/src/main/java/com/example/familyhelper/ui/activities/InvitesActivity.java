package com.example.familyhelper.ui.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familyhelper.R;
import com.example.familyhelper.data.models.FamilyInvite;
import com.example.familyhelper.ui.adapters.IncomingInviteAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class InvitesActivity extends AppCompatActivity {

    private RecyclerView rvInvites;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private List<FamilyInvite> invitesList;
    private IncomingInviteAdapter adapter;
    private ListenerRegistration invitesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invites);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        setupListeners();
        loadInvites();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvInvites = findViewById(R.id.rvInvites);

        invitesList = new ArrayList<>();
        adapter = new IncomingInviteAdapter(invitesList, new IncomingInviteAdapter.OnInviteActionListener() {
            @Override
            public void onAccept(FamilyInvite invite) {
                acceptInvite(invite);
            }

            @Override
            public void onDecline(FamilyInvite invite) {
                declineInvite(invite);
            }
        });

        rvInvites.setLayoutManager(new LinearLayoutManager(this));
        rvInvites.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadInvites() {
        invitesListener = db.collection("familyInvites")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, e) -> {
                    if (e != null || value == null) return;

                    invitesList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        FamilyInvite invite = doc.toObject(FamilyInvite.class);
                        if (invite != null) {
                            invite.setInviteId(doc.getId());
                            invitesList.add(invite);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void acceptInvite(FamilyInvite invite) {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String familyId = doc.getString("familyId");
                    if (familyId != null && !familyId.isEmpty()) {
                        Toast.makeText(this, "Вы уже в семье", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("families").document(invite.getFamilyId()).get()
                            .addOnSuccessListener(familyDoc -> {
                                List<String> memberIds = (List<String>) familyDoc.get("memberIds");
                                if (memberIds == null) memberIds = new ArrayList<>();

                                if (!memberIds.contains(currentUserId)) {
                                    memberIds.add(currentUserId);

                                    db.collection("families").document(invite.getFamilyId())
                                            .update("memberIds", memberIds);
                                }

                                List<String> pendingInvites = (List<String>) familyDoc.get("pendingInvites");
                                if (pendingInvites != null) {
                                    pendingInvites.remove(currentUserId);
                                    db.collection("families").document(invite.getFamilyId())
                                            .update("pendingInvites", pendingInvites);
                                }

                                long joinedAt = System.currentTimeMillis();
                                db.collection("users").document(currentUserId)
                                        .update(
                                                "familyId", invite.getFamilyId(),
                                                "joinedFamilyAt", joinedAt
                                        );

                                db.collection("familyInvites").document(invite.getInviteId())
                                        .update("status", "accepted");

                                Toast.makeText(this, "Вы вступили в семью!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }

    private void declineInvite(FamilyInvite invite) {
        db.collection("families").document(invite.getFamilyId()).get()
                .addOnSuccessListener(familyDoc -> {
                    List<String> pendingInvites = (List<String>) familyDoc.get("pendingInvites");
                    if (pendingInvites != null) {
                        pendingInvites.remove(currentUserId);
                        db.collection("families").document(invite.getFamilyId())
                                .update("pendingInvites", pendingInvites);
                    }
                });

        db.collection("familyInvites").document(invite.getInviteId())
                .update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Приглашение отклонено", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (invitesListener != null) invitesListener.remove();
    }
}