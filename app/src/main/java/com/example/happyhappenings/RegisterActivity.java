package com.example.happyhappenings;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, password;
    RadioGroup roleGroup;
    Button register;

    Spinner vendorCategory;
    LinearLayout vendorCategoryLayout;

    FirebaseAuth auth;
    FirebaseFirestore db;

    ArrayList<String> categoryNames = new ArrayList<>();
    ArrayList<String> categoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        roleGroup = findViewById(R.id.roleGroup);
        register = findViewById(R.id.register);

        vendorCategory = findViewById(R.id.vendorCategory);
        vendorCategoryLayout = findViewById(R.id.vendorCategoryLayout);

        TextView loginLink = findViewById(R.id.loginLink);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadCategories();

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.vendor) {
                vendorCategoryLayout.setVisibility(View.VISIBLE);
            } else {
                vendorCategoryLayout.setVisibility(View.GONE);
            }
        });

        register.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadCategories() {

        populateDefaultCategories();

        db.collection("categories")
                .get()
                .addOnSuccessListener(result -> {
                    if (result != null && !result.isEmpty()) {
                        for (DocumentSnapshot document : result.getDocuments()) {

                            String categoryName = document.getString("name");

                            if (categoryName == null ||
                                    categoryName.trim().isEmpty()) {

                                categoryName = document.getId();
                            }

                            categoryName = categoryName.substring(0, 1).toUpperCase()
                                    + categoryName.substring(1);

                            if (!categoryNames.contains(categoryName)) {
                                categoryNames.add(categoryName);
                                categoryIds.add(document.getId());
                            }
                        }
                        updateSpinnerAdapter();
                    }
                })
                .addOnFailureListener(e -> updateSpinnerAdapter());
    }

    private void populateDefaultCategories() {
        categoryNames.clear();
        categoryIds.clear();

        categoryNames.add("Decoration");
        categoryIds.add("decoration");

        categoryNames.add("Catering");
        categoryIds.add("catering");

        categoryNames.add("Dresses");
        categoryIds.add("dresses");

        categoryNames.add("Photography");
        categoryIds.add("photography");

        categoryNames.add("Music & DJ");
        categoryIds.add("music_dj");

        categoryNames.add("Venues");
        categoryIds.add("venues");

        categoryNames.add("Makeup & Hair");
        categoryIds.add("makeup_hair");

        categoryNames.add("Invitation Cards");
        categoryIds.add("invitation_cards");

        updateSpinnerAdapter();
    }

    private void updateSpinnerAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        vendorCategory.setAdapter(adapter);
    }

    private void registerUser() {

        String userName = name.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        int selectedRole = roleGroup.getCheckedRadioButtonId();

        if (userName.isEmpty() || userEmail.isEmpty()
                || userPassword.isEmpty() || selectedRole == -1) {

            Toast.makeText(this,
                    "Please fill all details",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (userPassword.length() < 6) {

            Toast.makeText(this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String role = selectedRole == R.id.user ? "user" : "vendor";

        String selectedCategory = "";
        String selectedCategoryId = "";

        if (role.equals("vendor")) {

            int position = vendorCategory.getSelectedItemPosition();

            if (categoryNames.isEmpty() || position < 0
                    || position >= categoryNames.size()) {

                Toast.makeText(this,
                        "Please select a valid category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            selectedCategory = categoryNames.get(position);
            selectedCategoryId = categoryIds.get(position);
        }

        String finalCategory = selectedCategory;
        String finalCategoryId = selectedCategoryId;

        register.setEnabled(false);

        auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            String uid = user.getUid();

                            Map<String, Object> userData = new HashMap<>();

                            userData.put("uid", uid);
                            userData.put("vendor_id", uid);
                            userData.put("name", userName);
                            userData.put("email", userEmail);
                            userData.put("role", role);

                            if (role.equals("vendor")) {

                                userData.put("category", finalCategory);
                                userData.put("categoryId", finalCategoryId);
                            }

                            db.collection("users")
                                    .document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {

                                        if (role.equals("vendor")) {
                                            db.collection("vendors").document(uid).set(userData);
                                        }

                                        Toast.makeText(this,
                                                "Registration Successful",
                                                Toast.LENGTH_SHORT).show();

                                        name.setText("");
                                        email.setText("");
                                        password.setText("");

                                        roleGroup.check(R.id.user);

                                        if (!categoryNames.isEmpty()) {
                                            vendorCategory.setSelection(0);
                                        }

                                        register.setEnabled(true);
                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(this,
                                                "Database Error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();

                                        register.setEnabled(true);
                                    });

                        } else {
                            register.setEnabled(true);
                        }

                    } else {

                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(this,
                                errorMsg,
                                Toast.LENGTH_LONG).show();

                        register.setEnabled(true);
                    }
                });
    }
}
