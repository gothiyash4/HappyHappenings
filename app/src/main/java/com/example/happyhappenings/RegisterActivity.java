package com.example.happyhappenings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, password;
    RadioGroup roleGroup;
    Button register;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        roleGroup = findViewById(R.id.roleGroup);
        register = findViewById(R.id.register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        register.setOnClickListener(v -> {

            String userName = name.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            int selectedRole = roleGroup.getCheckedRadioButtonId();

            if (userName.isEmpty() || userEmail.isEmpty()
                    || userPassword.isEmpty() || selectedRole == -1) {

                Toast.makeText(this, "Please fill all details",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String role = selectedRole == R.id.user ? "user" : "vendor";

            register.setEnabled(false);

            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                Map<String, Object> userData = new HashMap<>();
                                userData.put("uid", uid);
                                userData.put("name", userName);
                                userData.put("email", userEmail);
                                userData.put("role", role);

                                db.collection("users")
                                        .document(uid)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {

                                            Toast.makeText(this,
                                                    "Registration Successful",
                                                    Toast.LENGTH_SHORT).show();

                                            name.setText("");
                                            email.setText("");
                                            password.setText("");
                                            roleGroup.check(R.id.user);

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

                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            Toast.makeText(this,
                                    errorMsg,
                                    Toast.LENGTH_LONG).show();

                            register.setEnabled(true);
                        }
                    });
        });
    }
}
