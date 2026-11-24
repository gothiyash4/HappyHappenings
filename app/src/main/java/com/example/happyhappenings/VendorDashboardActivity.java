package com.example.happyhappenings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class VendorDashboardActivity extends AppCompatActivity {

    private Button addService, myServices, profile, logout;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_dashboard);

        addService = findViewById(R.id.addService);
        myServices = findViewById(R.id.myServices);
        Button vendorBookings = findViewById(R.id.vendorBookings);
        profile = findViewById(R.id.profile);
        logout = findViewById(R.id.logout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        addService.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditServiceActivity.class);
            startActivity(intent);
        });

        myServices.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyServicesActivity.class);
            startActivity(intent);
        });

        vendorBookings.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBookingsActivity.class);
            startActivity(intent);
        });

        profile.setOnClickListener(v -> showVendorProfile());

        logout.setOnClickListener(v -> {

            auth.signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }

    private void showVendorProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String category = document.getString("category");

                        new AlertDialog.Builder(this)
                                .setTitle("My Vendor Profile")
                                .setMessage("👤 Name: " + (name != null ? name : "Vendor") +
                                        "\n✉️ Email: " + (email != null ? email : user.getEmail()) +
                                        "\n🏷️ Category: " + (category != null ? category : "Event Vendor"))
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(this, "Profile info not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Profile Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
