package com.dti.keru.service;

import com.dti.keru.entity.*;
import com.dti.keru.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cada método usa Propagation.REQUIRES_NEW para que un error en una fila
 * NO contamine ni revierta las filas anteriores/siguientes.
 *
 * Regla de usuarios:
 *   - Si el DNI ya existe en la tabla usuarios → reutilizar SIN MODIFICAR
 *   - Si no existe → crear entrada mínima solo con los datos del Excel
 *
 * Esto elimina el error "Transaction silently rolled back because it has been
 * marked as rollback-only" causado por violaciones de unicidad en DNI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelRowService {

    private final org.springframework.context.ApplicationContext applicationContext;

    private final UsuarioRepository               usuarioRepo;
    private final UbicacionRepository             ubicacionRepo;
    private final ActivoMaestroRepository         activoRepo;
    private final EspecificacionComputoRepository espRepo;

    private static final Set<String> TIPOS_COMPUTO = Set.of(
        "LAPTOP", "DESKTOP", "ALL IN ONE", "WORKSTATION",
        "LAPTOP WORKSTATION", "DESKTOP TINY", "OPS", "TABLET",
        "CELULAR", "PANTALLA INTERACTIVA"
    );

    // ── PLANILLA: insertar usuario si no existe ───────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String guardarUsuario(String dni, Row row, Map<String, Integer> cols) {
        // DNI ya existe → skip total, sin tocar nada
        if (usuarioRepo.existsById(dni)) return "SKIP";

        Usuario u = new Usuario();
        u.setDni(dni);
        u.setNombre(str(row, cols, "Apellidos y Nombres"));
        u.setPuestoGeneral(str(row, cols, "Puesto"));
        u.setDepartamento(str(row, cols, "Departamento"));
        u.setArea(str(row, cols, "Área"));
        u.setCentroCosto(str(row, cols, "Codigo Centro de Costo Oracle"));
        u.setNombreCentroCosto(str(row, cols, "Nombre Centro de Costo Oracle"));
        String activo = str(row, cols, "Activo? Al 31/01/2025");
        u.setEstadoPlanilla(activo != null && activo.equalsIgnoreCase("si") ? "si" : "no");
        usuarioRepo.save(u);
        return "INSERT";
    }

    // ── CÓMPUTO: guardar activo + especificaciones ────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String guardarComputo(String serie, Row row, Map<String, Integer> cols) {
        Ubicacion ub  = resolverUbicacion(row, cols);
        Usuario   usr = resolverUsuario(row, cols);

        boolean esNuevo = !activoRepo.existsBySerie(serie);
        ActivoMaestro a = activoRepo.findBySerie(serie).orElse(new ActivoMaestro());
        llenarActivo(a, serie, row, cols, ub, usr);
        ActivoMaestro saved = activoRepo.save(a);

        String tipoUp = saved.getTipoEquipo() != null ? saved.getTipoEquipo().toUpperCase() : "";
        if (TIPOS_COMPUTO.contains(tipoUp)) {
            // Try by id first, then by serie (covers records from old buggy imports)
            EspecificacionComputo esp = espRepo
                .findByActivo_IdActivo(saved.getIdActivo())
                .orElseGet(() -> espRepo.findByActivo_Serie(saved.getSerie())
                    .orElse(new EspecificacionComputo()));
            esp.setActivo(saved);
            String hn = str(row, cols, "HOSTNAME");
            if (hn != null) hn = hn.trim();
            esp.setHostname(hn);
            esp.setProcesador(str(row, cols, "PROCESADOR"));
            esp.setRam(str(row, cols, "RAM"));
            String ssd = str(row, cols, "SSD");
            String hdd = str(row, cols, "HDD");
            esp.setDiscoSsd(ssd != null && !ssd.isBlank() ? ssd : hdd);
            esp.setSistemaOperativo(str(row, cols, "SISTEMA OPERATIVO"));
            esp.setSerieCargador(str(row, cols, "SERIE-CARGADOR"));
            String anydesk = str(row, cols, "N° DE ANYDESK");
            if (anydesk == null) anydesk = str(row, cols, "Nª ANYDESK ");
            esp.setNroAnydesk(anydesk);
            String gpuVal = str(row, cols, "GPU");
            if (gpuVal == null) gpuVal = str(row, cols, "TARJETA GRAFICA");
            if (gpuVal != null && !gpuVal.isBlank()) esp.setGpu(gpuVal);
            esp.setTieneFortiedr(hasVal(str(row, cols, "FORTIEDR")));
            esp.setTieneForticloud(hasVal(str(row, cols, "FORTICLOUD")));
            esp.setTieneInvgate(hasVal(str(row, cols, "INVGATE")));
            espRepo.save(esp);
        }
        return esNuevo ? "INSERT" : "UPDATE";
    }

    // ── PERIFÉRICO: guardar activo ────────────────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String guardarPeriferico(String serie, Row row, Map<String, Integer> cols) {
        Ubicacion ub  = resolverUbicacion(row, cols);
        Usuario   usr = resolverUsuario(row, cols);

        boolean esNuevo = !activoRepo.existsBySerie(serie);
        ActivoMaestro a = activoRepo.findBySerie(serie).orElse(new ActivoMaestro());
        llenarActivo(a, serie, row, cols, ub, usr);
        activoRepo.save(a);
        return esNuevo ? "INSERT" : "UPDATE";
    }

    // ── Llenado común de ActivoMaestro ────────────────────────────────────────
    private void llenarActivo(ActivoMaestro a, String serie, Row row,
                               Map<String, Integer> cols, Ubicacion ub, Usuario usr) {
        a.setSerie(serie);
        a.setTipoEquipo(up(str(row, cols, "TIPO DE EQUIPO")));
        a.setMarca(str(row, cols, "MARCA"));
        a.setModelo(str(row, cols, "MODELO"));
        a.setCodPatrimonioInventario(str(row, cols, "INVENTARIO"));
        a.setOrdenCompra(str(row, cols, "ORDEN DE COMPRA - PATRIMONIO"));
        String fin = str(row, cols, "FINANCIAMIENTO");
        if (fin == null || fin.isBlank()) fin = str(row, cols, "OCS");
        a.setFinanciamiento(fin);
        a.setAulaLabAud(str(row, cols, "AULA/LAB/AUD"));
        String uso2 = str(row, cols, "USO_2");
        String uso1 = str(row, cols, "USO");
        a.setUsoActivo(uso2 != null && !uso2.isBlank() ? uso2 : uso1);
        a.setGestion(up(str(row, cols, "GESTION")));
        a.setEstadoFisico(up(str(row, cols, "ESTADO")));
        a.setEstadoDisponibilidad(up(str(row, cols, "DISPONIBILIDAD")));
        a.setEnUso("EN USO".equalsIgnoreCase(str(row, cols, "DISPONIBILIDAD")));
        a.setObservacion(str(row, cols, "OBSERVACION"));
        String tecnico = str(row, cols, "NOMBRE DE TECNICO");
        if (tecnico == null) tecnico = str(row, cols, "NOMBRE DEL TECNICO");
        a.setTecnicoRegistro(tecnico);
        a.setUbicacion(ub);
        a.setResponsable(usr);

        // Modelo comercial (columna "MODELO COMERCIAL" si existe en el Excel)
        String modCom = str(row, cols, "MODELO COMERCIAL");
        if (modCom == null) modCom = str(row, cols, "NOMBRE COMERCIAL");
        if (modCom != null && !modCom.isBlank()) a.setModeloComercial(modCom);

        // Es nuevo: si CONDICION = "NUEVO" o columna "ES NUEVO" = "si"
        String condicion = up(str(row, cols, "CONDICION"));
        String esNuevoCol = str(row, cols, "ES NUEVO");
        boolean esNuevo = "NUEVO".equals(condicion)
            || (esNuevoCol != null && esNuevoCol.equalsIgnoreCase("si"));
        a.setEsNuevo(esNuevo);

        String anoStr = str(row, cols, "AÑO");
        if (anoStr != null && !anoStr.isBlank()) {
            try { a.setAnoCompra(Integer.parseInt(anoStr.replaceAll("\\.0$", "").trim())); }
            catch (NumberFormatException ignored) {}
        }
    }

    // ── Ubicación: buscar existente o crear nueva ─────────────────────────────
    private Ubicacion resolverUbicacion(Row row, Map<String, Integer> cols) {
        String campus = str(row, cols, "CAMPUS");
        if (campus == null || campus.isBlank()) return null;

        String pabellon = nvl(str(row, cols, "PABELLON"));
        String piso     = nvl(str(row, cols, "PISO"));
        String unidad   = nvl(str(row, cols, "UNIDAD"));
        // AREA es parte de la clave — sin esto equipos de la misma unidad
        // pero distinta área quedan mal asociados
        String area     = nvl(str(row, cols, "AREA"));

        List<Ubicacion> existentes = ubicacionRepo
            .findByCampusCombinacion(campus, pabellon, piso, unidad, area);
        if (!existentes.isEmpty()) return existentes.get(0);

        Ubicacion u = new Ubicacion();
        u.setCampus(campus);
        u.setPabellon(pabellon.isEmpty() ? null : pabellon);
        u.setPiso(piso.isEmpty() ? null : piso);
        u.setUnidad(unidad.isEmpty() ? null : unidad);
        u.setArea(area.isEmpty() ? null : area);
        u.setLugarFisico(str(row, cols, "UBICACIÓN FISICA"));
        return ubicacionRepo.save(u);
    }

    // ── Usuario: reutilizar si existe, crear mínimo si no ────────────────────
    private Usuario resolverUsuario(Row row, Map<String, Integer> cols) {
        String dniStr = str(row, cols, "DNI");
        if (dniStr == null || dniStr.isBlank()) return null;

        // Limpiar el ".0" que genera Excel con números
        String dni = dniStr.replaceAll("\\.0$", "").trim();
        if (dni.isBlank()) return null;

        // Si ya existe (de planilla o de importación previa) → NO modificar
        Optional<Usuario> existente = usuarioRepo.findById(dni);
        if (existente.isPresent()) return existente.get();

        // No existe → crear entrada mínima
        try {
            Usuario u = new Usuario();
            u.setDni(dni);
            String nombre = str(row, cols, "USUARIO");
            u.setNombre(nombre != null && !nombre.isBlank() ? nombre : "Sin nombre " + dni);
            u.setPuestoGeneral(str(row, cols, "CARGO1"));
            u.setEstadoPlanilla("si");
            return usuarioRepo.save(u);
        } catch (DataIntegrityViolationException e) {
            // Race condition: se insertó entre el findById y el save → buscar de nuevo
            log.warn("Race condition en DNI {}, reintentando findById", dni);
            return usuarioRepo.findById(dni).orElse(null);
        }
    }

    // ── Utilidades de lectura de celda ────────────────────────────────────────
    String str(Row row, Map<String, Integer> cols, String colName) {
        Integer idx = cols.get(colName);
        if (idx == null || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> null;
        };
    }

    private String up(String v)    { return v != null ? v.trim().toUpperCase() : null; }
    private String nvl(String v)   { return v != null ? v : ""; }
    private boolean hasVal(String v) { return v != null && !v.isBlank() && !v.equals("0"); }

// ── HISTORIAL: crear MovimientoHistorial desde fila del Excel ────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String guardarMovimientoHistorial(String nroActa, String serie,
                                              Row row, Map<String, Integer> cols) {
        com.dti.keru.repository.MovimientoHistorialRepository movRepo =
            applicationContext.getBean(com.dti.keru.repository.MovimientoHistorialRepository.class);

        // Ya existe este nro de acta + serie → skip
        if (movRepo.existsByNroActaAndSerieEquipo(nroActa, serie)) return "SKIP";

        com.dti.keru.entity.MovimientoHistorial mov = new com.dti.keru.entity.MovimientoHistorial();
        mov.setNroActa(nroActa);
        mov.setSerieEquipo(serie);

        // Tipo de movimiento — normalizar variantes del Excel
        String tipoRaw = up(str(row, cols, " TIPO DE ACTA"));
        if (tipoRaw == null) tipoRaw = up(str(row, cols, "TIPO DE ACTA"));
        mov.setTipoMovimiento(normalizarTipoActa(tipoRaw));
        mov.setTipoActa(mov.getTipoMovimiento());

        // Centro de costos
        mov.setCentroCostoMomento(str(row, cols, "CENTRO DE COSTOS"));

        // Técnico responsable
        mov.setTecnicoResponsable(str(row, cols, "NOMBRE DEL TECNICO"));

        // Fecha de entrega (Excel serial date → LocalDateTime)
        String fechaStr = str(row, cols, "FECHA DE ENTREGA");
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                long serial = (long) Double.parseDouble(fechaStr);
                java.time.LocalDate ld = java.time.LocalDate.of(1899, 12, 30)
                    .plusDays(serial);
                mov.setFechaMovimiento(ld.atStartOfDay());
            } catch (Exception ignored) {}
        }
        if (mov.getFechaMovimiento() == null) {
            mov.setFechaMovimiento(java.time.LocalDateTime.now());
        }

        // Usuario que recibe (por DNI o nombre del Excel)
        String dniStr = str(row, cols, "DNI");
        if (dniStr != null) {
            String dni = dniStr.replaceAll("\\.0$","").trim();
            usuarioRepo.findById(dni).ifPresent(mov::setUsuarioRecibe);
        }

        movRepo.save(mov);
        return "INSERT";
    }

    private String normalizarTipoActa(String raw) {
        if (raw == null) return "ASIGNACION";
        String u = raw.trim().toUpperCase();
        if (u.contains("DEVOL"))       return "DEVOLUCION";
        if (u.contains("PREST"))       return "PRESTAMO";
        if (u.contains("MOVIL") || u.contains("DEPOSITO")) return "MOVILIZACION";
        if (u.contains("BAJA"))        return "BAJA";
        return "ASIGNACION";
    }
}