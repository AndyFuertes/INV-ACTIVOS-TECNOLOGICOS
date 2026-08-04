package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "especificaciones_computo")
@Data
@NoArgsConstructor
public class EspecificacionComputo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comp")
    private Integer idComp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activo")
    private ActivoMaestro activo;

    @Column(name = "hostname", length = 100)
    private String hostname;

    @Column(name = "procesador", length = 100)
    private String procesador;

    @Column(name = "ram", length = 50)
    private String ram;

    @Column(name = "disco_ssd", length = 50)
    private String discoSsd;

    @Column(name = "sistema_operativo", length = 150)
    private String sistemaOperativo;

    @Column(name = "serie_cargador", length = 100)
    private String serieCargador;

    @Column(name = "nro_anydesk", length = 50)
    private String nroAnydesk;

    @Column(name = "gpu", length = 150)
    private String gpu;

    @Column(name = "tiene_fortiedr")
    private Boolean tieneFortiedr = false;

    @Column(name = "tiene_forticloud")
    private Boolean tieneForticloud = false;

    @Column(name = "tiene_invgate")
    private Boolean tieneInvgate = false;
}
