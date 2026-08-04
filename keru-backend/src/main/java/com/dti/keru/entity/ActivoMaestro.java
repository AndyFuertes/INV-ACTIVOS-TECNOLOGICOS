package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "activos_maestro")
@Data
@NoArgsConstructor
public class ActivoMaestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_activo")
    private Integer idActivo;

    @Column(name = "tipo_equipo", nullable = false, length = 50)
    private String tipoEquipo;

    @Column(name = "serie", unique = true, nullable = false, length = 100)
    private String serie;

    @Column(name = "cod_patrimonio_inventario", length = 100)
    private String codPatrimonioInventario;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "modelo_comercial", length = 150)
    private String modeloComercial;

    @Column(name = "ano_compra")
    private Integer anoCompra;

    @Column(name = "orden_compra", length = 100)
    private String ordenCompra;

    @Column(name = "financiamiento", length = 100)
    private String financiamiento;

    @Column(name = "aula_lab_aud", length = 100)
    private String aulaLabAud;

    @Column(name = "uso_activo", length = 50)
    private String usoActivo;

    @Column(name = "gestion", length = 50)
    private String gestion;

    @Column(name = "estado_fisico", length = 50)
    private String estadoFisico;

    @Column(name = "estado_disponibilidad", length = 50)
    private String estadoDisponibilidad;

    @Column(name = "en_uso")
    private Boolean enUso = false;

    @Column(name = "es_nuevo")
    private Boolean esNuevo = false;

    @Column(name = "ruta_pdf_equipo", columnDefinition = "NVARCHAR(MAX)")
    private String rutaPdfEquipo;

    // LAZY: no dispara SELECT adicional hasta que se acceda explícitamente
    // El query usa JOIN FETCH para traerlos en una sola consulta cuando se necesitan
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dni_responsable", referencedColumnName = "dni")
    private Usuario responsable;

    @Column(name = "id_activo_padre")
    private Integer idActivoPadre;

    @Column(name = "tecnico_registro", length = 150)
    private String tecnicoRegistro;

    @CreationTimestamp
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "observacion", columnDefinition = "NVARCHAR(MAX)")
    private String observacion;
}
