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
        statusTextView.setText("Loading " + categoryName + " services...");
        vendorListContainer.removeAllViews();

        db.collection("services")
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> matchingServices = extractMatchingDocs(result.getDocuments());
                    if (!matchingServices.isEmpty()) {
                        displayServiceDocs(matchingServices);
                    } else {
                        fetchFromUsersCollection();
                    }
                })
                .addOnFailureListener(e -> fetchFromUsersCollection());
    }

    private void fetchFromUsersCollection() {
        db.collection("users")
                .get()
                .addOnSuccessListener(result -> {
                    List<DocumentSnapshot> matchingVendors = extractMatchingDocs(result.getDocuments());
                    if (!matchingVendors.isEmpty()) {
                        displayVendorDocs(matchingVendors);
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
                    List<DocumentSnapshot> matchingVendors = extractMatchingDocs(result.getDocuments());
                    if (!matchingVendors.isEmpty()) {
                        displayVendorDocs(matchingVendors);
                    } else {
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText("No services or vendors found for " + categoryName);
                    }
                })
                .addOnFailureListener(e -> {
                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText("No services listed yet for " + categoryName);
                });
    }

    private List<DocumentSnapshot> extractMatchingDocs(List<DocumentSnapshot> documents) {
        List<DocumentSnapshot> matchingDocs = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {

            String docCategory = doc.getString("category");
            String docCategoryId = doc.getString("categoryId");

            boolean isCategoryMatch = docCategory != null &&
                    (docCategory.equalsIgnoreCase(categoryName) ||
                     docCategory.trim().toLowerCase().contains(categoryName.trim().toLowerCase()) ||
                     categoryName.trim().toLowerCase().contains(docCategory.trim().toLowerCase()));

            boolean isIdMatch = docCategoryId != null &&
                    categoryId != null &&
                    !categoryId.isEmpty() &&
                    docCategoryId.equalsIgnoreCase(categoryId);

            if (isCategoryMatch || isIdMatch) {
                matchingDocs.add(doc);
            }
        }
        return matchingDocs;
    }

    private void displayServiceDocs(List<DocumentSnapshot> services) {
        statusTextView.setVisibility(View.GONE);
        vendorListContainer.removeAllViews();

        for (DocumentSnapshot doc : services) {
            String title = doc.getString("title");
            String price = doc.getString("price");
            String desc = doc.getString("description");
            String category = doc.getString("category");
            String vendorName = doc.getString("vendorName");
            String email = doc.getString("email");
            if (email == null || email.trim().isEmpty()) {
                email = doc.getString("vendorEmail");
            }

            if (vendorName == null || vendorName.trim().isEmpty()) vendorName = "Event Vendor";
            if (email == null || email.trim().isEmpty()) email = "contact@happyservice.com";
            if (title == null || title.trim().isEmpty()) title = categoryName + " Service Package";

            addServiceCard(
                    title,
                    price != null ? "₹" + price : "Contact for Price",
                    vendorName,
                    email,
                    category != null ? category : categoryName,
                    desc != null ? desc : "Full " + categoryName + " service coverage."
            );
        }
    }

    private void displayVendorDocs(List<DocumentSnapshot> vendors) {
        statusTextView.setVisibility(View.GONE);
        vendorListContainer.removeAllViews();

        for (DocumentSnapshot vendorDoc : vendors) {
            String name = vendorDoc.getString("name");
            String email = vendorDoc.getString("email");
            String category = vendorDoc.getString("category");
            String phone = vendorDoc.getString("phone");
            String desc = vendorDoc.getString("description");

            addVendorCard(
                    name != null ? name : "Vendor",
                    email != null ? email : "No email provided",
                    category != null ? category : categoryName,
                    phone != null ? phone : "Contact via Email",
                    desc != null ? desc : "Professional " + (category != null ? category : categoryName) + " service provider."
            );
        }
    }

    private void addServiceCard(String title, String price, String vendorName, String email, String category, String description) {

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
        priceText.setText(price);
        priceText.setTextSize(16);
        priceText.setTextColor(Color.parseColor("#2563EB"));
        priceText.setTypeface(null, Typeface.BOLD);

        headerRow.addView(titleText);
        headerRow.addView(priceText);

        // Vendor & Category Info
        TextView vendorText = new TextView(this);
        vendorText.setText("By " + vendorName + " • " + category);
        vendorText.setTextSize(12);
        vendorText.setTextColor(Color.parseColor("#2563EB"));
        vendorText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams vendorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        vendorParams.topMargin = dp(4);
        vendorText.setLayoutParams(vendorParams);

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

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(14);
        actionsRow.setLayoutParams(actionsParams);

        // 1. View Profile Button
        TextView profileButton = new TextView(this);
        profileButton.setText("View Details");
        profileButton.setTextSize(13);
        profileButton.setTypeface(null, Typeface.BOLD);
        profileButton.setTextColor(Color.parseColor("#2563EB"));
        profileButton.setGravity(Gravity.CENTER);
        profileButton.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable profileBg = new GradientDrawable();
        profileBg.setColor(Color.parseColor("#EFF6FF"));
        profileBg.setCornerRadius(dp(10));
        profileBg.setStroke(dp(1), Color.parseColor("#BFDBFE"));
        profileButton.setBackground(profileBg);

        LinearLayout.LayoutParams profileBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        profileBtnParams.rightMargin = dp(6);
        profileButton.setLayoutParams(profileBtnParams);

        profileButton.setOnClickListener(v -> showVendorProfileDialog(vendorName, email, category, "Contact via Email", description));

        // 2. Contact / Book Button
        TextView contactButton = new TextView(this);
        contactButton.setText("Book / Contact");
        contactButton.setTextSize(13);
        contactButton.setTypeface(null, Typeface.BOLD);
        contactButton.setTextColor(Color.WHITE);
        contactButton.setGravity(Gravity.CENTER);
        contactButton.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable contactBtnBg = new GradientDrawable();
        contactBtnBg.setColor(Color.parseColor("#2563EB"));
        contactBtnBg.setCornerRadius(dp(10));
        contactButton.setBackground(contactBtnBg);

        LinearLayout.LayoutParams contactBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        contactBtnParams.leftMargin = dp(6);
        contactButton.setLayoutParams(contactBtnParams);

        contactButton.setOnClickListener(v -> {
            Intent bookIntent = new Intent(this, BookServiceActivity.class);
            bookIntent.putExtra("vendorName", vendorName);
            bookIntent.putExtra("vendorEmail", email);
            bookIntent.putExtra("email", email);
            bookIntent.putExtra("serviceTitle", title);
            bookIntent.putExtra("category", category);
            bookIntent.putExtra("price", price);
            startActivity(bookIntent);
        });

        actionsRow.addView(profileButton);
        actionsRow.addView(contactButton);

        card.addView(headerRow);
        card.addView(vendorText);
        card.addView(descText);
        card.addView(actionsRow);

        vendorListContainer.addView(card);
    }

    private void addVendorCard(String name, String email, String category, String phone, String description) {

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

        // Top Row: Avatar & Basic Info
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

        TextView categoryBadge = new TextView(this);
        categoryBadge.setText(category + " • Verified ★ 4.9");
        categoryBadge.setTextSize(11);
        categoryBadge.setTextColor(Color.parseColor("#2563EB"));
        categoryBadge.setTypeface(null, Typeface.BOLD);

        infoLayout.addView(nameText);
        infoLayout.addView(categoryBadge);

        topRow.addView(avatar);
        topRow.addView(infoLayout);

        // Brief Details Section
        LinearLayout briefBox = new LinearLayout(this);
        briefBox.setOrientation(LinearLayout.VERTICAL);
        briefBox.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable briefBg = new GradientDrawable();
        briefBg.setColor(Color.parseColor("#F8FAFC"));
        briefBg.setCornerRadius(dp(10));
        briefBox.setBackground(briefBg);

        LinearLayout.LayoutParams briefParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        briefParams.topMargin = dp(12);
        briefBox.setLayoutParams(briefParams);

        TextView emailDetail = new TextView(this);
        emailDetail.setText("📧 " + email);
        emailDetail.setTextSize(12);
        emailDetail.setTextColor(Color.parseColor("#475569"));

        TextView summaryDetail = new TextView(this);
        summaryDetail.setText("✨ " + description);
        summaryDetail.setTextSize(12);
        summaryDetail.setTextColor(Color.parseColor("#64748B"));
        summaryDetail.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) summaryDetail.getLayoutParams()).topMargin = dp(4);

        briefBox.addView(emailDetail);
        briefBox.addView(summaryDetail);

        // Action Buttons Row (View Profile & Contact Vendor)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(14);
        actionsRow.setLayoutParams(actionsParams);

        // 1. View Profile Button
        TextView profileButton = new TextView(this);
        profileButton.setText("View Profile");
        profileButton.setTextSize(13);
        profileButton.setTypeface(null, Typeface.BOLD);
        profileButton.setTextColor(Color.parseColor("#2563EB"));
        profileButton.setGravity(Gravity.CENTER);
        profileButton.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable profileBg = new GradientDrawable();
        profileBg.setColor(Color.parseColor("#EFF6FF"));
        profileBg.setCornerRadius(dp(10));
        profileBg.setStroke(dp(1), Color.parseColor("#BFDBFE"));
        profileButton.setBackground(profileBg);

        LinearLayout.LayoutParams profileBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        profileBtnParams.rightMargin = dp(6);
        profileButton.setLayoutParams(profileBtnParams);

        profileButton.setOnClickListener(v -> showVendorProfileDialog(name, email, category, phone, description));

        // 2. Contact Button
        TextView contactButton = new TextView(this);
        contactButton.setText("Contact Vendor");
        contactButton.setTextSize(13);
        contactButton.setTypeface(null, Typeface.BOLD);
        contactButton.setTextColor(Color.WHITE);
        contactButton.setGravity(Gravity.CENTER);
        contactButton.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable contactBtnBg = new GradientDrawable();
        contactBtnBg.setColor(Color.parseColor("#2563EB"));
        contactBtnBg.setCornerRadius(dp(10));
        contactButton.setBackground(contactBtnBg);

        LinearLayout.LayoutParams contactBtnParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        contactBtnParams.leftMargin = dp(6);
        contactButton.setLayoutParams(contactBtnParams);

        contactButton.setOnClickListener(v -> {
            Intent bookIntent = new Intent(this, BookServiceActivity.class);
            bookIntent.putExtra("vendorName", name);
            bookIntent.putExtra("vendorEmail", email);
            bookIntent.putExtra("email", email);
            bookIntent.putExtra("serviceTitle", category + " Vendor Service");
            bookIntent.putExtra("category", category);
            bookIntent.putExtra("price", "Contact for Quote");
            startActivity(bookIntent);
        });

        actionsRow.addView(profileButton);
        actionsRow.addView(contactButton);

        card.addView(topRow);
        card.addView(briefBox);
        card.addView(actionsRow);

        vendorListContainer.addView(card);
    }

    private void showVendorProfileDialog(String name, String email, String category, String phone, String description) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Vendor Profile");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#172B4D"));

        TextView nameText = new TextView(this);
        nameText.setText("👤 Name: " + name);
        nameText.setTextSize(15);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#172B4D"));
        nameText.setPadding(0, dp(10), 0, 0);

        TextView categoryText = new TextView(this);
        categoryText.setText("🏷️ Main Category: " + category);
        categoryText.setTextSize(14);
        categoryText.setTextColor(Color.parseColor("#2563EB"));
        categoryText.setTypeface(null, Typeface.BOLD);
        categoryText.setPadding(0, dp(6), 0, 0);

        TextView emailText = new TextView(this);
        emailText.setText("✉️ Email: " + email);
        emailText.setTextSize(14);
        emailText.setTextColor(Color.parseColor("#475569"));
        emailText.setPadding(0, dp(6), 0, 0);

        TextView phoneText = new TextView(this);
        phoneText.setText("📞 Contact: " + phone);
        phoneText.setTextSize(14);
        phoneText.setTextColor(Color.parseColor("#475569"));
        phoneText.setPadding(0, dp(6), 0, 0);

        TextView descText = new TextView(this);
        descText.setText("📋 Overview: " + description);
        descText.setTextSize(13);
        descText.setTextColor(Color.parseColor("#64748B"));
        descText.setPadding(0, dp(8), 0, 0);

        TextView servicesHeader = new TextView(this);
        servicesHeader.setText("🛍️ Services Offered:");
        servicesHeader.setTextSize(14);
        servicesHeader.setTypeface(null, Typeface.BOLD);
        servicesHeader.setTextColor(Color.parseColor("#172B4D"));
        servicesHeader.setPadding(0, dp(14), 0, dp(4));

        LinearLayout servicesContainer = new LinearLayout(this);
        servicesContainer.setOrientation(LinearLayout.VERTICAL);

        TextView loadingServicesText = new TextView(this);
        loadingServicesText.setText("Loading vendor services...");
        loadingServicesText.setTextSize(12);
        loadingServicesText.setTextColor(Color.parseColor("#7B8798"));
        servicesContainer.addView(loadingServicesText);

        dialogLayout.addView(title);
        dialogLayout.addView(nameText);
        dialogLayout.addView(categoryText);
        dialogLayout.addView(emailText);
        dialogLayout.addView(phoneText);
        dialogLayout.addView(descText);
        dialogLayout.addView(servicesHeader);
        dialogLayout.addView(servicesContainer);

        // Fetch services offered by this vendor
        db.collection("services")
                .get()
                .addOnSuccessListener(result -> {
                    servicesContainer.removeAllViews();
                    boolean foundAny = false;
                    if (result != null && !result.isEmpty()) {
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            String serviceVendorName = doc.getString("vendorName");
                            String serviceVendorEmail = doc.getString("email");
                            String serviceTitle = doc.getString("title");
                            String servicePrice = doc.getString("price");
                            String subCategory = doc.getString("subCategory");

                            boolean matchVendor = (email != null && email.equalsIgnoreCase(serviceVendorEmail))
                                    || (name != null && name.equalsIgnoreCase(serviceVendorName));

                            if (matchVendor) {
                                foundAny = true;
                                TextView serviceItem = new TextView(this);
                                String itemText = "• " + (serviceTitle != null ? serviceTitle : "Service") +
                                        (subCategory != null ? " (" + subCategory + ")" : "") +
                                        " — ₹" + (servicePrice != null ? servicePrice : "N/A");
                                serviceItem.setText(itemText);
                                serviceItem.setTextSize(13);
                                serviceItem.setTextColor(Color.parseColor("#2563EB"));
                                serviceItem.setPadding(0, dp(3), 0, dp(3));
                                servicesContainer.addView(serviceItem);
                            }
                        }
                    }
                    if (!foundAny) {
                        TextView noServices = new TextView(this);
                        noServices.setText("• Full " + category + " service coverage available.");
                        noServices.setTextSize(12);
                        noServices.setTextColor(Color.parseColor("#64748B"));
                        servicesContainer.addView(noServices);
                    }
                })
                .addOnFailureListener(e -> {
                    servicesContainer.removeAllViews();
                    TextView fallback = new TextView(this);
                    fallback.setText("• Full " + category + " service package available.");
                    fallback.setTextSize(12);
                    fallback.setTextColor(Color.parseColor("#64748B"));
                    servicesContainer.addView(fallback);
                });

        builder.setView(dialogLayout);
        builder.setPositiveButton("Send Email", (dialog, which) -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + email));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry for " + category + " service");
            try {
                startActivity(Intent.createChooser(emailIntent, "Contact " + name));
            } catch (Exception e) {
                Toast.makeText(this, "Email: " + email, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
