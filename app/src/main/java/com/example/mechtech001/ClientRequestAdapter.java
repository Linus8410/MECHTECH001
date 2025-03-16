package com.example.mechtech001;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ClientRequestAdapter extends RecyclerView.Adapter<ClientRequestAdapter.ViewHolder> {

    private final List<String> requestList;
    private final OnRequestAcceptListener listener;
    private final DatabaseReference databaseReference;

    public interface OnRequestAcceptListener {
        void onRequestAccept(String clientId);
        void onRequestDecline(String clientId); // Add this method
    }

    public ClientRequestAdapter(List<String> requestList, OnRequestAcceptListener listener) {
        this.requestList = requestList;
        this.listener = listener;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("requests");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mechanic_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String clientId = requestList.get(position);
        holder.clientNameTextView.setText(clientId);

        holder.acceptButton.setOnClickListener(v -> handleRequest(clientId, position, true, v));
        holder.declineButton.setOnClickListener(v -> handleRequest(clientId, position, false, v));
    }

    private void handleRequest(String clientId, int position, boolean isAccepted, View v) {
        if (isAccepted && listener != null) {
            listener.onRequestAccept(clientId);
        } else if (!isAccepted && listener != null) {
            listener.onRequestDecline(clientId); // Handle decline action
        }

        String message = isAccepted ? "Accepted: " : "Declined: ";
        Toast.makeText(v.getContext(), message + clientId, Toast.LENGTH_SHORT).show();

        databaseReference.child(clientId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                requestList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, requestList.size());
            } else {
                Toast.makeText(v.getContext(), "Error removing request", Toast.LENGTH_SHORT).show();
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
