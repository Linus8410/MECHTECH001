package com.example.mechtech001;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class Mechanic extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapsmech); // Ensure this is your main layout file

        RecyclerView recyclerView = findViewById(R.id.recyclerRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Sample data for testing
        List<ClientRequestAdapter.ClientRequest> requests = new ArrayList<>();
        requests.add(new ClientRequestAdapter.ClientRequest("John Doe", "Flat Tire", "123 Main St"));
        requests.add(new ClientRequestAdapter.ClientRequest("Jane Smith", "Engine Trouble", "456 Elm St"));
        requests.add(new ClientRequestAdapter.ClientRequest("Alice Johnson", "Dead Battery", "789 Pine St"));

        // Set adapter
        ClientRequestAdapter adapter = new ClientRequestAdapter(this, requests);
        recyclerView.setAdapter(adapter);
    }
}
