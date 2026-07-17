package com.montanaritech.contable.common.secuencia;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuenciaRepository extends JpaRepository<Secuencia, Long> {

    /**
     * {@code SELECT ... FOR UPDATE} (F3.1 §3.2): bloquea la fila hasta que la
     * transacción llamante haga commit/rollback, para que dos confirmaciones
     * concurrentes del mismo tenant nunca lean el mismo {@code valorActual}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Secuencia s WHERE s.tenantId = :tenantId AND s.nombre = :nombre")
    Optional<Secuencia> buscarParaActualizar(@Param("tenantId") Long tenantId, @Param("nombre") String nombre);
}
