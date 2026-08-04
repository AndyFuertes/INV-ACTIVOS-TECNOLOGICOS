package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @Column(name = "dni", length = 20)
    private String dni;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "puesto_general", length = 150)
    private String puestoGeneral;

    @Column(name = "departamento", length = 150)
    private String departamento;

    @Column(name = "area", length = 150)
    private String area;

    @Column(name = "centro_costo", length = 50)
    private String centroCosto;

    @Column(name = "nombre_centro_costo", length = 255)
    private String nombreCentroCosto;

    @Column(name = "jefe_inmediato", length = 255)
    private String jefeInmediato;

    @Column(name = "estado_planilla", length = 10)
    private String estadoPlanilla;
}
