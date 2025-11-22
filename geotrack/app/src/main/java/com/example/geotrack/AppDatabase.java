package com.example.geotrack;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Reporte.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ReporteDao reporteDao();

    private static volatile AppDatabase INSTANCIA;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "geotrack_database")
                            // ===============================================================
                            //  ARREGLO CLAVE: Si la migración falla, borra y crea de nuevo.
                            // ===============================================================
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}