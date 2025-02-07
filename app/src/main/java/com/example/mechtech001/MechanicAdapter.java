package com.example.mechtech001;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MechanicAdapter extends RecyclerView.Adapter<MechanicAdapter.MechanicViewHolder> {

    private ArrayList<String> mechanicsList;

    public MechanicAdapter(ArrayList<String> mechanicsList) {
        this.mechanicsList = mechanicsList;
    }

    @Override
    public MechanicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mechanic, parent, false);
        return new MechanicViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MechanicViewHolder holder, int position) {
        holder.mechanicName.setText(mechanicsList.get(position));
    }

    @Override
    public int getItemCount() {
        return mechanicsList.size();
    }

    public class MechanicViewHolder extends RecyclerView.ViewHolder {
        public TextView mechanicName;

        public MechanicViewHolder(View view) {
            super(view);
            mechanicName = view.findViewById(R.id.txtMechanicName);
        }
    }
}

