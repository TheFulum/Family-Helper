package com.example.familyhelper.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.familyhelper.R;
import com.example.familyhelper.data.models.Comment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private FirebaseFirestore db;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvCommentText.setText(comment.getText());

        // Форматируем время
        String time = new SimpleDateFormat("HH:mm dd.MM.yy", Locale.getDefault()).format(comment.getTimestamp());
        holder.tvCommentTime.setText(time);

        // Загружаем имя пользователя
        loadUserName(comment.getUserId(), holder.tvCommentUser);
    }

    private void loadUserName(String userId, TextView textView) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String name = doc.getString("name");

                        // Отображаем @username если есть, иначе имя
                        if (username != null && !username.isEmpty()) {
                            textView.setText("@" + username);
                        } else if (name != null && !name.isEmpty()) {
                            textView.setText(name);
                        } else {
                            textView.setText("Пользователь");
                        }
                    } else {
                        textView.setText("Пользователь");
                    }
                })
                .addOnFailureListener(e -> {
                    textView.setText("Пользователь");
                });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentText, tvCommentUser, tvCommentTime;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvCommentUser = itemView.findViewById(R.id.tvCommentUser);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
        }
    }
}