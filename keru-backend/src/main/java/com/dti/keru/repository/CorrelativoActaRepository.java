package com.dti.keru.repository;

import com.dti.keru.entity.CorrelativoActa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface CorrelativoActaRepository extends JpaRepository<CorrelativoActa, String> {

    Optional<CorrelativoActa> findByTipoActa(String tipoActa);

    @Modifying
    @Transactional
    @Query("UPDATE CorrelativoActa c SET c.ultimoNumero = c.ultimoNumero + 1 WHERE c.tipoActa = :tipo")
    void incrementar(@Param("tipo") String tipo);
}
