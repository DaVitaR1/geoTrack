package com.example.geotrack;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;

import java.util.concurrent.TimeUnit; // Importante

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // Llamamos al trabajo periódico
        iniciarSincronizacionPeriodica();
    }

    private void iniciarSincronizacionPeriodica() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // ===============================================================
        //  AQUÍ ESTABA LA PENDEJADA (YA CORREGIDO)
        //  Cambiado de 1 HORA a 15 MINUTOS (el mínimo permitido)
        // ===============================================================
        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_geotrack",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}