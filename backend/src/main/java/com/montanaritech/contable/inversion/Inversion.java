package com.montanaritech.contable.inversion;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Inversión (Fondo Fima u otro instrumento similar, F8.4): aparta dinero de
 * una {@code CuentaBancaria} de origen, con valuación calculada a partir de
 * sus {@link MovimientoInversion} (no hay campo de saldo persistido, evita
 * staleness — ver {@code InversionService}). Vínculo opcional a una
 * obligación futura ({@code vinculoTipo}/{@code vinculoRefId}, referencia
 * polimórfica sin FK, mismo patrón que {@code AtribucionImpuesto} y
 * {@code Vencimiento.origenGeneracion}) — si esa obligación sigue pendiente,
 * F8.3 proyecta la valuación actual como ingreso en su fecha (decisión F8.4:
 * "rescate planificado como ingreso proyectado").
 */
@Entity
@Table(name = "inversion")
@Getter
@Setter
public class Inversion extends EntidadNegocio {

    @Column(nullable = false, length = 120)
    private String instrumento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_origen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inversion_cuenta_origen"))
    private CuentaBancaria cuentaOrigen;

    @Column(name = "objetivo_del_dinero", length = 200)
    private String objetivoDelDinero;

    @Enumerated(EnumType.STRING)
    @Column(name = "vinculo_tipo", length = 20)
    private TipoVinculoInversion vinculoTipo;

    @Column(name = "vinculo_ref_id")
    private Long vinculoRefId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInversion estado = EstadoInversion.ACTIVA;

    @Column(nullable = false)
    private boolean activo = true;
}
