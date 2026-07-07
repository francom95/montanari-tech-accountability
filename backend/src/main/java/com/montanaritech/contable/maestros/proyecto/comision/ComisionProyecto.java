package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.comisionista.Comisionista;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Vínculo proyecto-comisionista (F2.7): no es un simple N:M, lleva atributos
 * propios (porcentaje, base de cálculo, moneda, importes, estado de pago),
 * así que es su propia entidad con dos FK en vez de una tabla de join pura
 * (patrón distinto al de {@code etapa_proveedor}/{@code proveedor_tipo_costo}).
 */
@Entity
@Table(name = "comision_proyecto")
@Getter
@Setter
public class ComisionProyecto extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comision_proyecto_proyecto"))
    private Proyecto proyecto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comisionista_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comision_proyecto_comisionista"))
    private Comisionista comisionista;

    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComision;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_calculo", nullable = false, length = 30)
    private BaseCalculo baseCalculo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comision_proyecto_moneda"))
    private Moneda moneda;

    @Column(name = "importe_estimado", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeEstimado;

    @Column(name = "importe_final", precision = 18, scale = 2)
    private BigDecimal importeFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(name = "fecha_estimada_pago")
    private LocalDate fechaEstimadaPago;

    @Column(length = 2000)
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;

    public enum BaseCalculo {
        MONTO_TOTAL,
        MONTO_SIN_IMPUESTOS,
        MONTO_COBRADO
    }

    public enum EstadoPago {
        PENDIENTE,
        PAGADO
    }
}
