package com.example.geotrack;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class ReporteAdapter extends FirestoreRecyclerAdapter<ReporteFirestore, ReporteAdapter.ReporteViewHolder> {

    private OnReporteClickListener listener;
    private Context context;
    private boolean isAdmin;

    public ReporteAdapter(@NonNull FirestoreRecyclerOptions<ReporteFirestore> options, Context context, boolean isAdmin) {
        super(options);
        this.context = context;
        this.isAdmin = isAdmin;
    }

    @Override
    protected void onBindViewHolder(@NonNull ReporteViewHolder holder, int position, @NonNull ReporteFirestore reporte) {
        holder.tituloTextView.setText(reporte.getTitulo());
        holder.descripcionTextView.setText(reporte.getDescripcion());
        holder.gpsTextView.setText(String.format("GPS: %.4f, %.4f", reporte.getLatitud(), reporte.getLongitud()));

        // Lógica de Status y Colores
        String status = reporte.getStatus();
        holder.reporteStatusTextView.setText(status);
        if (status == null) status = "Pendiente";

        int color;
        switch (status) {
            case "En Proceso":
                color = ContextCompat.getColor(context, R.color.status_en_proceso);
                break;
            case "Atendido con Éxito":
                color = ContextCompat.getColor(context, R.color.status_atendido);
                break;
            case "Cancelado":
                color = ContextCompat.getColor(context, R.color.status_cancelado);
                break;
            case "Pendiente":
            default:
                color = ContextCompat.getColor(context, R.color.status_pendiente);
                break;
        }
        holder.reporteStatusTextView.setTextColor(color);

        // Mostrar la PRIMERA foto en la lista
        if (reporte.getUrlFotos() != null && !reporte.getUrlFotos().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(reporte.getUrlFotos().get(0))
                    .centerCrop()
                    .placeholder(R.color.placeholder_bg)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageDrawable(null);
            holder.imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.placeholder_bg));
        }

        // ===============================================================
        //  NUEVO: Listener para el ITEM COMPLETO (Abre el Detalle)
        // ===============================================================
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
                listener.onItemClick(snapshot.getId()); // Llamamos al nuevo método
            }
        });

        // --- Lógica de botones basada en rol ---
        if (isAdmin) {
            holder.copiarButton.setVisibility(View.VISIBLE);
            holder.gestionarButton.setVisibility(View.VISIBLE);

            if (status.equals("Atendido con Éxito") || status.equals("Cancelado")) {
                holder.gestionarButton.setVisibility(View.GONE);
            }

            // Listeners
            holder.copiarButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCopiarGpsClick(reporte.getLatitud(), reporte.getLongitud());
                }
            });
            holder.gestionarButton.setOnClickListener(v -> {
                if (listener != null) {
                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
                    listener.onGestionarClick(snapshot, reporte.getStatus());
                }
            });

        } else {
            holder.gestionarButton.setVisibility(View.GONE);
            holder.copiarButton.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public ReporteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new ReporteViewHolder(view);
    }

    // Clase ViewHolder (sigue igual)
    class ReporteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tituloTextView, descripcionTextView, gpsTextView, reporteStatusTextView;
        Button gestionarButton, copiarButton;

        public ReporteViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.reporteImageView);
            tituloTextView = itemView.findViewById(R.id.reporteTituloTextView);
            descripcionTextView = itemView.findViewById(R.id.reporteDescripcionTextView);
            gpsTextView = itemView.findViewById(R.id.reporteGpsTextView);
            reporteStatusTextView = itemView.findViewById(R.id.reporteStatusTextView);
            gestionarButton = itemView.findViewById(R.id.gestionarButton);
            copiarButton = itemView.findViewById(R.id.copiarButton);
        }
    }

    // --- Interfaz Actualizada con onItemClick ---
    public interface OnReporteClickListener {
        void onItemClick(String reporteId); // <--- MÉTODO NUEVO
        void onCopiarGpsClick(double latitud, double longitud);
        void onGestionarClick(DocumentSnapshot snapshot, String statusActual);
    }

    public void setOnReporteClickListener(OnReporteClickListener listener) {
        this.listener = listener;
    }
}