package com.example.happyhappenings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyServicesActivity extends AppCompatActivity {

    private TextView statusTextView;
    private LinearLayout servicesListContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_services);

        TextView backButton = findViewById(R.id.backButton);
        Button addNewServiceButton = findViewById(R.id.addNewServiceButton);
        statusTextView = findViewById(R.id.statusTextView);
        servicesListContainer = findViewById(R.id.servicesListContainer);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());
        addNewServiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditServiceActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVendorServices();
    }

    private void loadVendorServices() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText("Vendor not authenticated");
            return;
        }

        String uid = user.getUid();
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText("Loading your listed services...");
        servicesListContainer.removeAllViews();

        db.collection("services")
                .whereEqualTo("vendorUid", uid)
                .get()
                .addOnSuccessListener(result -> {
                    if (result == null || result.isEmpty()) {
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText("No services listed yet. Click '+ Add New Service' to list your services.");
                    } else {
                        statusTextView.setVisibility(View.GONE);
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            String serviceId = doc.getId();
                            String title = doc.getString("title");
                            String price = doc.getString("price");
                            String description = doc.getString("description");
                            String category = doc.getString("category");

                            addServiceCard(
                                    serviceId,
                                    title != null ? title : "Service Package",
                                    price != null ? price : "0",
                                    description != null ? description : "No description provided",
                                    category != null ? category : "General"
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText("Error loading services: " + e.getMessage());
                });
    }

    private void addServiceCard(String serviceId, String title, String price, String description, String category) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), Color.parseColor("#E8EDF5"));

        card.setBackground(background);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);

        // Header Row: Title & Price
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(16);
        titleText.setTextColor(Color.parseColor("#172B4D"));
        titleText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        titleText.setLayoutParams(titleParams);

        TextView priceText = new TextView(this);
        priceText.setText("₹" + price);
        priceText.setTextSize(16);
        priceText.setTextColor(Color.parseColor("#2563EB"));
        priceText.setTypeface(null, Typeface.BOLD);

        headerRow.addView(titleText);
        headerRow.addView(priceText);

        // Category Tag
        TextView categoryBadge = new TextView(this);
        categoryBadge.setText(category);
        categoryBadge.setTextSize(11);
        categoryBadge.setTextColor(Color.parseColor("#2563EB"));
        categoryBadge.setTypeface(null, Typeface.BOLD);
        categoryBadge.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#EFF6FF"));
        badgeBg.setCornerRadius(dp(6));
        categoryBadge.setBackground(badgeBg);

        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.topMargin = dp(6);
        categoryBadge.setLayoutParams(badgeParams);

        // Description
        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextSize(13);
        descText.setTextColor(Color.parseColor("#64748B"));

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = dp(8);
        descText.setLayoutParams(descParams);

        // Actions Row: Edit & Delete
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(14);
        actionsRow.setLayoutParams(actionsParams);

        // Edit Button
        TextView editButton = new TextView(this);
        editButton.setText("Edit Service");
        editButton.setTextSize(13);
        editButton.setTypeface(null, Typeface.BOLD);
        editButton.setTextColor(Color.parseColor("#2563EB"));
        editButton.setGravity(Gravity.CENTER);
        editButton.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable editBg = new GradientDrawable();
        editBg.setColor(Color.parseColor("#EFF6FF"));
        editBg.setCornerRadius(dp(8));
        editBg.setStroke(dp(1), Color.parseColor("#BFDBFE"));
        editButton.setBackground(editBg);

        LinearLayout.LayoutParams editBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        editBtnParams.rightMargin = dp(6);
        editButton.setLayoutParams(editBtnParams);

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditServiceActivity.class);
            intent.putExtra("service_id", serviceId);
            intent.putExtra("title", title);
            intent.putExtra("price", price);
            intent.putExtra("description", description);
            intent.putExtra("category", category);
            startActivity(intent);
        });

        // Delete Button
        TextView deleteButton = new TextView(this);
        deleteButton.setText("Delete");
        deleteButton.setTextSize(13);
        deleteButton.setTypeface(null, Typeface.BOLD);
        deleteButton.setTextColor(Color.parseColor("#DC2626"));
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable deleteBg = new GradientDrawable();
        deleteBg.setColor(Color.parseColor("#FEF2F2"));
        deleteBg.setCornerRadius(dp(8));
        deleteBg.setStroke(dp(1), Color.parseColor("#FECACA"));
        deleteButton.setBackground(deleteBg);

        LinearLayout.LayoutParams deleteBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        deleteBtnParams.leftMargin = dp(6);
        deleteButton.setLayoutParams(deleteBtnParams);

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete '" + title + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteService(serviceId))
                .setNegativeButton("Cancel", null)
                .show());

        actionsRow.addView(editButton);
        actionsRow.addView(deleteButton);

        card.addView(headerRow);
        card.addView(categoryBadge);
        card.addView(descText);
        card.addView(actionsRow);

        servicesListContainer.addView(card);
    }

    private void deleteService(String serviceId) {
        db.collection("services")
                .document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Service deleted successfully", Toast.LENGTH_SHORT).show();
                    loadVendorServices();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
