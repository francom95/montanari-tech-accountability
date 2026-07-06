package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private boolean activo = true;
}
