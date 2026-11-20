package com.example.happyhappenings;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class VendorListActivity extends AppCompatActivity {

    private TextView statusTextView;
    private LinearLayout vendorListContainer;
    private FirebaseFirestore db;

    private String categoryName = "";
    private String categoryId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_list);

        TextView backButton = findViewById(R.id.backButton);
        TextView titleTextView = findViewById(R.id.titleTextView);
        TextView subtitleTextView = findViewById(R.id.subtitleTextView);
        statusTextView = findViewById(R.id.statusTextView);
        vendorListContainer = findViewById(R.id.vendorListContainer);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            categoryName = intent.getStringExtra("category_name");
            categoryId = intent.getStringExtra("category_id");
        }

        if (categoryName == null) {
            categoryName = "Vendor";
        }

        String displayCategory = categoryName.substring(0, 1).toUpperCase()
                + categoryName.substring(1);

        titleTextView.setText(displayCategory + " Vendors");
        subtitleTextView.setText("Browse top " + displayCategory.toLowerCase() + " services");

        backButton.setOnClickListener(v -> finish());

        loadVendors();
    }

    private void loadVendors() {

        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText("Loading " + categoryName + " vendors...");
        vendorListContainer.removeAllViews();

        db.collection("users")
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> matchingVendors = extractMatchingVendors(result.getDocuments());
                    if (!matchingVendors.isEmpty()) {
                        displayVendors(matchingVendors);
                    } else {
                        fetchFromVendorsCollection();
                    }
                })
                .addOnFailureListener(e -> fetchFromVendorsCollection());
    }

    private void fetchFromVendorsCollection() {
        db.collection("vendors")
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> matchingVendors = extractMatchingVendors(result.getDocuments());
                    if (!matchingVendors.isEmpty()) {
                        displayVendors(matchingVendors);
                    } else {
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText("No vendors found for " + categoryName);
                    }
                })
                .addOnFailureListener(e -> {
                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText("Firebase Error: " + e.getMessage());
                });
    }

    private List<DocumentSnapshot> extractMatchingVendors(List<DocumentSnapshot> documents) {
        List<DocumentSnapshot> matchingVendors = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {

            String role = doc.getString("role");
            String vendorCategory = doc.getString("category");
            String vendorCategoryId = doc.getString("categoryId");

            boolean isVendorRole = role == null || role.equalsIgnoreCase("vendor");

            boolean isCategoryMatch = vendorCategory != null &&
                    (vendorCategory.equalsIgnoreCase(categoryName) ||
                     vendorCategory.trim().toLowerCase().contains(categoryName.trim().toLowerCase()) ||
                     categoryName.trim().toLowerCase().contains(vendorCategory.trim().toLowerCase()));

            boolean isIdMatch = vendorCategoryId != null &&
                    categoryId != null &&
                    !categoryId.isEmpty() &&
                    vendorCategoryId.equalsIgnoreCase(categoryId);

            if (isVendorRole && (isCategoryMatch || isIdMatch)) {
                matchingVendors.add(doc);
            }
        }
        return matchingVendors;
    }

    private void displayVendors(List<DocumentSnapshot> vendors) {
        statusTextView.setVisibility(View.GONE);
        vendorListContainer.removeAllViews();

        for (DocumentSnapshot vendorDoc : vendors) {
            String name = vendorDoc.getString("name");
            String email = vendorDoc.getString("email");
            String category = vendorDoc.getString("category");

            addVendorCard(
                    name != null ? name : "Vendor",
                    email != null ? email : "No email provided",
                    category != null ? category : categoryName
            );
        }
    }

    private void addVendorCard(String name, String email, String category) {

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

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        String initial = !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "V";
        avatar.setText(initial);
        avatar.setTextSize(20);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setTextColor(Color.parseColor("#2563EB"));
        avatar.setGravity(Gravity.CENTER);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setColor(Color.parseColor("#EAF1FF"));
        avatarBg.setCornerRadius(dp(14));
        avatar.setBackground(avatarBg);

        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(50), dp(50));
        avatar.setLayoutParams(avatarParams);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(dp(14), 0, 0, 0);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        infoLayout.setLayoutParams(infoParams);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextSize(16);
        nameText.setTextColor(Color.parseColor("#172B4D"));
        nameText.setTypeface(null, Typeface.BOLD);

        TextView emailText = new TextView(this);
        emailText.setText(email);
        emailText.setTextSize(13);
        emailText.setTextColor(Color.parseColor("#7B8798"));

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

        infoLayout.addView(nameText);
        infoLayout.addView(emailText);
        infoLayout.addView(categoryBadge);

        topRow.addView(avatar);
        topRow.addView(infoLayout);

        TextView contactButton = new TextView(this);
        contactButton.setText("Contact Vendor");
        contactButton.setTextSize(13);
        contactButton.setTypeface(null, Typeface.BOLD);
        contactButton.setTextColor(Color.WHITE);
        contactButton.setGravity(Gravity.CENTER);
        contactButton.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#2563EB"));
        btnBg.setCornerRadius(dp(10));
        contactButton.setBackground(btnBg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.topMargin = dp(14);
        contactButton.setLayoutParams(btnParams);

        contactButton.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + email));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry for " + category + " service");

            try {
                startActivity(Intent.createChooser(emailIntent, "Contact " + name));
            } catch (Exception e) {
                Toast.makeText(this, "Vendor Email: " + email, Toast.LENGTH_LONG).show();
            }
        });

        card.addView(topRow);
        card.addView(contactButton);

        vendorListContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
