package com.montanaritech.contable.maestros.comisionista;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1 (F2.7). Comisionista: puede o no tener CUIT formal (a
 * diferencia de Cliente/Proveedor, que sí lo exigen) porque en la práctica
 * también cobra comisión gente del equipo interno (ver F2.7: Javier
 * Montanari, comisión especial del 20%).
 */
@Entity
@Table(name = "comisionista", uniqueConstraints = @UniqueConstraint(name = "uk_comisionista_tenant_cuit", columnNames = {"tenant_id", "cuit"}))
@Getter
@Setter
public class Comisionista extends EntidadNegocio {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 13)
    private String cuit;

    @Column(length = 100)
    private String contacto;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;
}
