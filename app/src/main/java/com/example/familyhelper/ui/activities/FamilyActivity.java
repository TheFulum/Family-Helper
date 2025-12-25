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
    private Button btnInviteMember, btnLeaveFamily, btnDeleteFamily;
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
        btnDeleteFamily = findViewById(R.id.btnDeleteFamily);
        rvMembers = findViewById(R.id.rvMembers);
        rvPendingInvites = findViewById(R.id.rvPendingInvites);
        pendingInvitesSection = findViewById(R.id.pendingInvitesSection);

        membersList = new ArrayList<>();
        memberAdapter = new FamilyMemberAdapter(membersList, "", currentUserId,
                new FamilyMemberAdapter.OnMemberClickListener() {
                    @Override
                    public void onMemberClick(User user) {
                        showUserProfile(user);
                    }

                    @Override
                    public void onMemberOptionsClick(User user) {
                        showMemberOptionsDialog(user);
                    }
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
        btnDeleteFamily.setOnClickListener(v -> showDeleteFamilyDialog());
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
                            btnDeleteFamily.setVisibility(View.VISIBLE);
                            btnLeaveFamily.setVisibility(View.GONE);
                            loadPendingInvites();
                        } else {
                            pendingInvitesSection.setVisibility(View.GONE);
                            btnDeleteFamily.setVisibility(View.GONE);
                            btnLeaveFamily.setVisibility(View.VISIBLE);
                        }

                        memberAdapter = new FamilyMemberAdapter(membersList,
                                currentFamily.getCreatorId(), currentUserId,
                                new FamilyMemberAdapter.OnMemberClickListener() {
                                    @Override
                                    public void onMemberClick(User user) {
                                        showUserProfile(user);
                                    }

                                    @Override
                                    public void onMemberOptionsClick(User user) {
                                        showMemberOptionsDialog(user);
                                    }
                                });
                        rvMembers.setAdapter(memberAdapter);

                        loadMembers();
                    }
                });
    }

    private void loadMembers() {
        if (currentFamily == null) return;

        membersList.clear();
        List<String> memberIds = currentFamily.getMemberIds();

        if (memberIds.isEmpty()) {
            memberAdapter.updateMembers(membersList);
            return;
        }

        final int[] loadedCount = {0};
        final int totalCount = memberIds.size();

        for (String memberId : memberIds) {
            db.collection("users").document(memberId).get()
                    .addOnSuccessListener(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            membersList.add(user);
                        }

                        loadedCount[0]++;

                        if (loadedCount[0] == totalCount) {
                            memberAdapter.updateMembers(membersList);
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

    private void showMemberOptionsDialog(User user) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_member_options, null);

        TextView tvMemberName = dialogView.findViewById(R.id.tvMemberName);
        Button btnTransferOwnership = dialogView.findViewById(R.id.btnTransferOwnership);
        Button btnRemoveMember = dialogView.findViewById(R.id.btnRemoveMember);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        String displayName = user.getName() != null ? user.getName() :
                ("@" + (user.getUsername() != null ? user.getUsername() : "user"));
        tvMemberName.setText("Управление: " + displayName);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnTransferOwnership.setOnClickListener(v -> {
            dialog.dismiss();
            showTransferOwnershipConfirmation(user);
        });

        btnRemoveMember.setOnClickListener(v -> {
            dialog.dismiss();
            showRemoveMemberConfirmation(user);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showTransferOwnershipConfirmation(User newOwner) {
        String displayName = newOwner.getName() != null ? newOwner.getName() :
                ("@" + (newOwner.getUsername() != null ? newOwner.getUsername() : "user"));

        new AlertDialog.Builder(this)
                .setTitle("Передать права главы?")
                .setMessage("Вы передаете права главы семьи пользователю " + displayName +
                        ". После этого вы не сможете управлять семьей. Продолжить?")
                .setPositiveButton("Да, передать", (dialog, which) -> transferOwnership(newOwner))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void transferOwnership(User newOwner) {
        db.collection("families").document(currentFamilyId)
                .update("creatorId", newOwner.getUserId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Права главы переданы", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка передачи прав", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRemoveMemberConfirmation(User member) {
        String displayName = member.getName() != null ? member.getName() :
                ("@" + (member.getUsername() != null ? member.getUsername() : "user"));

        new AlertDialog.Builder(this)
                .setTitle("Удалить участника?")
                .setMessage("Вы уверены, что хотите удалить " + displayName + " из семьи?")
                .setPositiveButton("Да, удалить", (dialog, which) -> removeMember(member))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void removeMember(User member) {
        currentFamily.removeMember(member.getUserId());

        db.collection("families").document(currentFamilyId)
                .update("memberIds", currentFamily.getMemberIds())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(member.getUserId())
                            .update("familyId", null);

                    Toast.makeText(this, "Участник удален из семьи", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка удаления участника", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteFamilyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить семью?")
                .setMessage("Вы уверены, что хотите удалить семью? Это действие нельзя отменить. " +
                        "Все участники будут исключены из семьи.")
                .setPositiveButton("Да, удалить", (dialog, which) -> deleteFamily())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteFamily() {
        for (String memberId : currentFamily.getMemberIds()) {
            db.collection("users").document(memberId)
                    .update("familyId", null);
        }

        db.collection("tasks")
                .whereEqualTo("familyId", currentFamilyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });

        db.collection("familyInvites")
                .whereEqualTo("familyId", currentFamilyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });

        db.collection("families").document(currentFamilyId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Семья удалена", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка удаления семьи", Toast.LENGTH_SHORT).show();
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
        new AlertDialog.Builder(this)
                .setTitle("Покинуть семью")
                .setMessage("Вы уверены, что хотите покинуть семью?")
                .setPositiveButton("Да", (dialog, which) -> leaveFamily())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void leaveFamily() {
        currentFamily.removeMember(currentUserId);
        db.collection("families").document(currentFamilyId)
                .update("memberIds", currentFamily.getMemberIds());

        db.collection("users").document(currentUserId)
                .update("familyId", null);

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