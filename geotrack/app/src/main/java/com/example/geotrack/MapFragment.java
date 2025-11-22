package com.example.geotrack;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // <-- ¡NUEVO!
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private GoogleMap mMap;
    private FirebaseFirestore firestoreDb;
    private ViewPager2 adminViewPager;
    private String currentUserEmail;
    private boolean isInitialLoad = true;

    // ===============================================================
    //  NUEVO: Listener para la limpieza
    // ===============================================================
    private ListenerRegistration reportesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreDb = FirebaseFirestore.getInstance();

        if (getActivity() != null) {
            adminViewPager = getActivity().findViewById(R.id.adminViewPager);
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "El mapa está listo.");

        if (adminViewPager != null) {
            mMap.setOnCameraMoveStartedListener(reason -> {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    adminViewPager.setUserInputEnabled(false);
                }
            });
            mMap.setOnCameraIdleListener(() -> {
                adminViewPager.setUserInputEnabled(true);
            });
        }

        cargarMarcadoresDeReportes();
    }

    private void cargarMarcadoresDeReportes() {
        if (currentUserEmail == null) {
            Log.e(TAG, "Error: Correo del Admin no encontrado para filtrar el mapa.");
            return;
        }

        // Si hay un listener activo, lo liberamos antes de crear uno nuevo
        if (reportesListener != null) {
            reportesListener.remove();
        }

        Query query = firestoreDb.collection("reportes")
                .whereEqualTo("organizationId", currentUserEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // ===============================================================
        //  ASIGNAMOS EL LISTENER A LA VARIABLE DE CLASE
        // ===============================================================
        reportesListener = query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Error al escuchar los reportes.", e);
                return;
            }

            if (mMap == null) return;
            mMap.clear();

            if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                if (isInitialLoad) {
                    Log.d(TAG, "No hay reportes, usando ubicación por defecto.");
                    LatLng mexicoCity = new LatLng(19.4326, -99.1332);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 10f));
                    isInitialLoad = false;
                }
                return;
            }

            if (isInitialLoad) {
                DocumentSnapshot latestDoc = queryDocumentSnapshots.getDocuments().get(0);

                ReporteFirestore latestReport = latestDoc.toObject(ReporteFirestore.class);

                if (latestReport.getLatitud() != 0 || latestReport.getLongitud() != 0) {
                    LatLng latestPosicion = new LatLng(latestReport.getLatitud(), latestReport.getLongitud());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latestPosicion, 15f));
                }
                isInitialLoad = false;
            }

            Log.d(TAG, "Cargando " + queryDocumentSnapshots.size() + " pines en el mapa.");

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                ReporteFirestore reporte = doc.toObject(ReporteFirestore.class);

                if (reporte.getLatitud() == 0 && reporte.getLongitud() == 0) {
                    continue;
                }

                LatLng posicion = new LatLng(reporte.getLatitud(), reporte.getLongitud());

                float pinColor;
                String status = reporte.getStatus();
                if (status == null) status = "Pendiente";

                switch (status) {
                    case "En Proceso":
                        pinColor = BitmapDescriptorFactory.HUE_AZURE;
                        break;
                    case "Atendido con Éxito":
                        pinColor = BitmapDescriptorFactory.HUE_GREEN;
                        break;
                    case "Cancelado":
                        pinColor = BitmapDescriptorFactory.HUE_RED;
                        break;
                    case "Pendiente":
                    default:
                        pinColor = BitmapDescriptorFactory.HUE_YELLOW;
                        break;
                }

                mMap.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title(reporte.getTitulo())
                        .snippet("Estado: " + status)
                        .icon(BitmapDescriptorFactory.defaultMarker(pinColor))
                );
            }
        });
    }

    // ===============================================================
    //  ARREGLO FINAL: Liberar el listener en onDestroyView
    //  Esto evita que el listener se ejecute cuando el Fragmento se destruye
    // ===============================================================
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reportesListener != null) {
            reportesListener.remove();
        }
    }
}