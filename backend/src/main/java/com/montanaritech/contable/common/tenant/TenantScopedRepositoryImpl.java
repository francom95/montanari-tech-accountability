package com.montanaritech.contable.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * F11.1/F11.2 B2: Hibernate NO aplica el {@code @Filter} de {@link EntidadNegocio}
 * a {@code EntityManager.find}/{@code getReference} (solo a HQL/JPQL), así que
 * todo acceso por id vía {@code JpaRepository.findById} quedaba sin aislar por
 * tenant aunque el filtro estuviera habilitado en la sesión. En vez de tocar
 * cada uno de los ~137 call-sites de {@code findById} en los servicios, esta
 * clase se registra como {@code repositoryBaseClass} de <b>todos</b> los
 * repositorios Spring Data del proyecto (ver {@code @EnableJpaRepositories} en
 * {@code ContableApplication}) y sobreescribe los métodos que resuelven por id
 * para que, si la entidad extiende {@link EntidadNegocio}, se descarte
 * silenciosamente cuando su {@code tenantId} no coincide con el
 * {@link TenantContext} actual — el mismo comportamiento que tendría un 404 por
 * "no existe", sin filtrar si la fila existe en otro tenant.
 *
 * <p>Entidades que no extienden {@code EntidadNegocio} (p. ej. {@code Tenant}
 * mismo) no tienen {@code tenantId} y pasan sin filtrar, como antes.
 */
public class TenantScopedRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    public TenantScopedRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    @Override
    public Optional<T> findById(ID id) {
        return super.findById(id).filter(this::perteneceAlTenantActual);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        return StreamSupport.stream(super.findAllById(ids).spliterator(), false)
                .filter(this::perteneceAlTenantActual)
                .toList();
    }

    @Override
    public T getReferenceById(ID id) {
        T referencia = super.getReferenceById(id);
        if (!perteneceAlTenantActual(referencia)) {
            throw new EntityNotFoundException("No existe la entidad con id " + id);
        }
        return referencia;
    }

    private boolean perteneceAlTenantActual(T entidad) {
        if (!(entidad instanceof EntidadNegocio entidadNegocio)) {
            return true;
        }
        return Objects.equals(entidadNegocio.getTenantId(), TenantContext.getTenantId());
    }
}
