package com.example.mechtech001;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ClientRequestAdapter extends RecyclerView.Adapter<ClientRequestAdapter.ViewHolder> {

    private List<String> requestList;
    private OnRequestActionListener listener;

    // Interface for callback
    public interface OnRequestActionListener {
        void onRequestAccept(String clientId);
        void onRequestDecline(String clientId);
    }

    public ClientRequestAdapter(List<String> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mechanic_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String clientId = requestList.get(position);
        holder.clientNameTextView.setText("Client: " + clientId);

        holder.acceptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestAccept(clientId);
            }
        });

        holder.declineButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestDecline(clientId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView clientNameTextView;
        Button acceptButton, declineButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            clientNameTextView = itemView.findViewById(R.id.requestText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}

