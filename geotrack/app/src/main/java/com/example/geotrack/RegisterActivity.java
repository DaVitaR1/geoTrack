package com.example.geotrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText, adminLinkEmailEditText, orgNameEditText;
    private TextInputLayout adminEmailInputLayout, orgNameInputLayout;
    private RadioGroup rolRadioGroup;
    private RadioButton radioInspector;
    private Button btnRegistrar;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Conectar vistas
        emailEditText = findViewById(R.id.regEmailEditText);
        passwordEditText = findViewById(R.id.regPasswordEditText);
        adminLinkEmailEditText = findViewById(R.id.adminLinkEmailEditText);
        orgNameEditText = findViewById(R.id.orgNameEditText);

        adminEmailInputLayout = findViewById(R.id.adminEmailInputLayout);
        orgNameInputLayout = findViewById(R.id.orgNameInputLayout);

        rolRadioGroup = findViewById(R.id.rolRadioGroup);
        radioInspector = findViewById(R.id.radioInspector);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressBar = findViewById(R.id.regProgressBar);

        // Lógica de visibilidad
        rolRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioInspector) {
                // Si es Inspector: Muestra "Vincular", Oculta "Nombre Negocio"
                adminEmailInputLayout.setVisibility(View.VISIBLE);
                orgNameInputLayout.setVisibility(View.GONE);
            } else {
                // Si es Admin: Oculta "Vincular", Muestra "Nombre Negocio"
                adminEmailInputLayout.setVisibility(View.GONE);
                orgNameInputLayout.setVisibility(View.VISIBLE);
            }
        });

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String adminEmail = adminLinkEmailEditText.getText().toString().trim();
        String orgName = orgNameEditText.getText().toString().trim();
        boolean esInspector = radioInspector.isChecked();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Mínimo 6 caracteres");
            return;
        }

        // Validaciones específicas
        if (esInspector) {
            if (adminEmail.isEmpty()) {
                adminLinkEmailEditText.setError("Indica el correo de tu Admin");
                return;
            }
        } else {
            // Es Admin
            if (orgName.isEmpty()) {
                orgNameEditText.setError("Pon el nombre de tu institución");
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegistrar.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarDatosEnFirestore(user.getUid(), email, esInspector, adminEmail, orgName);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegistrar.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarDatosEnFirestore(String uid, String email, boolean esInspector, String adminEmail, String orgName) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email);

        if (esInspector) {
            userMap.put("rol", "inspector");
            userMap.put("linkedAdminEmail", adminEmail);
        } else {
            userMap.put("rol", "admin");
            // GUARDAMOS EL NOMBRE DE LA INSTITUCIÓN
            userMap.put("organizationName", orgName);
        }

        db.collection("usuarios").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error al guardar datos", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnRegistrar.setEnabled(true);
                });
    }
}