package com.montanaritech.contable.contabilidad.mapeocuenta;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MapeoCuentaRepository extends JpaRepository<MapeoCuenta, Long> {

    Optional<MapeoCuenta> findByConceptoAndDiscriminadorTipoAndDiscriminadorValorAndActivoTrue(
            ConceptoContable concepto, String discriminadorTipo, String discriminadorValor);

    Optional<MapeoCuenta> findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(ConceptoContable concepto);

    @Query("""
            SELECT m FROM MapeoCuenta m
            WHERE (:concepto IS NULL OR m.concepto = :concepto)
              AND (:activo IS NULL OR m.activo = :activo)
            """)
    Page<MapeoCuenta> buscar(@Param("concepto") ConceptoContable concepto, @Param("activo") Boolean activo, Pageable pageable);
}
