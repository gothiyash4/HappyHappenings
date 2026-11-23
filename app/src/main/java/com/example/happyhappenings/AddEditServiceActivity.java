package com.example.happyhappenings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddEditServiceActivity extends AppCompatActivity {

    private EditText serviceTitle, servicePrice, serviceDescription;
    private Spinner mainCategorySpinner, subCategorySpinner;
    private Button saveServiceButton, deleteServiceButton;
    private TextView pageTitle;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String serviceId = null;
    private boolean isEditMode = false;

    private final List<String> mainCategories = new ArrayList<>();
    private final Map<String, List<String>> subCategoriesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_service);

        TextView backButton = findViewById(R.id.backButton);
        pageTitle = findViewById(R.id.pageTitle);
        serviceTitle = findViewById(R.id.serviceTitle);
        servicePrice = findViewById(R.id.servicePrice);
        serviceDescription = findViewById(R.id.serviceDescription);
        mainCategorySpinner = findViewById(R.id.mainCategorySpinner);
        subCategorySpinner = findViewById(R.id.subCategorySpinner);
        saveServiceButton = findViewById(R.id.saveServiceButton);
        deleteServiceButton = findViewById(R.id.deleteServiceButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupCategoriesData();
        setupSpinners();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("service_id")) {
            serviceId = intent.getStringExtra("service_id");
            isEditMode = serviceId != null && !serviceId.isEmpty();
        }

        if (isEditMode) {
            pageTitle.setText("Edit Service");
            saveServiceButton.setText("Update Service");
            deleteServiceButton.setVisibility(View.VISIBLE);

            if (intent != null) {
                serviceTitle.setText(intent.getStringExtra("title"));
                servicePrice.setText(intent.getStringExtra("price"));
                serviceDescription.setText(intent.getStringExtra("description"));

                String category = intent.getStringExtra("category");
                if (category != null && mainCategories.contains(category)) {
                    mainCategorySpinner.setSelection(mainCategories.indexOf(category));
                }
            }
        } else {
            pageTitle.setText("Add Service");
            saveServiceButton.setText("Save Service");
            deleteServiceButton.setVisibility(View.GONE);

            // Fetch logged-in vendor's primary category from Firestore if available
            fetchVendorDefaultCategory();
        }

        backButton.setOnClickListener(v -> finish());
        saveServiceButton.setOnClickListener(v -> saveOrUpdateService());
        deleteServiceButton.setOnClickListener(v -> confirmDeleteService());
    }

    private void setupCategoriesData() {
        mainCategories.add("Music & DJ");
        mainCategories.add("Catering");
        mainCategories.add("Decoration");
        mainCategories.add("Dresses");
        mainCategories.add("Photography");
        mainCategories.add("Venues");
        mainCategories.add("Makeup & Hair");
        mainCategories.add("Invitation Cards");

        // Sub-categories mapping
        List<String> musicSub = new ArrayList<>();
        musicSub.add("DJ Setup & Sound System");
        musicSub.add("Live Band / Acoustic Group");
        musicSub.add("Dhol & Brass Band");
        musicSub.add("Karaoke & Event Anchor");
        musicSub.add("Lighting & Laser Effects");
        subCategoriesMap.put("Music & DJ", musicSub);

        List<String> cateringSub = new ArrayList<>();
        cateringSub.add("Buffet Catering Package");
        cateringSub.add("Live Cooking & Food Stalls");
        cateringSub.add("Desserts & Bakery Counter");
        cateringSub.add("Beverages & Mocktail Bar");
        cateringSub.add("Traditional Thali Service");
        subCategoriesMap.put("Catering", cateringSub);

        List<String> decorSub = new ArrayList<>();
        decorSub.add("Stage & Floral Decoration");
        decorSub.add("Entrance & Mandap Decor");
        decorSub.add("Theme & Balloon Decoration");
        decorSub.add("Lighting & Ambiance");
        decorSub.add("Table & Chair Setup");
        subCategoriesMap.put("Decoration", decorSub);

        List<String> dressSub = new ArrayList<>();
        dressSub.add("Bridal Lehenga & Gown");
        dressSub.add("Groom Sherwani & Suit");
        dressSub.add("Party Wear & Indo-Western");
        dressSub.add("Designer Jewelry & Accessories");
        dressSub.add("Dress Rental Service");
        subCategoriesMap.put("Dresses", dressSub);

        List<String> photoSub = new ArrayList<>();
        photoSub.add("Pre-Wedding Shoot");
        photoSub.add("Wedding Candid Photography");
        photoSub.add("Cinematic Videography & Teaser");
        photoSub.add("Drone Aerial Coverage");
        photoSub.add("Photo Booth & Albums");
        subCategoriesMap.put("Photography", photoSub);

        List<String> venueSub = new ArrayList<>();
        venueSub.add("AC Banquet Hall");
        venueSub.add("Lawn & Open Air Garden");
        venueSub.add("Resort & Farmhouse");
        venueSub.add("Party Hall / Rooftop");
        venueSub.add("Conference & Marriage Hall");
        subCategoriesMap.put("Venues", venueSub);

        List<String> makeupSub = new ArrayList<>();
        makeupSub.add("Bridal HD & Airbrush Makeup");
        makeupSub.add("Party & Engagement Makeup");
        makeupSub.add("Hair Styling & Hair Spa");
        makeupSub.add("Saree & Dupatta Draping");
        makeupSub.add("Mehendi Art & Design");
        subCategoriesMap.put("Makeup & Hair", makeupSub);

        List<String> inviteSub = new ArrayList<>();
        inviteSub.add("Digital E-Invite & Video");
        inviteSub.add("Printed Wedding Cards");
        inviteSub.add("Custom Gift Boxes & Hampers");
        inviteSub.add("Luxury & Acrylic Cards");
        inviteSub.add("RSVP & Event Website");
        subCategoriesMap.put("Invitation Cards", inviteSub);
    }

    private void setupSpinners() {
        ArrayAdapter<String> mainAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                mainCategories
        );
        mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainCategorySpinner.setAdapter(mainAdapter);

        mainCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMain = mainCategories.get(position);
                updateSubCategorySpinner(selectedMain);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSubCategorySpinner(String mainCat) {
        List<String> subList = subCategoriesMap.get(mainCat);
        if (subList == null) {
            subList = new ArrayList<>();
            subList.add("General Service Package");
        }

        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                subList
        );
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subCategorySpinner.setAdapter(subAdapter);
    }

    private void fetchVendorDefaultCategory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String vendorCategory = doc.getString("category");
                        if (vendorCategory != null && mainCategories.contains(vendorCategory)) {
                            mainCategorySpinner.setSelection(mainCategories.indexOf(vendorCategory));
                        }
                    }
                });
    }

    private void saveOrUpdateService() {

        String title = serviceTitle.getText().toString().trim();
        String price = servicePrice.getText().toString().trim();
        String description = serviceDescription.getText().toString().trim();

        String mainCategory = mainCategorySpinner.getSelectedItem().toString();
        String subCategory = subCategorySpinner.getSelectedItem() != null ?
                subCategorySpinner.getSelectedItem().toString() : mainCategory;

        if (title.isEmpty()) {
            title = subCategory; // Auto-fill title from sub-category if blank
            serviceTitle.setText(title);
        }

        if (price.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in price and description details", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalTitle = title;
        final String finalPrice = price;
        final String finalDescription = description;
        final String finalMainCategory = mainCategory;
        final String finalSubCategory = subCategory;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        String userEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        saveServiceButton.setEnabled(false);

        // Fetch vendor's profile name from Firestore to attach to service record
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String vendorName = userDoc.exists() ? userDoc.getString("name") : "";
                    if (vendorName == null || vendorName.trim().isEmpty()) {
                        vendorName = userEmail;
                    }

                    Map<String, Object> serviceData = new HashMap<>();
                    serviceData.put("title", finalTitle);
                    serviceData.put("price", finalPrice);
                    serviceData.put("description", finalDescription);
                    serviceData.put("category", finalMainCategory);
                    serviceData.put("subCategory", finalSubCategory);
                    serviceData.put("vendorUid", uid);
                    serviceData.put("vendorName", vendorName);
                    serviceData.put("email", userEmail);
                    serviceData.put("vendorEmail", userEmail);
                    serviceData.put("updatedAt", System.currentTimeMillis());

                    if (isEditMode) {
                        db.collection("services")
                                .document(serviceId)
                                .update(serviceData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Service updated successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> handleFirestoreError("Update Error", e));
                    } else {
                        db.collection("services")
                                .add(serviceData)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(this, "Service added successfully!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> handleFirestoreError("Save Error", e));
                    }
                })
                .addOnFailureListener(e -> handleFirestoreError("Profile Error", e));
    }

    private void handleFirestoreError(String prefix, Exception e) {
        saveServiceButton.setEnabled(true);
        String msg = e.getMessage() != null ? e.getMessage() : "";

        if (msg.contains("PERMISSION_DENIED") || msg.contains("Missing or insufficient permissions")) {
            new AlertDialog.Builder(this)
                    .setTitle("Firebase Rules Update Needed")
                    .setMessage("Permission Denied: Please update Cloud Firestore Security Rules in Firebase Console for the 'services' collection to allow write access.")
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, prefix + ": " + msg, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteService() {
        if (!isEditMode || serviceId == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete this service?")
                .setPositiveButton("Delete", (dialog, which) -> deleteService())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteService() {
        deleteServiceButton.setEnabled(false);
        db.collection("services")
                .document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    deleteServiceButton.setEnabled(true);
                });
    }
}
