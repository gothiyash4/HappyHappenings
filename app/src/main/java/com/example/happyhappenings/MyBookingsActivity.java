package com.example.happyhappenings;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyBookingsActivity extends AppCompatActivity {

    private TextView pageTitle, pageSubtitle, statusTextView;
    private LinearLayout bookingsListContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        TextView backButton = findViewById(R.id.backButton);
        pageTitle = findViewById(R.id.pageTitle);
        pageSubtitle = findViewById(R.id.pageSubtitle);
        statusTextView = findViewById(R.id.statusTextView);
        bookingsListContainer = findViewById(R.id.bookingsListContainer);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());

        loadUserOrVendorBookings();
    }

    private void loadUserOrVendorBookings() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText("User not logged in");
            return;
        }

        String uid = currentUser.getUid();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";

        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText("Loading bookings...");
        bookingsListContainer.removeAllViews();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String role = userDoc.exists() ? userDoc.getString("role") : "user";
                    boolean isVendor = "vendor".equalsIgnoreCase(role);

                    if (isVendor) {
                        pageTitle.setText("Booking Requests");
                        pageSubtitle.setText("Incoming event booking requests from customers");
                    } else {
                        pageTitle.setText("My Bookings");
                        pageSubtitle.setText("View your submitted event bookings");
                    }

                    fetchBookingsFromFirestore(uid, userEmail, isVendor);
                })
                .addOnFailureListener(e -> fetchBookingsFromFirestore(uid, userEmail, false));
    }

    private void fetchBookingsFromFirestore(String uid, String userEmail, boolean isVendor) {
        db.collection("bookings")
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> matchingBookings = new ArrayList<>();

                    if (result != null && !result.isEmpty()) {
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            String bUserUid = doc.getString("userUid");
                            String bVendorUid = doc.getString("vendorUid");
                            String bVendorId = doc.getString("vendor_id");
                            String bVendorEmail = doc.getString("vendorEmail");

                            if (isVendor) {
                                boolean isVendorMatch = (uid.equals(bVendorUid) || uid.equals(bVendorId)) ||
                                        (!userEmail.isEmpty() && userEmail.equalsIgnoreCase(bVendorEmail));
                                if (isVendorMatch) {
                                    matchingBookings.add(doc);
                                }
                            } else {
                                if (uid.equals(bUserUid)) {
                                    matchingBookings.add(doc);
                                }
                            }
                        }
                    }

                    if (matchingBookings.isEmpty()) {
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText(isVendor ? "No booking requests received yet." : "No bookings found. Book a vendor service to get started!");
                    } else {
                        statusTextView.setVisibility(View.GONE);
                        for (DocumentSnapshot bDoc : matchingBookings) {
                            String serviceTitle = bDoc.getString("serviceTitle");
                            String vendorName = bDoc.getString("vendorName");
                            String userName = bDoc.getString("userName");
                            String price = bDoc.getString("price");
                            String category = bDoc.getString("category");
                            String bookingDate = bDoc.getString("bookingDate");
                            String note = bDoc.getString("note");
                            String status = bDoc.getString("status");

                            addBookingCard(
                                    serviceTitle != null ? serviceTitle : "Event Service",
                                    isVendor ? ("Customer: " + (userName != null ? userName : "User")) : ("Vendor: " + (vendorName != null ? vendorName : "Vendor")),
                                    price != null ? price : "N/A",
                                    category != null ? category : "Event",
                                    bookingDate != null ? bookingDate : "Date TBD",
                                    note != null ? note : "No additional notes",
                                    status != null ? status : "Pending"
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText("Failed to load bookings: " + e.getMessage());
                });
    }

    private void addBookingCard(String serviceTitle, String nameInfo, String price, String category, String date, String note, String status) {

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
        titleText.setText(serviceTitle);
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
        priceText.setText(price.startsWith("₹") ? price : ("₹" + price));
        priceText.setTextSize(16);
        priceText.setTextColor(Color.parseColor("#2563EB"));
        priceText.setTypeface(null, Typeface.BOLD);

        headerRow.addView(titleText);
        headerRow.addView(priceText);

        // Info Row: Name & Status
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = dp(6);
        infoRow.setLayoutParams(infoParams);

        TextView nameText = new TextView(this);
        nameText.setText(nameInfo + " • " + category);
        nameText.setTextSize(13);
        nameText.setTextColor(Color.parseColor("#475569"));
        nameText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        nameText.setLayoutParams(nameParams);

        TextView statusBadge = new TextView(this);
        statusBadge.setText(status);
        statusBadge.setTextSize(11);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setTextColor(Color.parseColor("#0284C7"));
        statusBadge.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#E0F2FE"));
        statusBg.setCornerRadius(dp(6));
        statusBadge.setBackground(statusBg);

        infoRow.addView(nameText);
        infoRow.addView(statusBadge);

        // Details Box (Date & Note)
        LinearLayout detailsBox = new LinearLayout(this);
        detailsBox.setOrientation(LinearLayout.VERTICAL);
        detailsBox.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable detailsBg = new GradientDrawable();
        detailsBg.setColor(Color.parseColor("#F8FAFC"));
        detailsBg.setCornerRadius(dp(10));
        detailsBox.setBackground(detailsBg);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailsParams.topMargin = dp(10);
        detailsBox.setLayoutParams(detailsParams);

        TextView dateText = new TextView(this);
        dateText.setText("📅 Event Date: " + date);
        dateText.setTextSize(13);
        dateText.setTextColor(Color.parseColor("#1E293B"));
        dateText.setTypeface(null, Typeface.BOLD);

        TextView noteText = new TextView(this);
        noteText.setText("📝 Note: " + note);
        noteText.setTextSize(12);
        noteText.setTextColor(Color.parseColor("#64748B"));
        noteText.setPadding(0, dp(4), 0, 0);

        detailsBox.addView(dateText);
        detailsBox.addView(noteText);

        card.addView(headerRow);
        card.addView(infoRow);
        card.addView(detailsBox);

        bookingsListContainer.addView(card);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
