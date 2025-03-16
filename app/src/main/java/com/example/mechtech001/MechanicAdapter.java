package com.example.mechtech001;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MechanicAdapter extends ArrayAdapter<String> {

    private final DatabaseReference databaseReference;

    public MechanicAdapter(Context context, List<String> requests) {
        super(context, R.layout.client_request_item, requests);
        this.databaseReference = FirebaseDatabase.getInstance().getReference("requests");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.client_request_item, parent, false);
            holder = new ViewHolder();
            holder.requestText = convertView.findViewById(R.id.requestText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String clientId = getItem(position);
        holder.requestText.setText("Client: " + clientId);

        return convertView;
    }

    static class ViewHolder {
        TextView requestText;
    }
}