package com.example.mechtech001;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MechanicAdapter extends ArrayAdapter<String> {

    public MechanicAdapter(Context context, List<String> requests) {
        super(context, R.layout.request_item, requests);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.request_item, parent, false);
        }

        String clientId = getItem(position);
        TextView requestText = convertView.findViewById(R.id.requestText);
        requestText.setText("Client: " + clientId);

        return convertView;
    }
}

