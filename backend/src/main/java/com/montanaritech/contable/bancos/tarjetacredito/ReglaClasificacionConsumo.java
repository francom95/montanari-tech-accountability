package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Regla de clasificación masiva de consumos de tarjeta (F5.4 §2):
 * "si la descripción contiene {@code patron}, clasificar con estos datos".
 * Configurable por el usuario — a diferencia de {@code ClasificadorMovimientoBancario}
 * (F5.3), que es un catálogo fijo de patrones de resúmenes bancarios
 * argentinos, acá las reglas las define quien usa el sistema (proveedores,
 * suscripciones y gastos son propios de Montanari Tech, no un patrón
 * genérico reutilizable entre empresas).
 */
@Entity
@Table(name = "regla_clasificacion_consumo", uniqueConstraints = @UniqueConstraint(name = "uk_regla_clasificacion_tenant_patron", columnNames = "patron"))
@Getter
@Setter
public class ReglaClasificacionConsumo extends EntidadNegocio {

    @Column(nullable = false, length = 200)
    private String patron;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_contable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_regla_clasificacion_cuenta"))
    private CuentaContable cuentaContable;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proveedor_id", foreignKey = @ForeignKey(name = "fk_regla_clasificacion_proveedor"))
    private Proveedor proveedor;

    @ManyToOne(optional = true)
    @JoinColumn(name = "proyecto_id", foreignKey = @ForeignKey(name = "fk_regla_clasificacion_proyecto"))
    private Proyecto proyecto;

    @ManyToOne(optional = true)
    @JoinColumn(name = "concepto_id", foreignKey = @ForeignKey(name = "fk_regla_clasificacion_concepto"))
    private Concepto concepto;

    @Column(nullable = false)
    private boolean activo = true;
}
