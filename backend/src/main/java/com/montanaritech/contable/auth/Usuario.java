package com.montanaritech.contable.auth;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de datos del usuario (F1.1 §7). La lógica de autenticación (JWT,
 * BCrypt, refresh, guards por rol) se implementa en F1.5; acá solo se
 * scaffoldea la entidad y su repositorio para que la migración V1 tenga
 * contrapartida JPA.
 */
@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_tenant_email", columnNames = {"tenant_id", "email"}))
@Getter
@Setter
public class Usuario extends EntidadNegocio {

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_login_en")
    private Instant ultimoLoginEn;
}
