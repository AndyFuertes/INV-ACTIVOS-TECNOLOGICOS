package com.dti.keru.controller;

import com.dti.keru.entity.*;
import com.dti.keru.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint para registrar nuevos activos desde la interfaz web.
 * Crea el activo, su ubicación, sus especificaciones de cómputo
 * y vincula al responsable — todo en una sola llamada POST.
 */
@RestController
@RequestMapping("/activos/nuevo")
@RequiredArgsConstructor
@Slf4j
public class RegistroActivoController {

    private final ActivoMaestroRepository         activoRepo;
    private final EspecificacionComputoRepository  espRepo;
    private final UsuarioRepository               usuarioRepo;
    private final UbicacionRepository             ubicacionRepo;

    private static final java.util.Set<String> TIPOS_COMPUTO = java.util.Set.of(
        "LAPTOP","DESKTOP","ALL IN ONE","WORKSTATION",
        "LAPTOP WORKSTATION","DESKTOP TINY","OPS","TABLET","CELULAR","PANTALLA INTERACTIVA"
    );

    /**
     * POST /api/activos/nuevo
     * Body JSON (todos opcionales salvo tipoEquipo y serie):
     * {
     *   // Datos del equipo
     *   "tipoEquipo":              "LAPTOP",
     *   "serie":                   "MP27CSGH",
     *   "marca":                   "LENOVO",
     *   "modelo":                  "20VE-005HLM",
     *   "modeloComercial":         "ThinkPad E14 Gen 4",
     *   "codPatrimonioInventario": "2438002001003",
     *   "ordenCompra":             "OC-1234",
     *   "anoCompra":               "2026",
     *   "financiamiento":          "PRESUPUESTO",
     *   "usoActivo":               "ADMINISTRATIVO",
     *   "esNuevo":                 "true",
     *   "estadoFisico":            "OPERATIVO",
     *   "estadoDisponibilidad":    "DISPONIBLE",
     *   "gestion":                 "DISPONIBLE",
     *   "observacion":             "...",
     *   "tecnicoRegistro":         "MEDINA CARO JOSE JOEL",
     *
     *   // Responsable
     *   "dniResponsable": "47915981",
     *
     *   // Ubicación
     *   "campus":    "SEDE CORPAC",
     *   "pabellon":  "EDIFICIO",
     *   "piso":      "PISO 4",
     *   "unidad":    "DTI",
     *   "area":      "SOPORTE TECNICO",
     *
     *   // Especificaciones cómputo (solo para laptops/desktops/etc.)
     *   "hostname":          "UPCH24LAP0001",
     *   "procesador":        "Intel i7 13° Generación",
     *   "ram":               "16 Gb",
     *   "discoSsd":          "512 Gb",
     *   "sistemaOperativo":  "Windows 11 Pro",
     *   "serieCargador":     "8SGX20Z70324",
     *   "nroAnydesk":        "123456789",
     *   "tieneFortiedr":     "true",
     *   "tieneForticloud":   "true",
     *   "tieneInvgate":      "true"
     * }
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {

        // ── Validaciones básicas ──────────────────────────────────────────────
        String serie     = get(body, "serie");
        String tipoEquipo = get(body, "tipoEquipo");

        if (serie == null || serie.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'serie' es obligatorio"));
        if (tipoEquipo == null || tipoEquipo.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'tipoEquipo' es obligatorio"));
        if (activoRepo.existsBySerie(serie))
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un equipo con la serie: " + serie));

        // ── Ubicación ─────────────────────────────────────────────────────────
        Ubicacion ub = null;
        String campus = get(body, "campus");
        if (campus != null && !campus.isBlank()) {
            String pabellon = nvl(get(body, "pabellon"));
            String piso     = nvl(get(body, "piso"));
            String unidad   = nvl(get(body, "unidad"));
            String area2    = get(body, "area");
            var existentes  = ubicacionRepo.findByCampusCombinacion(campus, pabellon, piso, unidad, nvl(area2));
            if (!existentes.isEmpty()) {
                ub = existentes.get(0);
            } else {
                ub = new Ubicacion();
                ub.setCampus(campus);
                ub.setPabellon(pabellon.isEmpty() ? null : pabellon);
                ub.setPiso(piso.isEmpty() ? null : piso);
                ub.setUnidad(unidad.isEmpty() ? null : unidad);
                ub.setArea(get(body, "area"));
                ub = ubicacionRepo.save(ub);
            }
        }

        // ── Responsable ───────────────────────────────────────────────────────
        Usuario responsable = null;
        String dni = get(body, "dniResponsable");
        if (dni != null && !dni.isBlank()) {
            responsable = usuarioRepo.findById(dni.trim()).orElse(null);
        }

        // ── Activo Maestro ────────────────────────────────────────────────────
        ActivoMaestro a = new ActivoMaestro();
        a.setSerie(serie);
        a.setTipoEquipo(tipoEquipo.toUpperCase().trim());
        a.setMarca(get(body, "marca"));
        a.setModelo(get(body, "modelo"));
        a.setModeloComercial(get(body, "modeloComercial"));
        a.setCodPatrimonioInventario(get(body, "codPatrimonioInventario"));
        a.setOrdenCompra(get(body, "ordenCompra"));
        a.setFinanciamiento(get(body, "financiamiento"));
        a.setUsoActivo(get(body, "usoActivo"));
        a.setObservacion(get(body, "observacion"));
        a.setTecnicoRegistro(get(body, "tecnicoRegistro"));

        String estadoFisico = get(body, "estadoFisico");
        a.setEstadoFisico(estadoFisico != null ? estadoFisico.toUpperCase() : "OPERATIVO");

        String estadoDisp = get(body, "estadoDisponibilidad");
        a.setEstadoDisponibilidad(estadoDisp != null ? estadoDisp.toUpperCase() : "DISPONIBLE");

        String gestion = get(body, "gestion");
        a.setGestion(gestion != null ? gestion.toUpperCase() : "DISPONIBLE");

        a.setEnUso("EN USO".equalsIgnoreCase(estadoDisp));
        a.setEsNuevo(Boolean.parseBoolean(body.getOrDefault("esNuevo", "false")));

        if (body.containsKey("anoCompra")) {
            try { a.setAnoCompra(Integer.parseInt(body.get("anoCompra"))); }
            catch (NumberFormatException ignored) {}
        }

        a.setUbicacion(ub);
        a.setResponsable(responsable);

        ActivoMaestro saved = activoRepo.save(a);

        // ── Especificaciones de cómputo ───────────────────────────────────────
        String tipoUp = tipoEquipo.toUpperCase().trim();
        if (TIPOS_COMPUTO.contains(tipoUp)) {
            EspecificacionComputo esp = new EspecificacionComputo();
            esp.setActivo(saved);
            esp.setHostname(get(body, "hostname"));
            esp.setProcesador(get(body, "procesador"));
            esp.setRam(get(body, "ram"));
            esp.setDiscoSsd(get(body, "discoSsd"));
            esp.setSistemaOperativo(get(body, "sistemaOperativo"));
            esp.setSerieCargador(get(body, "serieCargador"));
            esp.setNroAnydesk(get(body, "nroAnydesk"));
            esp.setGpu(get(body, "gpu"));
            esp.setTieneFortiedr(Boolean.parseBoolean(body.getOrDefault("tieneFortiedr","false")));
            esp.setTieneForticloud(Boolean.parseBoolean(body.getOrDefault("tieneForticloud","false")));
            esp.setTieneInvgate(Boolean.parseBoolean(body.getOrDefault("tieneInvgate","false")));
            espRepo.save(esp);
        }

        log.info("Activo registrado vía interfaz: {} {} ({})", tipoEquipo, serie, a.getMarca());
        return ResponseEntity.ok(Map.of(
            "ok",      true,
            "id",      saved.getIdActivo(),
            "serie",   saved.getSerie(),
            "tipo",    saved.getTipoEquipo(),
            "mensaje", "Equipo registrado correctamente"
        ));
    }

    private String get(Map<String, String> m, String k) {
        String v = m.get(k);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
    private String nvl(String v) { return v != null ? v : ""; }
}
