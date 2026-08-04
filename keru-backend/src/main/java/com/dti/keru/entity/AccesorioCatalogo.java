package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accesorios_catalogo")
@Data
@NoArgsConstructor
public class AccesorioCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_accesorio")
    private Integer idAccesorio;

    @Column(name = "nombre_item", nullable = false, length = 150)
    private String nombreItem;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "stock_actual")
    private Integer stockActual = 0;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 5;
}
