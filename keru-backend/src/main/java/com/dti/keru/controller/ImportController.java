package com.dti.keru.controller;

import com.dti.keru.service.ExcelImportService;
import com.dti.keru.service.ExcelImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final ExcelImportService importService;

    /**
     * POST /api/import/planilla
     * Recibe PLANILLA.xlsx → carga tabla usuarios
     */
    @PostMapping("/planilla")
    public ResponseEntity<Map<String, Object>> importarPlanilla(@RequestParam("file") MultipartFile file) {
        log.info("Importando planilla: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
        try {
            ImportResult result = importService.importarPlanilla(file);
            return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "insertados",  result.insertados,
                "actualizados",result.actualizados,
                "saltados",    result.saltados,
                "errores",     result.errores,
                "log",         result.log
            ));
        } catch (Exception e) {
            log.error("Error importando planilla", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/import/computo
     * Recibe EQUIPO-COMPUTO.xlsx → activos_maestro + especificaciones_computo
     */
    @PostMapping("/computo")
    public ResponseEntity<Map<String, Object>> importarComputo(@RequestParam("file") MultipartFile file) {
        log.info("Importando equipos cómputo: {}", file.getOriginalFilename());
        try {
            ImportResult result = importService.importarEquiposComputo(file);
            return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "insertados",  result.insertados,
                "actualizados",result.actualizados,
                "saltados",    result.saltados,
                "errores",     result.errores,
                "log",         result.log
            ));
        } catch (Exception e) {
            log.error("Error importando cómputo", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/import/perifericos
     * Recibe PERIFERICOS.xlsx → activos_maestro (sin especificaciones_computo)
     */
    @PostMapping("/perifericos")
    public ResponseEntity<Map<String, Object>> importarPerifericos(@RequestParam("file") MultipartFile file) {
        log.info("Importando periféricos: {}", file.getOriginalFilename());
        try {
            ImportResult result = importService.importarPerifericos(file);
            return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "insertados",  result.insertados,
                "actualizados",result.actualizados,
                "saltados",    result.saltados,
                "errores",     result.errores,
                "log",         result.log
            ));
        } catch (Exception e) {
            log.error("Error importando periféricos", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/import/historial
     * Importa historial de actas y asignaciones desde Excel de inventario
     * Lee columnas: SERIE, ACTA, FECHA DE ENTREGA, TIPO DE ACTA, USUARIO, DNI, CENTRO DE COSTOS
     */
    @PostMapping("/historial")
    public ResponseEntity<Map<String, Object>> importarHistorial(@RequestParam("file") MultipartFile file) {
        log.info("Importando historial: {}", file.getOriginalFilename());
        try {
            var result = importService.importarHistorialDesdeExcel(file);
            return ResponseEntity.ok(Map.of(
                "status",      "ok",
                "insertados",  result.insertados,
                "actualizados",result.actualizados,
                "saltados",    result.saltados,
                "errores",     result.errores,
                "log",         result.log
            ));
        } catch (Exception e) {
            log.error("Error importando historial", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status",  "error",
                "mensaje", e.getMessage()
            ));
        }
    }
}