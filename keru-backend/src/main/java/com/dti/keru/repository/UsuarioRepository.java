package com.dti.keru.repository;

import com.dti.keru.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    @Query("SELECT u FROM Usuario u WHERE " +
           "(:search IS NULL OR UPPER(u.nombre) LIKE UPPER(CONCAT('%',:search,'%')) OR u.dni LIKE CONCAT('%',:search,'%')) AND " +
           "(:departamento IS NULL OR u.departamento = :departamento)")
    Page<Usuario> buscar(
        @Param("search") String search,
        @Param("departamento") String departamento,
        Pageable pageable
    );

    @Query("SELECT DISTINCT u.departamento FROM Usuario u WHERE u.departamento IS NOT NULL ORDER BY u.departamento")
    List<String> findAllDepartamentos();

    long countByEstadoPlanilla(String estado);
}
