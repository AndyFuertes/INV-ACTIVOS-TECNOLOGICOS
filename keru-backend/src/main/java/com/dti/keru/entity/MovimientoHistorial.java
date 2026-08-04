package com.dti.keru.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_historial")
@Data
@NoArgsConstructor
public class MovimientoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dni_entrega", referencedColumnName = "dni")
    private Usuario usuarioEntrega;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dni_recibe", referencedColumnName = "dni")
    private Usuario usuarioRecibe;

    // ASIGNACION, PRESTAMO, DEVOLUCION, MOVILIZACION
    @Column(name = "tipo_movimiento", length = 50)
    private String tipoMovimiento;

    // DTI-LAP, DTI-DEV-LAP, DTI-PREST, ST-MOV
    @Column(name = "tipo_acta", length = 50)
    private String tipoActa;

    @Column(name = "nro_acta", length = 100)
    private String nroActa;

    @Column(name = "centro_costo_momento", length = 50)
    private String centroCostoMomento;

    @CreationTimestamp
    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;

    @Column(name = "ruta_pdf_generado", columnDefinition = "NVARCHAR(MAX)")
    private String rutaPdfGenerado;

    @Column(name = "ruta_pdf_firmado", columnDefinition = "NVARCHAR(MAX)")
    private String rutaPdfFirmado;

    @Column(name = "tecnico_responsable", length = 150)
    private String tecnicoResponsable;

    // Serie del equipo involucrado — permite buscar historial por equipo
    @Column(name = "serie_equipo", length = 100)
    private String serieEquipo;
}
