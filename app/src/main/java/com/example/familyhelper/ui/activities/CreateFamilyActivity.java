package com.example.familyhelper.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.familyhelper.MainActivity;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.Family;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateFamilyActivity extends AppCompatActivity {

    private EditText etFamilyName;
    private Button btnCreateFamily, btnCancel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_family);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        setupListeners();
        checkExistingFamily();
    }

    private void initViews() {
        etFamilyName = findViewById(R.id.etFamilyName);
        btnCreateFamily = findViewById(R.id.btnCreateFamily);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        btnCreateFamily.setOnClickListener(v -> createFamily());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void checkExistingFamily() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String familyId = doc.getString("familyId");
                    if (!TextUtils.isEmpty(familyId)) {
                        Toast.makeText(this, "Вы уже состоите в семье", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                });
    }

    private void createFamily() {
        String familyName = etFamilyName.getText().toString().trim();

        if (TextUtils.isEmpty(familyName)) {
            etFamilyName.setError("Введите название семьи");
            return;
        }

        if (familyName.length() < 3) {
            etFamilyName.setError("Минимум 3 символа");
            return;
        }

        Family family = new Family(familyName, currentUserId);

        db.collection("families")
                .add(family)
                .addOnSuccessListener(docRef -> {
                    String familyId = docRef.getId();

                    db.collection("families").document(familyId)
                            .update("familyId", familyId);

                    db.collection("users").document(currentUserId)
                            .update("familyId", familyId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Семья создана!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Ошибка: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка создания семьи: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}