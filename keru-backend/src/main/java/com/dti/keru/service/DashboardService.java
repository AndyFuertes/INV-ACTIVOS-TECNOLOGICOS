package com.dti.keru.service;

import com.dti.keru.repository.*;
import com.dti.keru.repository.EspecificacionComputoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ActivoMaestroRepository activoRepo;
    private final UsuarioRepository       usuarioRepo;
    private final MovimientoHistorialRepository movimientoRepo;
    private final AccesorioCatalogoRepository   accesorioRepo;
    private final EspecificacionComputoRepository espRepo;

    private static final Set<String> TIPOS_COMPUTO = Set.of(
        "LAPTOP","DESKTOP","ALL IN ONE","WORKSTATION","LAPTOP WORKSTATION",
        "DESKTOP TINY","OPS","TABLET","CELULAR","PANTALLA INTERACTIVA"
    );
    private static final Set<String> TIPOS_PERI = Set.of(
        "MONITOR","IMPRESORA","FOTOCOPIADORA","PROYECTOR","TELEFONO IP",
        "SCANNER","SCANER","PARLANTE","TELEVISOR","LECTOR DE CODIGO DE BARRAS",
        "TERMINAL PORTATIL PDT","LECTORA OPTICO"
    );

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // ── 2 queries en vez de 10+ ───────────────────────────────────────────
        // Query 1: conteo por tipo (reemplaza 8 COUNT queries individuales)
        Map<String, Long> porTipo = new HashMap<>();
        activoRepo.countPorTipo().forEach(row ->
            porTipo.put(((String) row[0]).toUpperCase(), (Long) row[1])
        );

        // Query 2: conteo por disponibilidad
        Map<String, Long> porDisp = new HashMap<>();
        activoRepo.countPorDisponibilidad().forEach(row -> {
            if (row[0] != null) porDisp.put(((String) row[0]).toUpperCase(), (Long) row[1]);
        });

        // Totales de cómputo
        long laptops  = porTipo.getOrDefault("LAPTOP",    0L);
        long desktops = porTipo.getOrDefault("DESKTOP",   0L)
                      + porTipo.getOrDefault("DESKTOP TINY", 0L);
        long aio      = porTipo.getOrDefault("ALL IN ONE", 0L);
        long workst   = porTipo.getOrDefault("WORKSTATION", 0L)
                      + porTipo.getOrDefault("LAPTOP WORKSTATION", 0L);
        long otros_c  = porTipo.getOrDefault("OPS",       0L)
                      + porTipo.getOrDefault("TABLET",    0L)
                      + porTipo.getOrDefault("CELULAR",   0L)
                      + porTipo.getOrDefault("DESKTOP TINY", 0L);

        // Totales de periféricos
        long monitores  = porTipo.getOrDefault("MONITOR",      0L);
        long impresoras = porTipo.getOrDefault("IMPRESORA",     0L)
                        + porTipo.getOrDefault("FOTOCOPIADORA", 0L);
        long proyect    = porTipo.getOrDefault("PROYECTOR",     0L);
        long telefonos  = porTipo.getOrDefault("TELEFONO IP",   0L);

        long totalComputo = TIPOS_COMPUTO.stream()
            .mapToLong(t -> porTipo.getOrDefault(t, 0L)).sum();
        long totalPeri = TIPOS_PERI.stream()
            .mapToLong(t -> porTipo.getOrDefault(t, 0L)).sum();
        long totalActivos = totalComputo + totalPeri;

        // Disponibilidad
        long enUso      = porDisp.getOrDefault("EN USO",     0L);
        long disponibles= porDisp.getOrDefault("DISPONIBLE", 0L);

        // Queries rápidas (indexadas)
        long bajas       = activoRepo.countBajas();
        long nuevos      = activoRepo.countNuevos();
        long usuarios    = usuarioRepo.count();
        long movimientos = movimientoRepo.count();
        long accesorios  = accesorioRepo.count();
        long stockBajo   = accesorioRepo.findStockBajo().size();

        stats.put("totalActivos",  totalActivos);
        stats.put("totalComputo",  totalComputo);
        stats.put("totalPerifericos", totalPeri);
        stats.put("enUso",         enUso);
        stats.put("disponibles",   disponibles);
        stats.put("bajas",         bajas);
        stats.put("nuevos",        nuevos);
        stats.put("totalUsuarios", usuarios);
        stats.put("laptops",       laptops);
        stats.put("desktops",      desktops);
        stats.put("aio",           aio);
        stats.put("workstations",  workst);
        stats.put("otrosComputo", otros_c);
        stats.put("monitores",     monitores);
        stats.put("impresoras",    impresoras);
        stats.put("proyectores",   proyect);
        stats.put("telefonos",     telefonos);
        stats.put("movimientos",   movimientos);
        stats.put("accesorios",    accesorios);
        stats.put("stockBajo",     stockBajo);
        stats.put("campuses",      activoRepo.findAllCampus());

        // InvGate stats
        long conInvgate = espRepo.countByTieneInvgate(true);
        long sinInvgate = espRepo.countByTieneInvgate(false);
        stats.put("conInvgate",  conInvgate);
        stats.put("sinInvgate",  sinInvgate);
        long totalComp = conInvgate + sinInvgate;
        stats.put("pctInvgate",  totalComp > 0 ? Math.round((double) conInvgate / totalComp * 100) : 0);

        return stats;
    }
}
