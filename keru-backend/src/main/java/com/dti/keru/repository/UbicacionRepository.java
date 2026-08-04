package com.dti.keru.repository;

import com.dti.keru.entity.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, Integer> {

    // Devuelve List para evitar NonUniqueResultException — tomamos el primero
    @Query("SELECT u FROM Ubicacion u WHERE " +
           "u.campus = :campus AND " +
           "COALESCE(u.pabellon,'') = COALESCE(:pabellon,'') AND " +
           "COALESCE(u.piso,'') = COALESCE(:piso,'') AND " +
           "COALESCE(u.unidad,'') = COALESCE(:unidad,'') AND " +
           "COALESCE(u.area,'') = COALESCE(:area,'')")
    List<Ubicacion> findByCampusCombinacion(
        @Param("campus") String campus,
        @Param("pabellon") String pabellon,
        @Param("piso") String piso,
        @Param("unidad") String unidad,
        @Param("area") String area
    );
}
