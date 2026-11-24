package com.example.happyhappenings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button login;
    TextView signup;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String savedRole = prefs.getString("user_role", null);

            if ("user".equals(savedRole)) {
                Intent intent = new Intent(this, UserDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            } else if ("vendor".equals(savedRole)) {
                Intent intent = new Intent(this, VendorDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        signup = findViewById(R.id.signup);

        if (auth.getCurrentUser() != null) {
            checkUserRoleAndRedirect(auth.getCurrentUser().getUid());
        }

        login.setOnClickListener(v -> {

            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "Please enter all details",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            login.setEnabled(false);

            auth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful() && auth.getCurrentUser() != null) {

                            String uid = auth.getCurrentUser().getUid();

                            db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(document -> {

                                        if (document.exists()) {

                                            String role = document.getString("role");

                                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                                    .edit()
                                                    .putString("user_role", role)
                                                    .apply();

                                            Intent intent;

                                            if ("user".equals(role)) {
                                                intent = new Intent(this,
                                                        UserDashboardActivity.class);
                                            } else if ("vendor".equals(role)) {
                                                intent = new Intent(this,
                                                        VendorDashboardActivity.class);
                                            } else {
                                                Toast.makeText(this,
                                                        "Invalid user role",
                                                        Toast.LENGTH_SHORT).show();
                                                login.setEnabled(true);
                                                return;
                                            }

                                            intent.setFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            );

                                            startActivity(intent);
                                            finish();

                                        } else {
                                            Toast.makeText(this,
                                                    "User data not found",
                                                    Toast.LENGTH_SHORT).show();
                                            login.setEnabled(true);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                        login.setEnabled(true);
                                    });

                        } else {

                            Toast.makeText(this,
                                    "Invalid Email or Password",
                                    Toast.LENGTH_SHORT).show();

                            login.setEnabled(true);
                        }
                    });
        });

        signup.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void checkUserRoleAndRedirect(String uid) {

        if (login != null) login.setEnabled(false);

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String role = document.getString("role");

                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_role", role)
                                .apply();

                        Intent intent;

                        if ("user".equals(role)) {
                            intent = new Intent(this, UserDashboardActivity.class);
                        } else if ("vendor".equals(role)) {
                            intent = new Intent(this, VendorDashboardActivity.class);
                        } else {
                            if (login != null) login.setEnabled(true);
                            return;
                        }

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                    } else {
                        if (login != null) login.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (login != null) login.setEnabled(true);
                });
    }
}
