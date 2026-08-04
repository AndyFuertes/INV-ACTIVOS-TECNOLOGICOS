package com.dti.keru.service;

import com.dti.keru.entity.CorrelativoActa;
import com.dti.keru.entity.MovimientoHistorial;
import com.dti.keru.entity.Usuario;
import com.dti.keru.repository.CorrelativoActaRepository;
import com.dti.keru.repository.MovimientoHistorialRepository;
import com.dti.keru.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActaService {

    private final MovimientoHistorialRepository movimientoRepo;
    private final CorrelativoActaRepository correlativoRepo;
    private final UsuarioRepository usuarioRepo;

    // Prefijos según modelo real de actas UPCH
    private static final Map<String, String> PREFIJOS = Map.of(
        "ASIGNACION",   "DTI-LAP-Nro.",
        "DEVOLUCION",   "DTI-DEV-LAP. Nro.",
        "PRESTAMO",     "DTI-PREST-",
        "MOVILIZACION", "ST-"
    );

    @Transactional
    public MovimientoHistorial crearMovimiento(
            String tipoMovimiento,
            String dniEntrega,
            String dniRecibe,
            String centroCosto,
            String tecnico,
            String serieEquipo
    ) {
        int anio = LocalDateTime.now().getYear();
        String tipoActa = tipoMovimiento.toUpperCase();

        // Obtener o crear correlativo
        CorrelativoActa correlativo = correlativoRepo.findByTipoActa(tipoActa)
            .orElseGet(() -> {
                CorrelativoActa c = new CorrelativoActa();
                c.setTipoActa(tipoActa);
                c.setUltimoNumero(0);
                c.setAno(anio);
                return correlativoRepo.save(c);
            });

        // Resetear si cambió el año
        if (correlativo.getAno() == null || !correlativo.getAno().equals(anio)) {
            correlativo.setUltimoNumero(0);
            correlativo.setAno(anio);
        }

        correlativo.setUltimoNumero(correlativo.getUltimoNumero() + 1);
        correlativoRepo.save(correlativo);

        String nroFormateado = String.format("%04d", correlativo.getUltimoNumero());
        String prefijo = PREFIJOS.getOrDefault(tipoActa, "DTI-");
        String nroActa = prefijo + " " + nroFormateado + "-" + anio;

        // Usuarios
        Usuario usuarioEntrega = dniEntrega != null ? usuarioRepo.findById(dniEntrega).orElse(null) : null;
        Usuario usuarioRecibe  = dniRecibe  != null ? usuarioRepo.findById(dniRecibe).orElse(null)  : null;

        MovimientoHistorial mov = new MovimientoHistorial();
        mov.setTipoMovimiento(tipoActa);
        mov.setTipoActa(tipoActa);
        mov.setNroActa(nroActa);
        mov.setUsuarioEntrega(usuarioEntrega);
        mov.setUsuarioRecibe(usuarioRecibe);
        mov.setCentroCostoMomento(centroCosto);
        mov.setTecnicoResponsable(tecnico);
        mov.setFechaMovimiento(LocalDateTime.now());
        mov.setSerieEquipo(serieEquipo);  // vincula historial al equipo

        return movimientoRepo.save(mov);
    }

    public Page<MovimientoHistorial> listar(int page, int size) {
        return movimientoRepo.findAllByOrderByFechaMovimientoDesc(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaMovimiento"))
        );
    }

    public String siguienteNroActa(String tipoMovimiento) {
        int anio = LocalDateTime.now().getYear();
        String tipoActa = tipoMovimiento.toUpperCase();
        int siguiente = correlativoRepo.findByTipoActa(tipoActa)
            .map(c -> c.getUltimoNumero() + 1)
            .orElse(1);
        String prefijo = PREFIJOS.getOrDefault(tipoActa, "DTI-");
        return prefijo + " " + String.format("%04d", siguiente) + "-" + anio;
    }
}
