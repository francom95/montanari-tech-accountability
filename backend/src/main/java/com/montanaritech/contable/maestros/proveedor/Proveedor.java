package com.montanaritech.contable.maestros.proveedor;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private boolean activo = true;
}
