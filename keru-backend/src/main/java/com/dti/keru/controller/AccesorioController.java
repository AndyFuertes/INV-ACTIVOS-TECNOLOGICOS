package com.dti.keru.controller;

import com.dti.keru.entity.AccesorioCatalogo;
import com.dti.keru.repository.AccesorioCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accesorios")
@RequiredArgsConstructor
public class AccesorioController {

    private final AccesorioCatalogoRepository repo;

    /** GET /api/accesorios */
    @GetMapping
    public ResponseEntity<Page<AccesorioCatalogo>> listar(
            @RequestParam(defaultValue="0")  int page,
            @RequestParam(defaultValue="50") int size) {
        return ResponseEntity.ok(repo.findAllByOrderByNombreItemAsc(
            PageRequest.of(page, size, Sort.by("nombreItem"))));
    }

    /** GET /api/accesorios/stock-bajo */
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<AccesorioCatalogo>> stockBajo() {
        return ResponseEntity.ok(repo.findStockBajo());
    }

    /** POST /api/accesorios */
    @PostMapping
    public ResponseEntity<AccesorioCatalogo> crear(@RequestBody AccesorioCatalogo acc) {
        return ResponseEntity.ok(repo.save(acc));
    }

    /** PUT /api/accesorios/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Integer id,
                                     @RequestBody AccesorioCatalogo datos) {
        return repo.findById(id).map(a -> {
            if (datos.getNombreItem() != null) a.setNombreItem(datos.getNombreItem());
            if (datos.getMarca()      != null) a.setMarca(datos.getMarca());
            if (datos.getStockActual() != null) a.setStockActual(datos.getStockActual());
            if (datos.getStockMinimo() != null) a.setStockMinimo(datos.getStockMinimo());
            return ResponseEntity.ok(repo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** PATCH /api/accesorios/{id}/ajustar-stock?cantidad=5&operacion=entrada */
    @PatchMapping("/{id}/ajustar-stock")
    public ResponseEntity<?> ajustarStock(@PathVariable Integer id,
                                           @RequestParam int cantidad,
                                           @RequestParam String operacion) {
        return repo.findById(id).map(a -> {
            int nuevo = "entrada".equalsIgnoreCase(operacion)
                ? a.getStockActual() + cantidad
                : Math.max(0, a.getStockActual() - cantidad);
            a.setStockActual(nuevo);
            return ResponseEntity.ok(Map.of(
                "id", a.getIdAccesorio(),
                "nombre", a.getNombreItem(),
                "stockAnterior", a.getStockActual(),
                "stockNuevo", nuevo,
                "alerta", nuevo <= a.getStockMinimo()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/accesorios/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
