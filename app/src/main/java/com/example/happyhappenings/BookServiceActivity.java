package com.example.happyhappenings;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookServiceActivity extends AppCompatActivity {

    private TextView summaryServiceTitle, summaryVendorName, summaryCategory, summaryPrice;
    private EditText eventNameEditText, bookingDateEditText, bookingNoteEditText;
    private Button confirmBookingButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String vendorUid = "";
    private String vendorName = "";
    private String vendorEmail = "";
    private String serviceTitle = "";
    private String category = "";
    private String price = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_service);

        TextView backButton = findViewById(R.id.backButton);
        summaryServiceTitle = findViewById(R.id.summaryServiceTitle);
        summaryVendorName = findViewById(R.id.summaryVendorName);
        summaryCategory = findViewById(R.id.summaryCategory);
        summaryPrice = findViewById(R.id.summaryPrice);
        eventNameEditText = findViewById(R.id.eventNameEditText);
        bookingDateEditText = findViewById(R.id.bookingDateEditText);
        bookingNoteEditText = findViewById(R.id.bookingNoteEditText);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            vendorUid = intent.getStringExtra("vendor_id");
            if (vendorUid == null || vendorUid.isEmpty()) {
                vendorUid = intent.getStringExtra("vendorUid");
            }
            vendorName = intent.getStringExtra("vendorName");
            vendorEmail = intent.getStringExtra("vendorEmail");
            if (vendorEmail == null || vendorEmail.isEmpty()) {
                vendorEmail = intent.getStringExtra("email");
            }
            serviceTitle = intent.getStringExtra("serviceTitle");
            category = intent.getStringExtra("category");
            price = intent.getStringExtra("price");
        }

        if (serviceTitle == null) serviceTitle = "Service Package";
        if (vendorName == null) vendorName = "Vendor";
        if (category == null) category = "General";
        if (price == null) price = "N/A";

        summaryServiceTitle.setText(serviceTitle);
        summaryVendorName.setText("By " + vendorName);
        summaryCategory.setText(category);
        summaryPrice.setText(price);

        backButton.setOnClickListener(v -> finish());
        bookingDateEditText.setOnClickListener(v -> showDatePicker());
        confirmBookingButton.setOnClickListener(v -> submitBooking());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    bookingDateEditText.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void submitBooking() {
        String eventName = eventNameEditText.getText().toString().trim();
        String bookingDate = bookingDateEditText.getText().toString().trim();
        String note = bookingNoteEditText.getText().toString().trim();

        if (eventName.isEmpty()) {
            eventName = serviceTitle + " Event";
        }

        if (bookingDate.isEmpty()) {
            Toast.makeText(this, "Please select an event date", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to submit a booking", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = currentUser.getUid();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        confirmBookingButton.setEnabled(false);

        final String finalEventName = eventName;

        // Fetch user's profile name from Firestore
        db.collection("users")
                .document(userUid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String userName = userDoc.exists() ? userDoc.getString("name") : "";
                    if (userName == null || userName.trim().isEmpty()) {
                        userName = userEmail;
                    }

                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("eventName", finalEventName);
                    bookingData.put("userUid", userUid);
                    bookingData.put("userName", userName);
                    bookingData.put("userEmail", userEmail);
                    bookingData.put("vendorUid", vendorUid != null ? vendorUid : "");
                    bookingData.put("vendor_id", vendorUid != null ? vendorUid : "");
                    bookingData.put("vendorName", vendorName);
                    bookingData.put("vendorEmail", vendorEmail != null ? vendorEmail : "");
                    bookingData.put("serviceTitle", serviceTitle);
                    bookingData.put("category", category);
                    bookingData.put("price", price);
                    bookingData.put("bookingDate", bookingDate);
                    bookingData.put("note", note);
                    bookingData.put("message", note);
                    bookingData.put("status", "Pending");
                    bookingData.put("createdAt", System.currentTimeMillis());

                    db.collection("bookings")
                            .add(bookingData)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(this, "Booking request submitted successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                confirmBookingButton.setEnabled(true);
                                String msg = e.getMessage() != null ? e.getMessage() : "";
                                if (msg.contains("PERMISSION_DENIED")) {
                                    new AlertDialog.Builder(this)
                                            .setTitle("Firebase Rules Update Needed")
                                            .setMessage("Permission Denied: Please update Firestore Security Rules in Firebase Console for the 'bookings' collection to allow write access.")
                                            .setPositiveButton("OK", null)
                                            .show();
                                } else {
                                    Toast.makeText(this, "Booking Error: " + msg, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    confirmBookingButton.setEnabled(true);
                    Toast.makeText(this, "Failed to load user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
