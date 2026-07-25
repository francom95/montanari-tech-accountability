package com.montanaritech.contable.alerta;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Lectura de una {@link Alerta} por un usuario (F9.1, decisión: leída es por
 * usuario, no global). Primera tabla de unión "muchos usuarios marcan
 * independientemente algo como hecho" del proyecto — sin precedente previo;
 * el idiom de unicidad (FK + FK) es el mismo que
 * {@code CuentaBancaria.uk_cuenta_bancaria_tenant_alias} aplicado a un par
 * de referencias en vez de tenant+campo.
 */
@Entity
@Table(name = "alerta_lectura", uniqueConstraints = @UniqueConstraint(name = "uk_alerta_lectura_alerta_usuario",
        columnNames = {"alerta_id", "usuario_id"}))
@Getter
@Setter
public class AlertaLectura extends EntidadNegocio {

    @ManyToOne(optional = false)
    @JoinColumn(name = "alerta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alerta_lectura_alerta"))
    private Alerta alerta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alerta_lectura_usuario"))
    private Usuario usuario;

    @Column(name = "leida_en", nullable = false)
    private Instant leidaEn;
}
