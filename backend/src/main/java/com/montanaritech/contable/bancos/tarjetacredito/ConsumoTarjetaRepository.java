package com.montanaritech.contable.bancos.tarjetacredito;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumoTarjetaRepository extends JpaRepository<ConsumoTarjeta, Long> {

    @Query("""
            SELECT c FROM ConsumoTarjeta c
            WHERE c.tarjetaCredito.id = :tarjetaCreditoId
              AND (:soloSinClasificar = FALSE OR c.cuentaContable IS NULL)
            ORDER BY c.fecha DESC
            """)
    Page<ConsumoTarjeta> buscar(
            @Param("tarjetaCreditoId") Long tarjetaCreditoId,
            @Param("soloSinClasificar") boolean soloSinClasificar,
            Pageable pageable);

    /** Clasificación masiva por reglas (F5.4 §2): todos los consumos de la tarjeta todavía sin clasificar. */
    List<ConsumoTarjeta> findByTarjetaCredito_IdAndCuentaContableIsNull(Long tarjetaCreditoId);

    /** Saldo de la tarjeta (F5.4, {@code RecalculoSaldoService}): todos los consumos posteriores al saldo inicial. */
    List<ConsumoTarjeta> findByTarjetaCredito_IdAndFechaAfter(Long tarjetaCreditoId, LocalDate fecha);

    long countByTarjetaCredito_IdAndCuentaContableIsNull(Long tarjetaCreditoId);

    /** Detección de duplicados al re-importar un resumen (F5.2/F5.4): mismo hash ya cargado en esta tarjeta. */
    boolean existsByTarjetaCredito_IdAndHashImportacion(Long tarjetaCreditoId, String hashImportacion);
}
