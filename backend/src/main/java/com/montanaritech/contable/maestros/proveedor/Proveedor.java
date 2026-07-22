package com.montanaritech.contable.maestros.proveedor;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proveedor", uniqueConstraints = @UniqueConstraint(name = "uk_proveedor_tenant_cuit", columnNames = {"tenant_id", "cuit"}))
@Getter
@Setter
public class Proveedor extends EntidadNegocio {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 13)
    private String cuit;

    @ManyToOne(optional = false)
    private Jurisdiccion jurisdiccion;

    @ManyToOne(optional = true)
    private Moneda monedaHabitual;

    @ManyToMany
    @JoinTable(
            name = "proveedor_tipo_costo",
            joinColumns = @JoinColumn(name = "proveedor_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_costo_id")
    )
    private Set<TipoCosto> tiposCosto = new HashSet<>();

    @Column(length = 100)
    private String contacto;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    /**
     * Condición frente al IVA (F4.1 §5, F4.3): junto con el tipo de
     * comprobante, determina si la factura de compra computa crédito
     * fiscal o si el IVA se absorbe en el costo.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva", nullable = false, length = 30)
    private CondicionIva condicionIva = CondicionIva.RESPONSABLE_INSCRIPTO;

    /**
     * Cuenta de deudas comerciales propia del proveedor (F4.1 §2.2, mismo
     * criterio que {@code Cliente.cuentaCxc}). Opcional: sin ella,
     * {@code ResolutorCuentas} cae a la fila por defecto de
     * {@code DEUDA_COMERCIAL} en {@code mapeo_cuenta}.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "cuenta_cxp_id", foreignKey = @ForeignKey(name = "fk_proveedor_cuenta_cxp"))
    private CuentaContable cuentaCxp;

    @Column(nullable = false)
    private boolean activo = true;
}
