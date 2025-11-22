package com.example.geotrack;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReporteDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_REPORTE_ID = "reporte_id";
    private String reporteId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private TextView detalleTitulo, detalleDescripcion, detalleMetadata;
    private RecyclerView detalleFotosRecyclerView, comentariosRecyclerView;
    private TextInputEditText comentarioEditText;
    private Button enviarComentarioButton;

    // Adapters
    private FotoAdapter detalleFotosAdapter;
    private ComentarioAdapter comentarioAdapter;
    private List<Comentario> comentariosList = new ArrayList<>();

    private List<Uri> fotoUriList = new ArrayList<>();

    // Listener para mantener la conversación en tiempo real
    private ListenerRegistration comentariosListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte_detalle);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Obtener el ID del reporte que se pasó
        reporteId = getIntent().getStringExtra(EXTRA_REPORTE_ID);
        if (reporteId == null) {
            finish();
            return;
        }

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.detalleToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Conectar Vistas
        detalleTitulo = findViewById(R.id.detalleTitulo);
        detalleDescripcion = findViewById(R.id.detalleDescripcion);
        detalleMetadata = findViewById(R.id.detalleMetadata);
        detalleFotosRecyclerView = findViewById(R.id.detalleFotosRecyclerView);
        comentariosRecyclerView = findViewById(R.id.comentariosRecyclerView);
        comentarioEditText = findViewById(R.id.comentarioEditText);
        enviarComentarioButton = findViewById(R.id.enviarComentarioButton);

        // Configurar RecyclerViews
        detalleFotosRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        detalleFotosAdapter = new FotoAdapter(fotoUriList, null);
        detalleFotosRecyclerView.setAdapter(detalleFotosAdapter);

        comentariosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        comentarioAdapter = new ComentarioAdapter(comentariosList);
        comentariosRecyclerView.setAdapter(comentarioAdapter);

        // Cargar los datos y la conversación
        cargarDetalleReporte();
        // Cargar comentarios en onResume

        // Listener para enviar el comentario
        enviarComentarioButton.setOnClickListener(v -> enviarComentario());
    }

    // Lógica para el botón de retroceso de la Toolbar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void cargarDetalleReporte() {
        db.collection("reportes").document(reporteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ReporteFirestore reporte = documentSnapshot.toObject(ReporteFirestore.class);
                        if (reporte == null) return;

                        detalleTitulo.setText(reporte.getTitulo());
                        detalleDescripcion.setText(reporte.getDescripcion());

                        // Metadatos
                        String metadata = String.format(Locale.getDefault(),
                                "Institución: %s | Status: %s | Inspector: %s | GPS: %.4f, %.4f",
                                reporte.getOrganizationName(),
                                reporte.getStatus(),
                                reporte.getInspectorId(),
                                reporte.getLatitud(), reporte.getLongitud());
                        detalleMetadata.setText(metadata);

                        // Cargar Galería de Fotos
                        if (reporte.getUrlFotos() != null && !reporte.getUrlFotos().isEmpty()) {
                            fotoUriList.clear();
                            for (String url : reporte.getUrlFotos()) {
                                fotoUriList.add(Uri.parse(url));
                            }
                            detalleFotosAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar reporte", Toast.LENGTH_SHORT).show());
    }

    private void cargarComentarios() {
        // Nos suscribimos a la sub-colección de comentarios
        comentariosListener = db.collection("reportes").document(reporteId).collection("comentarios")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("Detalle", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        comentariosList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            comentariosList.add(doc.toObject(Comentario.class));
                        }
                        comentarioAdapter.notifyDataSetChanged();
                        // Desplazar al último mensaje
                        comentariosRecyclerView.scrollToPosition(comentariosList.size() - 1);
                    }
                });
    }

    private void enviarComentario() {
        String mensaje = comentarioEditText.getText().toString().trim();
        String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Anónimo";

        if (mensaje.isEmpty()) return;

        db.collection("usuarios").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String autorRol = userDoc.getString("rol");
                    if (autorRol == null) autorRol = "Usuario";

                    Comentario nuevoComentario = new Comentario(userEmail, mensaje, System.currentTimeMillis(), autorRol);

                    db.collection("reportes").document(reporteId).collection("comentarios")
                            .add(nuevoComentario)
                            .addOnSuccessListener(documentReference -> {
                                comentarioEditText.setText("");
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error al enviar comentario.", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al verificar rol para comentario.", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarComentarios();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detener el listener al irnos de la actividad
        if (comentariosListener != null) {
            comentariosListener.remove();
            comentariosListener = null;
        }
    }
}