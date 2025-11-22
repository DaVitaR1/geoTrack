package com.example.geotrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView; // Ya no importamos LinearLayoutManager directo

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MisReportesActivity extends AppCompatActivity implements ReporteAdapter.OnReporteClickListener {

    private FirebaseFirestore firestoreDb;
    private RecyclerView misReportesRecyclerView;
    private ReporteAdapter adapter;
    private String inspectorUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_reportes);

        inspectorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestoreDb = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.misReportesToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        misReportesRecyclerView = findViewById(R.id.misReportesRecyclerView);

        // ===============================================================
        //  USAMOS EL LAYOUT MANAGER BLINDADO
        // ===============================================================
        misReportesRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        Query query = firestoreDb.collection("reportes")
                .whereEqualTo("inspectorId", inspectorUid)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ReporteFirestore> options =
                new FirestoreRecyclerOptions.Builder<ReporteFirestore>()
                        .setQuery(query, ReporteFirestore.class)
                        .build();

        adapter = new ReporteAdapter(options, this, false);
        misReportesRecyclerView.setAdapter(adapter);

        adapter.setOnReporteClickListener(this);
    }

    @Override
    public void onItemClick(String reporteId) {
        // CORRECCIÓN: No detenemos la escucha aquí
        Intent intent = new Intent(this, ReporteDetalleActivity.class);
        intent.putExtra(ReporteDetalleActivity.EXTRA_REPORTE_ID, reporteId);
        startActivity(intent);
    }

    @Override
    public void onGestionarClick(DocumentSnapshot snapshot, String statusActual) {
        onItemClick(snapshot.getId());
    }

    @Override
    public void onCopiarGpsClick(double latitud, double longitud) {
        Toast.makeText(this, "Solo el Administrador puede copiar el GPS", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}