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
    private OnRequestAcceptListener listener;

    // Interface for callback
    public interface OnRequestAcceptListener {
        void onRequestAccept(String clientId);
    }

    public ClientRequestAdapter(List<String> requestList, OnRequestAcceptListener listener) {
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
        holder.clientNameTextView.setText(clientId);

        holder.acceptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestAccept(clientId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView clientNameTextView;
        Button acceptButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            clientNameTextView = itemView.findViewById(R.id.clientRequestText);
            acceptButton = itemView.findViewById(R.id.btnAccept);
        }
    }
}
