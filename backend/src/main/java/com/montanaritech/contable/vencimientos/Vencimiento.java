package com.montanaritech.contable.vencimientos;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.impuestos.atribucion.TipoLiquidacion;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
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
 * Obligación de pago futura (F8.1): impuestos, tarjeta, sueldos, suscripciones,
 * etc. Se genera a mano o automáticamente (ver {@code VencimientoService.
 * generarAutomaticos}, disparado on-demand desde el frontend — el proyecto no
 * tiene infraestructura de scheduling y este paso no la introduce).
 *
 * <p>{@code estado} solo guarda PENDIENTE/PAGADO/REPROGRAMADO/CANCELADO;
 * VENCIDO se deriva en lectura comparando {@code fecha} contra hoy cuando
 * {@code estado == PENDIENTE}, mismo criterio que {@code EstadoVencimiento}
 * de F4.5 (F3.6/F4.5 nunca persisten ese estado, siempre lo recalculan).
 *
 * <p>{@code liquidacionTipo}/{@code liquidacionId} es una referencia
 * polimórfica sin FK real (mismo patrón que {@code AtribucionImpuesto} de
 * F6.3): sirve para IVA y para IIBB sin tocar esas entidades.
 */
@Entity
@Table(name = "vencimiento")
@Getter
@Setter
public class Vencimiento extends EntidadNegocio {

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoVencimiento tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "importe_estimado", precision = 18, scale = 2)
    private BigDecimal importeEstimado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moneda_id", foreignKey = @ForeignKey(name = "fk_vencimiento_moneda"))
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecurrencia recurrencia = TipoRecurrencia.UNICA;

    @Column(name = "intervalo_dias_personalizado")
    private Integer intervaloDiasPersonalizado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVencimientoObligacion estado = EstadoVencimientoObligacion.PENDIENTE;

    @ManyToOne
    @JoinColumn(name = "cuenta_contable_id", foreignKey = @ForeignKey(name = "fk_vencimiento_cuenta_contable"))
    private CuentaContable cuentaContable;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_vencimiento_proveedor"))
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "liquidacion_tipo", length = 10)
    private TipoLiquidacion liquidacionTipo;

    @Column(name = "liquidacion_id")
    private Long liquidacionId;

    @ManyToOne
    @JoinColumn(name = "tarjeta_credito_id", foreignKey = @ForeignKey(name = "fk_vencimiento_tarjeta_credito"))
    private TarjetaCredito tarjetaCredito;

    @ManyToOne
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_vencimiento_proyecto"))
    private Proyecto proyecto;

    @ManyToOne
    @JoinColumn(name = "concepto_recurrente_id", foreignKey = @ForeignKey(name = "fk_vencimiento_concepto_recurrente"))
    private Concepto conceptoRecurrente;

    /** Al marcar pagado, vínculo opcional al asiento real ya generado por el Cobro/Pago/PagoTarjeta correspondiente. */
    @ManyToOne
    @JoinColumn(name = "asiento_vinculado_id", foreignKey = @ForeignKey(name = "fk_vencimiento_asiento_vinculado"))
    private Asiento asientoVinculado;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_generacion", nullable = false, length = 30)
    private OrigenGeneracionVencimiento origenGeneracion = OrigenGeneracionVencimiento.MANUAL;

    @Column(name = "origen_generacion_ref_id")
    private Long origenGeneracionRefId;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;
}
