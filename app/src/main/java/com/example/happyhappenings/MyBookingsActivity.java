package com.example.happyhappenings;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MyBookingsActivity extends AppCompatActivity {

    private TextView pageTitle, pageSubtitle, statusTextView, dateFilterButton, resetFilterButton;
    private EditText searchFilterEditText;
    private Spinner categoryFilterSpinner;
    private LinearLayout bookingsListContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<DocumentSnapshot> allBookings = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();

    private String selectedDateFilter = null;
    private String selectedCategoryFilter = "All Categories";
    private String searchQuery = "";
    private boolean isVendorUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        TextView backButton = findViewById(R.id.backButton);
        pageTitle = findViewById(R.id.pageTitle);
        pageSubtitle = findViewById(R.id.pageSubtitle);
        statusTextView = findViewById(R.id.statusTextView);
        bookingsListContainer = findViewById(R.id.bookingsListContainer);

        searchFilterEditText = findViewById(R.id.searchFilterEditText);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        dateFilterButton = findViewById(R.id.dateFilterButton);
        resetFilterButton = findViewById(R.id.resetFilterButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton.setOnClickListener(v -> finish());

        setupFilterControls();
        loadUserOrVendorBookings();
    }

    private void setupFilterControls() {
        categories.add("All Categories");
        categories.add("Decoration");
        categories.add("Catering");
        categories.add("Dresses");
        categories.add("Photography");
        categories.add("Music & DJ");
        categories.add("Venues");
        categories.add("Makeup & Hair");
        categories.add("Invitation Cards");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(catAdapter);

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategoryFilter = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchFilterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dateFilterButton.setOnClickListener(v -> showDatePickerFilter());

        resetFilterButton.setOnClickListener(v -> {
            searchFilterEditText.setText("");
            searchQuery = "";
            selectedDateFilter = null;
            dateFilterButton.setText("📅 Date: All");
            categoryFilterSpinner.setSelection(0);
            selectedCategoryFilter = "All Categories";
            applyFilters();
            Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDatePickerFilter() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateFilter = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    dateFilterButton.setText("📅 " + selectedDateFilter);
                    applyFilters();
                },
                year, month, day
        );
        datePickerDialog.show();
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
                    isVendorUser = "vendor".equalsIgnoreCase(role);

                    if (isVendorUser) {
                        pageTitle.setText("Booking Requests");
                        pageSubtitle.setText("Incoming event booking requests from customers");
                    } else {
                        pageTitle.setText("My Bookings");
                        pageSubtitle.setText("View your submitted event bookings");
                    }

                    fetchBookingsFromFirestore(uid, userEmail, isVendorUser);
                })
                .addOnFailureListener(e -> fetchBookingsFromFirestore(uid, userEmail, false));
    }

    private void fetchBookingsFromFirestore(String uid, String userEmail, boolean isVendor) {
        db.collection("bookings")
                .get()
                .addOnSuccessListener(result -> {
                    allBookings.clear();

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
                                    allBookings.add(doc);
                                }
                            } else {
                                if (uid.equals(bUserUid)) {
                                    allBookings.add(doc);
                                }
                            }
                        }
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText("Failed to load bookings: " + e.getMessage());
                });
    }

    private void applyFilters() {
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : allBookings) {

            String category = doc.getString("category");
            String bookingDate = doc.getString("bookingDate");
            String vendorName = doc.getString("vendorName");
            String userName = doc.getString("userName");
            String serviceTitle = doc.getString("serviceTitle");

            boolean matchesCategory = selectedCategoryFilter.equals("All Categories") ||
                    (category != null && category.equalsIgnoreCase(selectedCategoryFilter));

            boolean matchesDate = selectedDateFilter == null ||
                    (bookingDate != null && bookingDate.contains(selectedDateFilter));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (vendorName != null && vendorName.toLowerCase().contains(searchQuery)) ||
                    (userName != null && userName.toLowerCase().contains(searchQuery)) ||
                    (serviceTitle != null && serviceTitle.toLowerCase().contains(searchQuery)) ||
                    (category != null && category.toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesDate && matchesSearch) {
                filteredList.add(doc);
            }
        }

        renderFilteredBookings(filteredList);
    }

    private void renderFilteredBookings(List<DocumentSnapshot> bookings) {
        bookingsListContainer.removeAllViews();

        if (bookings.isEmpty()) {
            statusTextView.setVisibility(View.VISIBLE);
            if (allBookings.isEmpty()) {
                statusTextView.setText(isVendorUser ? "No booking requests received yet." : "No bookings found. Book a vendor service to get started!");
            } else {
                statusTextView.setText("No bookings match your current filter criteria.");
            }
        } else {
            statusTextView.setVisibility(View.GONE);

            for (DocumentSnapshot bDoc : bookings) {
                String bookingId = bDoc.getId();
                String serviceTitle = bDoc.getString("serviceTitle");
                String vendorName = bDoc.getString("vendorName");
                String userName = bDoc.getString("userName");
                String price = bDoc.getString("price");
                String category = bDoc.getString("category");
                String bookingDate = bDoc.getString("bookingDate");
                String note = bDoc.getString("note");
                String status = bDoc.getString("status");

                addBookingCard(
                        bookingId,
                        serviceTitle != null ? serviceTitle : "Event Service",
                        isVendorUser ? ("Customer: " + (userName != null ? userName : "User")) : ("Vendor: " + (vendorName != null ? vendorName : "Vendor")),
                        price != null ? price : "N/A",
                        category != null ? category : "Event",
                        bookingDate != null ? bookingDate : "Date TBD",
                        note != null ? note : "No additional notes",
                        status != null ? status : "Pending",
                        isVendorUser
                );
            }
        }
    }

    private void addBookingCard(String bookingId, String serviceTitle, String nameInfo, String price, String category, String date, String note, String status, boolean isVendor) {

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
        statusBadge.setPadding(dp(8), dp(3), dp(8), dp(3));

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(dp(6));

        if ("Accepted".equalsIgnoreCase(status)) {
            statusBadge.setTextColor(Color.parseColor("#15803D"));
            statusBg.setColor(Color.parseColor("#DCFCE7"));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            statusBadge.setTextColor(Color.parseColor("#B91C1C"));
            statusBg.setColor(Color.parseColor("#FEE2E2"));
        } else {
            statusBadge.setTextColor(Color.parseColor("#0284C7"));
            statusBg.setColor(Color.parseColor("#E0F2FE"));
        }
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

        // Vendor Accept / Reject Actions
        if (isVendor && "Pending".equalsIgnoreCase(status)) {
            LinearLayout vendorActionsRow = new LinearLayout(this);
            vendorActionsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams vParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            vParams.topMargin = dp(12);
            vendorActionsRow.setLayoutParams(vParams);

            TextView acceptBtn = new TextView(this);
            acceptBtn.setText("Accept Request");
            acceptBtn.setTextSize(13);
            acceptBtn.setTypeface(null, Typeface.BOLD);
            acceptBtn.setTextColor(Color.WHITE);
            acceptBtn.setGravity(Gravity.CENTER);
            acceptBtn.setPadding(dp(10), dp(8), dp(10), dp(8));

            GradientDrawable acceptBg = new GradientDrawable();
            acceptBg.setColor(Color.parseColor("#16A34A"));
            acceptBg.setCornerRadius(dp(8));
            acceptBtn.setBackground(acceptBg);

            LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            acceptParams.rightMargin = dp(6);
            acceptBtn.setLayoutParams(acceptParams);

            acceptBtn.setOnClickListener(v -> updateBookingStatus(bookingId, "Accepted"));

            TextView rejectBtn = new TextView(this);
            rejectBtn.setText("Reject");
            rejectBtn.setTextSize(13);
            rejectBtn.setTypeface(null, Typeface.BOLD);
            rejectBtn.setTextColor(Color.WHITE);
            rejectBtn.setGravity(Gravity.CENTER);
            rejectBtn.setPadding(dp(10), dp(8), dp(10), dp(8));

            GradientDrawable rejectBg = new GradientDrawable();
            rejectBg.setColor(Color.parseColor("#DC2626"));
            rejectBg.setCornerRadius(dp(8));
            rejectBtn.setBackground(rejectBg);

            LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            rejectParams.leftMargin = dp(6);
            rejectBtn.setLayoutParams(rejectParams);

            rejectBtn.setOnClickListener(v -> updateBookingStatus(bookingId, "Rejected"));

            vendorActionsRow.addView(acceptBtn);
            vendorActionsRow.addView(rejectBtn);

            card.addView(vendorActionsRow);
        }

        bookingsListContainer.addView(card);
    }

    private void updateBookingStatus(String bookingId, String newStatus) {
        if (bookingId == null || bookingId.isEmpty()) return;

        db.collection("bookings")
                .document(bookingId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking " + newStatus.toLowerCase() + "!", Toast.LENGTH_SHORT).show();
                    loadUserOrVendorBookings();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
