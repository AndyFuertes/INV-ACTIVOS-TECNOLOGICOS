package com.dti.keru.service;

import com.dti.keru.entity.*;
import com.dti.keru.repository.ActivoMaestroRepository;
import com.dti.keru.repository.EspecificacionComputoRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfActaService {

    private final ActivoMaestroRepository activoRepo;
    private final EspecificacionComputoRepository espComputoRepo;

    @Value("${keru.storage.actas-pdf}")
    private String rutaActasPdf;

    private static final DeviceRgb UPCH_DARK  = new DeviceRgb(0x1A, 0x2E, 0x4A);
    private static final DeviceRgb UPCH_BLUE  = new DeviceRgb(0x00, 0x5B, 0xA6);
    private static final DeviceRgb UPCH_LIGHT = new DeviceRgb(0xF0, 0xF4, 0xF8);
    private static final DeviceRgb GRAY_TEXT  = new DeviceRgb(0x5A, 0x5A, 0x5A);

    public String generarPdf(MovimientoHistorial mov, String serieEquipo,
                              String ubicacion, String centroCosto) throws IOException {

        ActivoMaestro activo = null;
        EspecificacionComputo esp = null;

        if (serieEquipo != null && !serieEquipo.isBlank()) {
            activo = activoRepo.findBySerie(serieEquipo).orElse(null);
            if (activo != null) {
                esp = espComputoRepo.findByActivo_IdActivo(activo.getIdActivo()).orElse(null);
            }
        }

        String nroActa = mov.getNroActa() != null ? mov.getNroActa() : "SIN-NRO";
        String nombreArchivo = nroActa.replaceAll("[^a-zA-Z0-9_\\-]", "_")
            + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            + ".pdf";

        Path dir = Paths.get(rutaActasPdf);
        Files.createDirectories(dir);
        Path rutaArchivo = dir.resolve(nombreArchivo);

        try (PdfWriter writer = new PdfWriter(rutaArchivo.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(40, 50, 40, 50);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            agregarCabecera(doc, bold, regular);
            agregarNroYFecha(doc, mov, bold, regular);
            agregarTitulo(doc, mov.getTipoMovimiento(), bold);

            String tipo = mov.getTipoMovimiento() != null ? mov.getTipoMovimiento() : "";
            switch (tipo) {
                case "ASIGNACION"   -> cuerpoAsignacion(doc, mov, activo, esp, ubicacion, centroCosto, bold, regular, italic);
                case "DEVOLUCION"   -> cuerpoDevolucion(doc, mov, activo, bold, regular);
                case "PRESTAMO"     -> cuerpoPrestamo(doc, mov, activo, centroCosto, bold, regular);
                case "MOVILIZACION" -> cuerpoMovilizacion(doc, mov, activo, bold, regular);
                default             -> doc.add(new Paragraph("Tipo de acta: " + tipo).setFont(regular));
            }

            agregarFirmas(doc, mov, bold, regular, italic);
        }

        log.info("PDF generado: {}", rutaArchivo);
        return nombreArchivo;
    }

    // ── CABECERA ──────────────────────────────────────────────────────────────
    private void agregarCabecera(Document doc, PdfFont bold, PdfFont regular) throws IOException {
        doc.add(new Paragraph("UNIVERSIDAD PERUANA CAYETANO HEREDIA")
            .setFont(bold).setFontSize(13).setFontColor(UPCH_DARK)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(new Paragraph("Oficina Universitaria de Tecnologías de la Información — DTI")
            .setFont(regular).setFontSize(9).setFontColor(GRAY_TEXT)
            .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        SolidLine sl = new SolidLine(1.5f);
        sl.setColor(UPCH_BLUE);
        doc.add(new LineSeparator(sl).setMarginBottom(12));
    }

    private void agregarNroYFecha(Document doc, MovimientoHistorial mov,
                                   PdfFont bold, PdfFont regular) throws IOException {
        String nro = mov.getNroActa() != null ? mov.getNroActa() : "";
        doc.add(new Paragraph(nro)
            .setFont(bold).setFontSize(11).setFontColor(UPCH_BLUE)
            .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(2));

        LocalDateTime fecha = mov.getFechaMovimiento() != null ? mov.getFechaMovimiento() : LocalDateTime.now();
        String fechaFmt = fecha.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy").withLocale(Locale.forLanguageTag("es-PE")));
        doc.add(new Paragraph("Lima, " + fechaFmt)
            .setFont(regular).setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(16));
    }

    private void agregarTitulo(Document doc, String tipo, PdfFont bold) throws IOException {
        String titulo = switch (tipo != null ? tipo : "") {
            case "ASIGNACION"   -> "ACTA Y CONFORMIDAD DE ENTREGA DE ACTIVOS TECNOLÓGICOS";
            case "DEVOLUCION"   -> "ACTA DE DEVOLUCIÓN";
            case "PRESTAMO"     -> "ACTA DE PRÉSTAMO";
            case "MOVILIZACION" -> "SOLICITUD DE MOVILIZACIÓN DE BIENES";
            default             -> "ACTA";
        };
        doc.add(new Paragraph(titulo)
            .setFont(bold).setFontSize(13).setFontColor(UPCH_DARK)
            .setTextAlignment(TextAlignment.CENTER).setUnderline().setMarginBottom(14));
    }

    // ── CUERPOS ───────────────────────────────────────────────────────────────
    private void cuerpoAsignacion(Document doc, MovimientoHistorial mov,
                                   ActivoMaestro activo, EspecificacionComputo esp,
                                   String ubicacion, String centroCosto,
                                   PdfFont bold, PdfFont regular, PdfFont italic) throws IOException {

        doc.add(new Paragraph("El beneficiario se compromete a utilizar el equipo en las actividades " +
            "institucionales encomendadas por la Universidad Peruana Cayetano Heredia, a conservar " +
            "la integridad física del equipo y a reportar cualquier avería al Área de Soporte Técnico.")
            .setFont(regular).setFontSize(9).setFontColor(GRAY_TEXT)
            .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(12));

        seccionTitulo(doc, "CARACTERÍSTICAS DEL ACTIVO TECNOLÓGICO", bold);
        if (activo != null) tablaEquipo(doc, activo, esp, bold, regular);

        seccionTitulo(doc, "DATOS DE LA ASIGNACIÓN", bold);
        Usuario recibe = mov.getUsuarioRecibe();
        tablaDatos(doc, new String[][]{
            {"NOMBRES",       recibe != null ? recibe.getNombre()        : "—"},
            {"CARGO",         recibe != null ? recibe.getPuestoGeneral() : "—"},
            {"DNI",           recibe != null ? recibe.getDni()           : "—"},
            {"ÁREA / UNIDAD", recibe != null ? recibe.getDepartamento()  : "—"},
            {"UBICACIÓN",     ubicacion    != null ? ubicacion    : "—"},
            {"CENTRO COSTOS", centroCosto  != null ? centroCosto  : "—"},
        }, bold, regular);

        seccionTitulo(doc, "SOFTWARE INSTALADO", bold);
        String[] sw = {"1) Windows Pro 64-bit","2) Microsoft Office 365","3) FortiEDR",
                        "4) FortiClient","5) InvGate","6) Adobe Reader","7) Chrome / Firefox","8) Zoom"};
        for (String s : sw) doc.add(new Paragraph(s).setFont(regular).setFontSize(9).setMarginLeft(20).setMarginTop(1));
        doc.add(new Paragraph("Idioma de instalación: (X) Español   ( ) Inglés")
            .setFont(regular).setFontSize(9).setMarginTop(6).setMarginBottom(8));

        if (esp != null && esp.getSerieCargador() != null) {
            doc.add(new Paragraph("# SERIE DE CARGADOR: " + esp.getSerieCargador())
                .setFont(bold).setFontSize(9).setFontColor(UPCH_BLUE).setMarginBottom(10));
        }
    }

    private void cuerpoDevolucion(Document doc, MovimientoHistorial mov,
                                   ActivoMaestro activo, PdfFont bold, PdfFont regular) throws IOException {
        doc.add(new Paragraph("POR LA PRESENTE ACTA SE DEJA CONSTANCIA DE LA DEVOLUCIÓN DEL EQUIPO " +
            "TECNOLÓGICO, PROPIEDAD DE LA UNIVERSIDAD PERUANA CAYETANO HEREDIA")
            .setFont(regular).setFontSize(10).setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(14));

        Usuario entrega = mov.getUsuarioEntrega();
        tablaDatos(doc, new String[][]{
            {"NOMBRE QUE ENTREGA", entrega != null ? entrega.getNombre()        : "—"},
            {"DNI",                entrega != null ? entrega.getDni()           : "—"},
            {"CARGO",              entrega != null ? entrega.getPuestoGeneral() : "—"},
            {"ÁREA",               entrega != null ? entrega.getDepartamento()  : "—"},
            {"RECIBE",             mov.getTecnicoResponsable() != null ? mov.getTecnicoResponsable() : "—"},
            {"CARGO RECIBE",       "TÉCNICO INFORMÁTICO"},
        }, bold, regular);

        if (activo != null) tablaEquipoSimple(doc, activo, "ESTADO", "OPERATIVO", bold, regular);
    }

    private void cuerpoPrestamo(Document doc, MovimientoHistorial mov,
                                 ActivoMaestro activo, String centroCosto,
                                 PdfFont bold, PdfFont regular) throws IOException {
        doc.add(new Paragraph("POR LA PRESENTE ACTA SE DEJA CONSTANCIA DE LA ENTREGA DE EQUIPO INFORMÁTICO " +
            "EN CALIDAD DE PRÉSTAMO ASIGNADO A:")
            .setFont(regular).setFontSize(10).setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(14));

        Usuario recibe = mov.getUsuarioRecibe();
        tablaDatos(doc, new String[][]{
            {"NOMBRE QUE RECIBE", recibe != null ? recibe.getNombre()        : "—"},
            {"DNI",               recibe != null ? recibe.getDni()           : "—"},
            {"CARGO",             recibe != null ? recibe.getPuestoGeneral() : "—"},
            {"ÁREA",              recibe != null ? recibe.getDepartamento()  : "—"},
            {"CENTRO DE COSTO",   centroCosto != null ? centroCosto : "—"},
        }, bold, regular);

        if (activo != null) tablaEquipoSimple(doc, activo, "ACCESORIOS", "CON CARGADOR", bold, regular);
        doc.add(new Paragraph("NOTA: PRÉSTAMO TEMPORAL.").setFont(bold).setFontSize(9).setMarginTop(6));
    }

    private void cuerpoMovilizacion(Document doc, MovimientoHistorial mov,
                                     ActivoMaestro activo, PdfFont bold, PdfFont regular) throws IOException {
        LocalDateTime fecha = mov.getFechaMovimiento() != null ? mov.getFechaMovimiento() : LocalDateTime.now();
        doc.add(new Paragraph("SOLICITUD Nro: " + mov.getNroActa() +
            "     FECHA: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setFont(bold).setFontSize(10).setMarginBottom(8));
        doc.add(new Paragraph("PERSONA A COORDINAR: " + (mov.getTecnicoResponsable() != null ? mov.getTecnicoResponsable() : "—"))
            .setFont(regular).setFontSize(9).setMarginBottom(4));
        doc.add(new Paragraph("JUSTIFICACIÓN: CONFIGURACION-INSTALACION-MANTENIMIENTO")
            .setFont(regular).setFontSize(9).setMarginBottom(10));
        if (activo != null) tablaEquipoSimple(doc, activo, "CONDICIÓN", "OPERATIVO", bold, regular);
        doc.add(new Paragraph("ESTA MOVILIZACIÓN ES:   DEFINITIVA (X)     PROVISIONAL ( )")
            .setFont(bold).setFontSize(9).setMarginTop(8));
    }

    // ── FIRMAS ────────────────────────────────────────────────────────────────
    private void agregarFirmas(Document doc, MovimientoHistorial mov,
                                PdfFont bold, PdfFont regular, PdfFont italic) throws IOException {
        doc.add(new Paragraph("\n\n"));
        String tipo    = mov.getTipoMovimiento() != null ? mov.getTipoMovimiento() : "";
        String tecnico = mov.getTecnicoResponsable() != null ? mov.getTecnicoResponsable() : "MEDINA CARO JOSE JOEL";
        String recNombre = mov.getUsuarioRecibe()  != null ? mov.getUsuarioRecibe().getNombre()  : "—";
        String entNombre = mov.getUsuarioEntrega() != null ? mov.getUsuarioEntrega().getNombre() : "—";

        Table t = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginTop(24);

        if ("DEVOLUCION".equals(tipo)) {
            celdaFirma(t, entNombre,  "Devolución de equipo",      bold, regular, italic);
            celdaFirma(t, tecnico,    "Recepción del equipo",       bold, regular, italic);
            celdaFirma(t, "PRETELL ESPINOZA JOSE LUIS", "Jefe de Soporte Técnico – DTI", bold, regular, italic);
        } else if ("MOVILIZACION".equals(tipo)) {
            celdaFirma(t, tecnico,    "Solicitante",                bold, regular, italic);
            celdaFirma(t, "",         "",                           bold, regular, italic);
            celdaFirma(t, "PRETELL ESPINOZA JOSE LUIS", "Autorizado por", bold, regular, italic);
        } else {
            celdaFirma(t, tecnico,    "Técnico que entrega",        bold, regular, italic);
            celdaFirma(t, "PRETELL ESPINOZA JOSE LUIS", "Jefe de Soporte Técnico", bold, regular, italic);
            celdaFirma(t, recNombre,  "Beneficiario que recibe",    bold, regular, italic);
        }
        doc.add(t);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void seccionTitulo(Document doc, String titulo, PdfFont bold) throws IOException {
        doc.add(new Paragraph("\n" + titulo)
            .setFont(bold).setFontSize(10).setFontColor(UPCH_DARK)
            .setItalic().setUnderline().setMarginBottom(6));
    }

    private void tablaDatos(Document doc, String[][] filas, PdfFont bold, PdfFont regular) throws IOException {
        Table t = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(12);
        for (String[] fila : filas) {
            t.addCell(new Cell().add(new Paragraph(fila[0] + ":").setFont(bold).setFontSize(9))
                .setBorder(Border.NO_BORDER).setBackgroundColor(UPCH_LIGHT).setPadding(4));
            t.addCell(new Cell().add(new Paragraph(fila[1]).setFont(regular).setFontSize(9))
                .setBorder(Border.NO_BORDER).setPadding(4));
        }
        doc.add(t);
    }

    private void tablaEquipo(Document doc, ActivoMaestro a, EspecificacionComputo esp,
                              PdfFont bold, PdfFont regular) throws IOException {
        Table t = new Table(UnitValue.createPercentArray(new float[]{15,15,18,18,20,14}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        for (String h : new String[]{"EQUIPO","MARCA","MODELO","SERIE","INVENTARIO","USO"})
            celHdr(t, h, bold);
        for (String v : new String[]{
            n(a.getTipoEquipo()), n(a.getMarca()), n(a.getModelo()),
            n(a.getSerie()), n(a.getCodPatrimonioInventario()),
            a.getUsoActivo() != null ? a.getUsoActivo() : "ADMINISTRATIVO"})
            celDat(t, v, regular);
        doc.add(t);

        if (esp != null) {
            Table t2 = new Table(UnitValue.createPercentArray(new float[]{22,28,12,14,12,12}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);
            for (String h : new String[]{"HOSTNAME","PROCESADOR","RAM","SSD","GPU","GESTIÓN"})
                celHdr(t2, h, bold);
            for (String v : new String[]{
                n(esp.getHostname()), n(esp.getProcesador()), n(esp.getRam()),
                n(esp.getDiscoSsd()), esp.getGpu() != null ? esp.getGpu() : "—", a.getGestion() != null ? a.getGestion() : "ASIGNADO"})
                celDat(t2, v, regular);
            doc.add(t2);
        }
    }

    private void tablaEquipoSimple(Document doc, ActivoMaestro a,
                                    String lastHeader, String lastValue,
                                    PdfFont bold, PdfFont regular) throws IOException {
        Table t = new Table(UnitValue.createPercentArray(new float[]{15,15,18,18,20,14}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(12);
        for (String h : new String[]{"EQUIPO","MARCA","MODELO","SERIE","INVENTARIO", lastHeader})
            celHdr(t, h, bold);
        for (String v : new String[]{
            n(a.getTipoEquipo()), n(a.getMarca()), n(a.getModelo()),
            n(a.getSerie()), n(a.getCodPatrimonioInventario()), lastValue})
            celDat(t, v, regular);
        doc.add(t);
    }

    private void celHdr(Table t, String txt, PdfFont bold) {
        t.addCell(new Cell()
            .add(new Paragraph(txt).setFont(bold).setFontSize(8).setFontColor(new DeviceRgb(255,255,255)))
            .setBackgroundColor(UPCH_BLUE).setPadding(5).setTextAlignment(TextAlignment.CENTER));
    }

    private void celDat(Table t, String txt, PdfFont regular) {
        t.addCell(new Cell()
            .add(new Paragraph(txt).setFont(regular).setFontSize(8))
            .setPadding(5).setTextAlignment(TextAlignment.CENTER));
    }

    private void celdaFirma(Table t, String nombre, String rol,
                              PdfFont bold, PdfFont regular, PdfFont italic) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(4);
        c.add(new Paragraph(" ").setHeight(50));
        c.add(new Paragraph("_______________________________").setFont(regular).setFontSize(8).setFontColor(GRAY_TEXT));
        if (nombre != null && !nombre.isBlank())
            c.add(new Paragraph(nombre).setFont(bold).setFontSize(8).setFontColor(UPCH_DARK));
        if (rol != null && !rol.isBlank())
            c.add(new Paragraph(rol).setFont(italic).setFontSize(8).setFontColor(GRAY_TEXT));
        t.addCell(c);
    }

    private String n(String v) { return v != null ? v : "—"; }
}
