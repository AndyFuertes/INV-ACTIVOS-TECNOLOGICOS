package com.dti.keru.repository;

import com.dti.keru.entity.ActivoMaestro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivoMaestroRepository extends JpaRepository<ActivoMaestro, Integer> {

    Optional<ActivoMaestro> findBySerie(String serie);
    boolean existsBySerie(String serie);

    // ── Búsqueda principal con JOIN FETCH para evitar N+1 ────────────────────
    // JOIN FETCH trae ubicacion + responsable en UNA sola query en vez de N queries
    // EntityGraph loads ubicacion+responsable in a single IN query after pagination
    // This avoids both N+1 AND the Hibernate in-memory pagination bug
    @EntityGraph(attributePaths = {"ubicacion", "responsable"})
    @Query("SELECT a FROM ActivoMaestro a " +
           "LEFT JOIN a.ubicacion u " +
           "LEFT JOIN a.responsable r " +
           "WHERE (:tipos IS NULL OR UPPER(a.tipoEquipo) IN :tipos) " +
           "AND (:estado IS NULL OR UPPER(a.estadoFisico) = UPPER(:estado)) " +
           "AND (:gestion IS NULL OR UPPER(a.gestion) = UPPER(:gestion)) " +
           "AND (:campus IS NULL OR UPPER(u.campus) LIKE UPPER(CONCAT('%',:campus,'%'))) " +
           "AND (:includeBaja = true OR (UPPER(COALESCE(a.gestion,'')) != 'BAJA' " +
           "     AND UPPER(COALESCE(a.estadoFisico,'')) != 'BAJA')) " +
           "AND (:search IS NULL " +
           "     OR UPPER(a.serie) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(a.codPatrimonioInventario,'')) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(r.nombre,'')) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(r.dni,'')) LIKE UPPER(CONCAT('%',:search,'%')))")
    Page<ActivoMaestro> buscarConFiltros(
        @Param("tipos")       List<String> tipos,
        @Param("estado")      String estado,
        @Param("gestion")     String gestion,
        @Param("campus")      String campus,
        @Param("search")      String search,
        @Param("includeBaja") boolean includeBaja,
        Pageable pageable
    );

    // Misma query para COUNT (sin FETCH para que sea más rápida)
    @Query("SELECT COUNT(a) FROM ActivoMaestro a " +
           "LEFT JOIN a.ubicacion u LEFT JOIN a.responsable r " +
           "WHERE (:tipos IS NULL OR UPPER(a.tipoEquipo) IN :tipos) " +
           "AND (:estado IS NULL OR UPPER(a.estadoFisico) = UPPER(:estado)) " +
           "AND (:gestion IS NULL OR UPPER(a.gestion) = UPPER(:gestion)) " +
           "AND (:campus IS NULL OR UPPER(u.campus) LIKE UPPER(CONCAT('%',:campus,'%'))) " +
           "AND (:includeBaja = true OR (UPPER(COALESCE(a.gestion,'')) != 'BAJA' " +
           "     AND UPPER(COALESCE(a.estadoFisico,'')) != 'BAJA')) " +
           "AND (:search IS NULL " +
           "     OR UPPER(a.serie) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(a.codPatrimonioInventario,'')) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(r.nombre,'')) LIKE UPPER(CONCAT('%',:search,'%')) " +
           "     OR UPPER(COALESCE(r.dni,'')) LIKE UPPER(CONCAT('%',:search,'%')))")
    long countConFiltros(
        @Param("tipos")       List<String> tipos,
        @Param("estado")      String estado,
        @Param("gestion")     String gestion,
        @Param("campus")      String campus,
        @Param("search")      String search,
        @Param("includeBaja") boolean includeBaja
    );

    // Bajas con FETCH
    @EntityGraph(attributePaths = {"ubicacion", "responsable"})
    @Query("SELECT a FROM ActivoMaestro a " +
           "WHERE (:tipos IS NULL OR UPPER(a.tipoEquipo) IN :tipos) " +
           "AND (UPPER(COALESCE(a.gestion,'')) = 'BAJA' OR UPPER(COALESCE(a.estadoFisico,'')) = 'BAJA')")
    Page<ActivoMaestro> findBajas(@Param("tipos") List<String> tipos, Pageable pageable);

    // ── Dashboard: UN SOLO query para todos los conteos ──────────────────────
    @Query("SELECT a.tipoEquipo, COUNT(a) FROM ActivoMaestro a " +
           "WHERE UPPER(COALESCE(a.gestion,'')) != 'BAJA' " +
           "AND UPPER(COALESCE(a.estadoFisico,'')) != 'BAJA' " +
           "GROUP BY a.tipoEquipo")
    List<Object[]> countPorTipo();

    @Query("SELECT a.estadoDisponibilidad, COUNT(a) FROM ActivoMaestro a " +
           "WHERE UPPER(COALESCE(a.gestion,'')) != 'BAJA' " +
           "GROUP BY a.estadoDisponibilidad")
    List<Object[]> countPorDisponibilidad();

    @Query("SELECT COUNT(a) FROM ActivoMaestro a " +
           "WHERE UPPER(COALESCE(a.gestion,'')) = 'BAJA' " +
           "OR UPPER(COALESCE(a.estadoFisico,'')) = 'BAJA'")
    long countBajas();

    @Query("SELECT COUNT(a) FROM ActivoMaestro a " +
           "WHERE a.esNuevo = true " +
           "AND UPPER(COALESCE(a.gestion,'')) != 'BAJA'")
    long countNuevos();

    @Query("SELECT DISTINCT u.campus FROM ActivoMaestro a " +
           "JOIN a.ubicacion u WHERE u.campus IS NOT NULL ORDER BY u.campus")
    List<String> findAllCampus();

    // Detalle único con FETCH
    @Query("SELECT a FROM ActivoMaestro a " +
           "LEFT JOIN FETCH a.ubicacion " +
           "LEFT JOIN FETCH a.responsable " +
           "WHERE a.idActivo = :id")
    Optional<ActivoMaestro> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT a FROM ActivoMaestro a " +
           "LEFT JOIN FETCH a.ubicacion " +
           "LEFT JOIN FETCH a.responsable " +
           "WHERE a.serie = :serie")
    Optional<ActivoMaestro> findBySerieWithDetails(@Param("serie") String serie);
}
