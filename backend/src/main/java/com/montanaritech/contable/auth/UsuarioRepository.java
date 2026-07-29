package com.montanaritech.contable.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Filtrada por tenant (JPQL) — para chequeos de unicidad de email DENTRO de un tenant. */
    Optional<Usuario> findByEmail(String email);

    /**
     * F11.2 B2 (efecto colateral del fix de aislamiento): el login es la única búsqueda
     * legítimamente global del sistema — antes de autenticar no se sabe a qué tenant
     * pertenece el usuario, así que no se puede filtrar por tenant de antemano (el email
     * es único solo dentro de cada tenant, {@code uk_usuario_tenant_email}, no globalmente).
     * Nativa a propósito: el filtro Hibernate {@code tenantFilter} solo alcanza HQL/Criteria,
     * nunca SQL nativo, así que esta consulta queda deliberadamente fuera de su alcance.
     * Si el mismo email existiera en dos tenants (posible por esquema, no por uso real hoy —
     * el producto no expone multiempresa en la UI), devuelve el primero: caso conocido y no
     * resuelto, documentado acá en vez de silenciado.
     */
    @Query(value = "SELECT * FROM usuario WHERE email = :email", nativeQuery = true)
    List<Usuario> findByEmailGlobalParaLogin(String email);
}
