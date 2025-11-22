package com.example.geotrack;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private AppDatabase db;
    private FirebaseFirestore firestoreDb;
    private FirebaseStorage storage;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = AppDatabase.getDatabase(context);
        firestoreDb = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando sincronización...");

        String inspectorId = getInputData().getString("inspectorId");
        String adminEmail = getInputData().getString("adminEmail");
        // ==========================================
        // NUEVO: Recibimos el nombre de la org
        // ==========================================
        String orgName = getInputData().getString("orgName");

        if (inspectorId == null || adminEmail == null) {
            Log.e(TAG, "Faltan datos (ID o Admin Email).");
            return Result.failure();
        }

        List<Reporte> reportesNoSincronizados = db.reporteDao().obtenerReportesNoSincronizados();

        if (reportesNoSincronizados.isEmpty()) return Result.success();

        try {
            for (Reporte reporte : reportesNoSincronizados) {
                List<String> urlFotos = new ArrayList<>();

                for (String rutaLocal : reporte.rutaFotoLocal) {
                    Uri fotoUri = Uri.parse(rutaLocal);
                    String nombreArchivo = fotoUri.getLastPathSegment();
                    StorageReference storageRef = storage.getReference().child("imagenes_reportes").child(nombreArchivo);

                    UploadTask uploadTask = storageRef.putFile(fotoUri);
                    Tasks.await(uploadTask);
                    Task<Uri> urlTask = storageRef.getDownloadUrl();
                    Tasks.await(urlTask);
                    urlFotos.add(urlTask.getResult().toString());
                }

                Map<String, Object> reporteData = new HashMap<>();
                reporteData.put("titulo", reporte.titulo);
                reporteData.put("descripcion", reporte.descripcion);
                reporteData.put("latitud", reporte.latitud);
                reporteData.put("longitud", reporte.longitud);
                reporteData.put("urlFotos", urlFotos);
                reporteData.put("timestamp", System.currentTimeMillis());
                reporteData.put("status", "Pendiente");
                reporteData.put("inspectorId", inspectorId);
                reporteData.put("organizationId", adminEmail);

                // ==========================================
                // NUEVO: Guardamos el nombre en el reporte
                // ==========================================
                if (orgName != null) {
                    reporteData.put("organizationName", orgName);
                } else {
                    reporteData.put("organizationName", "Sin Nombre");
                }

                Tasks.await(firestoreDb.collection("reportes").add(reporteData));

                reporte.estaSincronizado = true;
                db.reporteDao().actualizarReporte(reporte);
            }
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error durante la sincronización: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}