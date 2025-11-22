package com.example.geotrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton, irARegistroButton; // Botón nuevo
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        irARegistroButton = findViewById(R.id.irARegistroButton); // Conectar
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> iniciarSesion());

        // --- Listener Nuevo ---
        irARegistroButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void iniciarSesion() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailEditText.setError("Email es requerido");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Contraseña es requerida");
            passwordEditText.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);
        irARegistroButton.setEnabled(false); // Desactivar mientras carga

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("LOGIN", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            verificarRol(user.getUid());
                        } else {
                            Log.w("LOGIN", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            loginButton.setVisibility(View.VISIBLE);
                            irARegistroButton.setEnabled(true);
                        }
                    }
                });
    }

    private void verificarRol(String uid) {
        DocumentReference docRef = db.collection("usuarios").document(uid);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String rol = document.getString("rol");
                        if (rol == null) {
                            Toast.makeText(LoginActivity.this, "Usuario sin rol asignado.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            loginButton.setVisibility(View.VISIBLE);
                            return;
                        }

                        if (rol.equals("admin")) {
                            Toast.makeText(LoginActivity.this, "Bienvenido Admin", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            startActivity(intent);
                            finish();

                        } else if (rol.equals("inspector")) {
                            Toast.makeText(LoginActivity.this, "Bienvenido Inspector", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, InspectorActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Rol desconocido: " + rol, Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            loginButton.setVisibility(View.VISIBLE);
                            irARegistroButton.setEnabled(true);
                        }
                    } else {
                        Log.d("ROL", "No se encontró el documento del usuario");
                        Toast.makeText(LoginActivity.this, "Error: No se encontraron datos de usuario.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        loginButton.setVisibility(View.VISIBLE);
                        irARegistroButton.setEnabled(true);
                    }
                } else {
                    Log.d("ROL", "Fallo al obtener documento: ", task.getException());
                    Toast.makeText(LoginActivity.this, "Error al verificar rol.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    loginButton.setVisibility(View.VISIBLE);
                    irARegistroButton.setEnabled(true);
                }
            }
        });
    }
}