package com.example.mechtech001;

public class Request {
    public String clientId;
    public double clientLatitude;
    public double clientLongitude;
    public String status;

    public Request() {}

    public Request(String clientId, double clientLatitude, double clientLongitude, String status) {
        this.clientId = clientId;
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
        this.status = status;
    }
}

