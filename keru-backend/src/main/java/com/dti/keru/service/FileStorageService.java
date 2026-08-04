package com.dti.keru.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class FileStorageService {

    @Value("${keru.storage.actas-pdf}")
    private String rutaActasPdf;

    @Value("${keru.storage.actas-firmadas}")
    private String rutaActasFirmadas;

    public String guardarPdfFirmado(MultipartFile file, String nroActa) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        String ext = obtenerExtension(file.getOriginalFilename());
        if (!ext.equalsIgnoreCase(".pdf")) throw new IllegalArgumentException("Solo se permiten archivos PDF");
        String nombre = nroActa.replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_FIRMADO_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        Path dir = Paths.get(rutaActasFirmadas);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        log.info("PDF firmado guardado: {}", nombre);
        return nombre;
    }

    public Resource cargarPdf(String nombreArchivo, String tipo) throws MalformedURLException {
        String carpeta = "firmado".equals(tipo) ? rutaActasFirmadas : rutaActasPdf;
        return cargarDesde(carpeta, nombreArchivo);
    }

    public Resource cargarPdfEquipo(String nombreArchivo) throws MalformedURLException {
        String carpeta = rutaActasPdf + "/equipos-pdf";
        return cargarDesde(carpeta, nombreArchivo);
    }

    private Resource cargarDesde(String carpeta, String nombre) throws MalformedURLException {
        Path ruta = Paths.get(carpeta).resolve(nombre).normalize();
        Resource resource = new UrlResource(ruta.toUri());
        if (!resource.exists()) throw new RuntimeException("Archivo no encontrado: " + nombre);
        return resource;
    }

    private String obtenerExtension(String nombre) {
        if (nombre == null || !nombre.contains(".")) return ".pdf";
        return nombre.substring(nombre.lastIndexOf("."));
    }
}
