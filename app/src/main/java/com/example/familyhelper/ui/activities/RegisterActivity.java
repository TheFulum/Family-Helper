package com.example.familyhelper.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.familyhelper.R;
import com.example.familyhelper.data.models.User;
import com.example.familyhelper.utils.PhoneMaskEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private PhoneMaskEditText etPhone;
    private com.google.android.material.textfield.TextInputEditText etUsername, etEmail, etPassword, etName;
    private Button btnRegister;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+375\\(\\d{2}\\)\\d{3}-\\d{2}-\\d{2}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getUnformattedPhone().trim();

        if (TextUtils.isEmpty(username) || !USERNAME_PATTERN.matcher(username).matches()) {
            etUsername.setError("Никнейм: 3-20 символов, буквы, цифры, _");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Введите email");
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Неверный формат email");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Пароль не менее 6 символов");
            return;
        }

        if (TextUtils.isEmpty(name)) {
            etName.setError("Введите имя");
            return;
        }

        if (!TextUtils.isEmpty(phone) && !etPhone.isComplete()) {
            etPhone.setError("Заполните номер полностью");
            return;
        }

        String formattedNoSpace = etPhone.getFormattedPhone().replace(" ", "");
        if (!TextUtils.isEmpty(phone) && !isValidPhone(formattedNoSpace)) {
            etPhone.setError("Неверный формат телефона (например: +375(29)123-45-67)");
            return;
        }

        checkUsernameUnique(username, email, password, name, phone);
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    private void checkUsernameUnique(String username, String email, String password,
                                     String name, String phone) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Этот никнейм уже занят", Toast.LENGTH_SHORT).show();
                        } else {
                            createFirebaseAccount(username, email, password, name, phone);
                        }
                    } else {
                        Toast.makeText(this, "Ошибка проверки никнейма", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createFirebaseAccount(String username, String email, String password,
                                       String name, String phone) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Toast.makeText(this,
                                                    "Письмо с подтверждением отправлено на " + email,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                            saveUserToFirestore(firebaseUser.getUid(), username, email, name, phone);
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Неизвестная ошибка";
                        Toast.makeText(this, "Ошибка регистрации: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email,
                                     String name, String phone) {
        User user = new User(userId, username, email, name, phone);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Регистрация успешна! Подтвердите ваш email",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
                });
    }
}