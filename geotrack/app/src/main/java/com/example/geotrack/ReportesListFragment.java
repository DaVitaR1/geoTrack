package com.example.geotrack;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView; // Nota: Ya no importamos LinearLayoutManager directo

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class ReportesListFragment extends Fragment {

    private static final String TAG = "ReportesListFragment";
    private static final String ARG_TIPO_LISTA = "tipo_lista";
    private FirebaseFirestore firestoreDb;
    private RecyclerView recyclerView;
    private ReporteAdapter adapter;
    private String tipoLista;
    private String currentUserEmail;

    public static ReportesListFragment newInstance(String tipoLista) {
        ReportesListFragment fragment = new ReportesListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TIPO_LISTA, tipoLista);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestoreDb = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        if (getArguments() != null) {
            tipoLista = getArguments().getString(ARG_TIPO_LISTA);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reportes_list, container, false);
        recyclerView = view.findViewById(R.id.reportesRecyclerView);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        Query query;
        if (tipoLista.equals("activos")) {
            query = firestoreDb.collection("reportes")
                    .whereEqualTo("organizationId", currentUserEmail)
                    .whereIn("status", Arrays.asList("Pendiente", "En Proceso"))
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            query = firestoreDb.collection("reportes")
                    .whereEqualTo("organizationId", currentUserEmail)
                    .whereIn("status", Arrays.asList("Atendido con Éxito", "Cancelado"))
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        FirestoreRecyclerOptions<ReporteFirestore> options =
                new FirestoreRecyclerOptions.Builder<ReporteFirestore>()
                        .setQuery(query, ReporteFirestore.class)
                        .build();

        adapter = new ReporteAdapter(options, getContext(), true);

        // ===============================================================
        //  USAMOS EL LAYOUT MANAGER BLINDADO
        // ===============================================================
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnReporteClickListener(new ReporteAdapter.OnReporteClickListener() {
            @Override
            public void onItemClick(String reporteId) {
                // ===============================================================
                //  CORRECCIÓN: YA NO DETENEMOS EL ADAPTADOR AQUÍ
                // ===============================================================
                Intent intent = new Intent(getActivity(), ReporteDetalleActivity.class);
                intent.putExtra(ReporteDetalleActivity.EXTRA_REPORTE_ID, reporteId);
                startActivity(intent);
            }

            @Override
            public void onCopiarGpsClick(double latitud, double longitud) {
                copiarAlPortapapeles(latitud, longitud);
            }

            @Override
            public void onGestionarClick(DocumentSnapshot snapshot, String statusActual) {
                mostrarDialogoGestionar(snapshot, statusActual);
            }
        });
    }

    // ... (copiarAlPortapapeles, mostrarDialogoGestionar, actualizarStatusEnFirestore igual que antes)
    private void copiarAlPortapapeles(double latitud, double longitud) {
        String coordenadas = latitud + ", " + longitud;
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Ubicación GPS", coordenadas);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "Ubicación copiada: " + coordenadas, Toast.LENGTH_SHORT).show();
    }

    private void mostrarDialogoGestionar(DocumentSnapshot snapshot, String statusActual) {
        final String[] opciones = {"En Proceso", "Atendido con Éxito", "Cancelado"};
        int checkedItem = -1;
        if(statusActual.equals("En Proceso")) checkedItem = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Actualizar Estado del Reporte");
        builder.setSingleChoiceItems(opciones, checkedItem, (dialog, which) -> {
            String nuevoStatus = opciones[which];
            actualizarStatusEnFirestore(snapshot, nuevoStatus, dialog);
        });
        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void actualizarStatusEnFirestore(DocumentSnapshot snapshot, String nuevoStatus, DialogInterface dialog) {
        String docId = snapshot.getId();
        firestoreDb.collection("reportes").document(docId)
                .update("status", nuevoStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Estado actualizado", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar estado", e);
                    Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
    }

    // ===============================================================
    //  CICLO DE VIDA ESTÁNDAR (onStart/onStop)
    // ===============================================================
    @Override
    public void onStart() {
        super.onStart();
        if(adapter!=null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapter!=null) adapter.stopListening();
    }
}