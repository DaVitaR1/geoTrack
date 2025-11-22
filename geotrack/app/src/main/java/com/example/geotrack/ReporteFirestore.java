package com.example.geotrack;

import java.util.List;

public class ReporteFirestore {

    private String titulo;
    private String descripcion;
    private double latitud;
    private double longitud;

    // AHORA ES UNA LISTA DE STRINGS
    private List<String> urlFotos;

    private long timestamp;
    private String status;
    private String inspectorId;
    private String organizationName;

    public ReporteFirestore() {}

    // --- Getters ---
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }

    // GETTER ACTUALIZADO
    public List<String> getUrlFotos() { return urlFotos; }

    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getInspectorId() { return inspectorId; }
    public String getOrganizationName() { return organizationName; }
}