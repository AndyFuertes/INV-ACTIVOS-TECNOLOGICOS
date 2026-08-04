package com.dti.keru.controller;

import com.dti.keru.entity.ActivoMaestro;
import com.dti.keru.entity.MovimientoHistorial;
import com.dti.keru.repository.MovimientoHistorialRepository;
import com.dti.keru.service.ActivoService;
import com.dti.keru.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activos")
@RequiredArgsConstructor
@Slf4j
public class ActivoController {

    private final ActivoService activoService;
    private final FileStorageService fileStorageService;
    private final MovimientoHistorialRepository movimientoRepo;

    // ── Endpoints por sección — incluye parámetro includeBaja ────────────────
    @GetMapping("/laptops")
    public ResponseEntity<Page<ActivoMaestro>> laptops(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_LAPTOP, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/desktops")
    public ResponseEntity<Page<ActivoMaestro>> desktops(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_DESKTOP, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/aio")
    public ResponseEntity<Page<ActivoMaestro>> aio(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_AIO, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/workstations")
    public ResponseEntity<Page<ActivoMaestro>> workstations(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_WORKSTATION, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/otros-computo")
    public ResponseEntity<Page<ActivoMaestro>> otrosComputo(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_OTROS_C, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/monitores")
    public ResponseEntity<Page<ActivoMaestro>> monitores(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_MONITOR, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/impresoras")
    public ResponseEntity<Page<ActivoMaestro>> impresoras(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_IMPRESORA, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/proyectores")
    public ResponseEntity<Page<ActivoMaestro>> proyectores(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_PROYECTOR, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    @GetMapping("/otros-perifericos")
    public ResponseEntity<Page<ActivoMaestro>> otrosPeri(@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String gestion, @RequestParam(defaultValue="") String campus,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="false") boolean includeBaja,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ok(activoService.buscar(ActivoService.TIPOS_OTROS_P, nn(estado), nn(gestion), nn(campus), nn(search), includeBaja, page, size));
    }

    // ── Bajas separadas ───────────────────────────────────────────────────────
    @GetMapping("/bajas")
    public ResponseEntity<Page<ActivoMaestro>> bajas(
            @RequestParam(required=false) List<String> tipos,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) {
        return ok(activoService.getBajas(tipos, page, size));
    }

    // ── Detalle / búsqueda por serie o ID ────────────────────────────────────
    @GetMapping("/serie/{serie}")
    public ResponseEntity<?> getBySerie(@PathVariable String serie) {
        return activoService.findBySerie(serie)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return activoService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Edición individual ────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id,
                                     @RequestBody Map<String, String> campos) {
        try {
            return ResponseEntity.ok(activoService.editarActivo(id, campos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Subir PDF al equipo ───────────────────────────────────────────────────
    @PostMapping("/{id}/pdf")
    public ResponseEntity<Map<String, Object>> subirPdf(@PathVariable Integer id,
                                                         @RequestParam("file") MultipartFile file) {
        try {
            String nombre = activoService.subirPdfEquipo(id, file);
            return ResponseEntity.ok(Map.of("ok", true, "nombreArchivo", nombre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    // ── Descargar PDF del equipo ──────────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable Integer id) {
        if (activoService.findById(id).isEmpty()) return ResponseEntity.<Resource>notFound().build();
        ActivoMaestro a = activoService.findById(id).get();
        try {
            String nombre = a.getRutaPdfEquipo();
            if (nombre == null || nombre.isBlank()) return ResponseEntity.<Resource>notFound().build();
            Resource pdf = fileStorageService.cargarPdfEquipo(nombre);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.<Resource>notFound().build();
        }
    }

    /** GET /api/activos/{id}/historial — actas vinculadas a un equipo */
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<MovimientoHistorial>> getHistorial(@PathVariable Integer id) {
        // Buscar movimientos donde el activo participa (por serie o por id en detalle)
        return activoService.findById(id).map(a -> {
            List<MovimientoHistorial> movs = movimientoRepo
                .findTop50BySerieEquipoOrderByFechaMovimientoDesc(a.getSerie());
            return ResponseEntity.ok(movs);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/campuses")
    public ResponseEntity<List<String>> getCampuses() {
        return ResponseEntity.ok(activoService.getCampuses());
    }

    private <T> ResponseEntity<T> ok(T body) { return ResponseEntity.ok(body); }
    private String nn(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}
