package com.montanaritech.contable.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Se persiste el hash del refresh token, nunca el valor crudo (F1.1
 * diccionario). Sin {@code tenant_id}/{@code version} propios: es un
 * artefacto técnico de sesión, no una entidad de negocio (F1.1 §2 solo
 * excluye tenant/moneda explícitamente, pero un refresh token no tiene
 * sentido fuera del tenant de su usuario, así que alcanza con la FK).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @PrePersist
    protected void alPersistir() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
