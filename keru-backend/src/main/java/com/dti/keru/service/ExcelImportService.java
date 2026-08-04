package com.dti.keru.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Orquestador de importación Excel.
 * Delega cada fila a ExcelRowService cuyo @Transactional(REQUIRES_NEW)
 * garantiza que un error en una fila NO contamine ni revierta las demás.
 * Esto resuelve el bug "Transaction silently rolled back" que ocurría
 * cuando un DNI duplicado marcaba rollback-only toda la transacción padre.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final ExcelRowService rowService;

    public static class ImportResult {
        public int insertados   = 0;
        public int actualizados = 0;
        public int saltados     = 0;
        public int errores      = 0;
        public List<String> log = new ArrayList<>();
        public void ok(String m)   { log.add("[OK] "   + m); }
        public void skip(String m) { log.add("[SKIP] " + m); saltados++; }
        public void err(String m)  { log.add("[ERR] "  + m); errores++; }
    }

    // ═══════════════════════════════════════════════════════
    // 1. PLANILLA → usuarios
    // ═══════════════════════════════════════════════════════
    public ImportResult importarPlanilla(MultipartFile file) throws IOException {
        ImportResult res = new ImportResult();
        Workbook wb = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);
        Map<String, Integer> cols = mapCols(sheet.getRow(0));
        log.info("Planilla: {} filas", sheet.getLastRowNum());

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String dni = rowService.str(row, cols, "DNI");
            if (dni == null || dni.isBlank()) continue;

            try {
                String r = rowService.guardarUsuario(dni, row, cols);
                if ("SKIP".equals(r))   { res.saltados++;    res.skip("DNI " + dni + " ya existe → sin tocar"); }
                else                    { res.insertados++;  }
            } catch (Exception e) {
                res.err("Fila " + (i+1) + " DNI=" + dni + ": " + e.getMessage());
                log.warn("Error planilla fila {}: {}", i+1, e.getMessage());
            }
        }
        wb.close();
        res.ok("Planilla: " + res.insertados + " nuevos, " + res.saltados
            + " ya existían (sin tocar), " + res.errores + " errores");
        return res;
    }

    // ═══════════════════════════════════════════════════════
    // 2. EQUIPOS CÓMPUTO → activos_maestro + especificaciones_computo
    // ═══════════════════════════════════════════════════════
    public ImportResult importarEquiposComputo(MultipartFile file) throws IOException {
        ImportResult res = new ImportResult();
        Workbook wb = new XSSFWorkbook(file.getInputStream());
        // Nombre de hoja actualizado: ahora es Hoja1 (antes Hoja2)
        Sheet sheet = wb.getSheet("Hoja1");
        if (sheet == null) sheet = wb.getSheet("Hoja2");
        if (sheet == null) sheet = wb.getSheetAt(0);
        Map<String, Integer> cols = mapCols(sheet.getRow(0));
        log.info("Cómputo: {} filas", sheet.getLastRowNum());

        int filasSinSerieC = 0;
        int seriesDuplicadasC = 0;
        java.util.Set<String> seriesComp = new java.util.HashSet<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) { filasSinSerieC++; continue; }
            String serie = rowService.str(row, cols, "SERIE");
            if (serie == null || serie.isBlank()) { filasSinSerieC++; continue; }

            if (seriesComp.contains(serie)) {
                seriesDuplicadasC++;
                res.skip("Fila " + (i+1) + " serie '" + serie + "' duplicada en el Excel");
                continue;
            }
            seriesComp.add(serie);

            try {
                String r = rowService.guardarComputo(serie, row, cols);
                if ("INSERT".equals(r)) res.insertados++; else res.actualizados++;
            } catch (Exception e) {
                res.err("Fila " + (i+1) + " [" + serie + "]: " + e.getMessage());
                log.warn("Error cómputo fila {} [{}]: {}", i+1, serie, e.getMessage());
            }
        }
        wb.close();
        res.ok("Cómputo: " + res.insertados + " nuevos, " + res.actualizados + " actualizados, "
            + res.errores + " errores, " + filasSinSerieC + " filas sin serie, "
            + seriesDuplicadasC + " series duplicadas en el Excel");
        if (filasSinSerieC > 0)
            res.skip(filasSinSerieC + " filas ignoradas por SERIE vacía");
        if (seriesDuplicadasC > 0)
            res.skip(seriesDuplicadasC + " filas con serie duplicada en el Excel");
        return res;
    }

    // ═══════════════════════════════════════════════════════
    // 3. PERIFÉRICOS → activos_maestro
    // ═══════════════════════════════════════════════════════
    public ImportResult importarPerifericos(MultipartFile file) throws IOException {
        ImportResult res = new ImportResult();
        Workbook wb = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);
        Map<String, Integer> cols = mapCols(sheet.getRow(0));
        log.info("Periféricos: {} filas", sheet.getLastRowNum());

        int filasSinSerie = 0;
        int seriesDuplicadas = 0;
        java.util.Set<String> seriesEnEstaImportacion = new java.util.HashSet<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) { filasSinSerie++; continue; }
            String serie = rowService.str(row, cols, "SERIE");
            if (serie == null || serie.isBlank()) { filasSinSerie++; continue; }

            // Detectar duplicados dentro del mismo Excel
            if (seriesEnEstaImportacion.contains(serie)) {
                seriesDuplicadas++;
                res.skip("Fila " + (i+1) + " serie '" + serie + "' duplicada en el Excel (ya procesada)");
                continue;
            }
            seriesEnEstaImportacion.add(serie);

            try {
                String r = rowService.guardarPeriferico(serie, row, cols);
                if ("INSERT".equals(r)) res.insertados++; else res.actualizados++;
            } catch (Exception e) {
                res.err("Fila " + (i+1) + " [" + serie + "]: " + e.getMessage());
                log.warn("Error periférico fila {} [{}]: {}", i+1, serie, e.getMessage());
            }
        }
        wb.close();
        res.ok("Periféricos: " + res.insertados + " nuevos, " + res.actualizados + " actualizados, "
            + res.errores + " errores, " + filasSinSerie + " filas sin serie, "
            + seriesDuplicadas + " series duplicadas en el Excel");
        if (filasSinSerie > 0)
            res.skip(filasSinSerie + " filas ignoradas por tener la columna SERIE vacía o nula");
        if (seriesDuplicadas > 0)
            res.skip(seriesDuplicadas + " filas ignoradas por serie duplicada dentro del mismo Excel");
        return res;
    }

    // ─── map columnas ─────────────────────────────────────
    private Map<String, Integer> mapCols(Row header) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (header == null) return m;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell != null) m.put(cell.getStringCellValue().trim(), c);
        }
        return m;
    }

// ═══════════════════════════════════════════════════════════════
    // 4. HISTORIAL desde Excel de inventario
    //    Lee columnas: SERIE, ACTA, FECHA DE ENTREGA, TIPO DE ACTA,
    //    USUARIO, DNI, CENTRO DE COSTOS
    //    Crea un MovimientoHistorial por cada fila con ACTA no vacía
    // ═══════════════════════════════════════════════════════════════
    public ImportResult importarHistorialDesdeExcel(MultipartFile file) throws IOException {
        ImportResult res = new ImportResult();
        Workbook wb = new XSSFWorkbook(file.getInputStream());

        int totalHojas = wb.getNumberOfSheets();
        for (int s = 0; s < totalHojas; s++) {
            Sheet sheet = wb.getSheetAt(s);
            Map<String, Integer> cols = mapCols(sheet.getRow(0));

            // Solo procesar si tiene la columna ACTA
            if (!cols.containsKey("ACTA ") && !cols.containsKey("ACTA")) {
                res.skip("Hoja '" + sheet.getSheetName() + "' no tiene columna ACTA — saltada");
                continue;
            }

            res.ok("Procesando hoja: " + sheet.getSheetName() + " (" + sheet.getLastRowNum() + " filas)");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String acta = rowService.str(row, cols, "ACTA ");
                if (acta == null) acta = rowService.str(row, cols, "ACTA");
                if (acta == null || acta.isBlank()) continue;

                String serie = rowService.str(row, cols, "SERIE");
                try {
                    String r = rowService.guardarMovimientoHistorial(acta.trim(), serie, row, cols);
                    if ("INSERT".equals(r)) res.insertados++;
                    else if ("SKIP".equals(r)) res.saltados++;
                    else res.actualizados++;
                } catch (Exception e) {
                    res.err("Fila " + (i+1) + " acta='" + acta + "': " + e.getMessage());
                    log.warn("Error historial fila {}: {}", i+1, e.getMessage());
                }
            }
        }
        wb.close();
        res.ok("Historial: " + res.insertados + " movimientos nuevos, "
            + res.saltados + " ya existían, " + res.errores + " errores");
        return res;
    }
}