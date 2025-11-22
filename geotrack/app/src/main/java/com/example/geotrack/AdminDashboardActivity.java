package com.example.geotrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private ViewPager2 adminViewPager;
    private TabLayout adminTabLayout;
    private AdminPagerAdapter pagerAdapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        cargarNombreInstitucion();

        adminViewPager = findViewById(R.id.adminViewPager);
        adminTabLayout = findViewById(R.id.adminTabLayout);

        pagerAdapter = new AdminPagerAdapter(this);
        adminViewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(adminTabLayout, adminViewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Activos");
                    else if (position == 1) tab.setText("Finalizados");
                    else tab.setText("Mapa");
                }
        ).attach();
    }

    private void cargarNombreInstitucion() {
        db.collection("usuarios").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String orgName = documentSnapshot.getString("organizationName");
                        if (orgName != null && !orgName.isEmpty()) {
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setSubtitle(orgName);
                            }
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            cerrarSesion();
            return true;
        } else if (item.getItemId() == R.id.action_edit_org) {
            mostrarDialogoEditarNombre();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoEditarNombre() {
        final EditText input = new EditText(this);
        if (getSupportActionBar() != null && getSupportActionBar().getSubtitle() != null) {
            input.setText(getSupportActionBar().getSubtitle());
        }

        new AlertDialog.Builder(this)
                .setTitle("Editar Nombre de Institución")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = input.getText().toString().trim();
                    if (!nuevoNombre.isEmpty()) {
                        actualizarNombreEnFirestore(nuevoNombre);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarNombreEnFirestore(String nuevoNombre) {
        db.collection("usuarios").document(currentUserId)
                .update("organizationName", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(nuevoNombre);
                    }
                    Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}