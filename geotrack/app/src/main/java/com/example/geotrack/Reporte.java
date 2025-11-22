package com.example.geotrack;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

@Entity(tableName = "reportes_tabla")
// Le decimos a Room que use nuestro conversor para List<String>
@TypeConverters({Converters.class})
public class Reporte {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String titulo;
    public String descripcion;
    public double latitud;
    public double longitud;

    // AHORA ES UNA LISTA
    public List<String> rutaFotoLocal;
    public boolean estaSincronizado;

    public Reporte() {}
}