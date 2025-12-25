package com.example.familyhelper.ui.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;
    private String currentUserId;
    private FirebaseFirestore db;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskStatusChanged(Task task, boolean isCompleted);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener, String currentUserId) {
        this.taskList = taskList;
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, area, createdBy, createdTime;
        CheckBox completed;
        ImageView priority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            area = itemView.findViewById(R.id.tvArea);
            createdBy = itemView.findViewById(R.id.tvCreatedBy);
            createdTime = itemView.findViewById(R.id.tvCreatedTime);
            completed = itemView.findViewById(R.id.cbCompleted);
            priority = itemView.findViewById(R.id.ivPriority);
        }

        void bind(Task task) {
            title.setText(task.getTitle());
            area.setText(task.getArea());

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = timeFormat.format(new Date(task.getCreatedAt()));
            createdTime.setText(time);

            loadUserName(task.getCreatedBy());

            boolean isCompleted = task.isCompleted();

            completed.setOnCheckedChangeListener(null);
            completed.setChecked(isCompleted);

            if (isCompleted) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setAlpha(0.5f);
            } else {
                title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                title.setAlpha(1.0f);
            }

            int priorityColor;
            switch (task.getPriority()) {
                case 1: priorityColor = 0xFFFFEB3B; break;
                case 2: priorityColor = 0xFFFF9800; break;
                case 3: priorityColor = 0xFFF44336; break;
                default: priorityColor = 0xFF4CAF50; break;
            }
            priority.setColorFilter(priorityColor);

            boolean canEditStatus = !isCompleted || (task.getCompletedBy() != null && task.getCompletedBy().equals(currentUserId));
            completed.setEnabled(canEditStatus);
            TextView closedLabel = itemView.findViewById(R.id.tvClosedLabel);
            if (!canEditStatus) {
                completed.setVisibility(View.GONE);
                if (closedLabel != null) closedLabel.setVisibility(View.VISIBLE);
            } else {
                completed.setVisibility(View.VISIBLE);
                if (closedLabel != null) closedLabel.setVisibility(View.GONE);
            }

            completed.setOnCheckedChangeListener((v, isChecked) -> {
                if (canEditStatus) {
                    listener.onTaskStatusChanged(task, isChecked);
                }
            });

            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }

        private void loadUserName(String userId) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String username = doc.getString("username");
                            String name = doc.getString("name");

                            if (username != null && !username.isEmpty()) {
                                createdBy.setText("@" + username);
                            } else if (name != null && !name.isEmpty()) {
                                createdBy.setText(name);
                            } else {
                                createdBy.setText("Пользователь");
                            }
                        } else {
                            createdBy.setText("Пользователь");
                        }
                    })
                    .addOnFailureListener(e -> {
                        createdBy.setText("Пользователь");
                    });
        }
    }
}