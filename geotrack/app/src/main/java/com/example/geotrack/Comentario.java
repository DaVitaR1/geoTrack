package com.example.geotrack;

public class Comentario {
    private String autorEmail;
    private String mensaje;
    private long timestamp;
    private String autorRol; // Admin o Inspector

    public Comentario() {}

    public Comentario(String autorEmail, String mensaje, long timestamp, String autorRol) {
        this.autorEmail = autorEmail;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
        this.autorRol = autorRol;
    }

    public String getAutorEmail() { return autorEmail; }
    public String getMensaje() { return mensaje; }
    public long getTimestamp() { return timestamp; }
    public String getAutorRol() { return autorRol; }
}