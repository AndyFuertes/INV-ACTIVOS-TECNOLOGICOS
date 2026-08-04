package com.dti.keru.repository;

import com.dti.keru.entity.MovimientoHistorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimientoHistorialRepository extends JpaRepository<MovimientoHistorial, Integer> {
    Page<MovimientoHistorial> findAllByOrderByFechaMovimientoDesc(Pageable pageable);
    List<MovimientoHistorial> findTop10ByOrderByFechaMovimientoDesc();
    /** Historial de un equipo específico por su serie */
    @Query("SELECT m FROM MovimientoHistorial m " +
           "LEFT JOIN FETCH m.usuarioRecibe " +
           "LEFT JOIN FETCH m.usuarioEntrega " +
           "WHERE m.serieEquipo = :serie " +
           "ORDER BY m.fechaMovimiento DESC")
    List<MovimientoHistorial> findTop50BySerieEquipoOrderByFechaMovimientoDesc(
        @Param("serie") String serie);
    boolean existsByNroActaAndSerieEquipo(String nroActa, String serieEquipo);
}
