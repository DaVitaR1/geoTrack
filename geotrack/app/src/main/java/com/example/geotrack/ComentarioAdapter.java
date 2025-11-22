package com.example.geotrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComentarioAdapter extends RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder> {

    private List<Comentario> comentariosList;

    public ComentarioAdapter(List<Comentario> comentariosList) {
        this.comentariosList = comentariosList;
    }

    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comentario, parent, false);
        return new ComentarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        Comentario comentario = comentariosList.get(position);

        // 1. Formatear la fecha (Ej: "19 Nov, 4:30 PM")
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        String fechaBonita = sdf.format(new Date(comentario.getTimestamp()));

        // 2. Mostrar Autor y Rol (Ej: "juan@gmail.com (Inspector)")
        String infoAutor = String.format("%s (%s)",
                comentario.getAutorEmail(),
                comentario.getAutorRol());

        // 3. Asignar textos a las vistas
        holder.autorTextView.setText(infoAutor);
        holder.mensajeTextView.setText(comentario.getMensaje());
        holder.fechaTextView.setText(fechaBonita);
    }

    @Override
    public int getItemCount() {
        return comentariosList.size();
    }

    static class ComentarioViewHolder extends RecyclerView.ViewHolder {
        TextView autorTextView;
        TextView mensajeTextView;
        TextView fechaTextView; // Nuevo campo para la fecha

        public ComentarioViewHolder(@NonNull View itemView) {
            super(itemView);
            autorTextView = itemView.findViewById(R.id.comentarioAutor);
            mensajeTextView = itemView.findViewById(R.id.comentarioMensaje);
            fechaTextView = itemView.findViewById(R.id.comentarioFecha);
        }
    }
}