package com.dti.keru.service;

import com.dti.keru.entity.EspecificacionComputo;
import com.dti.keru.repository.ActivoMaestroRepository;
import com.dti.keru.repository.EspecificacionComputoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvGateService {

    private final ActivoMaestroRepository        activoRepo;
    private final EspecificacionComputoRepository espRepo;

    private static final String API_URL       = "https://upch.is.cloud.invgate.net:443/public-api/";
    private static final String CLIENT_ID     = "lzhe4e0I42XUjZc1DVYT7aDK5QjhxRVbpAa6wVXW";
    private static final String CLIENT_SECRET = "bqhvBIqrsqvdryoIedprvKc8RhV0poJyfcH0eNTm05LWXzcGUN6s6WNl7zOqWb62wuxXUGiiAVrIxZvOgIpzB5JFcDs68X8RIH5ZViZ7dUaeSbxM8z6meelBWvzOpaHI";

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private String cachedToken = null;
    private long   tokenExpiry = 0L;

    // ── Resultado de sincronizacion ──────────────────────────────────────────
    public static class SyncResult {
        public int totalInvgate  = 0;
        public int coincidencias = 0;
        public int sinInvgate    = 0;
        public int actualizados  = 0;
        public int errores       = 0;
        public List<String> log  = new ArrayList<>();
    }

    // ── OAuth2 token ─────────────────────────────────────────────────────────
    private String getToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiry) {
            return cachedToken;
        }
        String body = "grant_type=client_credentials"
            + "&client_id="     + CLIENT_ID
            + "&client_secret=" + CLIENT_SECRET;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "o/token/"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("InvGate auth fallo HTTP " + resp.statusCode() + ": " + resp.body());
        }
        String token     = extractJson(resp.body(), "access_token");
        String expiresIn = extractJson(resp.body(), "expires_in");
        cachedToken = token;
        tokenExpiry = System.currentTimeMillis()
            + (Long.parseLong(expiresIn != null ? expiresIn : "3600") - 60) * 1000L;
        log.info("Token InvGate obtenido");
        return cachedToken;
    }

    // ── Obtener series de InvGate (paginado) ─────────────────────────────────
    public Set<String> obtenerSeriesInvGate() throws Exception {
        String token = getToken();
        Set<String> series = new HashSet<>();
        int page = 1;
        int pageSize = 500;
        int maxPages = 100;

        while (page <= maxPages) {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "v1/assets/?page=" + page + "&page_size=" + pageSize))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 401) {
                cachedToken = null;
                token = getToken();
                continue;
            }
            if (resp.statusCode() != 200) {
                log.warn("InvGate assets HTTP {}", resp.statusCode());
                break;
            }

            List<String> pageSeries = extractSerialNumbers(resp.body());
            if (pageSeries.isEmpty()) {
                break;
            }
            series.addAll(pageSeries);
            log.debug("InvGate pag {}: {} assets (total: {})", page, pageSeries.size(), series.size());

            if (pageSeries.size() < pageSize) {
                break;
            }
            page++;
        }

        log.info("InvGate: {} series unicas", series.size());
        return series;
    }

    // ── Sincronizar BD con InvGate (batch - no freeze) ───────────────────────
    @Transactional
    public SyncResult sincronizar() {
        SyncResult result = new SyncResult();
        try {
            result.log.add("[INFO] Conectando a InvGate UPCH...");
            Set<String> seriesInvGate = obtenerSeriesInvGate();
            result.totalInvgate = seriesInvGate.size();
            result.log.add("[OK] InvGate: " + seriesInvGate.size() + " assets encontrados");
            result.log.add("[INFO] Comparando con BD...");

            // Batch: carga todas las especificaciones en UNA query
            List<EspecificacionComputo> todasEsp = espRepo.findAll();
            List<EspecificacionComputo> toUpdate  = new ArrayList<>();

            for (EspecificacionComputo esp : todasEsp) {
                if (esp.getActivo() == null) continue;
                String serie = esp.getActivo().getSerie();
                if (serie == null || serie.isBlank()) continue;

                boolean tieneInvGate = seriesInvGate.contains(serie.trim());
                boolean anterior     = Boolean.TRUE.equals(esp.getTieneInvgate());

                if (tieneInvGate) result.coincidencias++;
                else              result.sinInvgate++;

                if (tieneInvGate != anterior) {
                    esp.setTieneInvgate(tieneInvGate);
                    toUpdate.add(esp);
                    result.actualizados++;
                }
            }

            // Guarda todos los cambios en UN batch
            if (!toUpdate.isEmpty()) {
                espRepo.saveAll(toUpdate);
            }

            result.log.add("[OK] Con InvGate: "      + result.coincidencias);
            result.log.add("[WARN] Sin InvGate: "     + result.sinInvgate);
            result.log.add("[OK] Actualizados en BD: " + result.actualizados);

        } catch (Exception e) {
            result.errores++;
            result.log.add("[ERR] " + e.getMessage());
            log.error("Error sync InvGate", e);
        }
        return result;
    }

    // ── Estado actual desde BD ────────────────────────────────────────────────
    public Map<String, Object> getEstadoActual() {
        long conInvgate = espRepo.countByTieneInvgate(true);
        long sinInvgate = espRepo.countByTieneInvgate(false);
        long total      = conInvgate + sinInvgate;
        return Map.of(
            "conInvgate", conInvgate,
            "sinInvgate", sinInvgate,
            "total",      total,
            "porcentaje", total > 0 ? Math.round((double) conInvgate / total * 100) : 0
        );
    }

    // ── Helpers de parseo JSON ────────────────────────────────────────────────
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length()
               && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < json.length()
               && json.charAt(end) != '"'
               && json.charAt(end) != ','
               && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim().replace("\"", "");
    }

    private List<String> extractSerialNumbers(String json) {
        List<String> serials = new ArrayList<>();
        String key = "\"serial_number\"";
        int pos = 0;
        int found;
        while ((found = json.indexOf(key, pos)) >= 0) {
            pos = found + key.length();
            int colon = json.indexOf(':', pos);
            if (colon < 0) break;
            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) break;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            String serial = json.substring(q1 + 1, q2).trim();
            if (!serial.isBlank() && !serial.equalsIgnoreCase("null")) {
                serials.add(serial);
            }
            pos = q2 + 1;
        }
        return serials;
    }
}
