package com.example.familyhelper.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.familyhelper.R;
import com.example.familyhelper.data.models.Family;
import com.example.familyhelper.data.models.FamilyInvite;
import com.example.familyhelper.data.models.User;
import com.example.familyhelper.ui.adapters.FamilyMemberAdapter;
import com.example.familyhelper.ui.adapters.FamilyInviteAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FamilyActivity extends AppCompatActivity {

    private TextView tvFamilyName, tvMembersCount;
    private RecyclerView rvMembers, rvPendingInvites;
    private Button btnInviteMember, btnLeaveFamily;
    private ImageButton btnBack;
    private View pendingInvitesSection;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId, currentFamilyId;
    private Family currentFamily;

    private List<User> membersList;
    private FamilyMemberAdapter memberAdapter;

    private List<FamilyInvite> pendingInvitesList;
    private FamilyInviteAdapter inviteAdapter;

    private ListenerRegistration familyListener, invitesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        setupListeners();
        loadFamilyData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvFamilyName = findViewById(R.id.tvFamilyName);
        tvMembersCount = findViewById(R.id.tvMembersCount);
        btnInviteMember = findViewById(R.id.btnInviteMember);
        btnLeaveFamily = findViewById(R.id.btnLeaveFamily);
        rvMembers = findViewById(R.id.rvMembers);
        rvPendingInvites = findViewById(R.id.rvPendingInvites);
        pendingInvitesSection = findViewById(R.id.pendingInvitesSection);

        membersList = new ArrayList<>();
        memberAdapter = new FamilyMemberAdapter(membersList, user -> {
            showUserProfile(user);
        });
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(memberAdapter);

        pendingInvitesList = new ArrayList<>();
        inviteAdapter = new FamilyInviteAdapter(pendingInvitesList, invite -> {
            cancelInvite(invite);
        });
        rvPendingInvites.setLayoutManager(new LinearLayoutManager(this));
        rvPendingInvites.setAdapter(inviteAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnInviteMember.setOnClickListener(v -> showInviteDialog());
        btnLeaveFamily.setOnClickListener(v -> showLeaveFamilyDialog());
    }

    private void loadFamilyData() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    currentFamilyId = doc.getString("familyId");

                    if (TextUtils.isEmpty(currentFamilyId)) {
                        Toast.makeText(this, "Вы не состоите в семье", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    loadFamily();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadFamily() {
        familyListener = db.collection("families").document(currentFamilyId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    currentFamily = snapshot.toObject(Family.class);
                    if (currentFamily != null) {
                        tvFamilyName.setText(currentFamily.getFamilyName());
                        tvMembersCount.setText("Членов: " + currentFamily.getMemberIds().size());

                        if (currentFamily.isCreator(currentUserId)) {
                            pendingInvitesSection.setVisibility(View.VISIBLE);
                            loadPendingInvites();
                        } else {
                            pendingInvitesSection.setVisibility(View.GONE);
                        }

                        loadMembers();
                    }
                });
    }

    private void loadMembers() {
        if (currentFamily == null) return;

        membersList.clear();
        List<String> memberIds = currentFamily.getMemberIds();

        if (memberIds.isEmpty()) {
            memberAdapter.notifyDataSetChanged();
            return;
        }

        for (String memberId : memberIds) {
            db.collection("users").document(memberId).get()
                    .addOnSuccessListener(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            membersList.add(user);
                            memberAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void loadPendingInvites() {
        invitesListener = db.collection("familyInvites")
                .whereEqualTo("familyId", currentFamilyId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, e) -> {
                    if (e != null || value == null) return;

                    pendingInvitesList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        FamilyInvite invite = doc.toObject(FamilyInvite.class);
                        if (invite != null) {
                            invite.setInviteId(doc.getId());
                            pendingInvitesList.add(invite);
                        }
                    }
                    inviteAdapter.notifyDataSetChanged();
                });
    }

    private void showInviteDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_invite_member, null);

        EditText etUserIdentifier = dialogView.findViewById(R.id.etUserIdentifier);

        new AlertDialog.Builder(this)
                .setTitle("Пригласить в семью")
                .setView(dialogView)
                .setPositiveButton("Отправить", (dialog, which) -> {
                    String identifier = etUserIdentifier.getText().toString().trim();
                    if (!identifier.isEmpty()) {
                        sendInvite(identifier);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void sendInvite(String identifier) {
        if (identifier.length() == 28) {
            sendInviteToUser(identifier);
        } else {
            db.collection("users")
                    .whereEqualTo("username", identifier)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            Toast.makeText(this, "Пользователь не найден",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userId = query.getDocuments().get(0).getId();
                        sendInviteToUser(userId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendInviteToUser(String targetUserId) {
        if (targetUserId.equals(currentUserId)) {
            Toast.makeText(this, "Вы не можете пригласить себя",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Пользователь не найден",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userFamilyId = doc.getString("familyId");
                    if (!TextUtils.isEmpty(userFamilyId)) {
                        Toast.makeText(this, "Пользователь уже в семье",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("familyInvites")
                            .whereEqualTo("familyId", currentFamilyId)
                            .whereEqualTo("toUserId", targetUserId)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(query -> {
                                if (!query.isEmpty()) {
                                    Toast.makeText(this, "Приглашение уже отправлено",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                createInvite(targetUserId);
                            });
                });
    }

    private void createInvite(String targetUserId) {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");

                    FamilyInvite invite = new FamilyInvite(
                            currentFamilyId,
                            currentFamily.getFamilyName(),
                            currentUserId,
                            username != null ? username : "user",
                            targetUserId
                    );

                    db.collection("familyInvites").add(invite)
                            .addOnSuccessListener(docRef -> {
                                currentFamily.addPendingInvite(targetUserId);
                                db.collection("families").document(currentFamilyId)
                                        .update("pendingInvites", currentFamily.getPendingInvites());

                                Toast.makeText(this, "Приглашение отправлено",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Ошибка отправки приглашения",
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void cancelInvite(FamilyInvite invite) {
        new AlertDialog.Builder(this)
                .setTitle("Отменить приглашение?")
                .setPositiveButton("Да", (dialog, which) -> {
                    db.collection("familyInvites").document(invite.getInviteId())
                            .update("status", "cancelled")
                            .addOnSuccessListener(aVoid -> {
                                currentFamily.removePendingInvite(invite.getToUserId());
                                db.collection("families").document(currentFamilyId)
                                        .update("pendingInvites", currentFamily.getPendingInvites());

                                Toast.makeText(this, "Приглашение отменено",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showUserProfile(User user) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("userId", user.getUserId());
        startActivity(intent);
    }

    private void showLeaveFamilyDialog() {
        String message = currentFamily.isCreator(currentUserId)
                ? "Вы создатель семьи. При выходе семья будет удалена."
                : "Вы уверены, что хотите покинуть семью?";

        new AlertDialog.Builder(this)
                .setTitle("Покинуть семью")
                .setMessage(message)
                .setPositiveButton("Да", (dialog, which) -> leaveFamily())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void leaveFamily() {
        if (currentFamily.isCreator(currentUserId)) {
            db.collection("families").document(currentFamilyId).delete();

            for (String memberId : currentFamily.getMemberIds()) {
                db.collection("users").document(memberId)
                        .update("familyId", null);
            }
        } else {
            currentFamily.removeMember(currentUserId);
            db.collection("families").document(currentFamilyId)
                    .update("memberIds", currentFamily.getMemberIds());

            db.collection("users").document(currentUserId)
                    .update("familyId", null);
        }

        Toast.makeText(this, "Вы покинули семью", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (familyListener != null) familyListener.remove();
        if (invitesListener != null) invitesListener.remove();
    }
}