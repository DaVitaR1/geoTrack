package com.example.geotrack;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Position 0 = Pestaña "Activos"
        // Position 1 = Pestaña "Finalizados"
        // ==================================
        // NUEVO: Position 2 = Pestaña "Mapa"
        // ==================================
        switch (position) {
            case 0:
                return ReportesListFragment.newInstance("activos");
            case 1:
                return ReportesListFragment.newInstance("finalizados");
            case 2:
                return new MapFragment(); // ¡El nuevo fragmento!
            default:
                return ReportesListFragment.newInstance("activos");
        }
    }

    @Override
    public int getItemCount() {
        // ==================================
        // NUEVO: Ahora son 3 pestañas
        // ==================================
        return 3;
    }
}