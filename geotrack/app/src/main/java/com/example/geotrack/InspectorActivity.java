package com.example.geotrack;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class InspectorActivity extends AppCompatActivity implements FotoAdapter.OnFotoDeleteListener {

    private static final String TAG = "InspectorActivity";
    private static final int REQUEST_CODE_PERMISOS = 1001;
    private TextInputEditText tituloEditText, descripcionEditText;
    private Button tomarFotoButton, guardarReporteButton, verReportesButton;
    private RecyclerView fotosRecyclerView;
    private TextView gpsTextView;
    private ProgressBar guardarProgressBar;
    private FusedLocationProviderClient fusedLocationClient;

    // Inicialización segura del callback
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) return;
            ubicacionActual = locationResult.getLastLocation();
            gpsTextView.setText(String.format("GPS: %.4f, %.4f", ubicacionActual.getLatitude(), ubicacionActual.getLongitude()));
        }
    };

    private Location ubicacionActual;
    private List<Uri> fotoUriList = new ArrayList<>();
    private FotoAdapter fotoAdapter;
    private Uri tempFotoUri;
    private ActivityResultLauncher<Uri> tomarFotoLauncher;
    private AppDatabase db;
    private ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private String inspectorUid;
    private FirebaseFirestore firestoreDb;

    private String adminEmailForReport;
    // ==========================================
    // NUEVO: Variable para guardar el nombre
    // ==========================================
    private String adminOrgNameForReport;

    private final int MAX_FOTOS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector);

        Toolbar toolbar = findViewById(R.id.inspectorToolbar);
        setSupportActionBar(toolbar);

        db = AppDatabase.getDatabase(getApplicationContext());
        firestoreDb = FirebaseFirestore.getInstance();
        inspectorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        cargarDatosDelJefe();

        tituloEditText = findViewById(R.id.tituloEditText);
        descripcionEditText = findViewById(R.id.descripcionEditText);
        tomarFotoButton = findViewById(R.id.tomarFotoButton);
        guardarReporteButton = findViewById(R.id.guardarReporteButton);
        gpsTextView = findViewById(R.id.gpsTextView);
        guardarProgressBar = findViewById(R.id.guardarProgressBar);
        verReportesButton = findViewById(R.id.verReportesButton);

        fotosRecyclerView = findViewById(R.id.fotosRecyclerView);
        fotosRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fotoAdapter = new FotoAdapter(fotoUriList, this);
        fotosRecyclerView.setAdapter(fotoAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                (Boolean exitoso) -> {
                    if (exitoso) {
                        if (tempFotoUri != null) {
                            fotoUriList.add(tempFotoUri);
                            fotoAdapter.notifyItemInserted(fotoUriList.size() - 1);
                            Toast.makeText(this, "Foto " + fotoUriList.size() + " añadida", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Toma de foto cancelada", Toast.LENGTH_SHORT).show();
                    }
                    tempFotoUri = null;
                }
        );

        tomarFotoButton.setOnClickListener(v -> verificarYTomarFoto());
        guardarReporteButton.setOnClickListener(v -> guardarReporte());
        verReportesButton.setOnClickListener(v -> {
            Intent intent = new Intent(InspectorActivity.this, MisReportesActivity.class);
            startActivity(intent);
        });

        verificarPermisosEIniciarGPS();
    }

    @Override public void onDelete(int position) {
        fotoUriList.remove(position);
        fotoAdapter.notifyItemRemoved(position);
        Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show();
    }

    private void tomarFoto() {
        try {
            File archivoFoto = crearArchivoFotoTemporal();
            tempFotoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", archivoFoto);
            tomarFotoLauncher.launch(tempFotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error: No se pudo crear el archivo de imagen.", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFotoTemporal() throws IOException {
        String nombreArchivo = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(nombreArchivo, ".jpg", storageDir);
    }

    private void cargarDatosDelJefe() {
        firestoreDb.collection("usuarios").document(inspectorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String adminEmail = doc.getString("linkedAdminEmail");
                        if (adminEmail != null && !adminEmail.isEmpty()) {
                            adminEmailForReport = adminEmail;
                            firestoreDb.collection("usuarios")
                                    .whereEqualTo("email", adminEmail)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            String orgName = querySnapshot.getDocuments().get(0).getString("organizationName");

                                            // ==========================================
                                            // NUEVO: Guardamos el nombre en la variable
                                            // ==========================================
                                            adminOrgNameForReport = orgName;

                                            if (orgName != null && getSupportActionBar() != null) {
                                                getSupportActionBar().setSubtitle(orgName);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // (Menú y Permisos igual que antes...)
    @Override public boolean onCreateOptionsMenu(Menu menu) { MenuInflater inflater = getMenuInflater(); inflater.inflate(R.menu.main_menu, menu); MenuItem editItem = menu.findItem(R.id.action_edit_org); if (editItem != null) editItem.setVisible(false); return true; }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) { if (item.getItemId() == R.id.action_logout) { cerrarSesion(); return true; } return super.onOptionsItemSelected(item); }
    private void cerrarSesion() { FirebaseAuth.getInstance().signOut(); Intent intent = new Intent(InspectorActivity.this, LoginActivity.class); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(intent); finish(); }
    private void verificarPermisosEIniciarGPS() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISOS); } else { iniciarActualizacionesDeGPS(); } }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == REQUEST_CODE_PERMISOS) { if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { iniciarActualizacionesDeGPS(); } } if (requestCode == REQUEST_CODE_PERMISOS + 1) { if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { tomarFoto(); } else { Toast.makeText(this, "Permisos denegados.", Toast.LENGTH_LONG).show(); } } }
    private void verificarYTomarFoto() { if (fotoUriList.size() >= MAX_FOTOS) { Toast.makeText(this, "Máximo " + MAX_FOTOS + " fotos.", Toast.LENGTH_SHORT).show(); return; } if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISOS + 1); } else { tomarFoto(); } }
    private void iniciarActualizacionesDeGPS() { LocationRequest locationRequest = LocationRequest.create(); locationRequest.setInterval(10000); locationRequest.setFastestInterval(5000); locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()); } }

    private void guardarReporte() {
        String titulo = tituloEditText.getText().toString().trim();
        String descripcion = descripcionEditText.getText().toString().trim();

        if (titulo.isEmpty()) { tituloEditText.setError("Obligatorio"); return; }
        if (ubicacionActual == null) { Toast.makeText(this, "Esperando GPS...", Toast.LENGTH_SHORT).show(); return; }
        if (fotoUriList.isEmpty()) { Toast.makeText(this, "Debe añadir al menos una foto.", Toast.LENGTH_SHORT).show(); return; }
        if (adminEmailForReport == null) { Toast.makeText(this, "Error: No se ha identificado a tu administrador.", Toast.LENGTH_LONG).show(); return; }

        guardarProgressBar.setVisibility(View.VISIBLE);
        guardarReporteButton.setEnabled(false);

        List<String> rutaFotoLocalString = fotoUriList.stream().map(Uri::toString).collect(Collectors.toList());

        Reporte nuevoReporte = new Reporte();
        nuevoReporte.titulo = titulo;
        nuevoReporte.descripcion = descripcion;
        nuevoReporte.latitud = ubicacionActual.getLatitude();
        nuevoReporte.longitud = ubicacionActual.getLongitude();
        nuevoReporte.rutaFotoLocal = rutaFotoLocalString;
        nuevoReporte.estaSincronizado = false;

        databaseExecutor.execute(() -> {
            try {
                db.reporteDao().insertarReporte(nuevoReporte);
                runOnUiThread(() -> {
                    guardarProgressBar.setVisibility(View.GONE);
                    guardarReporteButton.setEnabled(true);
                    Toast.makeText(InspectorActivity.this, "Guardado localmente", Toast.LENGTH_LONG).show();

                    tituloEditText.setText(""); descripcionEditText.setText("");
                    fotoUriList.clear(); fotoAdapter.notifyDataSetChanged();

                    // ==========================================
                    // NUEVO: Pasamos también el orgName al Worker
                    // ==========================================
                    Data.Builder dataBuilder = new Data.Builder()
                            .putString("inspectorId", inspectorUid)
                            .putString("adminEmail", adminEmailForReport);

                    if (adminOrgNameForReport != null) {
                        dataBuilder.putString("orgName", adminOrgNameForReport); // ¡AQUÍ!
                    }

                    Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                    OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                            .setConstraints(constraints)
                            .setInputData(dataBuilder.build())
                            .build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(syncRequest);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al guardar en Room: ", e);
                runOnUiThread(() -> { guardarProgressBar.setVisibility(View.GONE); guardarReporteButton.setEnabled(true); });
            }
        });
    }
    @Override protected void onPause() { super.onPause(); if (fusedLocationClient != null && locationCallback != null) { fusedLocationClient.removeLocationUpdates(locationCallback); } }
    @Override protected void onResume() { super.onResume(); if (locationCallback != null && ubicacionActual == null) { iniciarActualizacionesDeGPS(); } }
}