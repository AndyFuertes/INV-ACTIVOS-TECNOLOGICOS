package com.dti.keru.controller;

import com.dti.keru.entity.MovimientoHistorial;
import com.dti.keru.repository.MovimientoHistorialRepository;
import com.dti.keru.service.ActaService;
import com.dti.keru.service.FileStorageService;
import com.dti.keru.service.PdfActaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/actas")
@RequiredArgsConstructor
@Slf4j
public class ActaController {

    private final ActaService actaService;
    private final PdfActaService pdfActaService;
    private final FileStorageService fileStorageService;
    private final MovimientoHistorialRepository movimientoRepo;

    @GetMapping("/siguiente-nro")
    public ResponseEntity<Map<String, String>> siguienteNro(@RequestParam String tipo) {
        return ResponseEntity.ok(Map.of("nro", actaService.siguienteNroActa(tipo)));
    }

    @GetMapping
    public ResponseEntity<Page<MovimientoHistorial>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(actaService.listar(page, size));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        try {
            MovimientoHistorial mov = actaService.crearMovimiento(
                body.get("tipoMovimiento"), body.get("dniEntrega"),
                body.get("dniRecibe"), body.get("centroCosto"),
                body.get("tecnico"), body.get("serieEquipo"));
            try {
                String nombrePdf = pdfActaService.generarPdf(
                    mov, body.get("serieEquipo"), body.get("ubicacion"), body.get("centroCosto"));
                mov.setRutaPdfGenerado(nombrePdf);
                movimientoRepo.save(mov);
            } catch (Exception e) {
                log.error("PDF generation error: {}", e.getMessage());
            }
            return ResponseEntity.ok(Map.of(
                "id", mov.getIdMovimiento(), "nroActa", mov.getNroActa(),
                "tipo", mov.getTipoMovimiento(),
                "pdfNombre", mov.getRutaPdfGenerado() != null ? mov.getRutaPdfGenerado() : "",
                "fecha", mov.getFechaMovimiento().toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> descargarPdf(@PathVariable Integer id) {
        return movimientoRepo.findById(id).map(mov -> {
            try {
                String nombre = mov.getRutaPdfGenerado();
                if (nombre == null || nombre.isBlank()) {
                    nombre = pdfActaService.generarPdf(mov, null, null, null);
                    mov.setRutaPdfGenerado(nombre); movimientoRepo.save(mov);
                }
                Resource pdf = fileStorageService.cargarPdf(nombre, "generado");
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .body(pdf);
            } catch (Exception e) { throw new RuntimeException(e.getMessage()); }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pdf-firmado")
    public ResponseEntity<Map<String, Object>> subirPdfFirmado(
            @PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return movimientoRepo.findById(id).map(mov -> {
            try {
                String nombre = fileStorageService.guardarPdfFirmado(file, mov.getNroActa());
                mov.setRutaPdfFirmado(nombre); movimientoRepo.save(mov);
                return ResponseEntity.ok(Map.<String, Object>of(
                    "ok", true, "nroActa", mov.getNroActa(), "nombreArchivo", nombre));
            } catch (Exception e) {
                return ResponseEntity.badRequest().<Map<String, Object>>body(Map.of("ok", false, "mensaje", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf-firmado")
    public ResponseEntity<Resource> descargarPdfFirmado(@PathVariable Integer id) {
        if (movimientoRepo.findById(id).isEmpty()) return ResponseEntity.<Resource>notFound().build();
        MovimientoHistorial mov = movimientoRepo.findById(id).get();
        try {
            String nombre = mov.getRutaPdfFirmado();
            if (nombre == null || nombre.isBlank()) return ResponseEntity.<Resource>notFound().build();
            Resource pdf = fileStorageService.cargarPdf(nombre, "firmado");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(pdf);
        } catch (Exception e) { return ResponseEntity.<Resource>notFound().build(); }
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<Map<String, Object>> estadoActa(@PathVariable Integer id) {
        return movimientoRepo.findById(id).map(mov ->
            ResponseEntity.ok(Map.<String, Object>of(
                "id", mov.getIdMovimiento(), "nroActa", mov.getNroActa(),
                "tienePdf",     mov.getRutaPdfGenerado() != null && !mov.getRutaPdfGenerado().isBlank(),
                "tieneFirmado", mov.getRutaPdfFirmado()  != null && !mov.getRutaPdfFirmado().isBlank(),
                "nombrePdf",    mov.getRutaPdfGenerado() != null ? mov.getRutaPdfGenerado() : "",
                "nombreFirmado",mov.getRutaPdfFirmado()  != null ? mov.getRutaPdfFirmado()  : ""))
        ).orElse(ResponseEntity.notFound().build());
    }
}
