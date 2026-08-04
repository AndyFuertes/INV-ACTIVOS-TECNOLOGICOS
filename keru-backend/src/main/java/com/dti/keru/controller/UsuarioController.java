package com.dti.keru.controller;

import com.dti.keru.entity.Usuario;
import com.dti.keru.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepo;

    /** GET /api/usuarios?search=...&departamento=...&page=0&size=50 */
    @GetMapping
    public ResponseEntity<Page<Usuario>> listar(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String departamento,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(usuarioRepo.buscar(
            nn(search), nn(departamento),
            PageRequest.of(page, size, Sort.by("nombre"))
        ));
    }

    /** GET /api/usuarios/{dni} - usado para autocompletar en formulario de acta */
    @GetMapping("/{dni}")
    public ResponseEntity<Usuario> getByDni(@PathVariable String dni) {
        return usuarioRepo.findById(dni)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/usuarios/departamentos */
    @GetMapping("/departamentos")
    public ResponseEntity<List<String>> getDepartamentos() {
        return ResponseEntity.ok(usuarioRepo.findAllDepartamentos());
    }

    private String nn(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
