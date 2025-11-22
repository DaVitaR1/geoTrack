package com.example.geotrack;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.bumptech.glide.Glide;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.FotoViewHolder> {

    private List<Uri> fotoUriList;
    private OnFotoDeleteListener deleteListener;

    public FotoAdapter(List<Uri> fotoUriList, OnFotoDeleteListener deleteListener) {
        this.fotoUriList = fotoUriList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto_miniatura, parent, false);
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        Uri uri = fotoUriList.get(position);

        // Usamos Glide para cargar la miniatura
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.fotoMiniatura);

        // Listener para el botón de eliminar
        holder.btnEliminarFoto.setOnClickListener(v -> {
            if (deleteListener != null) {
                // Notificamos a la Actividad que este URI debe ser eliminado
                deleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fotoUriList.size();
    }

    // Interfaz para notificar a la actividad principal
    public interface OnFotoDeleteListener {
        void onDelete(int position);
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView fotoMiniatura;
        ImageView btnEliminarFoto;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            fotoMiniatura = itemView.findViewById(R.id.fotoMiniatura);
            btnEliminarFoto = itemView.findViewById(R.id.btnEliminarFoto);
        }
    }
}