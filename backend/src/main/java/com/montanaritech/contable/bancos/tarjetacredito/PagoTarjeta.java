package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Pago del resumen de tarjeta (F5.4 §3, molde PL-4/PL-5): puede ser parcial
 * (pago mínimo) — el saldo pendiente sale de sumar consumos menos pagos
 * confirmados ({@code RecalculoSaldoService}), no de un campo propio. Al
 * confirmar genera un asiento (Haber la cuenta bancaria de débito de la
 * tarjeta, con {@code cuentaBancariaId} seteado — eso alcanza para que la
 * conciliación de F5.3 lo pueda matchear contra el movimiento bancario real
 * sin ningún cambio en F5.3).
 */
@Entity
@Table(name = "pago_tarjeta", uniqueConstraints = @UniqueConstraint(name = "uk_pago_tarjeta_asiento", columnNames = "asiento_id"))
@Getter
@Setter
public class PagoTarjeta extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "tarjeta_credito_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_tarjeta_tarjeta"))
    private TarjetaCredito tarjetaCredito;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal importe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pago_tarjeta_moneda"))
    private Moneda moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 19, scale = 6)
    private BigDecimal tipoCambio;

    @Column(name = "importe_ars", nullable = false, precision = 18, scale = 2)
    private BigDecimal importeArs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_pago_tarjeta_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;
}
