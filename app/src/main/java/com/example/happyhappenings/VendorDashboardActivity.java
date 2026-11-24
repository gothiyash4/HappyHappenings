package com.example.happyhappenings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class VendorDashboardActivity extends AppCompatActivity {

    private TextView vendorAvatar, vendorWelcomeText, vendorCategoryBadge;
    private View cardAddService, cardMyServices, cardBookingRequests, cardMyProfile;
    private Button logout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_dashboard);

        vendorAvatar = findViewById(R.id.vendorAvatar);
        vendorWelcomeText = findViewById(R.id.vendorWelcomeText);
        vendorCategoryBadge = findViewById(R.id.vendorCategoryBadge);

        cardAddService = findViewById(R.id.cardAddService);
        cardMyServices = findViewById(R.id.cardMyServices);
        cardBookingRequests = findViewById(R.id.cardBookingRequests);
        cardMyProfile = findViewById(R.id.cardMyProfile);
        logout = findViewById(R.id.logout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadVendorInfo();

        cardAddService.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditServiceActivity.class);
            startActivity(intent);
        });

        cardMyServices.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyServicesActivity.class);
            startActivity(intent);
        });

        cardBookingRequests.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBookingsActivity.class);
            startActivity(intent);
        });

        cardMyProfile.setOnClickListener(v -> showVendorProfile());

        logout.setOnClickListener(v -> {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            auth.signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }

    private void loadVendorInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String category = document.getString("category");

                        if (name != null && !name.trim().isEmpty()) {
                            vendorWelcomeText.setText("Welcome, " + name + "!");
                            vendorAvatar.setText(name.substring(0, 1).toUpperCase());
                        }

                        if (category != null && !category.trim().isEmpty()) {
                            vendorCategoryBadge.setText("Category: " + category);
                        }
                    }
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
