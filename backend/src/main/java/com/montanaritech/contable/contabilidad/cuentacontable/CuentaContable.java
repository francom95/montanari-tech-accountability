package com.montanaritech.contable.contabilidad.cuentacontable;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.rubro.Rubro;
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

/**
 * Plan de cuentas (F3.2, sobre el diseño validado de F3.1 §2). Jerarquía por
 * adjacency list ({@code padre}); {@code naturaleza} reutiliza el enum de
 * {@link Categoria} (mismas 5 categorías fijas del funcional §4.2, sin
 * duplicar la taxonomía). {@code imputable} no se deriva solo de "tiene
 * hijos": el usuario puede declarar una cuenta como madre pura antes de
 * cargarle hijos; el service además auto-apaga {@code imputable} al
 * agregar la primera hija (F3.1 §2.2 inv. 1).
 */
@Entity
@Table(name = "cuenta_contable", uniqueConstraints = @UniqueConstraint(name = "uk_cuenta_contable_tenant_codigo", columnNames = {"tenant_id", "codigo"}))
@Getter
@Setter
public class CuentaContable extends EntidadNegocio {

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @ManyToOne(optional = true)
    @JoinColumn(name = "padre_id", foreignKey = @ForeignKey(name = "fk_cuenta_contable_padre"))
    private CuentaContable padre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria.TipoCategoria naturaleza;

    @ManyToOne(optional = true)
    @JoinColumn(name = "rubro_id", foreignKey = @ForeignKey(name = "fk_cuenta_contable_rubro"))
    private Rubro rubro;

    @Column(nullable = false)
    private boolean imputable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "saldo_esperado", nullable = false, length = 10)
    private SaldoEsperado saldoEsperado;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany
    @JoinTable(
            name = "cuenta_contable_proyecto",
            joinColumns = @JoinColumn(name = "cuenta_contable_id"),
            inverseJoinColumns = @JoinColumn(name = "proyecto_id")
    )
    private Set<Proyecto> proyectosUsoHabitual = new HashSet<>();

    public enum SaldoEsperado {
        DEUDOR,
        ACREEDOR
    }
}
