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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class UserDashboardActivity extends AppCompatActivity {

    Button logout, bookings;
    LinearLayout categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        logout = findViewById(R.id.logout);
        bookings = findViewById(R.id.bookings);
        categoryList = findViewById(R.id.categoryList);

        loadCategories();

        logout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });

        bookings.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyBookingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadCategories() {

        categoryList.removeAllViews();
        populateDefaultCategoryCards();

        FirebaseFirestore.getInstance()
                .collection("categories")
                .get()
                .addOnSuccessListener(result -> {
                    if (result != null && !result.isEmpty()) {
                        for (DocumentSnapshot document : result.getDocuments()) {

                            Boolean status = document.getBoolean("status");
                            if (Boolean.FALSE.equals(status)) {
                                continue;
                            }

                            String categoryId = document.getId();
                            String name = document.getString("name");

                            if (name == null || name.trim().isEmpty()) {
                                name = categoryId;
                            }

                            name = name.substring(0, 1).toUpperCase() + name.substring(1);

                            // Add if not already present
                            if (!containsCategory(name)) {
                                addCategoryCard(name, categoryId);
                            }
                        }
                    }
                });
    }

    private boolean containsCategory(String categoryName) {
        for (int i = 0; i < categoryList.getChildCount(); i++) {
            View child = categoryList.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout card = (LinearLayout) child;
                if (card.getChildCount() > 1 && card.getChildAt(1) instanceof LinearLayout) {
                    LinearLayout details = (LinearLayout) card.getChildAt(1);
                    if (details.getChildCount() > 0 && details.getChildAt(0) instanceof TextView) {
                        TextView title = (TextView) details.getChildAt(0);
                        if (title.getText().toString().equalsIgnoreCase(categoryName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void populateDefaultCategoryCards() {
        addCategoryCard("Decoration", "decoration");
        addCategoryCard("Catering", "catering");
        addCategoryCard("Dresses", "dresses");
        addCategoryCard("Photography", "photography");
        addCategoryCard("Music & DJ", "music_dj");
        addCategoryCard("Venues", "venues");
        addCategoryCard("Makeup & Hair", "makeup_hair");
        addCategoryCard("Invitation Cards", "invitation_cards");
    }

    private void addCategoryCard(String name, String categoryId) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), Color.parseColor("#E8EDF5"));

        card.setBackground(background);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(90)
                );

        cardParams.bottomMargin = dp(15);
        card.setLayoutParams(cardParams);

        TextView symbol = new TextView(this);
        symbol.setText(getCategoryIcon(name));
        symbol.setTextSize(27);
        symbol.setGravity(Gravity.CENTER);

        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(Color.parseColor("#EAF1FF"));
        iconBackground.setCornerRadius(dp(12));
        symbol.setBackground(iconBackground);

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(55), dp(55));

        symbol.setLayoutParams(iconParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(14), 0, 0, 0);
        details.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#172B4D"));
        title.setTypeface(null, Typeface.BOLD);

        TextView description = new TextView(this);
        description.setText(getCategoryDescription(name));
        description.setTextSize(12);
        description.setTextColor(Color.parseColor("#7B8798"));

        details.addView(title);
        details.addView(description);

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        details.setLayoutParams(detailParams);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(30);
        arrow.setTextColor(Color.parseColor("#2563EB"));

        card.addView(symbol);
        card.addView(details);
        card.addView(arrow);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, VendorListActivity.class);
            intent.putExtra("category_name", name);
            intent.putExtra("category_id", categoryId);
            startActivity(intent);
        });

        categoryList.addView(card);
    }

    private String getCategoryIcon(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("decorat")) return "🎈";
        if (lower.contains("cater")) return "🍽️";
        if (lower.contains("dress") || lower.contains("outfit")) return "👗";
        if (lower.contains("photo") || lower.contains("camera")) return "📷";
        if (lower.contains("music") || lower.contains("dj")) return "🎧";
        if (lower.contains("venue") || lower.contains("hall")) return "🏰";
        if (lower.contains("makeup") || lower.contains("hair")) return "💄";
        if (lower.contains("invit") || lower.contains("card")) return "💌";
        return "🎉";
    }

    private String getCategoryDescription(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("decorat")) return "Stage and venue decoration";
        if (lower.contains("cater")) return "Food and catering services";
        if (lower.contains("dress") || lower.contains("outfit")) return "Find your perfect outfit";
        if (lower.contains("photo") || lower.contains("camera")) return "Capture your special moments";
        if (lower.contains("music") || lower.contains("dj")) return "Sound system and DJ services";
        if (lower.contains("venue") || lower.contains("hall")) return "Banquet halls and event spaces";
        if (lower.contains("makeup") || lower.contains("hair")) return "Bridal makeup and styling";
        if (lower.contains("invit") || lower.contains("card")) return "Design and print invitations";
        return "Explore event services";
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }
}
