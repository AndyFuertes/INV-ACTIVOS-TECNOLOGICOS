package com.dti.keru.repository;

import com.dti.keru.entity.EspecificacionComputo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EspecificacionComputoRepository extends JpaRepository<EspecificacionComputo, Integer> {
    Optional<EspecificacionComputo> findByActivo_IdActivo(Integer idActivo);
    Optional<EspecificacionComputo> findByActivo_Serie(String serie);
    long countByTieneInvgate(Boolean tieneInvgate);
}
