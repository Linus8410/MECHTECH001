package com.example.mechtech001;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClientRequestAdapter extends RecyclerView.Adapter<ClientRequestAdapter.RequestViewHolder> {
    // ClientRequest.java
    public static class ClientRequest {
        private String clientName;
        private String issueDescription;
        private String clientLocation;

        // Constructor
        public ClientRequest(String clientName, String issueDescription, String clientLocation) {
            this.clientName = clientName;
            this.issueDescription = issueDescription;
            this.clientLocation = clientLocation;
        }

        // Getters
        public String getClientName() {
            return clientName;
        }

        public String getIssueDescription() {
            return issueDescription;
        }

        public String getClientLocation() {
            return clientLocation;
        }
    }


    private Context context;
    private List<ClientRequest> requestList;

    // Constructor
    public ClientRequestAdapter(Context context, List<ClientRequest> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    // ViewHolder class to represent each item
    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView clientName, issueDescription, clientLocation;
        Button acceptButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            clientName = itemView.findViewById(R.id.txtClientName);
            issueDescription = itemView.findViewById(R.id.txtIssueDescription);
            clientLocation = itemView.findViewById(R.id.txtClientLocation);
            acceptButton = itemView.findViewById(R.id.btnAccept);
        }
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.mapsmech, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        // Bind data to the ViewHolder
        ClientRequest request = requestList.get(position);
        holder.clientName.setText(request.getClientName());
        holder.issueDescription.setText("Issue: " + request.getIssueDescription());
        holder.clientLocation.setText("Location: " + request.getClientLocation());

        // Handle Accept button click
        holder.acceptButton.setOnClickListener(v -> {
            Toast.makeText(context, "Accepted Request from " + request.getClientName(), Toast.LENGTH_SHORT).show();

            // Perform further actions (e.g., show mechanic's location to client)
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }
}
