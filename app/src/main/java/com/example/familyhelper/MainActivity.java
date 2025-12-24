package com.example.familyhelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.familyhelper.data.models.Comment;
import com.example.familyhelper.data.models.Task;
import com.example.familyhelper.data.models.User;
import com.example.familyhelper.ui.activities.CreateFamilyActivity;
import com.example.familyhelper.ui.activities.FamilyActivity;
import com.example.familyhelper.ui.activities.InvitesActivity;
import com.example.familyhelper.ui.activities.LoginActivity;
import com.example.familyhelper.ui.activities.ProfileActivity;
import com.example.familyhelper.ui.adapters.CommentAdapter;
import com.example.familyhelper.ui.adapters.TaskAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private CalendarView calendarView;
    private RecyclerView tasksRecyclerView;
    private FloatingActionButton fabAddTask;
    private ImageButton btnProfile, btnFamily, btnInvites;
    private TextView tvSelectedDate;
    private View inviteBadge;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId, selectedDate, currentFamilyId;
    private List<Task> taskList;
    private TaskAdapter taskAdapter;
    private ListenerRegistration taskListener, invitesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        currentUserId = mAuth.getCurrentUser().getUid();

        initViews();
        setupListeners();

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this, currentUserId);
        tasksRecyclerView.setAdapter(taskAdapter);

        selectedDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        tvSelectedDate.setText("Задачи на " + selectedDate);

        loadUser();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        btnProfile = findViewById(R.id.btnProfile);
        btnFamily = findViewById(R.id.btnFamily);
        btnInvites = findViewById(R.id.btnInvites);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        inviteBadge = findViewById(R.id.inviteBadge);
    }

    private void setupListeners() {
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnFamily.setOnClickListener(v -> openFamilyActivity());
        btnInvites.setOnClickListener(v -> startActivity(new Intent(this, InvitesActivity.class)));
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        calendarView.setOnDateChangeListener((v, y, m, d) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d.%02d.%d", d, m + 1, y);
            tvSelectedDate.setText("Задачи на " + selectedDate);
            Log.d("MainActivity", "Date changed to: " + selectedDate);
            if (!TextUtils.isEmpty(currentFamilyId)) {
                loadTasks();
            } else {
                Log.d("MainActivity", "FamilyId not loaded yet, skipping loadTasks");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null || !mAuth.getCurrentUser().isEmailVerified()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUser();
    }

    private void loadUser() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            User u = doc.toObject(User.class);
            if (u != null) {
                currentFamilyId = u.getFamilyId();
                if (TextUtils.isEmpty(currentFamilyId)) {
                    Log.d("MainActivity", "FamilyId is empty or null");
                } else {
                    loadTasks();
                    checkBirthdays();
                }
                checkPendingInvites();
            }
        }).addOnFailureListener(e -> Log.e("MainActivity", "Error loading user: " + e.getMessage()));
    }

    private void openFamilyActivity() {
        if (TextUtils.isEmpty(currentFamilyId)) {
            startActivity(new Intent(this, CreateFamilyActivity.class));
        } else {
            startActivity(new Intent(this, FamilyActivity.class));
        }
    }

    private void checkPendingInvites() {
        if (invitesListener != null) {
            invitesListener.remove();
        }

        invitesListener = db.collection("familyInvites")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, e) -> {
                    if (e != null || value == null) {
                        inviteBadge.setVisibility(View.GONE);
                        return;
                    }

                    if (value.size() > 0) {
                        inviteBadge.setVisibility(View.VISIBLE);
                    } else {
                        inviteBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void loadTasks() {
        if (taskListener != null) taskListener.remove();
        if (TextUtils.isEmpty(currentFamilyId)) {
            Log.d("MainActivity", "Skipping loadTasks: familyId empty");
            return;
        }
        Log.d("MainActivity", "Loading tasks for date: " + selectedDate + ", family: " + currentFamilyId);
        taskListener = db.collection("tasks")
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("familyId", currentFamilyId)
                .orderBy("priority", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "Tasks listener error: " + e.getMessage());
                        return;
                    }
                    if (value == null) {
                        Log.d("MainActivity", "Tasks snapshot null");
                        return;
                    }
                    taskList.clear();
                    Log.d("MainActivity", "Tasks found: " + value.size());
                    for (QueryDocumentSnapshot doc : value) {
                        Task t = doc.toObject(Task.class);
                        t.setTaskId(doc.getId());
                        taskList.add(t);
                        Log.d("MainActivity", "Task added: " + t.getTitle());
                    }
                    taskAdapter.notifyDataSetChanged();
                });
    }

    private void checkBirthdays() {
        String today = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
        db.collection("users")
                .whereEqualTo("familyId", currentFamilyId)
                .get()
                .addOnSuccessListener(query -> {
                    StringBuilder alert = new StringBuilder();
                    for (QueryDocumentSnapshot doc : query) {
                        User u = doc.toObject(User.class);
                        if (u != null && u.getBirthday() != null && u.getBirthday().length() >= 5) {
                            String userBirthday = u.getBirthday().substring(0, 5);
                            if (userBirthday.equals(today)) {
                                alert.append("День рождения у ").append(u.getName()).append("! ");
                            }
                        }
                    }
                    View birthdayCard = findViewById(R.id.birthdayCard);
                    if (alert.length() > 0) {
                        birthdayCard.setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.tvBirthdayAlert)).setText(alert.toString());
                    } else {
                        birthdayCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка проверки дней рождения", Toast.LENGTH_SHORT).show());
    }

    private void showAddTaskDialog() {
        if (TextUtils.isEmpty(currentFamilyId)) {
            Toast.makeText(this, "Сначала создайте или вступите в семью", Toast.LENGTH_SHORT).show();
            return;
        }

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etT = v.findViewById(R.id.etTaskTitle);
        EditText etD = v.findViewById(R.id.etTaskDesc);
        Spinner sA = v.findViewById(R.id.spinnerArea);
        Spinner sP = v.findViewById(R.id.spinnerPriority);

        sA.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Дом", "Еда", "Работа", "Дети"}));

        List<PriorityItem> items = new ArrayList<>();
        items.add(new PriorityItem("Низкий", 0xFF4CAF50));
        items.add(new PriorityItem("Средний", 0xFFFFEB3B));
        items.add(new PriorityItem("Высокий", 0xFFFF9800));
        items.add(new PriorityItem("Критический", 0xFFF44336));
        sP.setAdapter(new PriorityAdapter(this, items));

        new AlertDialog.Builder(this).setView(v).setPositiveButton("Создать", (d, w) -> {
            String title = etT.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Введите название задачи", Toast.LENGTH_SHORT).show();
                return;
            }
            Task task = new Task(title, etD.getText().toString(), sA.getSelectedItem().toString(),
                    sP.getSelectedItemPosition(), selectedDate, currentFamilyId, currentUserId);
            db.collection("tasks").add(task)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Задача создана", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка создания задачи", Toast.LENGTH_SHORT).show();
                    });
        }).setNegativeButton("Отмена", null).show();
    }

    @Override
    public void onTaskClick(Task task) {
        showTaskDetailsDialog(task);
    }

    @Override
    public void onTaskStatusChanged(Task task, boolean isCompleted) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", isCompleted);
        if (isCompleted) {
            updates.put("completedBy", currentUserId);
        } else {
            updates.put("completedBy", null);
        }
        db.collection("tasks").document(task.getTaskId()).update(updates);
    }

    private void showTaskDetailsDialog(Task task) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_task_details, null);
        TextView tvTitle = v.findViewById(R.id.tvTaskTitle);
        TextView tvDesc = v.findViewById(R.id.tvTaskDesc);
        TextView tvArea = v.findViewById(R.id.tvTaskArea);
        TextView tvPriority = v.findViewById(R.id.tvTaskPriority);
        RecyclerView rvComments = v.findViewById(R.id.rvComments);
        EditText etNewComment = v.findViewById(R.id.etNewComment);
        Button btnAddComment = v.findViewById(R.id.btnAddComment);

        tvTitle.setText(task.getTitle());
        tvDesc.setText(task.getDescription());

        // Форматируем категорию
        String[] categories = {"Дом", "Еда", "Работа", "Дети"};
        tvArea.setText(task.getArea());

        // Форматируем приоритет
        String[] priorities = {"Низкий", "Средний", "Высокий", "Критический"};
        int priorityIndex = task.getPriority();
        if (priorityIndex >= 0 && priorityIndex < priorities.length) {
            tvPriority.setText(priorities[priorityIndex]);
        } else {
            tvPriority.setText("Неизвестно");
        }


        List<Comment> comments = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        ListenerRegistration commentListener = db.collection("tasks").document(task.getTaskId())
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null || value == null) return;
                    comments.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Comment c = doc.toObject(Comment.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            comments.add(c);
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                });

        btnAddComment.setOnClickListener(view -> {
            String text = etNewComment.getText().toString().trim();
            if (!text.isEmpty()) {
                Comment newComment = new Comment(text, currentUserId, System.currentTimeMillis());
                db.collection("tasks").document(task.getTaskId())
                        .collection("comments")
                        .add(newComment);
                etNewComment.setText("");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .setNegativeButton("Закрыть", null)
                .create();

        dialog.show();
        dialog.setOnDismissListener(d -> commentListener.remove());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
        if (invitesListener != null) invitesListener.remove();
    }

    private static class PriorityItem {
        String l;
        int c;
        PriorityItem(String l, int c) {
            this.l=l;
            this.c=c;
        }
    }

    private class PriorityAdapter extends ArrayAdapter<PriorityItem> {
        PriorityAdapter(Context c, List<PriorityItem> i) {
            super(c, 0, i);
        }

        @NonNull
        @Override
        public View getView(int p, View v, ViewGroup g) {
            return getPView(p, v, g);
        }

        @Override
        public View getDropDownView(int p, View v, ViewGroup g) {
            return getPView(p, v, g);
        }

        private View getPView(int p, View v, ViewGroup g) {
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.item_priority, g, false);
            }
            PriorityItem item = getItem(p);
            if (item != null) {
                ((TextView)v.findViewById(R.id.priorityTextView)).setText(item.l);
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(item.c);
                v.findViewById(R.id.priorityColorView).setBackground(gd);
            }
            return v;
        }
    }
}