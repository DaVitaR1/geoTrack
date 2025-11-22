// Paquete (asegúrate de que sea el tuyo)
package com.example.geotrack;

// Importaciones de Room
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao // Le dice a Room que esto es un DAO
public interface ReporteDao {

    // Instrucción para INSERTAR un nuevo reporte
    @Insert
    void insertarReporte(Reporte reporte);

    // Instrucción para ACTUALIZAR un reporte (ej. para marcarlo como sincronizado)
    @Update
    void actualizarReporte(Reporte reporte);

    // Instrucción para LEER todos los reportes que NO se han subido a Firebase
    @Query("SELECT * FROM reportes_tabla WHERE estaSincronizado = 0") // 0 significa 'false'
    List<Reporte> obtenerReportesNoSincronizados();

    // (Opcional) Instrucción para LEER todos los reportes (para un historial local)
    @Query("SELECT * FROM reportes_tabla ORDER BY id DESC")
    List<Reporte> obtenerTodosLosReportes();
}