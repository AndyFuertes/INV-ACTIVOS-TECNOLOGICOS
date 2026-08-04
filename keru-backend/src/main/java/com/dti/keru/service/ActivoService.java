package com.dti.keru.service;

import com.dti.keru.entity.ActivoMaestro;
import com.dti.keru.entity.EspecificacionComputo;
import com.dti.keru.entity.Ubicacion;
import com.dti.keru.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivoService {

    private final ActivoMaestroRepository       repo;
    private final EspecificacionComputoRepository espRepo;
    private final UsuarioRepository              usuarioRepo;
    private final UbicacionRepository            ubicacionRepo;

    @Value("${keru.storage.actas-pdf}")
    private String rutaStorage;

    // ── Grupos de tipos ──────────────────────────────────────────────────────
    public static final List<String> TIPOS_LAPTOP    = List.of("LAPTOP");
    public static final List<String> TIPOS_DESKTOP   = List.of("DESKTOP","DESKTOP TINY");
    public static final List<String> TIPOS_AIO       = List.of("ALL IN ONE");
    public static final List<String> TIPOS_WORKSTATION = List.of("WORKSTATION","LAPTOP WORKSTATION");
    public static final List<String> TIPOS_OTROS_C   = List.of("OPS","TABLET","CELULAR","PANTALLA INTERACTIVA","DESKTOP TINY");
    public static final List<String> TIPOS_MONITOR   = List.of("MONITOR");
    public static final List<String> TIPOS_IMPRESORA = List.of("IMPRESORA","FOTOCOPIADORA");
    public static final List<String> TIPOS_PROYECTOR = List.of("PROYECTOR");
    public static final List<String> TIPOS_OTROS_P   = List.of("TELEFONO IP","SCANNER","SCANER","PARLANTE",
            "LECTOR DE CODIGO DE BARRAS","TERMINAL PORTATIL PDT","LECTORA OPTICO","TELEVISOR");

    // ── Búsqueda paginada ────────────────────────────────────────────────────
    public Page<ActivoMaestro> buscar(List<String> tipos, String estado, String gestion,
                                       String campus, String search,
                                       boolean includeBaja, int page, int size) {
        List<String> tiposUpper = tipos != null ? tipos.stream().map(String::toUpperCase).toList() : null;
        return repo.buscarConFiltros(tiposUpper, estado, gestion, campus, search, includeBaja,
            PageRequest.of(page, size, Sort.by("serie")));
    }

    public Page<ActivoMaestro> getBajas(List<String> tipos, int page, int size) {
        List<String> tiposUpper = tipos != null ? tipos.stream().map(String::toUpperCase).toList() : null;
        return repo.findBajas(tiposUpper, PageRequest.of(page, size, Sort.by("serie")));
    }

    public Optional<ActivoMaestro> findBySerie(String serie) { return repo.findBySerieWithDetails(serie); }
    public Optional<ActivoMaestro> findById(Integer id)       { return repo.findByIdWithDetails(id); }
    public List<String> getCampuses()                          { return repo.findAllCampus(); }

    // ── Edición completa de un activo ────────────────────────────────────────
    @Transactional
    public ActivoMaestro editarActivo(Integer idActivo, Map<String, String> c) {
        ActivoMaestro a = repo.findById(idActivo)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado: " + idActivo));

        // ── Datos maestros ────────────────────────────────────────────────
        if (has(c, "tipoEquipo"))              a.setTipoEquipo(c.get("tipoEquipo"));
        if (has(c, "modeloComercial"))          a.setModeloComercial(c.get("modeloComercial"));
        if (c.containsKey("esNuevo"))           a.setEsNuevo(Boolean.parseBoolean(c.get("esNuevo")));
        if (has(c, "marca"))                   a.setMarca(c.get("marca"));
        if (has(c, "modelo"))                  a.setModelo(c.get("modelo"));
        if (has(c, "serie"))                   a.setSerie(c.get("serie"));
        if (has(c, "codPatrimonioInventario")) a.setCodPatrimonioInventario(c.get("codPatrimonioInventario"));
        if (has(c, "ordenCompra"))             a.setOrdenCompra(c.get("ordenCompra"));
        if (has(c, "financiamiento"))          a.setFinanciamiento(c.get("financiamiento"));
        if (has(c, "aulaLabAud"))              a.setAulaLabAud(c.get("aulaLabAud"));
        if (has(c, "usoActivo"))               a.setUsoActivo(c.get("usoActivo"));
        if (has(c, "observacion"))             a.setObservacion(c.get("observacion"));
        if (has(c, "tecnicoRegistro"))         a.setTecnicoRegistro(c.get("tecnicoRegistro"));

        if (has(c, "anoCompra")) {
            try { a.setAnoCompra(Integer.parseInt(c.get("anoCompra"))); } catch (NumberFormatException ignored) {}
        }

        // ── Estado / gestión ──────────────────────────────────────────────
        if (has(c, "estadoFisico"))            a.setEstadoFisico(c.get("estadoFisico").toUpperCase());
        if (has(c, "estadoDisponibilidad"))    a.setEstadoDisponibilidad(c.get("estadoDisponibilidad").toUpperCase());
        if (has(c, "gestion"))                 a.setGestion(c.get("gestion").toUpperCase());
        if (c.containsKey("enUso"))            a.setEnUso(Boolean.parseBoolean(c.get("enUso")));

        // ── Responsable (reasignación por DNI) ────────────────────────────
        if (c.containsKey("dniResponsable")) {
            String dni = c.get("dniResponsable");
            if (dni == null || dni.isBlank()) {
                a.setResponsable(null);
            } else {
                usuarioRepo.findById(dni.trim()).ifPresent(a::setResponsable);
            }
        }

        // ── Ubicación ─────────────────────────────────────────────────────
        boolean tieneUbi = c.containsKey("campus") || c.containsKey("pabellon") ||
                           c.containsKey("piso")   || c.containsKey("unidad")   ||
                           c.containsKey("area")   || c.containsKey("lugarFisico");
        if (tieneUbi) {
            Ubicacion ub = a.getUbicacion() != null ? a.getUbicacion() : new Ubicacion();
            if (has(c, "campus"))      ub.setCampus(c.get("campus"));
            if (has(c, "pabellon"))    ub.setPabellon(c.get("pabellon"));
            if (has(c, "piso"))        ub.setPiso(c.get("piso"));
            if (has(c, "unidad"))      ub.setUnidad(c.get("unidad"));
            if (has(c, "area"))        ub.setArea(c.get("area"));
            if (has(c, "lugarFisico")) ub.setLugarFisico(c.get("lugarFisico"));
            Ubicacion savedUb = ubicacionRepo.save(ub);
            a.setUbicacion(savedUb);
        }

        // ── Especificaciones de cómputo ───────────────────────────────────
        boolean tieneEsp = c.containsKey("hostname")          || c.containsKey("procesador") ||
                           c.containsKey("ram")               || c.containsKey("discoSsd")   ||
                           c.containsKey("sistemaOperativo")  || c.containsKey("serieCargador") ||
                           c.containsKey("nroAnydesk")        || c.containsKey("tieneFortiedr") ||
                           c.containsKey("tieneForticloud")   || c.containsKey("tieneInvgate");
        if (tieneEsp) {
            EspecificacionComputo esp = espRepo.findByActivo_IdActivo(idActivo)
                .orElseGet(() -> { EspecificacionComputo e = new EspecificacionComputo(); e.setActivo(a); return e; });
            if (has(c, "hostname"))         esp.setHostname(c.get("hostname"));
            if (has(c, "procesador"))       esp.setProcesador(c.get("procesador"));
            if (has(c, "ram"))              esp.setRam(c.get("ram"));
            if (has(c, "discoSsd"))         esp.setDiscoSsd(c.get("discoSsd"));
            if (has(c, "sistemaOperativo")) esp.setSistemaOperativo(c.get("sistemaOperativo"));
            if (has(c, "serieCargador"))    esp.setSerieCargador(c.get("serieCargador"));
            if (has(c, "nroAnydesk"))       esp.setNroAnydesk(c.get("nroAnydesk"));
            if (has(c, "gpu"))               esp.setGpu(c.get("gpu"));
            if (c.containsKey("tieneFortiedr"))  esp.setTieneFortiedr(Boolean.parseBoolean(c.get("tieneFortiedr")));
            if (c.containsKey("tieneForticloud")) esp.setTieneForticloud(Boolean.parseBoolean(c.get("tieneForticloud")));
            if (c.containsKey("tieneInvgate"))    esp.setTieneInvgate(Boolean.parseBoolean(c.get("tieneInvgate")));
            espRepo.save(esp);
        }

        return repo.save(a);
    }

    // ── Subir PDF al equipo ───────────────────────────────────────────────────
    @Transactional
    public String subirPdfEquipo(Integer idActivo, MultipartFile file) throws IOException {
        ActivoMaestro a = repo.findById(idActivo)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado: " + idActivo));

        String nombre = "equipo_" + idActivo + "_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

        Path dir = Paths.get(rutaStorage, "equipos-pdf");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);

        a.setRutaPdfEquipo(nombre);
        repo.save(a);
        log.info("PDF subido al activo {}: {}", idActivo, nombre);
        return nombre;
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private boolean has(Map<String, String> m, String key) {
        return m.containsKey(key) && m.get(key) != null && !m.get(key).isBlank();
    }
}
