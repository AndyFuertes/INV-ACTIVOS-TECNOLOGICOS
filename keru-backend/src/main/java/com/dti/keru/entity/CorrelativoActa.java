package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "correlativos_actas")
@Data
@NoArgsConstructor
public class CorrelativoActa {

    @Id
    @Column(name = "tipo_acta", length = 50)
    private String tipoActa;

    @Column(name = "ultimo_numero")
    private Integer ultimoNumero = 0;

    @Column(name = "ano")
    private Integer ano;
}
