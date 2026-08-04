package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ubicaciones")
@Data
@NoArgsConstructor
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Integer idUbicacion;

    @Column(name = "campus", nullable = false, length = 100)
    private String campus;

    @Column(name = "pabellon", length = 100)
    private String pabellon;

    @Column(name = "piso", length = 50)
    private String piso;

    @Column(name = "unidad", length = 150)
    private String unidad;

    @Column(name = "area", length = 150)
    private String area;

    @Column(name = "lugar_fisico", length = 255)
    private String lugarFisico;

    @Column(name = "referencia", columnDefinition = "NVARCHAR(MAX)")
    private String referencia;
}
