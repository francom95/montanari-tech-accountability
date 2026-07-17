package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cliente", uniqueConstraints = @UniqueConstraint(name = "uk_cliente_tenant_cuit", columnNames = {"tenant_id", "cuit"}))
@Getter
@Setter
public class Cliente extends EntidadNegocio {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 13)
    private String cuit;

    @ManyToOne(optional = false)
    private Jurisdiccion jurisdiccion;

    @Column(length = 100)
    private String contacto;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    /**
     * Cuenta de créditos por ventas propia del cliente (F4.1 §2.1, checkpoint
     * confirmado: opción A — mantener cuentas por cliente en vez de una
     * genérica). Opcional: sin ella, {@code ResolutorCuentas} cae a la fila
     * por defecto de {@code CREDITO_POR_VENTA} en {@code mapeo_cuenta} (hoy
     * ausente a propósito — ver F4.1).
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_cxc_id", foreignKey = @ForeignKey(name = "fk_cliente_cuenta_cxc"))
    private CuentaContable cuentaCxc;

    @Column(nullable = false)
    private boolean activo = true;
}
