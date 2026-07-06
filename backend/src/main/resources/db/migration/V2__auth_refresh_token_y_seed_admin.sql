-- F1.5 — refresh tokens (F1.1 diccionario) y usuario admin inicial para poder arrancar el sistema.

CREATE TABLE refresh_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expira_en   DATETIME(6)  NOT NULL,
    revocado    BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);

-- Usuario admin inicial. Password: "changeme123" — DEBE rotarse antes de ir a
-- producción (ver README, seccion de seguridad). Hash generado con la misma
-- BCryptPasswordEncoder que usa el backend (fuerza por defecto de Spring, 10).
INSERT INTO usuario (tenant_id, email, nombre, password_hash, rol, activo, creado_en, creado_por, actualizado_en, actualizado_por, version)
VALUES (1, 'admin@montanaritech.com', 'Administrador', '$2a$10$aohOi3870arn7xArwxveY.VNp6zI5RcXuzpAe2/cccURbetuwLAn6', 'ADMINISTRADOR', TRUE, UTC_TIMESTAMP(6), 'flyway', UTC_TIMESTAMP(6), 'flyway', 0);
