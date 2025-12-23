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
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;
    private String currentUserId;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskStatusChanged(Task task, boolean isCompleted);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener, String currentUserId) {
        this.taskList = taskList;
        this.listener = listener;
        this.currentUserId = currentUserId;
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
        TextView title, area;
        CheckBox completed;
        ImageView priority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            area = itemView.findViewById(R.id.tvArea);
            completed = itemView.findViewById(R.id.cbCompleted);
            priority = itemView.findViewById(R.id.ivPriority);
        }

        void bind(Task task) {
            title.setText(task.getTitle());
            area.setText(task.getArea());

            boolean isCompleted = task.isCompleted();
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
    }
}