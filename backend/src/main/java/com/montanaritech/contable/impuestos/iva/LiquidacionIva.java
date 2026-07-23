package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Liquidación mensual de IVA de un período (F6.1, moldes PL-4/PL-5). Los
 * importes de cada componente viven en {@link LiquidacionIvaComponente}; acá
 * solo queda el resultado ya resuelto en sus dos caras excluyentes: uno de los
 * dos siempre es cero.
 *
 * <p>{@code saldoAFavor} es el que arrastra el período siguiente como
 * {@code SALDO_TECNICO_ANTERIOR} — de ahí que el arrastre se lea de la
 * liquidación anterior confirmada y no de los asientos: es un dato explícito y
 * auditable, no algo a re-derivar.
 */
@Entity
@Table(name = "liquidacion_iva",
        uniqueConstraints = @UniqueConstraint(name = "uk_liquidacion_iva_asiento", columnNames = "asiento_id"))
@Getter
@Setter
public class LiquidacionIva extends EntidadNegocio {

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @Column(name = "saldo_a_pagar", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAPagar = BigDecimal.ZERO;

    /**
     * Saldo <b>técnico</b> a favor (art. 24, 1er párrafo): sobrante de la etapa
     * técnica, solo computable contra débitos fiscales futuros. La columna
     * conserva el nombre {@code saldo_a_favor} de la primera versión, cuando era
     * el único acumulador.
     */
    @Column(name = "saldo_a_favor", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoAFavor = BigDecimal.ZERO;

    /**
     * Saldo de <b>libre disponibilidad</b> (art. 24, 2do párrafo): sobrante de
     * los ingresos directos por sobre el impuesto determinado. Además de
     * arrastrarse, se compensa con otros impuestos, se transfiere y se puede
     * pedir devuelto — por eso no se puede sumar con el técnico.
     */
    @Column(name = "saldo_libre_disponibilidad", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoLibreDisponibilidad = BigDecimal.ZERO;

    @ManyToOne(optional = true)
    @JoinColumn(name = "asiento_id", foreignKey = @ForeignKey(name = "fk_liquidacion_iva_asiento"))
    private Asiento asiento;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "liquidacionIva", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<LiquidacionIvaComponente> componentes = new ArrayList<>();
}
