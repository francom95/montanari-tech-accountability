package com.montanaritech.contable.contabilidad.mapeocuenta;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Fila de mapeo concepto→cuenta (F4.1 §1): un {@code AsientoGenerator} nunca
 * hardcodea el plan de cuentas, resuelve la cuenta a usar vía
 * {@link ResolutorCuentas}. {@code discriminadorTipo/discriminadorValor}
 * NULL = fila por defecto del concepto (ver {@link ResolutorCuentas} para el
 * algoritmo de resolución). Editable por ADMINISTRADOR.
 */
@Entity
@Table(name = "mapeo_cuenta", uniqueConstraints = @UniqueConstraint(
        name = "uk_mapeo_cuenta", columnNames = {"tenant_id", "concepto", "discriminador_tipo", "discriminador_valor"}))
@Getter
@Setter
public class MapeoCuenta extends EntidadNegocio {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConceptoContable concepto;

    @Column(name = "discriminador_tipo", length = 30)
    private String discriminadorTipo;

    @Column(name = "discriminador_valor", length = 60)
    private String discriminadorValor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_contable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapeo_cuenta_cuenta"))
    private CuentaContable cuentaContable;

    @Column(nullable = false)
    private boolean activo = true;
}
