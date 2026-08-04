package com.dti.keru.controller;

import com.dti.keru.service.InvGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/invgate")
@RequiredArgsConstructor
@Slf4j
public class InvGateController {

    private final InvGateService invGateService;

    /** GET /api/invgate/estado — contadores actuales desde BD */
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> getEstado() {
        return ResponseEntity.ok(invGateService.getEstadoActual());
    }

    /** POST /api/invgate/sincronizar — consulta InvGate y actualiza BD */
    @PostMapping("/sincronizar")
    public ResponseEntity<Map<String, Object>> sincronizar() {
        log.info("Iniciando sincronización InvGate");
        InvGateService.SyncResult result = invGateService.sincronizar();
        return ResponseEntity.ok(Map.of(
            "ok",             result.errores == 0,
            "totalInvgate",   result.totalInvgate,
            "coincidencias",  result.coincidencias,
            "sinInvgate",     result.sinInvgate,
            "actualizados",   result.actualizados,
            "errores",        result.errores,
            "log",            result.log
        ));
    }
}
