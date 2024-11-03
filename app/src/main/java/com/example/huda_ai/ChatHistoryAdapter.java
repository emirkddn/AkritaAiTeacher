package com.example.huda_ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {

    private final List<String> chatHistoryList;
    private final OnChatHistoryClickListener listener;

    public interface OnChatHistoryClickListener {
        void onChatHistoryClick(String fileName);
    }

    public ChatHistoryAdapter(List<String> chatHistoryList, OnChatHistoryClickListener listener) {
        this.chatHistoryList = chatHistoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fileName = chatHistoryList.get(position);
        holder.textView.setText(fileName);
        holder.itemView.setOnClickListener(v -> listener.onChatHistoryClick(fileName));
    }

    @Override
    public int getItemCount() {
        return chatHistoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

