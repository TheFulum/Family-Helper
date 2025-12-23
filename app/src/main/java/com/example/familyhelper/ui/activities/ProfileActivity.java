package com.example.familyhelper.ui.activities;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.familyhelper.R;
import com.example.familyhelper.config.AppConfig;
import com.example.familyhelper.data.models.User;
import com.example.familyhelper.utils.PhoneMaskEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView usernameTv, emailTv, userIdTv, nameTv, phoneTv, birthdayTv;
    private ImageButton logoutBtn, editBirthdayBtn, editNameBtn, editPhoneBtn, backBtn;
    private Button editUsernameBtn;
    private View changePhotoBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    private static final Pattern BY_PHONE_PATTERN = Pattern.compile("^\\+375\\(\\d{2}\\)\\d{3}-\\d{2}-\\d{2}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null || !mAuth.getCurrentUser().isEmailVerified()) {
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void initViews() {
        profileImageView = findViewById(R.id.profile_image_view);
        usernameTv = findViewById(R.id.username_tv);
        emailTv = findViewById(R.id.email_tv);
        userIdTv = findViewById(R.id.user_id_tv);
        nameTv = findViewById(R.id.name_tv);
        phoneTv = findViewById(R.id.phone_tv);
        birthdayTv = findViewById(R.id.birthday_tv);

        backBtn = findViewById(R.id.back_btn);
        logoutBtn = findViewById(R.id.logout_btn);
        editBirthdayBtn = findViewById(R.id.edit_birthday_btn);
        editUsernameBtn = findViewById(R.id.edit_username_btn);
        editNameBtn = findViewById(R.id.edit_name_btn);
        editPhoneBtn = findViewById(R.id.edit_phone_btn);
        changePhotoBtn = findViewById(R.id.camera_icon_card);
    }

    private void setupListeners() {
        backBtn.setOnClickListener(v -> finish());
        logoutBtn.setOnClickListener(v -> logout());
        editBirthdayBtn.setOnClickListener(v -> showDatePicker());
        changePhotoBtn.setOnClickListener(v -> pickImage());

        editUsernameBtn.setOnClickListener(v -> showEditDialog("username", "Никнейм", value -> {
            if (!USERNAME_PATTERN.matcher(value).matches()) {
                Toast.makeText(this, "Никнейм: 3-20 символов, буквы, цифры, _", Toast.LENGTH_SHORT).show();
                return;
            }
            checkUsernameUnique(value, () -> {
                updateUserField("username", value);
                usernameTv.setText("@" + value);
            });
        }));

        editNameBtn.setOnClickListener(v -> showEditDialog("name", "Ваше имя", value -> {
            updateUserField("name", value);
            nameTv.setText(value);
        }));

        editPhoneBtn.setOnClickListener(v -> showEditPhoneDialog());

        userIdTv.setOnClickListener(v -> {
            String userId = userIdTv.getText().toString().replace("ID: ", "").trim();
            if (!TextUtils.isEmpty(userId)) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", userId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID скопирован: " + userId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        userIdTv.setText("ID: " + uid);

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            currentUser = doc.toObject(User.class);
            if (currentUser != null) {
                usernameTv.setText("@" + (currentUser.getUsername() != null ? currentUser.getUsername() : "user"));
                emailTv.setText(currentUser.getEmail());
                nameTv.setText(currentUser.getName() != null ? currentUser.getName() : "Не указано");
                String phone = currentUser.getPhone();
                phoneTv.setText(phone != null ? formatPhone(phone) : "Не указано");
                birthdayTv.setText(currentUser.getBirthday() != null ? currentUser.getBirthday() : "Не указано");

                if (!TextUtils.isEmpty(currentUser.getProfileImageUrl())) {
                    Glide.with(this)
                            .load(currentUser.getProfileImageUrl())
                            .placeholder(R.drawable.username_icon)
                            .error(R.drawable.username_icon)
                            .into(profileImageView);
                }
            }
        });
    }

    private String formatPhone(String unformatted) {
        if (unformatted == null || !unformatted.startsWith("+375") || unformatted.length() != 13) {
            return unformatted;
        }
        String digits = unformatted.substring(4);
        return "+375(" + digits.substring(0, 2) + ") " + digits.substring(2, 5) + "-" + digits.substring(5, 7) + "-" + digits.substring(7);
    }

    private void showEditDialog(String field, String hint, UpdateCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить " + field);

        EditText input = new EditText(this);
        input.setHint(hint);
        String currentValue = "";
        if (field.equals("username")) {
            currentValue = usernameTv.getText().toString().replace("@", "");
        } else if (field.equals("name")) {
            currentValue = nameTv.getText().toString();
        }
        input.setText(currentValue);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) callback.onUpdate(val);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить телефон");

        PhoneMaskEditText input = new PhoneMaskEditText(this);
        String currentPhone = currentUser.getPhone();
        input.setPhone(currentPhone);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            if (!input.isComplete()) {
                Toast.makeText(this, "Заполните номер полностью", Toast.LENGTH_SHORT).show();
                return;
            }
            String formattedNoSpace = input.getFormattedPhone().replace(" ", "");
            if (!BY_PHONE_PATTERN.matcher(formattedNoSpace).matches()) {
                Toast.makeText(this, "Ошибка! Формат: +375(29)123-45-67", Toast.LENGTH_LONG).show();
                return;
            }
            String val = input.getUnformattedPhone();
            updateUserField("phone", val);
            phoneTv.setText(input.getFormattedPhone());
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void checkUsernameUnique(String username, Runnable onUnique) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        onUnique.run();
                    } else {
                        Toast.makeText(this, "Никнейм занят", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка проверки", Toast.LENGTH_SHORT).show());
    }

    private void updateUserField(String field, String value) {
        if (mAuth.getUid() == null) return;
        db.collection("users").document(mAuth.getUid()).update(field, value)
                .addOnSuccessListener(a -> Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%02d.%02d.%d", d, m + 1, y);
            birthdayTv.setText(date);
            updateUserField("birthday", date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickImage() {
        mGetContent.launch("image/*");
    }

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    profileImageView.setImageURI(uri);
                    uploadImageToCloudinary(uri);
                }
            }
    );

    private void uploadImageToCloudinary(Uri imageUri) {
        Toast.makeText(this, "Загрузка фото...", Toast.LENGTH_SHORT).show();

        File file = uriToFile(imageUri);
        if (file == null) {
            Toast.makeText(this, "Ошибка файла", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis() / 1000;
        String stringToSign = "timestamp=" + timestamp + AppConfig.CLOUDINARY_API_SECRET;
        String signature = sha1(stringToSign);

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/*")))
                .addFormDataPart("api_key", AppConfig.CLOUDINARY_API_KEY)
                .addFormDataPart("timestamp", String.valueOf(timestamp))
                .addFormDataPart("signature", signature)
                .build();

        Request request = new Request.Builder()
                .url(AppConfig.CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Сеть: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        String url = jsonObject.getString("secure_url");

                        updateUserField("profileImageUrl", url);
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Фото обновлено!", Toast.LENGTH_SHORT).show());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    String err = response.body() != null ? response.body().string() : "";
                    Log.e("Upload", "Error: " + err);
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Ошибка: " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private String sha1(String input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
            byte[] result = mDigest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    interface UpdateCallback {
        void onUpdate(String value);
    }
}