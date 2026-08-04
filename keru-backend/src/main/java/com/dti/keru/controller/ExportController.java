package com.dti.keru.controller;

import com.dti.keru.entity.ActivoMaestro;
import com.dti.keru.entity.EspecificacionComputo;
import com.dti.keru.repository.ActivoMaestroRepository;
import com.dti.keru.repository.EspecificacionComputoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ActivoMaestroRepository        activoRepo;
    private final EspecificacionComputoRepository espRepo;

    // Tipos de cómputo (tienen especificaciones técnicas)
    private static final Set<String> TIPOS_COMPUTO = Set.of(
        "LAPTOP","DESKTOP","ALL IN ONE","WORKSTATION",
        "LAPTOP WORKSTATION","DESKTOP TINY","OPS","TABLET","CELULAR","PANTALLA INTERACTIVA"
    );

    /**
     * GET /api/export/excel
     *
     * Parámetros:
     *   tipos      = LAPTOP,DESKTOP,MONITOR,...  (separados por coma, vacío = todos)
     *   estado     = OPERATIVO | INOPERATIVO | ...
     *   gestion    = ASIGNADO | DISPONIBLE | ...
     *   campus     = texto parcial
     *   includeBaja= true | false (default false)
     *   formato    = UNICA_HOJA | POR_TIPO  (default POR_TIPO)
     *   columnas   = BASICOS,SPECS,UBICACION,RESPONSABLE,ESTADO,AGENTES,TODAS (default TODAS)
     *
     * Devuelve: archivo .xlsx para descarga
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(defaultValue = "")      String tipos,
            @RequestParam(defaultValue = "")      String estado,
            @RequestParam(defaultValue = "")      String gestion,
            @RequestParam(defaultValue = "")      String campus,
            @RequestParam(defaultValue = "false") boolean includeBaja,
            @RequestParam(defaultValue = "POR_TIPO") String formato,
            @RequestParam(defaultValue = "TODAS") String columnas
    ) throws IOException {

        List<String> tiposList = parseTipos(tipos);
        Set<String> colSet     = parseColumnas(columnas);

        // Obtener todos los activos (sin paginación para export)
        List<ActivoMaestro> activos = activoRepo.buscarConFiltros(
            tiposList.isEmpty() ? null : tiposList.stream().map(String::toUpperCase).toList(),
            nn(estado), nn(gestion), nn(campus), null, includeBaja,
            PageRequest.of(0, 50000, Sort.by("tipoEquipo","serie"))
        ).getContent();

        log.info("Exportando {} activos, formato={}, columnas={}", activos.size(), formato, columnas);

        byte[] excelBytes;
        if ("UNICA_HOJA".equals(formato)) {
            excelBytes = generarUnaHoja(activos, colSet);
        } else {
            excelBytes = generarPorTipo(activos, colSet);
        }

        String nombreArchivo = "KERU_Inventario_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            + ".xlsx";

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
            .body(excelBytes);
    }

    // ── Formato: una hoja por tipo de equipo ──────────────────────────────────
    private byte[] generarPorTipo(List<ActivoMaestro> activos, Set<String> cols) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();

        // Agrupar por tipo
        Map<String, List<ActivoMaestro>> porTipo = new LinkedHashMap<>();
        for (ActivoMaestro a : activos) {
            String t = a.getTipoEquipo() != null ? a.getTipoEquipo() : "SIN TIPO";
            porTipo.computeIfAbsent(t, k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<String, List<ActivoMaestro>> entry : porTipo.entrySet()) {
            String tipo   = entry.getKey();
            boolean esComp = TIPOS_COMPUTO.contains(tipo.toUpperCase());
            XSSFSheet sheet = wb.createSheet(sheetName(tipo));
            escribirHoja(wb, sheet, entry.getValue(), cols, esComp);
        }

        // Hoja resumen
        crearHojaResumen(wb, porTipo);

        return toBytes(wb);
    }

    // ── Formato: todo en una sola hoja ────────────────────────────────────────
    private byte[] generarUnaHoja(List<ActivoMaestro> activos, Set<String> cols) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Inventario DTI");
        escribirHoja(wb, sheet, activos, cols, true); // incluye todas las cols
        return toBytes(wb);
    }

    // ── Escribir una hoja ─────────────────────────────────────────────────────
    private void escribirHoja(XSSFWorkbook wb, XSSFSheet sheet,
                               List<ActivoMaestro> activos, Set<String> cols, boolean incluirSpecs) {

        // Estilos
        CellStyle headerStyle = crearEstiloHeader(wb);
        CellStyle dataStyle   = crearEstiloDato(wb);
        CellStyle nuevoStyle  = crearEstiloNuevo(wb);
        CellStyle bajaStyle   = crearEstiloBaja(wb);

        // Construir lista de columnas según filtro
        List<String[]> columnas = buildColumnas(cols, incluirSpecs);

        // Fila de encabezado
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas.get(i)[0]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, Math.max(columnas.get(i)[0].length() + 4, 14) * 256);
        }
        header.setHeightInPoints(18);

        // Filas de datos
        int rowNum = 1;
        for (ActivoMaestro a : activos) {
            EspecificacionComputo esp = null;
            if (incluirSpecs && a.getIdActivo() != null) {
                esp = espRepo.findByActivo_IdActivo(a.getIdActivo()).orElse(null);
            }

            Row row = sheet.createRow(rowNum++);
            boolean esNuevo = Boolean.TRUE.equals(a.getEsNuevo());
            boolean esBaja  = "BAJA".equalsIgnoreCase(a.getGestion()) ||
                              "BAJA".equalsIgnoreCase(a.getEstadoFisico());
            CellStyle rowStyle = esBaja ? bajaStyle : (esNuevo ? nuevoStyle : dataStyle);

            for (int i = 0; i < columnas.size(); i++) {
                Cell cell = row.createCell(i);
                String valor = getValor(columnas.get(i)[1], a, esp);
                cell.setCellValue(valor != null ? valor : "");
                cell.setCellStyle(rowStyle);
            }
        }

        // Auto-filter en encabezado
        if (!activos.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnas.size() - 1));
        }

        // Congelar primera fila
        sheet.createFreezePane(0, 1);
    }

    // ── Hoja resumen ──────────────────────────────────────────────────────────
    private void crearHojaResumen(XSSFWorkbook wb, Map<String, List<ActivoMaestro>> porTipo) {
        XSSFSheet sheet = wb.createSheet("RESUMEN");
        wb.setSheetOrder("RESUMEN", 0); // mover al principio

        CellStyle titleStyle  = crearEstiloHeader(wb);
        CellStyle subtitleStyle = wb.createCellStyle();
        XSSFFont subFont = wb.createFont();
        subFont.setBold(true);
        subtitleStyle.setFont(subFont);
        subtitleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x1A,(byte)0x2E,(byte)0x4A}, null));
        subtitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont whiteFont = wb.createFont();
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        whiteFont.setBold(true);
        subtitleStyle.setFont(whiteFont);

        // Título
        Row titulo = sheet.createRow(0);
        Cell tCell = titulo.createCell(0);
        tCell.setCellValue("INVENTARIO DTI - UPCH " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        tCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Encabezados resumen
        Row hRow = sheet.createRow(2);
        String[] headers = {"Tipo de Equipo", "Total", "En Uso", "Disponibles", "Nuevos"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(subtitleStyle);
            sheet.setColumnWidth(i, 5000);
        }

        // Datos
        int rowIdx = 3;
        int grandTotal = 0;
        for (Map.Entry<String, List<ActivoMaestro>> e : porTipo.entrySet()) {
            List<ActivoMaestro> lista = e.getValue();
            long enUso   = lista.stream().filter(a -> "EN USO".equalsIgnoreCase(a.getEstadoDisponibilidad())).count();
            long dispon  = lista.stream().filter(a -> "DISPONIBLE".equalsIgnoreCase(a.getEstadoDisponibilidad())).count();
            long nuevos  = lista.stream().filter(a -> Boolean.TRUE.equals(a.getEsNuevo())).count();

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(lista.size());
            row.createCell(2).setCellValue(enUso);
            row.createCell(3).setCellValue(dispon);
            row.createCell(4).setCellValue(nuevos);
            grandTotal += lista.size();
        }

        // Total general
        Row totalRow = sheet.createRow(rowIdx + 1);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL GENERAL");
        totalLabel.setCellStyle(subtitleStyle);
        Cell totalVal = totalRow.createCell(1);
        totalVal.setCellValue(grandTotal);
        totalVal.setCellStyle(subtitleStyle);
    }

    // ── Definición de columnas disponibles ───────────────────────────────────
    private List<String[]> buildColumnas(Set<String> cols, boolean incluirSpecs) {
        List<String[]> list = new ArrayList<>();

        // BÁSICOS (siempre incluidos)
        list.add(new String[]{"#", "idx"});
        list.add(new String[]{"TIPO EQUIPO", "tipoEquipo"});
        list.add(new String[]{"SERIE", "serie"});
        list.add(new String[]{"N° INVENTARIO", "codPatrimonioInventario"});
        list.add(new String[]{"MARCA", "marca"});
        list.add(new String[]{"MODELO", "modelo"});
        list.add(new String[]{"MODELO COMERCIAL", "modeloComercial"});
        list.add(new String[]{"AÑO COMPRA", "anoCompra"});
        list.add(new String[]{"ORDEN DE COMPRA", "ordenCompra"});
        list.add(new String[]{"FINANCIAMIENTO", "financiamiento"});
        list.add(new String[]{"USO", "usoActivo"});
        list.add(new String[]{"NUEVO", "esNuevo"});

        if (cols.contains("ESTADO") || cols.contains("TODAS")) {
            list.add(new String[]{"ESTADO FÍSICO", "estadoFisico"});
            list.add(new String[]{"DISPONIBILIDAD", "estadoDisponibilidad"});
            list.add(new String[]{"GESTIÓN", "gestion"});
        }

        if (incluirSpecs && (cols.contains("SPECS") || cols.contains("TODAS"))) {
            list.add(new String[]{"HOSTNAME", "hostname"});
            list.add(new String[]{"PROCESADOR", "procesador"});
            list.add(new String[]{"RAM", "ram"});
            list.add(new String[]{"SSD / DISCO", "discoSsd"});
            list.add(new String[]{"SISTEMA OPERATIVO", "sistemaOperativo"});
            list.add(new String[]{"SERIE CARGADOR", "serieCargador"});
            list.add(new String[]{"N° ANYDESK", "nroAnydesk"});
            list.add(new String[]{"GPU", "gpu"});
        }

        if (cols.contains("UBICACION") || cols.contains("TODAS")) {
            list.add(new String[]{"CAMPUS", "campus"});
            list.add(new String[]{"PABELLÓN", "pabellon"});
            list.add(new String[]{"PISO", "piso"});
            list.add(new String[]{"UNIDAD", "unidad"});
            list.add(new String[]{"ÁREA", "area"});
        }

        if (cols.contains("RESPONSABLE") || cols.contains("TODAS")) {
            list.add(new String[]{"DNI RESPONSABLE", "dniResponsable"});
            list.add(new String[]{"NOMBRE RESPONSABLE", "nombreResponsable"});
            list.add(new String[]{"CARGO RESPONSABLE", "cargoResponsable"});
            list.add(new String[]{"CENTRO DE COSTOS", "centroCosto"});
            list.add(new String[]{"NOMBRE C.C.", "nombreCentroCosto"});
        }

        if (incluirSpecs && (cols.contains("AGENTES") || cols.contains("TODAS"))) {
            list.add(new String[]{"FORTIEDR", "fortiedr"});
            list.add(new String[]{"FORTICLOUD", "forticloud"});
            list.add(new String[]{"INVGATE", "invgate"});
        }

        list.add(new String[]{"OBSERVACIÓN", "observacion"});
        list.add(new String[]{"TÉCNICO REGISTRO", "tecnicoRegistro"});

        return list;
    }

    // ── Obtener valor de campo de un activo ───────────────────────────────────
    private String getValor(String campo, ActivoMaestro a, EspecificacionComputo esp) {
        return switch (campo) {
            case "idx"                 -> "";  // se asigna después
            case "tipoEquipo"          -> a.getTipoEquipo();
            case "serie"               -> a.getSerie();
            case "codPatrimonioInventario" -> a.getCodPatrimonioInventario();
            case "marca"               -> a.getMarca();
            case "modelo"              -> a.getModelo();
            case "modeloComercial"     -> a.getModeloComercial();
            case "anoCompra"           -> a.getAnoCompra() != null ? String.valueOf(a.getAnoCompra()) : "";
            case "ordenCompra"         -> a.getOrdenCompra();
            case "financiamiento"      -> a.getFinanciamiento();
            case "usoActivo"           -> a.getUsoActivo();
            case "esNuevo"             -> Boolean.TRUE.equals(a.getEsNuevo()) ? "SÍ" : "NO";
            case "estadoFisico"        -> a.getEstadoFisico();
            case "estadoDisponibilidad"-> a.getEstadoDisponibilidad();
            case "gestion"             -> a.getGestion();
            case "observacion"         -> a.getObservacion();
            case "tecnicoRegistro"     -> a.getTecnicoRegistro();
            case "hostname"            -> esp != null ? esp.getHostname()         : "";
            case "procesador"          -> esp != null ? esp.getProcesador()       : "";
            case "ram"                 -> esp != null ? esp.getRam()              : "";
            case "discoSsd"            -> esp != null ? esp.getDiscoSsd()         : "";
            case "sistemaOperativo"    -> esp != null ? esp.getSistemaOperativo() : "";
            case "serieCargador"       -> esp != null ? esp.getSerieCargador()    : "";
            case "nroAnydesk"          -> esp != null ? esp.getNroAnydesk()       : "";
            case "gpu"                 -> esp != null ? esp.getGpu()              : "";
            case "fortiedr"            -> esp != null && Boolean.TRUE.equals(esp.getTieneFortiedr())   ? "✓" : "";
            case "forticloud"          -> esp != null && Boolean.TRUE.equals(esp.getTieneForticloud())  ? "✓" : "";
            case "invgate"             -> esp != null && Boolean.TRUE.equals(esp.getTieneInvgate())     ? "✓" : "";
            case "campus"              -> a.getUbicacion() != null ? a.getUbicacion().getCampus()    : "";
            case "pabellon"            -> a.getUbicacion() != null ? a.getUbicacion().getPabellon()  : "";
            case "piso"                -> a.getUbicacion() != null ? a.getUbicacion().getPiso()      : "";
            case "unidad"              -> a.getUbicacion() != null ? a.getUbicacion().getUnidad()    : "";
            case "area"                -> a.getUbicacion() != null ? a.getUbicacion().getArea()      : "";
            case "dniResponsable"      -> a.getResponsable() != null ? a.getResponsable().getDni()            : "";
            case "nombreResponsable"   -> a.getResponsable() != null ? a.getResponsable().getNombre()         : "";
            case "cargoResponsable"    -> a.getResponsable() != null ? a.getResponsable().getPuestoGeneral()  : "";
            case "centroCosto"         -> a.getResponsable() != null ? a.getResponsable().getCentroCosto()        : "";
            case "nombreCentroCosto"   -> a.getResponsable() != null ? a.getResponsable().getNombreCentroCosto()  : "";
            default                    -> "";
        };
    }

    // ── Estilos POI ──────────────────────────────────────────────────────────
    private CellStyle crearEstiloHeader(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{0x00, 0x5B, (byte)0xA6}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setBold(true); f.setFontHeightInPoints((short)11);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle crearEstiloDato(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderRight(BorderStyle.HAIR);
        return s;
    }

    private CellStyle crearEstiloNuevo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xE1,(byte)0xF5,(byte)0xEE}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private CellStyle crearEstiloBaja(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFC,(byte)0xEB,(byte)0xEB}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setStrikeout(true);
        f.setColor(new XSSFColor(new byte[]{(byte)0xA3,0x2D,0x2D}, null));
        s.setFont(f);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<String> parseTipos(String tipos) {
        if (tipos == null || tipos.isBlank()) return List.of();
        return Arrays.stream(tipos.split(","))
            .map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private Set<String> parseColumnas(String columnas) {
        if (columnas == null || columnas.isBlank() || "TODAS".equals(columnas))
            return Set.of("TODAS");
        Set<String> set = new HashSet<>();
        for (String c : columnas.split(",")) set.add(c.trim().toUpperCase());
        return set;
    }

    private String nn(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }

    private String sheetName(String tipo) {
        // Excel sheet names max 31 chars, no special chars
        String name = tipo.replaceAll("[\\[\\]\\*\\?/\\\\:]", "");
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }
}
