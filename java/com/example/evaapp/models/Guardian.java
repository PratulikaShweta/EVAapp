package com.example.evaapp.models;

public class Guardian {
    private String id;
    private String name;
    private String phone;

    public Guardian() {}  // Required for Firestore

    public Guardian(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
