package com.example.familyhelper.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvUsername, tvName, tvEmail, tvPhone, tvBirthday;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        if (TextUtils.isEmpty(userId)) {
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadUserData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfileImage = findViewById(R.id.ivUserProfileImage);
        tvUsername = findViewById(R.id.tvUserUsername);
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        tvPhone = findViewById(R.id.tvUserPhone);
        tvBirthday = findViewById(R.id.tvUserBirthday);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        displayUserData(user);
                    }
                })
                .addOnFailureListener(e -> finish());
    }

    private void displayUserData(User user) {
        tvUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));
        tvName.setText(user.getName() != null ? user.getName() : "Не указано");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Не указано");

        String phone = user.getPhone();
        tvPhone.setText(phone != null ? formatPhone(phone) : "Не указано");

        tvBirthday.setText(user.getBirthday() != null ? user.getBirthday() : "Не указано");

        if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.username_icon)
                    .error(R.drawable.username_icon)
                    .circleCrop()
                    .into(ivProfileImage);
        }
    }

    private String formatPhone(String unformatted) {
        if (unformatted == null || !unformatted.startsWith("+375") || unformatted.length() != 13) {
            return unformatted;
        }
        String digits = unformatted.substring(4);
        return "+375(" + digits.substring(0, 2) + ") " + digits.substring(2, 5) +
                "-" + digits.substring(5, 7) + "-" + digits.substring(7);
    }
}