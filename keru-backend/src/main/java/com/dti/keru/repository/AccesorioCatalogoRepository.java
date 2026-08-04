package com.dti.keru.repository;

import com.dti.keru.entity.AccesorioCatalogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccesorioCatalogoRepository extends JpaRepository<AccesorioCatalogo, Integer> {

    Page<AccesorioCatalogo> findAllByOrderByNombreItemAsc(Pageable pageable);

    @Query("SELECT a FROM AccesorioCatalogo a WHERE a.stockActual <= a.stockMinimo ORDER BY a.stockActual ASC")
    List<AccesorioCatalogo> findStockBajo();
}
