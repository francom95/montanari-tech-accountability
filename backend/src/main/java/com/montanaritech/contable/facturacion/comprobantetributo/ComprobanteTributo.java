package com.montanaritech.contable.facturacion.comprobantetributo;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Percepción/retención/tributo de un comprobante (F1.1 §6.5): tabla única
 * para venta, compra, cobro y pago (vínculo polimórfico simple por
 * {@code comprobanteTipo}/{@code comprobanteId}, sin FK real). Captura
 * informativa que alimenta F6.1/F6.2; no todo tributo genera por sí solo una
 * línea de asiento (F4.1 confirmó que la venta no lleva percepciones
 * practicadas — ver {@code ConceptoContable}).
 */
@Entity
@Table(name = "comprobante_tributo")
@Getter
@Setter
public class ComprobanteTributo extends EntidadNegocio {

    @Enumerated(EnumType.STRING)
    @Column(name = "comprobante_tipo", nullable = false, length = 20)
    private ComprobanteTipo comprobanteTipo;

    @Column(name = "comprobante_id", nullable = false)
    private Long comprobanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoTributo tipo;

    @ManyToOne(optional = true)
    @JoinColumn(name = "jurisdiccion_id", foreignKey = @ForeignKey(name = "fk_comprobante_tributo_jurisdiccion"))
    private Jurisdiccion jurisdiccion;

    @Column(precision = 18, scale = 2)
    private BigDecimal base;

    @Column(precision = 5, scale = 2)
    private BigDecimal alicuota;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;
}
